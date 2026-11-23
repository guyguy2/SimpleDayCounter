package com.happypuppy.daycounter.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date

/**
 * Unit tests for [localDateToUtcPickerMillis] and [utcPickerMillisToLocalDate] across negative,
 * UTC, and positive timezones, including late-night and early-morning hours.
 */
class DatePickerConversionTest {

    private val chicago = ZoneId.of("America/Chicago") // UTC-6 (CST) / UTC-5 (CDT)
    private val utc = ZoneId.of("UTC")
    private val tokyo = ZoneId.of("Asia/Tokyo") // UTC+9

    private fun expectedUtcMidnight(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `late night in negative offset timezone stays on same local calendar date`() {
        // 2026-11-22 23:30 CST is 2026-11-23 05:30 UTC; picker must still open on Nov 22.
        val lateNightChicago = Date.from(
            LocalDateTime.of(2026, 11, 22, 23, 30).atZone(chicago).toInstant()
        )
        val pickerMillis = localDateToUtcPickerMillis(lateNightChicago, zoneId = chicago)
        assertEquals(expectedUtcMidnight(2026, 11, 22), pickerMillis)
    }

    @Test
    fun `early morning in positive offset timezone stays on same local calendar date`() {
        // 2026-11-22 00:30 JST is 2026-11-21 15:30 UTC; picker must still open on Nov 22.
        val earlyMorningTokyo = Date.from(
            LocalDateTime.of(2026, 11, 22, 0, 30).atZone(tokyo).toInstant()
        )
        val pickerMillis = localDateToUtcPickerMillis(earlyMorningTokyo, zoneId = tokyo)
        assertEquals(expectedUtcMidnight(2026, 11, 22), pickerMillis)
    }

    @Test
    fun `UTC timezone preserves exact calendar date`() {
        val lateUtc = Date.from(
            LocalDateTime.of(2026, 12, 31, 23, 59).atZone(utc).toInstant()
        )
        val pickerMillis = localDateToUtcPickerMillis(lateUtc, zoneId = utc)
        assertEquals(expectedUtcMidnight(2026, 12, 31), pickerMillis)
    }

    @Test
    fun `utcPickerMillisToLocalDate anchors at local noon in negative, UTC, and positive zones`() {
        val utcMillis = expectedUtcMidnight(2026, 7, 4)

        for (zone in listOf(chicago, utc, tokyo)) {
            val localDate = utcPickerMillisToLocalDate(utcMillis, zoneId = zone)
            val zoned = Instant.ofEpochMilli(localDate.time).atZone(zone)
            assertEquals("date mismatch in $zone", LocalDate.of(2026, 7, 4), zoned.toLocalDate())
            assertEquals("hour should be noon in $zone", 12, zoned.hour)
            assertEquals("minute should be 0 in $zone", 0, zoned.minute)
        }
    }

    @Test
    fun `round trip preserves calendar date across all timezones`() {
        for (zone in listOf(chicago, utc, tokyo)) {
            val original = Date.from(
                LocalDateTime.of(2026, 3, 8, 23, 45).atZone(zone).toInstant()
            )
            val pickerMillis = localDateToUtcPickerMillis(original, zoneId = zone)
            val restored = utcPickerMillisToLocalDate(pickerMillis, zoneId = zone)
            val restoredLocalDate = restored.toInstant().atZone(zone).toLocalDate()
            assertEquals("round trip failed in $zone", LocalDate.of(2026, 3, 8), restoredLocalDate)
        }
    }
}
