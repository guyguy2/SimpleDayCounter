package com.happypuppy.daycounter.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.daycounter.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app opens straight to the event list (UX-3): no in-app splash, no timer between the system
 * splash and the first screen. The test clock is paused and moved one frame at a time, so any
 * `delay(...)` before the main screen would show up as virtual time.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class StartupInstrumentedTest {

    @get:Rule
    val rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> =
        createAndroidComposeRule<MainActivity>()

    private fun mainScreenShown() =
        rule.onAllNodes(hasText("Add event"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun startsOnTheMainScreenWithoutWaitingForAnyTimer() {
        rule.mainClock.autoAdvance = false
        var frames = 0
        val deadline = System.currentTimeMillis() + 10_000
        while (!mainScreenShown() && System.currentTimeMillis() < deadline) {
            rule.mainClock.advanceTimeByFrame()
            Thread.sleep(20)
            frames++
        }
        val virtualMs = frames * 16L

        assertTrue("main screen never shown", mainScreenShown())
        assertTrue("main screen needed $virtualMs ms of test clock, expected no timer", virtualMs < 1_000)
    }
}
