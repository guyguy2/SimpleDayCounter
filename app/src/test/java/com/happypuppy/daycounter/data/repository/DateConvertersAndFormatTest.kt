package com.happypuppy.daycounter.data.repository

import com.happypuppy.daycounter.data.preferences.AppTheme
import com.happypuppy.daycounter.data.preferences.DateFormat
import com.happypuppy.daycounter.data.preferences.formatDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DateConvertersAndFormatTest {

    private val converters = DateConverters()
    private lateinit var originalLocale: Locale
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        originalTimeZone = TimeZone.getDefault()
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `DateConverters handles null values`() {
        assertNull(converters.fromTimestamp(null))
        assertNull(converters.dateToTimestamp(null))
    }

    @Test
    fun `DateConverters round-trips epoch, positive, and negative timestamps`() {
        for (ts in listOf(-86_400_000L, 0L, 1_700_000_000_000L)) {
            val date = converters.fromTimestamp(ts)
            assertEquals(Date(ts), date)
            assertEquals(ts, converters.dateToTimestamp(date))
        }
    }

    @Test
    fun `formatDate formats date for every DateFormat option`() {
        val sample = Date.from(LocalDateTime.of(2026, 12, 31, 12, 0).toInstant(ZoneOffset.UTC))

        assertEquals("Dec 31, 2026", formatDate(sample, DateFormat.DEFAULT))
        assertEquals("12/31/2026", formatDate(sample, DateFormat.US))
        assertEquals("31/12/2026", formatDate(sample, DateFormat.EU))
        assertEquals("2026-12-31", formatDate(sample, DateFormat.ISO))
        assertEquals("December 31, 2026", formatDate(sample, DateFormat.FULL))
    }

    @Test
    fun `DateFormat and AppTheme fromString fall back safely on unknown values`() {
        assertEquals(DateFormat.ISO, DateFormat.fromString("iso"))
        assertEquals(DateFormat.DEFAULT, DateFormat.fromString("nonexistent"))

        assertEquals(AppTheme.DARK, AppTheme.fromString("dark"))
        assertEquals(AppTheme.SYSTEM_DEFAULT, AppTheme.fromString("unknown"))
    }
}
