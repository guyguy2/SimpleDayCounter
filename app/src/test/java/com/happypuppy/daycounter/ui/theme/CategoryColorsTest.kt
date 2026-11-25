package com.happypuppy.daycounter.ui.theme

import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.WORK_CATEGORY_ID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for [CategoryColors].
 *
 * The custom-category color generator must be deterministic per category name so
 * users see the same color across launches and UUID migrations. Default-category colors
 * come from fixed maps keyed by stable ID.
 */
class CategoryColorsTest {

    @After
    fun tearDown() {
        CategoryColors.clearCustomColorCache()
    }

    @Test
    fun `default category returns palette color for light theme by stable id`() {
        assertEquals(
            CategoryColors.defaultCategoryColorsByIdLight[DEFAULT_CATEGORY_ID],
            CategoryColors.getCategoryColor(DEFAULT_CATEGORY_ID, "Personal", isDarkTheme = false)
        )
    }

    @Test
    fun `default category returns palette color for dark theme by stable id`() {
        assertEquals(
            CategoryColors.defaultCategoryColorsByIdDark[WORK_CATEGORY_ID],
            CategoryColors.getCategoryColor(WORK_CATEGORY_ID, "Work", isDarkTheme = true)
        )
    }

    @Test
    fun `custom category color is deterministic per name regardless of UUID id`() {
        val first = CategoryColors.getCategoryColor("uuid-1", "Travel", isDarkTheme = false)
        val second = CategoryColors.getCategoryColor("uuid-2", "Travel", isDarkTheme = false)
        assertEquals(first, second)
    }

    @Test
    fun `custom category color is deterministic across cache clears`() {
        val first = CategoryColors.getCategoryColor("uuid-a", "Hobbies", isDarkTheme = false)
        CategoryColors.clearCustomColorCache()
        val second = CategoryColors.getCategoryColor("uuid-a", "Hobbies", isDarkTheme = false)
        assertEquals(first, second)
    }

    @Test
    fun `light and dark variants differ for the same custom category`() {
        val light = CategoryColors.getCategoryColor("uuid-b", "Books", isDarkTheme = false)
        val dark = CategoryColors.getCategoryColor("uuid-b", "Books", isDarkTheme = true)
        assertNotEquals(light, dark)
    }
}
