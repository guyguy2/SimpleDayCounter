package com.happypuppy.daycounter.ui.theme

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for [CategoryColors].
 *
 * The custom-category color generator must be deterministic per category name so
 * users see the same color across launches. Default-category colors come from
 * fixed maps and must match the published palette.
 */
class CategoryColorsTest {

    @After
    fun tearDown() {
        CategoryColors.clearCustomColorCache()
    }

    @Test
    fun `default category returns palette color for light theme`() {
        assertEquals(
            CategoryColors.defaultCategoryColorsLight["Personal"],
            CategoryColors.getCategoryColor("Personal", isDarkTheme = false)
        )
    }

    @Test
    fun `default category returns palette color for dark theme`() {
        assertEquals(
            CategoryColors.defaultCategoryColorsDark["Work"],
            CategoryColors.getCategoryColor("Work", isDarkTheme = true)
        )
    }

    @Test
    fun `custom category color is deterministic across calls`() {
        val first = CategoryColors.getCategoryColor("Travel", isDarkTheme = false)
        val second = CategoryColors.getCategoryColor("Travel", isDarkTheme = false)
        assertEquals(first, second)
    }

    @Test
    fun `custom category color is deterministic across cache clears`() {
        val first = CategoryColors.getCategoryColor("Hobbies", isDarkTheme = false)
        CategoryColors.clearCustomColorCache()
        val second = CategoryColors.getCategoryColor("Hobbies", isDarkTheme = false)
        assertEquals(first, second)
    }

    @Test
    fun `light and dark variants differ for the same custom category`() {
        val light = CategoryColors.getCategoryColor("Books", isDarkTheme = false)
        val dark = CategoryColors.getCategoryColor("Books", isDarkTheme = true)
        assertNotEquals(light, dark)
    }
}
