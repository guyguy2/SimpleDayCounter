package com.happypuppy.daycounter.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.happypuppy.daycounter.util.isColorBright
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The color pairs EventCard draws with theme roles, in both brand schemes: text on the state
 * container needs 4.5:1 (WCAG 1.4.3) and the day-count digits on the category badge need 3:1
 * (28sp bold, large text). The category icon and the badge disc are not asserted against the card
 * container: the icon is decorative (the category name is text next to it) and the disc only
 * frames digits that are checked against it. In the light scheme they measure 1.3 to 2.9:1.
 */
class EventCardColorsTest {

    @After
    fun tearDown() = CategoryColors.clearCustomColorCache()

    private val light = brandEventCardColors(LightColorScheme, darkTheme = false)
    private val dark = brandEventCardColors(DarkColorScheme, darkTheme = true)

    private fun states(colors: EventCardColors) =
        listOf("past" to colors.past, "today" to colors.today, "future" to colors.future)

    private fun argb(c: Color): Long = c.toArgb().toLong() and 0xFFFFFFFFL

    @Test
    fun `state containers keep the tints the app always used`() {
        assertEquals(
            listOf(0xFFFFF0F6, 0xFFF9F0FF, 0xFFFFFBE6),
            listOf(light.past, light.today, light.future).map { argb(it.container) }
        )
        assertEquals(
            listOf(0xFF331A24, 0xFF2B1A33, 0xFF332912),
            listOf(dark.past, dark.today, dark.future).map { argb(it.container) }
        )
    }

    @Test
    fun `days left picks past today or future`() {
        assertEquals(light.past, light.forDaysLeft(-1))
        assertEquals(light.today, light.forDaysLeft(0))
        assertEquals(light.future, light.forDaysLeft(1))
        assertEquals(dark.past, dark.forDaysLeft(-400))
    }

    @Test
    fun `text on every state container reaches 4_5 to 1 in both schemes`() {
        for ((scheme, colors) in listOf("light" to light, "dark" to dark)) {
            for ((state, c) in states(colors)) {
                val primary = Wcag.contrast(c.content, c.container)
                val secondary = Wcag.contrast(c.contentVariant, c.container)
                assertTrue("$scheme $state primary text $primary", primary >= 4.5)
                assertTrue("$scheme $state secondary text $secondary", secondary >= 4.5)
            }
        }
    }

    private fun badgeText(background: Color) =
        if (isColorBright(background)) Color.Black else Color.White

    @Test
    fun `day count digits on every default category badge reach 3 to 1`() {
        for (dark in listOf(false, true)) {
            for (category in listOf("Personal", "Work", "Health", "Finance")) {
                val badge = CategoryColors.getCategoryColor(category, dark)
                val ratio = Wcag.contrast(badgeText(badge), badge)
                assertTrue("dark=$dark $category badge digits $ratio", ratio >= 3.0)
            }
        }
    }
}
