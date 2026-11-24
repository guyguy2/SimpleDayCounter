package com.happypuppy.daycounter.util

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.ui.screens.EventError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiTextInstrumentedTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun resolvesDynamicStringResourceAndPluralWithContext() {
        assertEquals("as typed", UiText.DynamicString("as typed").asString(context))
        assertEquals("Today!", UiText.StringResource(R.string.event_card_today).asString(context))
        assertEquals("day left", UiText.PluralResource(R.plurals.event_card_days_left, 1).asString(context))
        assertEquals("days left", UiText.PluralResource(R.plurals.event_card_days_left, 2).asString(context))
    }

    @Test
    fun resolvesInCompositionAndShowsTheTypedErrorText() {
        rule.setContent { Text(EventError.SaveFailed.message.asString()) }

        rule.onNodeWithText("Failed to save event").assertIsDisplayed()
    }
}
