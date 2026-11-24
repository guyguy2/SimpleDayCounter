package com.happypuppy.daycounter.data.preferences

import com.happypuppy.daycounter.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The persisted `value` strings are an on-disk contract (DataStore + cloud backup), so they must be
 * unique and round-trip. The date format display names are string resources with an embedded
 * example; this test keeps those examples honest.
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
        // "system" was added after the five patterns; the five existing values must never change.
        assertEquals(listOf("system", "default", "us", "eu", "iso", "full"), DateFormat.entries.map { it.value })
    }

    @Test
    fun `display name examples match what formatDate produces for 31 Dec 2020 in US locale`() {
        val originalLocale = Locale.getDefault()
        val originalZone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.US)
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val sample = Date.from(LocalDateTime.of(2020, 12, 31, 12, 0).atZone(ZoneId.of("UTC")).toInstant())
            val names = dateFormatDisplayNames()
            for (format in DateFormat.entries) {
                // Convention: the display name of format "iso" is the string resource date_format_iso.
                val displayName = names.getValue("date_format_${format.value}")
                // The system option has no fixed example: its text follows the device locale.
                if (format == DateFormat.SYSTEM) continue
                val example = displayName.substringAfter('(').substringBefore(')')
                assertEquals("example in '$displayName'", example, formatDate(sample, format))
            }
        } finally {
            Locale.setDefault(originalLocale)
            TimeZone.setDefault(originalZone)
        }
    }

    @Test
    fun `every date format has its own display name resource`() {
        assertEquals(DateFormat.entries.size, DateFormat.entries.map { it.displayNameRes }.toSet().size)
    }

    @Test
    fun `each date format points at the display name resource named after its value`() {
        // A swapped reference (US showing the EU name) must fail: look the id up by name.
        for (format in DateFormat.entries) {
            val expected = R.string::class.java.getField("date_format_${format.value}").getInt(null)
            assertEquals("display name of '${format.value}'", expected, format.displayNameRes)
        }
    }

    /** Reads the English display names straight from strings.xml (the JVM has no Android resources). */
    private fun dateFormatDisplayNames(): Map<String, String> {
        val file = File("src/main/res/values/strings.xml")
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            .getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) }
            .associate { it.attributes.getNamedItem("name").nodeValue to it.textContent }
            .filterKeys { it.startsWith("date_format_") }
    }

    @Test
    fun `date format patterns never use the week-year field`() {
        // 'YYYY' (week year) is a classic SimpleDateFormat bug around New Year.
        DateFormat.entries.forEach {
            assertTrue("${it.value}: ${it.formatPattern}", it.formatPattern?.contains("YYYY") != true)
        }
    }
}
