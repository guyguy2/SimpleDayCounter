package com.happypuppy.daycounter.ui.layout

import androidx.window.core.layout.WindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Wave 6 adaptive layout breakpoint mapping (UX-4).
 */
class AdaptiveLayoutTest {

    @Test
    fun `compact portrait uses single column list with swipe and bottom sheet`() {
        for (width in listOf(320, 360, 412, 599)) {
            val spec = adaptiveLayoutSpecFor(WindowSizeClass(width, 800))
            assertFalse("width=$width should use LazyColumn", spec.useGrid)
            assertTrue("width=$width should keep swipe enabled", spec.enableSwipeToDelete)
            assertFalse("width=$width portrait should use ModalBottomSheet", spec.useDialogForEventForm)
        }
    }

    @Test
    fun `compact width with short height uses list with swipe and dialog for event form`() {
        val spec = adaptiveLayoutSpecFor(WindowSizeClass(540, 360))
        assertFalse(spec.useGrid)
        assertTrue(spec.enableSwipeToDelete)
        assertTrue(spec.useDialogForEventForm)
    }

    @Test
    fun `medium width uses centered grid, disables swipe, and uses event form dialog`() {
        for ((width, height) in listOf(600 to 800, 640 to 360, 720 to 900, 839 to 600)) {
            val spec = adaptiveLayoutSpecFor(WindowSizeClass(width, height))
            assertEquals(
                AdaptiveLayoutSpec(
                    useGrid = true,
                    enableSwipeToDelete = false,
                    useDialogForEventForm = true
                ),
                spec
            )
        }
    }

    @Test
    fun `expanded width uses centered grid, disables swipe, and uses event form dialog`() {
        for ((width, height) in listOf(840 to 900, 1024 to 768, 1200 to 800)) {
            val spec = adaptiveLayoutSpecFor(WindowSizeClass(width, height))
            assertTrue("width=$width should use grid", spec.useGrid)
            assertFalse("width=$width should disable swipe", spec.enableSwipeToDelete)
            assertTrue("width=$width should use dialog", spec.useDialogForEventForm)
        }
    }
}
