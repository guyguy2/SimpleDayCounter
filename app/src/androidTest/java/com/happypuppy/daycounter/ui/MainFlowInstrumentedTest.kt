package com.happypuppy.daycounter.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.daycounter.MainActivity
import com.happypuppy.daycounter.data.database.AppDatabase
import com.happypuppy.daycounter.data.preferences.AppTheme
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * End-to-end Compose flows against the real app graph (Koin, Room, DataStore). The app starts on
 * a fixed 1500 ms splash, so every test first waits for the main screen. State is reset in
 * [setUp]/[tearDown]; run on an emulator because it clears the app database.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MainFlowInstrumentedTest {

    @get:Rule
    val rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> =
        createAndroidComposeRule<MainActivity>()

    private val koin get() = GlobalContext.get()

    private companion object {
        const val SPLASH_MS = 2_000L
    }

    @Before
    fun setUp() {
        resetState()
        // Splash pops itself after a fixed 1500 ms delay that runs on the test (virtual) clock.
        rule.mainClock.advanceTimeBy(SPLASH_MS)
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodes(hasText("Add event"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun tearDown() {
        resetState()
    }

    private fun resetState() = runBlocking(Dispatchers.IO) {
        koin.get<AppDatabase>().clearAllTables()
        val store = koin.get<DataStoreManager>()
        store.clearCustomCategories()
        store.setTheme(AppTheme.SYSTEM_DEFAULT)
    }

    /**
     * The FAB label is looked up in the UNMERGED tree: in Material3 1.5.0 the label Text sits under
     * a node with clearAndSetSemantics, so the merged FAB button has no text (see the @Ignore test
     * below and PLAN.md).
     */
    private fun clickAddEventFab() {
        rule.onNode(hasText("Add event"), useUnmergedTree = true).performClick()
    }

    private fun addEvent(name: String) {
        clickAddEventFab()
        rule.waitUntilExactlyOneExists(hasText("Add New Event"), timeoutMillis = 5_000)
        rule.onNodeWithText("Event Name").performTextInput(name)
        rule.onNodeWithText("Save Event").performClick()
        rule.waitUntilExactlyOneExists(hasText(name), timeoutMillis = 5_000)
    }

    @Test
    fun addEventFabExposesAnAccessibleName() {
        rule.onNode(hasClickAction() and (hasText("Add event") or hasContentDescription("Add event")))
            .assertExists()
    }

    @Test
    fun emptyStateIsShownWhenThereAreNoEvents() {
        rule.onNodeWithText("No Events Yet").assertExists()
    }

    @Test
    fun saveIsDisabledUntilANameIsTyped() {
        clickAddEventFab()
        rule.waitUntilExactlyOneExists(hasText("Add New Event"), timeoutMillis = 5_000)

        rule.onNodeWithText("Save Event").assertIsNotEnabled()
        rule.onNodeWithText("Event Name").performTextInput("Launch day")
        rule.onNodeWithText("Save Event").assertIsEnabled()
    }

    @Test
    fun addedEventAppearsAsTodayCard() {
        addEvent("Launch day")

        rule.onNodeWithText("Launch day").assertExists()
        rule.onNodeWithText("Today!").assertExists()
        rule.onNodeWithText("No Events Yet").assertDoesNotExist()
    }

    @Test
    fun editingAnEventRenamesIt() {
        addEvent("Old name")

        rule.onNodeWithText("Old name").performClick()
        rule.waitUntilExactlyOneExists(hasText("Edit Event"), timeoutMillis = 5_000)
        rule.onNodeWithText("Event Name").performTextClearance()
        rule.onNodeWithText("Event Name").performTextInput("New name")
        rule.onNodeWithText("Save").performClick()

        rule.waitUntilExactlyOneExists(hasText("New name"), timeoutMillis = 5_000)
        rule.onNodeWithText("Old name").assertDoesNotExist()
    }

    @Test
    fun deleteFromEditSheetShowsUndoAndUndoRestoresTheEvent() {
        addEvent("Doomed")

        rule.onNodeWithText("Doomed").performClick()
        rule.waitUntilExactlyOneExists(hasText("Edit Event"), timeoutMillis = 5_000)
        rule.onNodeWithText("Delete").performClick()

        rule.waitUntilExactlyOneExists(hasText("Undo"), timeoutMillis = 5_000)
        rule.onNodeWithText("Undo").performClick()

        rule.waitUntilExactlyOneExists(hasText("Doomed"), timeoutMillis = 5_000)
    }

    @Test
    fun swipeToDeleteShowsUndoSnackbar() {
        addEvent("Swiped")

        rule.onNodeWithText("Swiped").performTouchInput { swipeLeft() }

        rule.waitUntilExactlyOneExists(hasText("Undo"), timeoutMillis = 5_000)
        rule.onNodeWithText("\"Swiped\" deleted").assertExists()
    }

    @Test
    fun pastFilterHidesTodaysEventAndClearingRestoresIt() {
        addEvent("Today only")

        rule.onNodeWithContentDescription("Filter").performClick()
        rule.onNodeWithText("Past Events").performClick()
        rule.waitUntilExactlyOneExists(hasText("No Events Match Filter"), timeoutMillis = 5_000)
        rule.onNodeWithText("Today only").assertDoesNotExist()

        rule.onNodeWithContentDescription("Clear Filter").performClick()
        rule.waitUntilExactlyOneExists(hasText("Today only"), timeoutMillis = 5_000)
    }

    @Test
    fun settingsThemeSelectionIsPersistedAndShownAsSelected() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitUntilExactlyOneExists(hasText("Theme"), timeoutMillis = 5_000)

        rule.onNodeWithText("Dark").performClick()

        rule.waitUntil(timeoutMillis = 5_000) {
            runBlocking(Dispatchers.IO) { koin.get<DataStoreManager>().themeFlow.first() } == AppTheme.DARK
        }
        rule.onNode(hasText("Dark") and isSelectable()).assertIsSelected()
    }

    @Test
    fun customCategoryCanBeAddedFromManageCategories() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitUntilExactlyOneExists(hasText("Manage Categories"), timeoutMillis = 5_000)
        rule.onNodeWithText("Manage Categories").performClick()
        rule.waitUntilExactlyOneExists(hasText("No custom categories yet. Add one using the + button."), timeoutMillis = 5_000)

        rule.onNodeWithContentDescription("Add Custom Category").performClick()
        rule.onNodeWithText("New Category").performTextInput("travel plans")
        rule.onNodeWithContentDescription("Confirm").performClick()

        // Normalized to Title Case by normalizeCategoryName.
        rule.waitUntilExactlyOneExists(hasText("Travel Plans"), timeoutMillis = 5_000)
    }
}
