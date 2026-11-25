package com.happypuppy.daycounter.ui.screens

import com.happypuppy.daycounter.data.database.Item
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class SortItemsTest {

    private val itemAlpha = Item(
        id = 1,
        name = "alpha",
        itemCategory = "work",
        createdAt = Date(2_000L),
        targetDate = Date(20_000L)
    )
    private val itemBravo = Item(
        id = 2,
        name = "Bravo",
        itemCategory = "Finance",
        createdAt = Date(1_000L),
        targetDate = Date(30_000L)
    )
    private val itemCharlie = Item(
        id = 3,
        name = "charlie",
        itemCategory = "Personal",
        createdAt = Date(3_000L),
        targetDate = Date(10_000L)
    )

    private val sample = listOf(itemAlpha, itemBravo, itemCharlie)

    @Test
    fun `NAME_ASC sorts case-insensitively ascending`() {
        assertEquals(listOf(itemAlpha, itemBravo, itemCharlie), sortItems(sample, SortOption.NAME_ASC))
    }

    @Test
    fun `NAME_DESC sorts case-insensitively descending`() {
        assertEquals(listOf(itemCharlie, itemBravo, itemAlpha), sortItems(sample, SortOption.NAME_DESC))
    }

    @Test
    fun `CATEGORY_ASC sorts case-insensitively ascending`() {
        assertEquals(listOf(itemBravo, itemCharlie, itemAlpha), sortItems(sample, SortOption.CATEGORY_ASC))
    }

    @Test
    fun `CATEGORY_DESC sorts case-insensitively descending`() {
        assertEquals(listOf(itemAlpha, itemCharlie, itemBravo), sortItems(sample, SortOption.CATEGORY_DESC))
    }

    @Test
    fun `TARGET_DATE_ASC sorts earliest target date first`() {
        assertEquals(listOf(itemCharlie, itemAlpha, itemBravo), sortItems(sample, SortOption.TARGET_DATE_ASC))
    }

    @Test
    fun `TARGET_DATE_DESC sorts latest target date first`() {
        assertEquals(listOf(itemBravo, itemAlpha, itemCharlie), sortItems(sample, SortOption.TARGET_DATE_DESC))
    }

    @Test
    fun `DATE_ADDED_ASC sorts oldest createdAt first`() {
        assertEquals(listOf(itemBravo, itemAlpha, itemCharlie), sortItems(sample, SortOption.DATE_ADDED_ASC))
    }

    @Test
    fun `DATE_ADDED_DESC sorts newest createdAt first`() {
        assertEquals(listOf(itemCharlie, itemAlpha, itemBravo), sortItems(sample, SortOption.DATE_ADDED_DESC))
    }

    @Test
    fun `empty list returns empty list for all sort options`() {
        for (option in SortOption.entries) {
            assertEquals(emptyList<Item>(), sortItems(emptyList(), option))
        }
    }

    @Test
    fun `ties preserve original relative order`() {
        val first = Item(id = 10, name = "Same", itemCategory = "Work", createdAt = Date(1_000L), targetDate = Date(5_000L))
        val second = Item(id = 11, name = "same", itemCategory = "work", createdAt = Date(1_000L), targetDate = Date(5_000L))
        val input = listOf(first, second)

        for (option in SortOption.entries) {
            assertEquals(
                "Expected relative order preserved on tie for $option",
                listOf(first, second),
                sortItems(input, option)
            )
        }
    }

    @Test
    fun `input list is not mutated`() {
        val mutableInput = mutableListOf(itemCharlie, itemAlpha, itemBravo)
        val snapshot = mutableInput.toList()

        for (option in SortOption.entries) {
            sortItems(mutableInput, option)
            assertEquals(snapshot, mutableInput)
        }
    }

    @Test
    fun `SortOption fromName resolves valid names and falls back to DATE_ADDED_DESC`() {
        for (option in SortOption.entries) {
            assertEquals(option, SortOption.fromName(option.name))
        }
        assertEquals(SortOption.DATE_ADDED_DESC, SortOption.fromName(null))
        assertEquals(SortOption.DATE_ADDED_DESC, SortOption.fromName("UNKNOWN"))
    }

    @Test
    fun `FilterOption fromName resolves valid names and falls back to ALL`() {
        for (option in FilterOption.entries) {
            assertEquals(option, FilterOption.fromName(option.name))
        }
        assertEquals(FilterOption.ALL, FilterOption.fromName(null))
        assertEquals(FilterOption.ALL, FilterOption.fromName("UNKNOWN"))
    }
}
