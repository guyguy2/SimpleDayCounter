package com.happypuppy.daycounter.data.database

import com.happypuppy.daycounter.data.preferences.CategoryMigrationPreferences
import com.happypuppy.daycounter.ui.screens.FilterOption
import com.happypuppy.daycounter.ui.screens.FilterState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.util.UUID

interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

class InMemoryCategoryMigrationPreferences(
    initialCustomCategories: Set<String> = emptySet(),
    initialMigrated: Boolean = true,
    initialFilterState: FilterState = FilterState()
) : CategoryMigrationPreferences {
    private val _customCategories = MutableStateFlow(initialCustomCategories)
    private val _migrated = MutableStateFlow(initialMigrated)
    private val _filterState = MutableStateFlow(initialFilterState)

    override val customCategoriesFlow: Flow<Set<String>> = _customCategories.asStateFlow()
    override val categoriesMigratedToRoomFlow: Flow<Boolean> = _migrated.asStateFlow()
    override val filterStateFlow: Flow<FilterState> = _filterState.asStateFlow()

    override suspend fun markCategoriesMigratedToRoom() {
        _migrated.value = true
    }

    override suspend fun setFilterState(state: FilterState) {
        _filterState.value = if (state.option == FilterOption.CATEGORY) {
            state
        } else {
            FilterState(option = state.option, category = null)
        }
    }
}

data class DeletedCategoryEntry(
    val category: Category,
    val affectedItemIds: List<Int>
)

data class DeletedCategorySnapshot(
    val entries: List<DeletedCategoryEntry>,
    val isDeleteAll: Boolean = false,
    val previousFilterState: FilterState? = null
) {
    val totalAffectedItems: Int
        get() = entries.sumOf { it.affectedItemIds.size }
}

sealed interface AddCategoryResult {
    data class Added(val category: Category) : AddCategoryResult
    data class Duplicate(val existing: Category) : AddCategoryResult
    data object Invalid : AddCategoryResult
}

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao,
    private val migrationPreferences: CategoryMigrationPreferences = InMemoryCategoryMigrationPreferences(),
    private val transactionRunner: TransactionRunner = object : TransactionRunner {
        override suspend fun <T> run(block: suspend () -> T): T = block()
    },
    private val clock: Clock = Clock.systemDefaultZone(),
    private val idGenerator: () -> String = { UUID.randomUUID().toString() }
) {
    private val initMutex = Mutex()

    fun observeCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    fun observeCategoriesWithUsageCount(): Flow<List<CategoryWithCount>> =
        categoryDao.getCategoriesWithUsageCount()

    /**
     * Ensures default categories exist in Room and performs the one-time idempotent import of
     * legacy DataStore `custom_categories` (including 0-event categories) and legacy
     * `filter_category` names into Room category IDs.
     */
    suspend fun ensureInitialized() {
        initMutex.withLock {
            categoryDao.insertCategories(DEFAULT_CATEGORIES)
            if (!migrationPreferences.categoriesMigratedToRoomFlow.first()) {
                val legacyCustomCategories = migrationPreferences.customCategoriesFlow.first()
                val filterState = migrationPreferences.filterStateFlow.first()
                transactionRunner.run {
                    for (rawName in legacyCustomCategories) {
                        val trimmed = rawName.trim()
                        if (trimmed.isNotEmpty() && categoryDao.findByNameIgnoreCase(trimmed) == null) {
                            categoryDao.insertCategory(
                                Category(
                                    id = idGenerator(),
                                    name = trimmed,
                                    isDefault = false,
                                    createdAt = clock.millis()
                                )
                            )
                        }
                    }
                    if (filterState.option == FilterOption.CATEGORY && !filterState.category.isNullOrBlank()) {
                        if (categoryDao.getCategoryById(filterState.category) == null) {
                            val matched = categoryDao.findByNameIgnoreCase(filterState.category)
                            if (matched != null) {
                                migrationPreferences.setFilterState(
                                    FilterState(option = FilterOption.CATEGORY, category = matched.id)
                                )
                            } else {
                                migrationPreferences.setFilterState(FilterState())
                            }
                        }
                    }
                }
                migrationPreferences.markCategoriesMigratedToRoom()
            }
        }
    }

    suspend fun getCategoryById(id: String): Category? = categoryDao.getCategoryById(id)

    suspend fun addCustomCategory(rawName: String): AddCategoryResult {
        ensureInitialized()
        val normalized = normalizeCategoryNameValue(rawName)
        if (normalized.isEmpty()) return AddCategoryResult.Invalid
        return transactionRunner.run {
            val existing = categoryDao.findByNameIgnoreCase(normalized)
            if (existing != null) {
                AddCategoryResult.Duplicate(existing)
            } else {
                val category = Category(
                    id = idGenerator(),
                    name = normalized,
                    isDefault = false,
                    createdAt = clock.millis()
                )
                categoryDao.insertCategory(category)
                AddCategoryResult.Added(category)
            }
        }
    }

    suspend fun deleteCustomCategory(categoryId: String): DeletedCategorySnapshot? {
        ensureInitialized()
        return transactionRunner.run {
            val category = categoryDao.getCategoryById(categoryId) ?: return@run null
            if (category.isDefault) return@run null
            val affectedIds = categoryDao.getItemIdsForCategory(categoryId)
            if (affectedIds.isNotEmpty()) {
                itemDao.reassignCategory(fromCategoryId = categoryId, toCategoryId = DEFAULT_CATEGORY_ID)
            }
            val deleted = categoryDao.deleteCustomCategoryById(categoryId)
            if (deleted == 0) return@run null
            DeletedCategorySnapshot(
                entries = listOf(DeletedCategoryEntry(category = category, affectedItemIds = affectedIds)),
                isDeleteAll = false
            )
        }
    }

    suspend fun deleteAllCustomCategories(): DeletedCategorySnapshot? {
        ensureInitialized()
        return transactionRunner.run {
            val customCategories = categoryDao.getCustomCategories()
            if (customCategories.isEmpty()) return@run null
            val refs = categoryDao.getItemIdsForCustomCategories()
            val idsByCategory = refs.groupBy({ it.categoryId }, { it.itemId })
            val entries = customCategories.map { category ->
                DeletedCategoryEntry(
                    category = category,
                    affectedItemIds = idsByCategory[category.id].orEmpty()
                )
            }
            itemDao.reassignAllCustomCategoriesToDefault(DEFAULT_CATEGORY_ID)
            categoryDao.deleteAllCustomCategories()
            DeletedCategorySnapshot(
                entries = entries,
                isDeleteAll = true
            )
        }
    }

    suspend fun restoreDeletedCategories(snapshot: DeletedCategorySnapshot) {
        ensureInitialized()
        transactionRunner.run {
            categoryDao.insertCategories(snapshot.entries.map { it.category })
            for (entry in snapshot.entries) {
                if (entry.affectedItemIds.isNotEmpty()) {
                    entry.affectedItemIds.chunked(500).forEach { chunk ->
                        itemDao.reassignItemsToCategory(itemIds = chunk, toCategoryId = entry.category.id)
                    }
                }
            }
        }
    }
}
