package com.happypuppy.daycounter.ui.screens

import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory test double for [ItemDao].
 *
 * Stores items in insertion order so that `getAllItems()` emits them deterministically
 * (production DAO orders by `created_at DESC`, but the ViewModel tests do not depend on
 * a particular ordering — only on the membership of the list after each mutation).
 *
 * `insertItem` mirrors Room's `OnConflictStrategy.REPLACE` behavior: an existing entry
 * with the same `id` is replaced in place; otherwise the item is appended. `deleteItem`
 * matches by `id`. `getAllItems` returns a hot [Flow] that re-emits on every mutation,
 * mirroring Room's reactive query behavior so the ViewModel under test sees fresh lists
 * without explicit re-queries.
 */
class FakeItemDao : ItemDao {
    private val itemsFlow = MutableStateFlow<List<Item>>(emptyList())

    override fun getAllItems(): Flow<List<Item>> = itemsFlow.asStateFlow()

    override suspend fun insertItem(item: Item) {
        val current = itemsFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == item.id && item.id != 0 }
        if (existingIndex >= 0) {
            current[existingIndex] = item
        } else {
            current.add(item)
        }
        itemsFlow.value = current
    }

    override suspend fun deleteItem(item: Item) {
        itemsFlow.value = itemsFlow.value.filterNot { it.id == item.id }
    }
}
