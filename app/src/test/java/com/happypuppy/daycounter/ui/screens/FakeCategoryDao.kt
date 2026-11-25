package com.happypuppy.daycounter.ui.screens

import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.data.database.CategoryDao
import com.happypuppy.daycounter.data.database.CategoryWithCount
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.FINANCE_CATEGORY_ID
import com.happypuppy.daycounter.data.database.HEALTH_CATEGORY_ID
import com.happypuppy.daycounter.data.database.ItemCategoryRef
import com.happypuppy.daycounter.data.database.WORK_CATEGORY_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * In-memory test double for [CategoryDao] wired to a [FakeItemDao] so usage counts and
 * category-item queries mirror Room's SQL behavior.
 */
class FakeCategoryDao(
    private val itemDao: FakeItemDao = FakeItemDao(),
    private val flowError: Throwable? = null
) : CategoryDao {
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())

    var insertError: Throwable? = null
    var deleteError: Throwable? = null

    val currentCategories: List<Category> get() = categoriesFlow.value

    private fun sorted(list: List<Category>): List<Category> =
        list.sortedWith(
            compareBy<Category> { !it.isDefault }
                .thenBy {
                    when (it.id) {
                        DEFAULT_CATEGORY_ID -> 0
                        WORK_CATEGORY_ID -> 1
                        HEALTH_CATEGORY_ID -> 2
                        FINANCE_CATEGORY_ID -> 3
                        else -> 4
                    }
                }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        )

    override fun getAllCategories(): Flow<List<Category>> =
        if (flowError != null) {
            flow { throw flowError }
        } else {
            categoriesFlow.map { sorted(it) }
        }

    override fun getCategoriesWithUsageCount(): Flow<List<CategoryWithCount>> =
        if (flowError != null) {
            flow { throw flowError }
        } else {
            combine(categoriesFlow, itemDao.getAllItems()) { categories, items ->
                val counts = items.groupingBy { it.categoryId }.eachCount()
                sorted(categories).map { category ->
                    CategoryWithCount(category = category, eventCount = counts[category.id] ?: 0)
                }
            }
        }

    override suspend fun getCategoryById(id: String): Category? =
        categoriesFlow.value.find { it.id == id }

    override suspend fun getAllCategoriesList(): List<Category> =
        sorted(categoriesFlow.value)

    override suspend fun findByNameIgnoreCase(name: String): Category? =
        categoriesFlow.value.find { it.name.equals(name, ignoreCase = true) }

    override suspend fun getCustomCategories(): List<Category> =
        categoriesFlow.value
            .filterNot { it.isDefault }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    override suspend fun insertCategory(category: Category) {
        insertError?.let { throw it }
        val updated = categoriesFlow.value.filterNot { it.id == category.id } + category
        categoriesFlow.value = updated
    }

    override suspend fun insertCategories(categories: List<Category>) {
        insertError?.let { throw it }
        val current = categoriesFlow.value.toMutableList()
        for (cat in categories) {
            if (current.none { it.id == cat.id }) {
                current.add(cat)
            }
        }
        categoriesFlow.value = current
    }

    override suspend fun deleteCustomCategoryById(id: String): Int {
        deleteError?.let { throw it }
        val target = categoriesFlow.value.find { it.id == id && !it.isDefault } ?: return 0
        categoriesFlow.value = categoriesFlow.value - target
        return 1
    }

    override suspend fun deleteAllCustomCategories(): Int {
        deleteError?.let { throw it }
        val before = categoriesFlow.value.size
        categoriesFlow.value = categoriesFlow.value.filter { it.isDefault }
        return before - categoriesFlow.value.size
    }

    override suspend fun getItemIdsForCategory(categoryId: String): List<Int> =
        itemDao.currentItems.filter { it.categoryId == categoryId }.map { it.id }

    override suspend fun getItemIdsForCustomCategories(): List<ItemCategoryRef> {
        val customIds = categoriesFlow.value.filterNot { it.isDefault }.map { it.id }.toSet()
        return itemDao.currentItems
            .filter { it.categoryId in customIds }
            .map { ItemCategoryRef(itemId = it.id, categoryId = it.categoryId) }
    }
}
