package com.happypuppy.daycounter.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.ui.theme.SimpleDayCounterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * Characterization of the day count badge and the label under it (PLAN.md CQ-2, UX-7).
 * The badge shows the absolute day count; the label is a separate text node: "Today!", or
 * "day left" / "days left" for future dates, or "day past" / "days past" for past dates.
 */
@RunWith(AndroidJUnit4::class)
class EventCardDayLabelInstrumentedTest {

    @get:Rule
    val rule = createComposeRule()

    private fun noonIn(days: Long): Date =
        Date.from(LocalDate.now().plusDays(days).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant())

    private fun renderCard(daysFromToday: Long) {
        val item = Item(name = "Sample event", itemCategory = "Personal", targetDate = noonIn(daysFromToday))
        rule.setContent {
            SimpleDayCounterTheme(darkTheme = false) { EventCard(item = item, onClick = {}) }
        }
    }

    private fun assertBadgeAndLabel(daysFromToday: Long, badge: String, label: String) {
        renderCard(daysFromToday)
        rule.onNodeWithText(badge, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(label, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun todayShowsZeroAndTodayLabel() = assertBadgeAndLabel(0, "0", "Today!")

    @Test
    fun tomorrowShowsOneAndSingularDayLeft() = assertBadgeAndLabel(1, "1", "day left")

    @Test
    fun twoDaysAheadShowsTwoAndPluralDaysLeft() = assertBadgeAndLabel(2, "2", "days left")

    @Test
    fun yesterdayShowsOneAndSingularDayPast() = assertBadgeAndLabel(-1, "1", "day past")

    @Test
    fun twoDaysAgoShowsTwoAndPluralDaysPast() = assertBadgeAndLabel(-2, "2", "days past")

    @Test
    fun largeFutureCountShowsFourDigitsAndPluralDaysLeft() = assertBadgeAndLabel(1135, "1135", "days left")

    @Test
    fun largePastCountShowsAbsoluteValueWithoutMinusAndPluralDaysPast() =
        assertBadgeAndLabel(-1135, "1135", "days past")
}
