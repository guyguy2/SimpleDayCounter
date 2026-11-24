package com.happypuppy.daycounter.ui.screens

import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.util.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

class EventErrorTest {

    @Test
    fun `each error maps to its own string resource without arguments`() {
        assertEquals(UiText.StringResource(R.string.main_error_load_failed), EventError.LoadFailed.message)
        assertEquals(UiText.StringResource(R.string.main_error_save_failed), EventError.SaveFailed.message)
        assertEquals(UiText.StringResource(R.string.main_error_update_failed), EventError.UpdateFailed.message)
        assertEquals(UiText.StringResource(R.string.main_error_delete_failed), EventError.DeleteFailed.message)
        assertEquals(UiText.StringResource(R.string.main_error_restore_failed), EventError.RestoreFailed.message)
    }

    @Test
    fun `error messages are distinct`() {
        val messages = listOf(
            EventError.LoadFailed, EventError.SaveFailed, EventError.UpdateFailed,
            EventError.DeleteFailed, EventError.RestoreFailed
        ).map { it.message }
        assertEquals(messages.size, messages.toSet().size)
    }
}
