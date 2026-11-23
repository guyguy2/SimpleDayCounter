package com.happypuppy.daycounter.ui.theme

import androidx.compose.ui.graphics.Color
import com.happypuppy.daycounter.util.isColorBright
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Guards the day-count badge in EventCard: the badge text is Black or White depending on
 * [isColorBright], and must reach the WCAG 2.x AA ratio for large/bold text (3.0). We assert 3.0
 * (the badge uses 28sp bold) and additionally report the worst ratio so regressions are visible.
 *
 * Reference: https://www.w3.org/TR/WCAG22/#contrast-minimum
 */
class CategoryColorContrastTest {

    @After
    fun tearDown() {
        CategoryColors.clearCustomColorCache()
    }

    private fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun badgeTextFor(background: Color): Color =
        if (isColorBright(background)) Color.Black else Color.White

    private fun worstContrast(colors: List<Color>): Double =
        colors.minOf { contrast(badgeTextFor(it), it) }

    @Test
    fun `default category badges reach large-text contrast in both themes`() {
        val light = CategoryColors.defaultCategoryColorsLight.values.toList()
        val dark = CategoryColors.defaultCategoryColorsDark.values.toList()
        assertTrue("light worst=${worstContrast(light)}", worstContrast(light) >= 3.0)
        assertTrue("dark worst=${worstContrast(dark)}", worstContrast(dark) >= 3.0)
    }

    @Test
    fun `generated custom category badges reach large-text contrast in both themes`() {
        val names = (0 until 2000).map { "Category $it" }
        val light = names.map { CategoryColors.getCategoryColor(it, isDarkTheme = false) }
        val dark = names.map { CategoryColors.getCategoryColor(it, isDarkTheme = true) }
        assertTrue("light worst=${worstContrast(light)}", worstContrast(light) >= 3.0)
        assertTrue("dark worst=${worstContrast(dark)}", worstContrast(dark) >= 3.0)
    }

    @Test
    fun `contrast helper matches known WCAG reference values`() {
        assertEquals(21.0, contrast(Color.Black, Color.White), 0.01)
        assertEquals(1.0, contrast(Color.White, Color.White), 0.001)
    }
}
