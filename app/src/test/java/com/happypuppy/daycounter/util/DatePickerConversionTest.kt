package com.happypuppy.daycounter.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Unit tests for [localDateToUtcPickerMillis] and [utcPickerMillisToLocalDate] across
 * standard, leap, and year-boundary dates.
 */
class DatePickerConversionTest {

    private fun expectedUtcMidnight(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `localDateToUtcPickerMillis converts LocalDate to UTC midnight millis`() {
        assertEquals(
            expectedUtcMidnight(2026, 11, 22),
            localDateToUtcPickerMillis(LocalDate.of(2026, 11, 22))
        )
        assertEquals(
            expectedUtcMidnight(2026, 12, 31),
            localDateToUtcPickerMillis(LocalDate.of(2026, 12, 31))
        )
    }

    @Test
    fun `utcPickerMillisToLocalDate restores exact UTC calendar date`() {
        val utcMillis = expectedUtcMidnight(2026, 7, 4)
        assertEquals(LocalDate.of(2026, 7, 4), utcPickerMillisToLocalDate(utcMillis))
    }

    @Test
    fun `round trip preserves calendar date across leap and DST dates`() {
        val samples = listOf(
            LocalDate.of(2024, 2, 29),
            LocalDate.of(2026, 3, 8),
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2027, 1, 1)
        )
        for (original in samples) {
            val pickerMillis = localDateToUtcPickerMillis(original)
            val restored = utcPickerMillisToLocalDate(pickerMillis)
            assertEquals(original, restored)
        }
    }
}
