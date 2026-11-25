package com.happypuppy.daycounter.data.repository

import com.happypuppy.daycounter.data.preferences.AppTheme
import com.happypuppy.daycounter.data.preferences.DateFormat
import com.happypuppy.daycounter.data.preferences.formatDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
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
        assertNull(converters.fromLocalDateString(null))
        assertNull(converters.localDateToString(null))
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
    fun `DateConverters round-trips ISO-8601 LocalDate strings`() {
        val samples = listOf(
            LocalDate.of(1970, 1, 1) to "1970-01-01",
            LocalDate.of(2024, 2, 29) to "2024-02-29",
            LocalDate.of(2026, 12, 31) to "2026-12-31"
        )
        for ((date, iso) in samples) {
            assertEquals(iso, converters.localDateToString(date))
            assertEquals(date, converters.fromLocalDateString(iso))
        }
    }

    @Test
    fun `formatDate formats LocalDate and Date for every DateFormat option`() {
        val localDate = LocalDate.of(2026, 12, 31)
        assertEquals("Dec 31, 2026", formatDate(localDate, DateFormat.DEFAULT))
        assertEquals("12/31/2026", formatDate(localDate, DateFormat.US))
        assertEquals("31/12/2026", formatDate(localDate, DateFormat.EU))
        assertEquals("2026-12-31", formatDate(localDate, DateFormat.ISO))
        assertEquals("December 31, 2026", formatDate(localDate, DateFormat.FULL))

        val sample = Date.from(LocalDateTime.of(2026, 12, 31, 12, 0).toInstant(ZoneOffset.UTC))
        assertEquals("Dec 31, 2026", formatDate(sample, DateFormat.DEFAULT))
    }

    @Test
    fun `DateFormat and AppTheme fromString fall back safely on unknown values`() {
        assertEquals(DateFormat.ISO, DateFormat.fromString("iso"))
        assertEquals(DateFormat.DEFAULT, DateFormat.fromString("nonexistent"))

        assertEquals(AppTheme.DARK, AppTheme.fromString("dark"))
        assertEquals(AppTheme.SYSTEM_DEFAULT, AppTheme.fromString("unknown"))
    }
}
