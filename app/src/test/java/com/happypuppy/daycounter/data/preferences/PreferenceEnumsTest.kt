package com.happypuppy.daycounter.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The persisted `value` strings are an on-disk contract (DataStore + cloud backup), so they must be
 * unique and round-trip. DateFormat.displayName is hardcoded English text with an embedded example;
 * this test keeps those examples honest until they are replaced by string resources.
 */
class PreferenceEnumsTest {

    @Test
    fun `AppTheme values are unique and round-trip`() {
        assertEquals(AppTheme.entries.size, AppTheme.entries.map { it.value }.toSet().size)
        AppTheme.entries.forEach { assertEquals(it, AppTheme.fromString(it.value)) }
    }

    @Test
    fun `DateFormat values are unique and round-trip`() {
        assertEquals(DateFormat.entries.size, DateFormat.entries.map { it.value }.toSet().size)
        DateFormat.entries.forEach { assertEquals(it, DateFormat.fromString(it.value)) }
    }

    @Test
    fun `persisted preference value strings are frozen`() {
        // Changing any of these silently resets users to the default after an update.
        assertEquals(listOf("system_default", "light", "dark"), AppTheme.entries.map { it.value })
        assertEquals(listOf("default", "us", "eu", "iso", "full"), DateFormat.entries.map { it.value })
    }

    @Test
    fun `displayName examples match what formatDate produces for 31 Dec 2020 in US locale`() {
        val originalLocale = Locale.getDefault()
        val originalZone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.US)
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val sample = Date.from(LocalDateTime.of(2020, 12, 31, 12, 0).atZone(ZoneId.of("UTC")).toInstant())
            for (format in DateFormat.entries) {
                val example = format.displayName.substringAfter('(').substringBefore(')')
                assertEquals("example in '${format.displayName}'", example, formatDate(sample, format))
            }
        } finally {
            Locale.setDefault(originalLocale)
            TimeZone.setDefault(originalZone)
        }
    }

    @Test
    fun `date format patterns never use the week-year field`() {
        // 'YYYY' (week year) is a classic SimpleDateFormat bug around New Year.
        DateFormat.entries.forEach {
            assertTrue("${it.value}: ${it.formatPattern}", !it.formatPattern.contains("YYYY"))
        }
    }
}
