package com.happypuppy.daycounter.ui.screens

import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemDao

/**
 * In-memory test double for [ItemDao].
 *
 * Stores items in insertion order so that `getAllItems()` returns them deterministically
 * (production DAO orders by `created_at DESC`, but the ViewModel tests do not depend on
 * a particular ordering — only on the membership of the list after each mutation).
 *
 * `insertItem` mirrors Room's `OnConflictStrategy.REPLACE` behavior: an existing entry
 * with the same `id` is replaced in place; otherwise the item is appended. `deleteItem`
 * matches by `id`. `getAllItems` returns a defensive copy so callers cannot mutate the
 * backing store.
 */
class FakeItemDao : ItemDao {
    private val items = mutableListOf<Item>()

    override suspend fun getAllItems(): List<Item> = items.toList()

    override suspend fun insertItem(item: Item) {
        val existingIndex = items.indexOfFirst { it.id == item.id && item.id != 0 }
        if (existingIndex >= 0) {
            items[existingIndex] = item
        } else {
            items.add(item)
        }
    }

    override suspend fun deleteItem(item: Item) {
        items.removeAll { it.id == item.id }
    }

    override suspend fun deleteAllItems() {
        items.clear()
    }
}
