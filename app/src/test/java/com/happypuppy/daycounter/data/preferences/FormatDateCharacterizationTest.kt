package com.happypuppy.daycounter.data.preferences

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Characterization of [formatDate] beyond the US sample in DateConvertersAndFormatTest: a
 * single-digit day and month, and a non-US default locale. Today only the month names follow the
 * device locale; the field order and separators are fixed by the stored pattern (PLAN.md UX-7).
 */
class FormatDateCharacterizationTest {

    private lateinit var originalLocale: Locale
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
    }

    private val singleDigitDay: Date =
        Date.from(LocalDateTime.of(2026, 1, 5, 12, 0).toInstant(ZoneOffset.UTC))

    @Test
    fun `single digit day and month in the US locale`() {
        Locale.setDefault(Locale.US)

        assertEquals("Jan 5, 2026", formatDate(singleDigitDay, DateFormat.DEFAULT))
        assertEquals("01/05/2026", formatDate(singleDigitDay, DateFormat.US))
        assertEquals("05/01/2026", formatDate(singleDigitDay, DateFormat.EU))
        assertEquals("2026-01-05", formatDate(singleDigitDay, DateFormat.ISO))
        assertEquals("January 5, 2026", formatDate(singleDigitDay, DateFormat.FULL))
    }

    @Test
    fun `numeric patterns ignore the device locale`() {
        Locale.setDefault(Locale.GERMANY)

        assertEquals("01/05/2026", formatDate(singleDigitDay, DateFormat.US))
        assertEquals("05/01/2026", formatDate(singleDigitDay, DateFormat.EU))
        assertEquals("2026-01-05", formatDate(singleDigitDay, DateFormat.ISO))
    }

    @Test
    fun `month names follow the device locale but the pattern order does not`() {
        Locale.setDefault(Locale.GERMANY)

        assertEquals("Januar 5, 2026", formatDate(singleDigitDay, DateFormat.FULL))
    }

    @Test
    fun `system default follows the device locale for the order, separators and month names`() {
        val sample = Date.from(LocalDateTime.of(2026, 12, 31, 12, 0).toInstant(ZoneOffset.UTC))

        assertEquals("Dec 31, 2026", formatDate(sample, DateFormat.SYSTEM, Locale.US))
        assertEquals("31 Dec 2026", formatDate(sample, DateFormat.SYSTEM, Locale.UK))
        assertEquals("31.12.2026", formatDate(sample, DateFormat.SYSTEM, Locale.GERMANY))
        assertEquals("31 d\u00e9c. 2026", formatDate(sample, DateFormat.SYSTEM, Locale.FRANCE))
        assertEquals("2026/12/31", formatDate(sample, DateFormat.SYSTEM, Locale.JAPAN))
    }

    @Test
    fun `system default uses the default locale when none is passed`() {
        val sample = Date.from(LocalDateTime.of(2026, 12, 31, 12, 0).toInstant(ZoneOffset.UTC))
        Locale.setDefault(Locale.GERMANY)

        assertEquals("31.12.2026", formatDate(sample, DateFormat.SYSTEM))
    }

    @Test
    fun `system default in the US locale reads the same as the old default pattern`() {
        // Users with no stored choice moved from the old default pattern to the system default.
        val sample = Date.from(LocalDateTime.of(2026, 1, 5, 12, 0).toInstant(ZoneOffset.UTC))

        assertEquals(formatDate(sample, DateFormat.DEFAULT, Locale.US), formatDate(sample, DateFormat.SYSTEM, Locale.US))
    }
}
