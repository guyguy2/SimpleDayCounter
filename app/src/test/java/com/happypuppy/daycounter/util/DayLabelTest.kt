package com.happypuppy.daycounter.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DayLabelTest {

    @Test
    fun `zero is today`() {
        assertEquals(DayLabel.Today, dayLabelFor(0))
    }

    @Test
    fun `positive counts are left with the same count`() {
        assertEquals(DayLabel.Left(1), dayLabelFor(1))
        assertEquals(DayLabel.Left(2), dayLabelFor(2))
        assertEquals(DayLabel.Left(1135), dayLabelFor(1135))
    }

    @Test
    fun `negative counts are past with a positive count`() {
        assertEquals(DayLabel.Past(1), dayLabelFor(-1))
        assertEquals(DayLabel.Past(2), dayLabelFor(-2))
        assertEquals(DayLabel.Past(1135), dayLabelFor(-1135))
    }
}
