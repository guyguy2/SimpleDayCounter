package com.happypuppy.daycounter.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.daycounter.ui.screens.EventUi
import com.happypuppy.daycounter.ui.theme.SimpleDayCounterTheme
import com.happypuppy.daycounter.util.dayLabelFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Verifies two-stage swipe-to-delete reveal behavior (Wave 6 UX-21):
 * - At rest (`Settled`), the delete icon is not shown and the background stays transparent.
 * - A partial `StartToEnd` drag (< 50%) reveals the delete icon on the start side with a visible
 *   non-transparent destructive background without dispatching delete.
 * - A partial `EndToStart` drag (< 50%) reveals the delete icon on the end side with a visible
 *   non-transparent destructive background without dispatching delete.
 */
@RunWith(AndroidJUnit4::class)
class SwipeToDeleteItemInstrumentedTest {

    @get:Rule
    val rule = createComposeRule()

    private fun sampleEvent(name: String = "Flight Home"): EventUi = EventUi(
        id = 1,
        name = name,
        category = "Personal",
        targetDate = LocalDate.of(2026, 6, 20),
        daysLeft = 5,
        dayLabel = dayLabelFor(5),
        emoji = "",
        createdAtMillis = 1_000L
    )

    private fun backgroundColor(): Color {
        val color = rule.onNodeWithTag(SWIPE_DELETE_BACKGROUND_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SwipeDeleteBackgroundColorKey)
        assertNotNull("Swipe background should expose its resolved color", color)
        return color!!
    }

    @Test
    fun atRestDeleteIconIsNotShownAndBackgroundIsTransparent() {
        rule.setContent {
            SimpleDayCounterTheme(darkTheme = false) {
                Box(modifier = Modifier.width(360.dp).padding(16.dp)) {
                    SwipeToDeleteItem(
                        event = sampleEvent(),
                        isVibrationEnabled = false,
                        onDelete = {},
                        onClick = {}
                    )
                }
            }
        }

        rule.onNodeWithTag(SWIPE_DELETE_ICON_TAG, useUnmergedTree = true).assertDoesNotExist()
        assertEquals(Color.Transparent, backgroundColor())
    }

    @Test
    fun partialStartToEndDragRevealsIconOnStartSideWithDestructiveBackground() {
        var deleteCount = 0
        rule.setContent {
            SimpleDayCounterTheme(darkTheme = false) {
                Box(modifier = Modifier.width(360.dp).padding(16.dp)) {
                    SwipeToDeleteItem(
                        event = sampleEvent("Start Drag"),
                        isVibrationEnabled = false,
                        onDelete = { deleteCount++ },
                        onClick = {}
                    )
                }
            }
        }

        rule.onNodeWithText("Start Drag").performTouchInput {
            down(center)
            moveBy(Offset(width * 0.25f, 0f))
        }
        rule.waitForIdle()

        val bgBounds = rule.onNodeWithTag(SWIPE_DELETE_BACKGROUND_TAG, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val iconBounds = rule.onNodeWithTag(SWIPE_DELETE_ICON_TAG, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val color = backgroundColor()

        assertTrue(
            "StartToEnd icon (${iconBounds.center.x}) should sit on start half (< ${bgBounds.center.x})",
            iconBounds.center.x < bgBounds.center.x
        )
        assertNotEquals("Partial StartToEnd drag must show destructive tint", Color.Transparent, color)
        assertTrue("Background alpha should be visible, was ${color.alpha}", color.alpha > 0.5f)
        assertEquals("Partial drag below 50% must not trigger delete", 0, deleteCount)

        rule.onNodeWithText("Start Drag").performTouchInput { cancel() }
    }

    @Test
    fun partialEndToStartDragRevealsIconOnEndSideWithDestructiveBackground() {
        var deleteCount = 0
        rule.setContent {
            SimpleDayCounterTheme(darkTheme = false) {
                Box(modifier = Modifier.width(360.dp).padding(16.dp)) {
                    SwipeToDeleteItem(
                        event = sampleEvent("End Drag"),
                        isVibrationEnabled = false,
                        onDelete = { deleteCount++ },
                        onClick = {}
                    )
                }
            }
        }

        rule.onNodeWithText("End Drag").performTouchInput {
            down(center)
            moveBy(Offset(-width * 0.25f, 0f))
        }
        rule.waitForIdle()

        val bgBounds = rule.onNodeWithTag(SWIPE_DELETE_BACKGROUND_TAG, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val iconBounds = rule.onNodeWithTag(SWIPE_DELETE_ICON_TAG, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val color = backgroundColor()

        assertTrue(
            "EndToStart icon (${iconBounds.center.x}) should sit on end half (> ${bgBounds.center.x})",
            iconBounds.center.x > bgBounds.center.x
        )
        assertNotEquals("Partial EndToStart drag must show destructive tint", Color.Transparent, color)
        assertTrue("Background alpha should be visible, was ${color.alpha}", color.alpha > 0.5f)
        assertEquals("Partial drag below 50% must not trigger delete", 0, deleteCount)

        rule.onNodeWithText("End Drag").performTouchInput { cancel() }
    }

    @Test
    fun partialDragInDarkThemeRevealsDestructiveBackgroundAndIcon() {
        rule.setContent {
            SimpleDayCounterTheme(darkTheme = true) {
                Box(modifier = Modifier.width(360.dp).padding(16.dp)) {
                    SwipeToDeleteItem(
                        event = sampleEvent("Dark Drag"),
                        isVibrationEnabled = false,
                        onDelete = {},
                        onClick = {}
                    )
                }
            }
        }

        rule.onNodeWithText("Dark Drag").performTouchInput {
            down(center)
            moveBy(Offset(-width * 0.25f, 0f))
        }
        rule.waitForIdle()

        rule.onNodeWithTag(SWIPE_DELETE_ICON_TAG, useUnmergedTree = true).assertExists()
        val color = backgroundColor()
        assertNotEquals(Color.Transparent, color)
        assertTrue("Dark stage 1 alpha should be visible, was ${color.alpha}", color.alpha > 0.5f)

        rule.onNodeWithText("Dark Drag").performTouchInput { cancel() }
    }
}
