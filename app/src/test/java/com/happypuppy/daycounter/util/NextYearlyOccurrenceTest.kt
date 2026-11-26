package com.happypuppy.daycounter.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [nextYearlyOccurrence], pinning midnight roll, year boundaries, and
 * Feb 29 -> Feb 28 clamping in non-leap years.
 */
class NextYearlyOccurrenceTest {

    @Test
    fun `original date in the future returns original date`() {
        val today = LocalDate.of(2026, 6, 15)
        val future = LocalDate.of(2028, 3, 10)
        assertEquals(future, nextYearlyOccurrence(future, today))
    }

    @Test
    fun `original date equal to today returns today`() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(today, nextYearlyOccurrence(today, today))
    }

    @Test
    fun `earlier this year rolls to next year`() {
        val today = LocalDate.of(2026, 6, 15)
        val earlierThisYear = LocalDate.of(2026, 3, 1)
        assertEquals(LocalDate.of(2027, 3, 1), nextYearlyOccurrence(earlierThisYear, today))
    }

    @Test
    fun `later this year from past year returns this year`() {
        val today = LocalDate.of(2026, 6, 15)
        val pastLaterInYear = LocalDate.of(2020, 10, 20)
        assertEquals(LocalDate.of(2026, 10, 20), nextYearlyOccurrence(pastLaterInYear, today))
    }

    @Test
    fun `same month and day from previous year returns today`() {
        val today = LocalDate.of(2026, 6, 15)
        val fiveYearsAgo = LocalDate.of(2021, 6, 15)
        assertEquals(today, nextYearlyOccurrence(fiveYearsAgo, today))
    }

    @Test
    fun `yesterday from previous year rolls to next year`() {
        val today = LocalDate.of(2026, 6, 15)
        val anniversaryYesterday = LocalDate.of(2021, 6, 14)
        assertEquals(LocalDate.of(2027, 6, 14), nextYearlyOccurrence(anniversaryYesterday, today))
    }

    @Test
    fun `feb 29 in non-leap year clamps to feb 28`() {
        val leapBirthday = LocalDate.of(2000, 2, 29)

        // On Feb 28 of a non-leap year, the clamped occurrence is today.
        assertEquals(
            LocalDate.of(2025, 2, 28),
            nextYearlyOccurrence(leapBirthday, LocalDate.of(2025, 2, 28))
        )
        // On Mar 1 of a non-leap year, rolls to Feb 28 of the next non-leap year.
        assertEquals(
            LocalDate.of(2026, 2, 28),
            nextYearlyOccurrence(leapBirthday, LocalDate.of(2025, 3, 1))
        )
        // Rolling from 2027 into leap year 2028 keeps Feb 29.
        assertEquals(
            LocalDate.of(2028, 2, 29),
            nextYearlyOccurrence(leapBirthday, LocalDate.of(2027, 3, 1))
        )
    }

    @Test
    fun `feb 29 in leap year resolves to feb 29`() {
        val leapBirthday = LocalDate.of(2000, 2, 29)

        assertEquals(
            LocalDate.of(2024, 2, 29),
            nextYearlyOccurrence(leapBirthday, LocalDate.of(2024, 2, 28))
        )
        assertEquals(
            LocalDate.of(2024, 2, 29),
            nextYearlyOccurrence(leapBirthday, LocalDate.of(2024, 2, 29))
        )
        assertEquals(
            LocalDate.of(2025, 2, 28),
            nextYearlyOccurrence(leapBirthday, LocalDate.of(2024, 3, 1))
        )
    }

    @Test
    fun `dec 31 and jan 1 year boundary`() {
        val dec31 = LocalDate.of(2020, 12, 31)
        assertEquals(
            LocalDate.of(2026, 12, 31),
            nextYearlyOccurrence(dec31, LocalDate.of(2026, 12, 31))
        )
        assertEquals(
            LocalDate.of(2027, 12, 31),
            nextYearlyOccurrence(dec31, LocalDate.of(2027, 1, 1))
        )

        val jan1 = LocalDate.of(2020, 1, 1)
        assertEquals(
            LocalDate.of(2027, 1, 1),
            nextYearlyOccurrence(jan1, LocalDate.of(2026, 12, 31))
        )
        assertEquals(
            LocalDate.of(2027, 1, 1),
            nextYearlyOccurrence(jan1, LocalDate.of(2027, 1, 1))
        )
        assertEquals(
            LocalDate.of(2028, 1, 1),
            nextYearlyOccurrence(jan1, LocalDate.of(2027, 1, 2))
        )
    }
}
