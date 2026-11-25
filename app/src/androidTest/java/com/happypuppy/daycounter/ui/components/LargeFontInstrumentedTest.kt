package com.happypuppy.daycounter.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.daycounter.ui.screens.EventUi
import com.happypuppy.daycounter.ui.screens.MainTopBarTitle
import com.happypuppy.daycounter.ui.theme.SimpleDayCounterTheme
import com.happypuppy.daycounter.util.dayLabelFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.math.ceil

/**
 * The Android maximum font scale (2.0) must not break the day-count badge or the app bar title
 * (PLAN.md UX-2). Composables are rendered directly at a fixed 360dp width so the result does not
 * depend on the emulator's screen size.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class LargeFontInstrumentedTest {

    @get:Rule
    val rule = createComposeRule()

    private fun eventUi(name: String, daysFromToday: Int): EventUi = EventUi(
        id = 1,
        name = name,
        category = "Personal",
        targetDate = LocalDate.of(2026, 6, 15).plusDays(daysFromToday.toLong()),
        daysLeft = daysFromToday,
        dayLabel = dayLabelFor(daysFromToday),
        emoji = "",
        createdAtMillis = 0L
    )

    private fun layoutOf(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single()
    }

    private fun setLargeFontContent(content: @Composable () -> Unit) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                SimpleDayCounterTheme(darkTheme = false) {
                    Box(modifier = Modifier.width(360.dp)) { content() }
                }
            }
        }
    }

    @Test
    fun fourDigitDayCountStaysOnOneLineAtMaxFontScale() {
        val event = eventUi(name = "Far away", daysFromToday = 1135)
        setLargeFontContent { EventCard(event = event, onClick = {}) }

        val layout = layoutOf("1135")

        assertEquals("day count must render on a single line", 1, layout.lineCount)
        // The badge must grow to the number's natural width instead of clipping it.
        assertTrue(
            "day count must get its full natural width",
            layout.size.width >= ceil(layout.multiParagraph.maxIntrinsicWidth).toInt()
        )
    }

    @Test
    fun appBarTitleStaysOnOneLineAtMaxFontScale() {
        setLargeFontContent {
            TopAppBar(
                title = { MainTopBarTitle() },
                navigationIcon = {
                    Icon(imageVector = Icons.Default.Event, contentDescription = null)
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.FilterList, contentDescription = "Filter") }
                    IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                }
            )
        }

        assertEquals("app bar title must render on a single line", 1, layoutOf("Simple Day Counter").lineCount)
    }
}
