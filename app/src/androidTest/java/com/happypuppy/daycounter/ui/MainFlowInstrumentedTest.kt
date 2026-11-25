package com.happypuppy.daycounter.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.daycounter.MainActivity
import com.happypuppy.daycounter.data.database.AppDatabase
import com.happypuppy.daycounter.data.database.CategoryDao
import com.happypuppy.daycounter.data.database.CategoryRepository
import com.happypuppy.daycounter.data.preferences.AppTheme
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.ui.screens.FilterOption
import com.happypuppy.daycounter.ui.screens.FilterState
import com.happypuppy.daycounter.ui.screens.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * End-to-end Compose flows against the real app graph (Koin, Room, DataStore). Every test first
 * waits for the main screen. State is reset in [setUp]/[tearDown]; run on an emulator because it
 * clears the app database.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MainFlowInstrumentedTest {

    @get:Rule
    val rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> =
        createAndroidComposeRule<MainActivity>()

    private val koin get() = GlobalContext.get()

    @Before
    fun setUp() {
        resetState()
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
        store.setDynamicColor(false)
        store.setSortOption(SortOption.DATE_ADDED_DESC)
        store.setFilterState(FilterState())
        koin.get<CategoryRepository>().ensureInitialized()
    }

    /**
     * The FAB label is looked up in the UNMERGED tree: in Material3 1.5.0 the label Text sits under
     * a node with clearAndSetSemantics, so the merged FAB button has no text (see
     * addEventFabExposesAnAccessibleName below and PLAN.md).
     */
    private fun clickAddEventFab() {
        rule.onNode(hasText("Add event"), useUnmergedTree = true).performClick()
    }

    private fun saveAddSheet(expectedCardText: String) {
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        rule.onNodeWithText("Save Event").performScrollTo().performClick()
        rule.waitUntilDoesNotExist(hasText("Add New Event"), timeoutMillis = 5_000)
        rule.waitUntilExactlyOneExists(hasText(expectedCardText), timeoutMillis = 5_000)
    }

    private fun saveEditSheet(expectedCardText: String) {
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        rule.onNodeWithText("Save").performScrollTo().performClick()
        rule.waitUntilDoesNotExist(hasText("Edit Event"), timeoutMillis = 5_000)
        rule.waitUntilExactlyOneExists(hasText(expectedCardText), timeoutMillis = 5_000)
    }

    private fun addEvent(name: String) {
        clickAddEventFab()
        rule.waitUntilExactlyOneExists(hasText("Add New Event"), timeoutMillis = 5_000)
        rule.onNodeWithText("Event Name").performTextInput(name)
        saveAddSheet(name)
    }

    @Test
    fun addEventFabExposesAnAccessibleName() {
        rule.onNode(hasClickAction() and (hasText("Add event") or hasContentDescription("Add event")))
            .assertExists()
    }

    // Read-only fields (UX-6): these are exercised through semantics actions, which is what a
    // TalkBack double-tap performs, not through touch injection.

    /** A node that can be clicked but exposes no text, content description or editable text. */
    private val unlabelledClickTarget = SemanticsMatcher("clickable with no text or description") { node ->
        val config = node.config
        SemanticsActions.OnClick in config &&
            SemanticsProperties.Text !in config &&
            SemanticsProperties.EditableText !in config &&
            SemanticsProperties.ContentDescription !in config
    }

    private fun fieldNode(label: String) = rule.onNode(hasText(label) and hasClickAction())

    private fun semanticClick(label: String) {
        fieldNode(label).performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun openAddSheet() {
        clickAddEventFab()
        rule.waitUntilExactlyOneExists(hasText("Add New Event"), timeoutMillis = 5_000)
    }

    private fun openEditSheet(name: String) {
        addEvent(name)
        rule.onNodeWithText(name).performClick()
        rule.waitUntilExactlyOneExists(hasText("Edit Event"), timeoutMillis = 5_000)
    }

    private fun assertCategoryFieldOpensMenu() {
        fieldNode("Category").assertExists()
        semanticClick("Category")
        rule.waitUntilExactlyOneExists(hasText("Work"), timeoutMillis = 5_000)
        rule.onNodeWithText("Add Custom\u2026").assertExists()
    }

    private fun assertDateFieldOpensPickerAndIsASingleButton() {
        rule.onAllNodes(hasText("Target Date")).assertCountEquals(1)
        fieldNode("Target Date").assert(
            SemanticsMatcher("Button role with a click label") { node ->
                val config = node.config
                config.getOrNull(SemanticsProperties.Role) == Role.Button &&
                    !config.getOrNull(SemanticsActions.OnClick)?.label.isNullOrBlank()
            }
        )
        semanticClick("Target Date")
        rule.waitUntilExactlyOneExists(hasText("Confirm"), timeoutMillis = 5_000)
    }

    private fun assertNoUnlabelledClickTargets() {
        rule.onAllNodes(unlabelledClickTarget).assertCountEquals(0)
    }

    @Test
    fun addSheetCategoryFieldOpensItsMenuFromASemanticClick() {
        openAddSheet()
        assertCategoryFieldOpensMenu()
    }

    @Test
    fun addSheetDateFieldIsOneButtonAndOpensThePicker() {
        openAddSheet()
        assertDateFieldOpensPickerAndIsASingleButton()
    }

    @Test
    fun addSheetDateFieldOpensThePickerFromATouchTap() {
        openAddSheet()
        fieldNode("Target Date").performClick()
        rule.waitUntilExactlyOneExists(hasText("Confirm"), timeoutMillis = 5_000)
    }

    @Test
    fun addSheetCategoryFieldOpensItsMenuFromATouchTap() {
        openAddSheet()
        fieldNode("Category").performClick()
        rule.waitUntilExactlyOneExists(hasText("Work"), timeoutMillis = 5_000)
    }

    @Test
    fun addSheetHasNoUnlabelledClickTargets() {
        openAddSheet()
        assertNoUnlabelledClickTargets()
    }

    @Test
    fun editSheetCategoryFieldOpensItsMenuFromASemanticClick() {
        openEditSheet("Editable")
        assertCategoryFieldOpensMenu()
    }

    @Test
    fun editSheetDateFieldIsOneButtonAndOpensThePicker() {
        openEditSheet("Editable")
        assertDateFieldOpensPickerAndIsASingleButton()
    }

    @Test
    fun editSheetDateFieldOpensThePickerFromATouchTap() {
        openEditSheet("Editable")
        fieldNode("Target Date").performClick()
        rule.waitUntilExactlyOneExists(hasText("Confirm"), timeoutMillis = 5_000)
    }

    @Test
    fun editSheetHasNoUnlabelledClickTargets() {
        openEditSheet("Editable")
        assertNoUnlabelledClickTargets()
    }

    private fun openSettings() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitUntilExactlyOneExists(hasText("Theme"), timeoutMillis = 5_000)
    }

    @Test
    fun settingsDateFormatFieldOpensItsMenuFromASemanticClick() {
        openSettings()
        fieldNode("Format").performScrollTo()
        semanticClick("Format")
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodes(hasText("Example:", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun settingsRowsAreGroupedUnderHeadingsInOrder() {
        openSettings()

        // The settings column is not lazy, so every node has a position even when scrolled out.
        val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        fun top(text: String, heading: Boolean): Float {
            val matcher = if (heading) hasText(text) and isHeading else hasText(text) and !isHeading
            return rule.onNode(matcher).fetchSemanticsNode().positionInRoot.y
        }

        val expectedOrder = listOf(
            "Appearance" to true, "Theme" to false,
            "General" to true, "Vibration" to false,
            "Dates" to true, "Date Display Format" to false,
            "Categories" to true, "Manage Categories" to false,
            "Support" to true, "Rate App" to false
        )
        val tops = expectedOrder.map { (text, heading) -> top(text, heading) }

        assertEquals("headings and rows are in section order", tops.sorted(), tops)
        assertEquals("every position is distinct", tops.size, tops.toSet().size)
    }

    @Test
    fun settingsHasNoUnlabelledClickTargets() {
        openSettings()
        assertNoUnlabelledClickTargets()
    }

    @Test
    fun emptyStateIsShownWhenThereAreNoEvents() {
        rule.onNodeWithText("No Events Yet").assertExists()
    }

    @Test
    fun saveIsDisabledUntilANameIsTyped() {
        clickAddEventFab()
        rule.waitUntilExactlyOneExists(hasText("Add New Event"), timeoutMillis = 5_000)

        rule.onNodeWithText("Save Event").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Event Name").performTextInput("Launch day")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        rule.onNodeWithText("Save Event").performScrollTo().assertIsEnabled()
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
        saveEditSheet("New name")

        rule.onNodeWithText("Old name").assertDoesNotExist()
    }

    @Test
    fun deleteFromEditSheetShowsUndoAndUndoRestoresTheEvent() {
        addEvent("Doomed")

        rule.onNodeWithText("Doomed").performClick()
        rule.waitUntilExactlyOneExists(hasText("Edit Event"), timeoutMillis = 5_000)
        rule.onNodeWithText("Delete").performScrollTo().performClick()

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

    // UX-20: swipe must not be the only way to delete; cards expose custom accessibility actions.
    @Test
    fun deleteCustomActionRemovesTheEventAndShowsUndo() {
        addEvent("Actioned")

        rule.onNodeWithText("Actioned").performCustomAccessibilityActionWithLabel("Delete")

        rule.waitUntilExactlyOneExists(hasText("Undo"), timeoutMillis = 5_000)
        rule.onNodeWithText("\"Actioned\" deleted").assertExists()

        rule.onNodeWithText("Undo").performClick()
        rule.waitUntilExactlyOneExists(hasText("Actioned"), timeoutMillis = 5_000)
    }

    // Edit is reachable without touch through the card's labelled click action (what a TalkBack
    // double-tap performs), and the custom actions list Delete once and no duplicate Edit.
    @Test
    fun editIsReachableWithoutTouchAndTheCardActionsAreNotDuplicated() {
        addEvent("Actioned")

        rule.onNodeWithText("Actioned").assert(
            SemanticsMatcher("one Delete custom action, no custom Edit, click labelled 'Edit event'") { node ->
                val config = node.config
                val customLabels = config.getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label }
                customLabels.count { it == "Delete" } == 1 &&
                    customLabels.none { it == "Edit" } &&
                    config.getOrNull(SemanticsActions.OnClick)?.label == "Edit event"
            }
        )

        rule.onNodeWithText("Actioned").performSemanticsAction(SemanticsActions.OnClick)

        rule.waitUntilExactlyOneExists(hasText("Edit Event"), timeoutMillis = 5_000)
    }

    // UX-12 (snackbar duration only): the Undo must stay reachable well past the 4 s "Short"
    // duration. The snackbar timer runs on the test clock, so time is advanced manually.
    @Test
    fun undoSnackbarIsStillShownSixSecondsAfterDeleteAndUndoRestoresTheEvent() {
        addEvent("Slow undo")

        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Slow undo").performCustomAccessibilityActionWithLabel("Delete")
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Undo").assertExists()

        rule.mainClock.advanceTimeBy(6_000)
        rule.onNodeWithText("Undo").assertExists()

        rule.onNodeWithText("Undo").performClick()
        rule.mainClock.autoAdvance = true
        rule.waitUntilExactlyOneExists(hasText("Slow undo"), timeoutMillis = 5_000)
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
    fun dynamicColorSwitchIsOffByDefaultAndPersistsWhenTurnedOn() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitUntilExactlyOneExists(hasText("Dynamic color"), timeoutMillis = 5_000)

        val toggle = hasText("Dynamic color") and isToggleable()
        rule.onNode(toggle).assertIsOff()
        runBlocking(Dispatchers.IO) { assertEquals(false, koin.get<DataStoreManager>().dynamicColorFlow.first()) }

        rule.onNode(toggle).performClick()

        rule.waitUntil(timeoutMillis = 5_000) {
            runBlocking(Dispatchers.IO) { koin.get<DataStoreManager>().dynamicColorFlow.first() }
        }
        rule.onNode(toggle).assertIsOn()
    }

    @Test
    fun customCategoryCanBeAddedFromManageCategories() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitUntilExactlyOneExists(hasText("Manage Categories"), timeoutMillis = 5_000)
        rule.onNodeWithText("Manage Categories").performClick()
        rule.waitUntilExactlyOneExists(hasText("No custom categories yet. Add one using the + button."), timeoutMillis = 5_000)

        rule.onNodeWithContentDescription("Add Custom Category").performClick()
        rule.onNodeWithText("New Category").performTextInput("travel plans")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Confirm").performClick()

        // Normalized to Title Case by normalizeCategoryName.
        rule.waitUntilExactlyOneExists(hasText("Travel Plans"), timeoutMillis = 5_000)
    }

    @Test
    fun emptyStateCtaOpensAddEventSheet() {
        rule.onNodeWithText("Add Your First Event").performClick()
        rule.waitUntilExactlyOneExists(hasText("Add New Event"), timeoutMillis = 5_000)
    }

    @Test
    fun filteredEmptyStateClearFilterButtonRestoresEvents() {
        addEvent("Today only")

        rule.onNodeWithContentDescription("Filter").performClick()
        rule.onNodeWithText("Past Events").performClick()
        rule.waitUntilExactlyOneExists(hasText("No Events Match Filter"), timeoutMillis = 5_000)
        rule.onNodeWithText("Today only").assertDoesNotExist()

        rule.onNodeWithText("Clear Filter").performClick()
        rule.waitUntilExactlyOneExists(hasText("Today only"), timeoutMillis = 5_000)
    }

    @Test
    fun multiUndoRestoresTwoConsecutivelyDeletedEvents() {
        addEvent("First event")
        addEvent("Second event")

        rule.onNodeWithText("First event").performCustomAccessibilityActionWithLabel("Delete")
        rule.waitUntilExactlyOneExists(hasText("\"First event\" deleted"), timeoutMillis = 5_000)

        rule.onNodeWithText("Second event").performCustomAccessibilityActionWithLabel("Delete")
        rule.waitUntilExactlyOneExists(hasText("\"Second event\" deleted"), timeoutMillis = 5_000)

        rule.onNodeWithText("Undo").performClick()
        rule.waitUntilExactlyOneExists(hasText("Second event"), timeoutMillis = 5_000)
        rule.waitUntilExactlyOneExists(hasText("\"First event\" deleted"), timeoutMillis = 5_000)

        rule.onNodeWithText("Undo").performClick()
        rule.waitUntilExactlyOneExists(hasText("First event"), timeoutMillis = 5_000)
    }

    @Test
    fun sortAndFilterPreferencesPersistInDataStore() {
        rule.onNodeWithContentDescription("Sort").performClick()
        rule.waitUntilExactlyOneExists(hasText("Name (A-Z)"), timeoutMillis = 5_000)
        rule.onNodeWithText("Name (A-Z)").performClick()

        rule.onNodeWithContentDescription("Filter").performClick()
        rule.waitUntilExactlyOneExists(hasText("Future Events"), timeoutMillis = 5_000)
        rule.onNodeWithText("Future Events").performClick()

        val store = koin.get<DataStoreManager>()
        rule.waitUntil(timeoutMillis = 5_000) {
            runBlocking(Dispatchers.IO) {
                store.sortOptionFlow.first() == SortOption.NAME_ASC &&
                    store.filterStateFlow.first() == FilterState(FilterOption.FUTURE)
            }
        }
    }

    @Test
    fun addSheetInlineCustomCategoryPreservesFormAndAutoSelects() {
        openAddSheet()
        rule.onNodeWithText("Event Name").performTextInput("Marathon")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()

        fieldNode("Category").performScrollTo().performClick()
        rule.waitUntilExactlyOneExists(hasText("Add Custom\u2026"), timeoutMillis = 5_000)
        rule.onNodeWithText("Add Custom\u2026").performClick()

        rule.waitUntilExactlyOneExists(hasText("Add Custom Category"), timeoutMillis = 5_000)
        rule.onNodeWithText("New Category").performTextInput("fitness goals")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        rule.onNodeWithText("Add").performClick()

        rule.waitUntilExactlyOneExists(hasText("Fitness Goals"), timeoutMillis = 5_000)
        rule.onNodeWithText("Marathon").assertExists()

        saveAddSheet("Marathon")

        val categoryDao = koin.get<CategoryDao>()
        rule.waitUntil(timeoutMillis = 5_000) {
            runBlocking(Dispatchers.IO) {
                categoryDao.findByNameIgnoreCase("Fitness Goals") != null
            }
        }
    }

    @Test
    fun editSheetInlineCustomCategoryPreservesEditsAndAutoSelects() {
        openEditSheet("Checkup")

        rule.onNodeWithText("Event Name").performTextClearance()
        rule.onNodeWithText("Event Name").performTextInput("Annual checkup")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()

        fieldNode("Category").performScrollTo().performClick()
        rule.waitUntilExactlyOneExists(hasText("Add Custom\u2026"), timeoutMillis = 5_000)
        rule.onNodeWithText("Add Custom\u2026").performClick()

        rule.waitUntilExactlyOneExists(hasText("Add Custom Category"), timeoutMillis = 5_000)
        rule.onNodeWithText("New Category").performTextInput("medical")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        rule.onNodeWithText("Add").performClick()

        rule.waitUntilExactlyOneExists(hasText("Medical"), timeoutMillis = 5_000)
        rule.onNodeWithText("Annual checkup").assertExists()

        saveEditSheet("Annual checkup")
        rule.onNodeWithText("Checkup").assertDoesNotExist()
    }

    @Test
    fun addSheetEmojiSuggestionPopulatesFieldAndSaves() {
        openAddSheet()
        rule.onNodeWithText("Event Name").performTextInput("Birthday")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Birthday emoji").performClick()
        saveAddSheet("Birthday")

        rule.onNodeWithText("\uD83C\uDF82").assertExists()
    }

    @Test
    fun deletingInUseCustomCategoryShowsUsageCountReassignsToPersonalAndUndoRestores() {
        openAddSheet()
        rule.onNodeWithText("Event Name").performTextInput("Tokyo Flight")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        fieldNode("Category").performScrollTo().performClick()
        rule.waitUntilExactlyOneExists(hasText("Add Custom\u2026"), timeoutMillis = 5_000)
        rule.onNodeWithText("Add Custom\u2026").performClick()
        rule.waitUntilExactlyOneExists(hasText("Add Custom Category"), timeoutMillis = 5_000)
        rule.onNodeWithText("New Category").performTextInput("travel")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        rule.onNodeWithText("Add").performClick()
        rule.waitUntilExactlyOneExists(hasText("Travel"), timeoutMillis = 5_000)
        saveAddSheet("Tokyo Flight")

        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitUntilExactlyOneExists(hasText("Manage Categories"), timeoutMillis = 5_000)
        rule.onNodeWithText("Manage Categories").performClick()
        rule.waitUntilExactlyOneExists(hasText("Travel"), timeoutMillis = 5_000)
        rule.onNodeWithText("1 event").assertExists()

        rule.onNodeWithContentDescription("Delete Category").performClick()
        rule.waitUntilExactlyOneExists(
            hasText("\"Travel\" is used by 1 event. Deleting it will move that event to Personal."),
            timeoutMillis = 5_000
        )
        rule.onNodeWithText("Delete").performClick()

        rule.waitUntilExactlyOneExists(hasText("\"Travel\" deleted"), timeoutMillis = 5_000)
        rule.onNodeWithText("Travel").assertDoesNotExist()
        rule.onNodeWithText("Undo").performClick()

        rule.waitUntilExactlyOneExists(hasText("Travel"), timeoutMillis = 5_000)
        rule.onNodeWithText("1 event").assertExists()

        // Delete again and confirm events are reassigned to Personal on the main screen.
        rule.onNodeWithContentDescription("Delete Category").performClick()
        rule.waitUntilExactlyOneExists(hasText("Delete"), timeoutMillis = 5_000)
        rule.onNodeWithText("Delete").performClick()
        rule.waitUntilExactlyOneExists(hasText("\"Travel\" deleted"), timeoutMillis = 5_000)

        rule.onNodeWithContentDescription("Back").performClick()
        rule.waitUntilExactlyOneExists(hasText("Settings"), timeoutMillis = 5_000)
        rule.onNodeWithContentDescription("Back").performClick()
        rule.waitUntilExactlyOneExists(hasText("Tokyo Flight"), timeoutMillis = 5_000)
        rule.onNodeWithText("Personal").assertExists()
    }

    @Test
    fun deleteAllCustomCategoriesShowsTotalUsageCountReassignsAndUndoRestores() {
        openAddSheet()
        rule.onNodeWithText("Event Name").performTextInput("Flight")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        fieldNode("Category").performScrollTo().performClick()
        rule.waitUntilExactlyOneExists(hasText("Add Custom\u2026"), timeoutMillis = 5_000)
        rule.onNodeWithText("Add Custom\u2026").performClick()
        rule.waitUntilExactlyOneExists(hasText("Add Custom Category"), timeoutMillis = 5_000)
        rule.onNodeWithText("New Category").performTextInput("travel")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        rule.onNodeWithText("Add").performClick()
        rule.waitUntilExactlyOneExists(hasText("Travel"), timeoutMillis = 5_000)
        saveAddSheet("Flight")

        openAddSheet()
        rule.onNodeWithText("Event Name").performTextInput("Novel")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        fieldNode("Category").performScrollTo().performClick()
        rule.waitUntilExactlyOneExists(hasText("Add Custom\u2026"), timeoutMillis = 5_000)
        rule.onNodeWithText("Add Custom\u2026").performClick()
        rule.waitUntilExactlyOneExists(hasText("Add Custom Category"), timeoutMillis = 5_000)
        rule.onNodeWithText("New Category").performTextInput("books")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        rule.onNodeWithText("Add").performClick()
        rule.waitUntilExactlyOneExists(hasText("Books"), timeoutMillis = 5_000)
        saveAddSheet("Novel")

        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitUntilExactlyOneExists(hasText("Manage Categories"), timeoutMillis = 5_000)
        rule.onNodeWithText("Manage Categories").performClick()
        rule.waitUntilExactlyOneExists(hasText("Travel"), timeoutMillis = 5_000)

        rule.onNodeWithContentDescription("Delete All Categories").performClick()
        rule.waitUntilExactlyOneExists(
            hasText("Delete all custom categories? 2 events will be moved to Personal."),
            timeoutMillis = 5_000
        )
        rule.onNodeWithText("Delete All").performClick()

        rule.waitUntilExactlyOneExists(hasText("All custom categories deleted"), timeoutMillis = 5_000)
        rule.onNodeWithText("Travel").assertDoesNotExist()
        rule.onNodeWithText("Books").assertDoesNotExist()

        rule.onNodeWithText("Undo").performClick()
        rule.waitUntilExactlyOneExists(hasText("Travel"), timeoutMillis = 5_000)
        rule.onNodeWithText("Books").assertExists()
    }
}
