package com.happypuppy.daycounter.ui.screens

import com.happypuppy.daycounter.data.database.Item
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Date

/**
 * Unit tests for [filterItems], the pure filter helper used by MainScreen.
 *
 * Each filter option is exercised against a small fixture of past, present, and
 * future events spanning two categories.
 */
class FilterItemsTest {

    private fun dayOffset(days: Int): Date = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, days)
    }.time

    private val past = Item(id = 1, name = "Past", itemCategory = "Work", targetDate = dayOffset(-5))
    private val today = Item(id = 2, name = "Today", itemCategory = "Personal", targetDate = dayOffset(0))
    private val future = Item(id = 3, name = "Future", itemCategory = "Work", targetDate = dayOffset(10))
    private val items = listOf(past, today, future)

    @Test
    fun `ALL returns every item`() {
        val result = filterItems(items, FilterState(option = FilterOption.ALL))
        assertEquals(items, result)
    }

    @Test
    fun `FUTURE keeps only items with target after today`() {
        val result = filterItems(items, FilterState(option = FilterOption.FUTURE))
        assertEquals(listOf(future), result)
    }

    @Test
    fun `TODAY keeps only items whose target is today`() {
        val result = filterItems(items, FilterState(option = FilterOption.TODAY))
        assertEquals(listOf(today), result)
    }

    @Test
    fun `PAST keeps only items whose target is before today`() {
        val result = filterItems(items, FilterState(option = FilterOption.PAST))
        assertEquals(listOf(past), result)
    }

    @Test
    fun `CATEGORY keeps only items in the chosen category`() {
        val result = filterItems(
            items,
            FilterState(option = FilterOption.CATEGORY, category = "Work")
        )
        assertEquals(listOf(past, future), result)
    }

    @Test
    fun `CATEGORY with null category falls back to all items`() {
        // The implementation deliberately returns the unfiltered list when a
        // CATEGORY filter is selected without a chosen category - guard against
        // a regression that would silently return an empty list.
        val result = filterItems(
            items,
            FilterState(option = FilterOption.CATEGORY, category = null)
        )
        assertEquals(items, result)
    }

    @Test
    fun `CATEGORY with unknown category returns empty list`() {
        val result = filterItems(
            items,
            FilterState(option = FilterOption.CATEGORY, category = "Nope")
        )
        assertEquals(emptyList<Item>(), result)
    }
}
