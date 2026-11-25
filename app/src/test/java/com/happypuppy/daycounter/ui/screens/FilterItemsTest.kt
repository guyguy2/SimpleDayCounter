package com.happypuppy.daycounter.ui.screens

import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.WORK_CATEGORY_ID
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [filterItems], the pure filter helper.
 */
class FilterItemsTest {

    private val todayDate = LocalDate.of(2026, 6, 15)

    private val past = Item(
        id = 1,
        name = "Past",
        categoryId = WORK_CATEGORY_ID,
        targetDate = todayDate.minusDays(5)
    )
    private val today = Item(
        id = 2,
        name = "Today",
        categoryId = DEFAULT_CATEGORY_ID,
        targetDate = todayDate
    )
    private val future = Item(
        id = 3,
        name = "Future",
        categoryId = WORK_CATEGORY_ID,
        targetDate = todayDate.plusDays(10)
    )
    private val items = listOf(past, today, future)

    @Test
    fun `ALL returns every item`() {
        val result = filterItems(items, FilterState(option = FilterOption.ALL), today = todayDate)
        assertEquals(items, result)
    }

    @Test
    fun `FUTURE keeps only items with target after today`() {
        val result = filterItems(items, FilterState(option = FilterOption.FUTURE), today = todayDate)
        assertEquals(listOf(future), result)
    }

    @Test
    fun `TODAY keeps only items whose target is today`() {
        val result = filterItems(items, FilterState(option = FilterOption.TODAY), today = todayDate)
        assertEquals(listOf(today), result)
    }

    @Test
    fun `PAST keeps only items whose target is before today`() {
        val result = filterItems(items, FilterState(option = FilterOption.PAST), today = todayDate)
        assertEquals(listOf(past), result)
    }

    @Test
    fun `CATEGORY keeps only items in the chosen category`() {
        val result = filterItems(
            items,
            FilterState(option = FilterOption.CATEGORY, category = WORK_CATEGORY_ID),
            today = todayDate
        )
        assertEquals(listOf(past, future), result)
    }

    @Test
    fun `CATEGORY with null category falls back to all items`() {
        val result = filterItems(
            items,
            FilterState(option = FilterOption.CATEGORY, category = null),
            today = todayDate
        )
        assertEquals(items, result)
    }

    @Test
    fun `CATEGORY with unknown category returns empty list`() {
        val result = filterItems(
            items,
            FilterState(option = FilterOption.CATEGORY, category = "Nope"),
            today = todayDate
        )
        assertEquals(emptyList<Item>(), result)
    }
}
