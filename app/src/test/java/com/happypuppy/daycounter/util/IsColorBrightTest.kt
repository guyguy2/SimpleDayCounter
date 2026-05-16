package com.happypuppy.daycounter.util

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [isColorBright].
 *
 * Brightness is computed as a luma-weighted average of the linear RGB
 * components: 0.299*R + 0.587*G + 0.114*B, with the boundary at 0.5.
 */
class IsColorBrightTest {

    @Test
    fun `white is bright`() {
        assertTrue(isColorBright(Color.White))
    }

    @Test
    fun `black is not bright`() {
        assertFalse(isColorBright(Color.Black))
    }

    @Test
    fun `pure green is bright`() {
        // Green has the highest luma coefficient (0.587), so pure green crosses
        // the 0.5 threshold even on its own.
        assertTrue(isColorBright(Color.Green))
    }

    @Test
    fun `pure blue is not bright`() {
        // Blue has the smallest luma coefficient (0.114), well below 0.5.
        assertFalse(isColorBright(Color.Blue))
    }

    @Test
    fun `pure red is not bright`() {
        // Red weight is 0.299, also below the 0.5 threshold.
        assertFalse(isColorBright(Color.Red))
    }
}
