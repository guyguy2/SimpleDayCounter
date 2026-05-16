package com.happypuppy.daycounter.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

/**
 * Unit tests for [calculateDaysLeft].
 *
 * The function returns the signed day delta between today and the supplied date,
 * with both anchored to local midnight. Positive = future, 0 = today, negative = past.
 */
class CalculateDaysLeftTest {

    private fun today(): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun todayPlusDays(days: Int): Date = today().apply {
        add(Calendar.DAY_OF_YEAR, days)
    }.time

    @Test
    fun `today returns zero`() {
        assertEquals(0, calculateDaysLeft(todayPlusDays(0)))
    }

    @Test
    fun `tomorrow returns one`() {
        assertEquals(1, calculateDaysLeft(todayPlusDays(1)))
    }

    @Test
    fun `yesterday returns minus one`() {
        assertEquals(-1, calculateDaysLeft(todayPlusDays(-1)))
    }

    @Test
    fun `seven days ahead returns seven`() {
        assertEquals(7, calculateDaysLeft(todayPlusDays(7)))
    }

    @Test
    fun `thirty days behind returns minus thirty`() {
        assertEquals(-30, calculateDaysLeft(todayPlusDays(-30)))
    }

    @Test
    fun `time of day within today is treated as today`() {
        // A timestamp later in the same day must still resolve to 0 because the
        // function normalises to midnight before subtracting.
        val laterToday = today().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }.time
        assertEquals(0, calculateDaysLeft(laterToday))
    }

    @Test
    fun `far future date is positive`() {
        assertTrue(calculateDaysLeft(todayPlusDays(365)) > 0)
    }
}
