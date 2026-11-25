package com.happypuppy.daycounter.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Deterministic unit tests for [calculateDaysLeft] using [LocalDate], including DST boundary dates
 * and leap years.
 */
class CalculateDaysLeftTest {

    @Test
    fun `today returns zero`() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(0, calculateDaysLeft(LocalDate.of(2026, 6, 15), today = today))
    }

    @Test
    fun `tomorrow returns one`() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(1, calculateDaysLeft(LocalDate.of(2026, 6, 16), today = today))
    }

    @Test
    fun `yesterday returns minus one`() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(-1, calculateDaysLeft(LocalDate.of(2026, 6, 14), today = today))
    }

    @Test
    fun `seven days ahead returns seven`() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(7, calculateDaysLeft(LocalDate.of(2026, 6, 22), today = today))
    }

    @Test
    fun `thirty days behind returns minus thirty`() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(-30, calculateDaysLeft(LocalDate.of(2026, 5, 16), today = today))
    }

    @Test
    fun `spring-forward DST calendar span counts exact calendar days`() {
        val beforeDst = LocalDate.of(2026, 3, 7)
        val afterDst = LocalDate.of(2026, 3, 9)
        assertEquals(2, calculateDaysLeft(afterDst, today = beforeDst))
        assertEquals(-2, calculateDaysLeft(beforeDst, today = afterDst))
    }

    @Test
    fun `fall-back DST calendar span counts exact calendar days`() {
        val beforeFallBack = LocalDate.of(2026, 10, 31)
        val afterFallBack = LocalDate.of(2026, 11, 2)
        assertEquals(2, calculateDaysLeft(afterFallBack, today = beforeFallBack))
        assertEquals(-2, calculateDaysLeft(beforeFallBack, today = afterFallBack))
    }

    @Test
    fun `leap year February has 29 days`() {
        assertEquals(2, calculateDaysLeft(LocalDate.of(2024, 3, 1), today = LocalDate.of(2024, 2, 28)))
        assertEquals(1, calculateDaysLeft(LocalDate.of(2026, 3, 1), today = LocalDate.of(2026, 2, 28)))
    }

    @Test
    fun `far future date is positive`() {
        val today = LocalDate.of(2026, 1, 1)
        assertEquals(365, calculateDaysLeft(LocalDate.of(2027, 1, 1), today = today))
    }
}
