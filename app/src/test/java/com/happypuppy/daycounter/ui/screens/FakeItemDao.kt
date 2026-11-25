package com.happypuppy.daycounter.ui.screens

import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORIES
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * In-memory test double for [ItemDao] with optional error injection for testing error propagation.
 */
class FakeItemDao(
    private val flowError: Throwable? = null
) : ItemDao {
    private val itemsFlow = MutableStateFlow<List<Item>>(emptyList())
    private var nextAutoId = 1

    var insertError: Throwable? = null
    var deleteError: Throwable? = null
    var reassignError: Throwable? = null

    val currentItems: List<Item> get() = itemsFlow.value

    override fun getAllItems(): Flow<List<Item>> =
        if (flowError != null) {
            flow { throw flowError }
        } else {
            itemsFlow.asStateFlow()
        }

    override suspend fun insertItem(item: Item) {
        insertError?.let { throw it }
        val current = itemsFlow.value.toMutableList()
        val storedItem = if (item.id == 0) {
            while (current.any { it.id == nextAutoId }) {
                nextAutoId++
            }
            item.copy(id = nextAutoId++)
        } else {
            item
        }
        val existingIndex = current.indexOfFirst { it.id == storedItem.id }
        if (existingIndex >= 0) {
            current[existingIndex] = storedItem
        } else {
            current.add(storedItem)
        }
        itemsFlow.value = current
    }

    override suspend fun deleteItem(item: Item) {
        deleteError?.let { throw it }
        itemsFlow.value = itemsFlow.value.filterNot { it.id == item.id }
    }

    override suspend fun reassignCategory(fromCategoryId: String, toCategoryId: String) {
        reassignError?.let { throw it }
        itemsFlow.value = itemsFlow.value.map { item ->
            if (item.categoryId == fromCategoryId) item.copy(categoryId = toCategoryId) else item
        }
    }

    override suspend fun reassignItemsToCategory(itemIds: List<Int>, toCategoryId: String) {
        reassignError?.let { throw it }
        val idSet = itemIds.toSet()
        itemsFlow.value = itemsFlow.value.map { item ->
            if (item.id in idSet) item.copy(categoryId = toCategoryId) else item
        }
    }

    override suspend fun reassignAllCustomCategoriesToDefault(defaultCategoryId: String) {
        reassignError?.let { throw it }
        val defaultIds = DEFAULT_CATEGORIES.map { it.id }.toSet()
        itemsFlow.value = itemsFlow.value.map { item ->
            if (item.categoryId !in defaultIds) item.copy(categoryId = defaultCategoryId) else item
        }
    }
}
