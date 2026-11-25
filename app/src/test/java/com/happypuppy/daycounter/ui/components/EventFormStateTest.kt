package com.happypuppy.daycounter.ui.components

import androidx.compose.runtime.saveable.SaverScope
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.WORK_CATEGORY_ID
import com.happypuppy.daycounter.util.isSingleGrapheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class EventFormStateTest {

    private val scope = SaverScope { true }

    private val utc = ZoneId.of("UTC")
    private val tokyo = ZoneId.of("Asia/Tokyo")

    private fun roundTrip(state: EventFormState): EventFormState? {
        val saved = with(EventFormStateSaver) { scope.save(state) }
        assertNotNull("saver must produce a value for $state", saved)
        return EventFormStateSaver.restore(saved!!)
    }

    @Test
    fun `defaultNew uses fixed clock date in clock zone`() {
        val instant = Instant.parse("2026-11-25T23:30:00Z")
        // In Tokyo (+9), 2026-11-25T23:30Z is already 2026-11-26.
        val tokyoClock = Clock.fixed(instant, tokyo)
        val state = EventFormState.defaultNew(tokyoClock)
        assertEquals(LocalDate.of(2026, 11, 26), state.targetDate)
        assertEquals(DEFAULT_CATEGORY_ID, state.selectedCategoryId)
    }

    @Test
    fun `blank and whitespace names cannot save`() {
        val empty = EventFormState(name = "")
        val spaces = EventFormState(name = "   \t\n  ")
        assertFalse(empty.isNameTooLong)
        assertFalse(empty.canSave)
        assertFalse(spaces.isNameTooLong)
        assertFalse(spaces.canSave)
    }

    @Test
    fun `trimmed valid name up to max length can save`() {
        val padded = EventFormState(name = "  Launch Day  ")
        assertEquals("Launch Day", padded.trimmedName)
        assertFalse(padded.isNameTooLong)
        assertTrue(padded.canSave)

        val exactMax = EventFormState(name = "a".repeat(MAX_EVENT_NAME_LENGTH))
        assertFalse(exactMax.isNameTooLong)
        assertTrue(exactMax.canSave)
    }

    @Test
    fun `name over max length marks too long and disables save`() {
        val overMax = EventFormState(name = "a".repeat(MAX_EVENT_NAME_LENGTH + 1))
        assertTrue(overMax.isNameTooLong)
        assertFalse(overMax.canSave)
    }

    @Test
    fun `withEmoji accepts single grapheme or empty and rejects multi-grapheme`() {
        val base = EventFormState(name = "Party")
        val withBirthday = base.withEmoji("\uD83C\uDF82")
        assertEquals("\uD83C\uDF82", withBirthday.emoji)
        assertTrue(withBirthday.isEmojiValid)
        assertTrue(withBirthday.canSave)

        val withZwj = base.withEmoji("\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67")
        assertEquals("\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67", withZwj.emoji)
        assertTrue(withZwj.isEmojiValid)

        val rejected = withBirthday.withEmoji("ab")
        assertEquals("\uD83C\uDF82", rejected.emoji)

        val cleared = withBirthday.withEmoji("")
        assertEquals("", cleared.emoji)
        assertTrue(cleared.isEmojiValid)

        val invalidDirect = base.copy(emoji = "\uD83C\uDF82\uD83C\uDF89")
        assertFalse(invalidDirect.isEmojiValid)
        assertFalse(invalidDirect.canSave)
    }

    @Test
    fun `all default emoji suggestions are single graphemes`() {
        for (suggestion in defaultEmojiSuggestions) {
            assertTrue(
                "Expected single grapheme for ${suggestion.emoji}",
                isSingleGrapheme(suggestion.emoji)
            )
        }
    }

    @Test
    fun `toggleEmojiSuggestion selects and toggles off`() {
        val initial = EventFormState(name = "Trip")
        val selected = initial.toggleEmojiSuggestion("\u2708\uFE0F")
        assertEquals("\u2708\uFE0F", selected.emoji)

        val switched = selected.toggleEmojiSuggestion("\uD83C\uDF89")
        assertEquals("\uD83C\uDF89", switched.emoji)

        val toggledOff = switched.toggleEmojiSuggestion("\uD83C\uDF89")
        assertEquals("", toggledOff.emoji)
    }

    @Test
    fun `quick date presets resolve to exact LocalDate offsets`() {
        val today = LocalDate.of(2026, 6, 15)
        val expectedDates = mapOf(
            QuickDatePreset.TODAY to LocalDate.of(2026, 6, 15),
            QuickDatePreset.TOMORROW to LocalDate.of(2026, 6, 16),
            QuickDatePreset.NEXT_WEEK to LocalDate.of(2026, 6, 22),
            QuickDatePreset.NEXT_MONTH to LocalDate.of(2026, 7, 15)
        )

        for ((preset, expectedLocalDate) in expectedDates) {
            assertEquals("date mismatch for $preset", expectedLocalDate, preset.resolveDate(today))
        }
    }

    @Test
    fun `fromItem and defaultNew populate expected fields`() {
        val fixedClock = Clock.fixed(Instant.parse("2026-08-10T15:00:00Z"), utc)
        val defaultState = EventFormState.defaultNew(fixedClock)
        assertEquals("", defaultState.name)
        assertEquals("", defaultState.emoji)
        assertEquals(LocalDate.of(2026, 8, 10), defaultState.targetDate)
        assertEquals(DEFAULT_CATEGORY_ID, defaultState.selectedCategoryId)

        val target = LocalDate.of(2026, 12, 25)
        val item = Item(
            id = 7,
            name = "Holiday",
            categoryId = "family-uuid",
            createdAt = Date(1_000L),
            targetDate = target,
            emoji = "\uD83C\uDF89"
        )
        val fromItem = EventFormState.fromItem(item)
        assertEquals("Holiday", fromItem.name)
        assertEquals("\uD83C\uDF89", fromItem.emoji)
        assertEquals(target, fromItem.targetDate)
        assertEquals("family-uuid", fromItem.selectedCategoryId)
    }

    @Test
    fun `EventFormStateSaver round-trips populated and default states`() {
        val defaultState = EventFormState(
            targetDate = LocalDate.of(2026, 6, 15)
        )
        assertEquals(defaultState, roundTrip(defaultState))

        val populated = EventFormState(
            name = "Anniversary",
            emoji = "\u2764\uFE0F",
            targetDate = LocalDate.of(2026, 9, 1),
            selectedCategoryId = "fitness-uuid"
        )
        assertEquals(populated, roundTrip(populated))
    }

    @Test
    fun `EventFormStateSaver saved form uses Bundle-friendly primitives`() {
        val date = LocalDate.of(2026, 9, 1)
        val saved = with(EventFormStateSaver) {
            scope.save(
                EventFormState(
                    name = "Trip",
                    emoji = "\u2708\uFE0F",
                    targetDate = date,
                    selectedCategoryId = WORK_CATEGORY_ID
                )
            )
        } as List<*>
        assertEquals(listOf("Trip", "\u2708\uFE0F", "2026-09-01", WORK_CATEGORY_ID), saved)
    }

    @Test
    fun `EventFormStateSaver tolerates incomplete or malformed saved lists`() {
        val emptyRestored = EventFormStateSaver.restore(emptyList<Any>())
        assertNotNull(emptyRestored)
        assertEquals("", emptyRestored!!.name)
        assertEquals("", emptyRestored.emoji)
        assertEquals(DEFAULT_CATEGORY_ID, emptyRestored.selectedCategoryId)

        val shortRestored = EventFormStateSaver.restore(listOf("Partial"))
        assertNotNull(shortRestored)
        assertEquals("Partial", shortRestored!!.name)
        assertEquals("", shortRestored.emoji)
        assertEquals(DEFAULT_CATEGORY_ID, shortRestored.selectedCategoryId)

        val wrongTypesRestored = EventFormStateSaver.restore(listOf(42, "ab", "not-a-date", ""))
        assertNotNull(wrongTypesRestored)
        assertEquals("", wrongTypesRestored!!.name)
        assertEquals("", wrongTypesRestored.emoji)
        assertEquals(DEFAULT_CATEGORY_ID, wrongTypesRestored.selectedCategoryId)
    }
}
