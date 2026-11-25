package com.happypuppy.daycounter.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORIES
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.FINANCE_CATEGORY_ID
import com.happypuppy.daycounter.data.database.HEALTH_CATEGORY_ID
import com.happypuppy.daycounter.data.database.WORK_CATEGORY_ID
import com.happypuppy.daycounter.ui.components.AddEventBottomSheet
import com.happypuppy.daycounter.ui.components.EventFormState
import com.happypuppy.daycounter.ui.components.SWIPE_DELETE_BACKGROUND_TAG
import com.happypuppy.daycounter.ui.layout.ADAPTIVE_EVENT_FORM_DIALOG_TAG
import com.happypuppy.daycounter.ui.layout.ADAPTIVE_EVENT_FORM_SHEET_TAG
import com.happypuppy.daycounter.ui.layout.AdaptiveEventFormContainer
import com.happypuppy.daycounter.ui.layout.rememberAdaptiveLayoutSpec
import com.happypuppy.daycounter.ui.screens.EventUi
import com.happypuppy.daycounter.ui.screens.MAIN_EVENT_GRID_TAG
import com.happypuppy.daycounter.ui.screens.MAIN_EVENT_LIST_TAG
import com.happypuppy.daycounter.ui.screens.MainScreenContent
import com.happypuppy.daycounter.ui.screens.MainScreenUiState
import com.happypuppy.daycounter.ui.theme.SimpleDayCounterTheme
import com.happypuppy.daycounter.util.dayLabelFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.math.abs

/**
 * Verifies Wave 6 adaptive layouts (UX-4) across Compact (`360.dp x 800.dp`), Expanded
 * (`840.dp x 900.dp`), and wide tablet (`1200.dp x 800.dp`) windows against stateless
 * [MainScreenContent] and [AdaptiveEventFormContainer].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AdaptiveLayoutInstrumentedTest {

    @get:Rule
    val rule = createComposeRule()

    private fun sampleEvents(): List<EventUi> {
        val today = LocalDate.of(2026, 6, 15)
        return listOf(
            EventUi(1, "Alpha", DEFAULT_CATEGORY_ID, "Personal", today.plusDays(10), 10, dayLabelFor(10), "", 4_000L),
            EventUi(2, "Bravo", WORK_CATEGORY_ID, "Work", today.plusDays(5), 5, dayLabelFor(5), "", 3_000L),
            EventUi(3, "Charlie", HEALTH_CATEGORY_ID, "Health", today, 0, dayLabelFor(0), "", 2_000L),
            EventUi(4, "Delta", FINANCE_CATEGORY_ID, "Finance", today.minusDays(2), -2, dayLabelFor(-2), "", 1_000L)
        )
    }

    @Test
    fun compactWidthRendersSingleColumnListWithSwipeEnabled() {
        var deletedId: Int? = null
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(360.dp, 800.dp))) {
                SimpleDayCounterTheme(darkTheme = false) {
                    MainScreenContent(
                        uiState = MainScreenUiState(events = sampleEvents(), hasAnyEvents = true),
                        categories = DEFAULT_CATEGORIES,
                        isVibrationEnabled = false,
                        snackbarHostState = remember { SnackbarHostState() },
                        onOpenSettings = {},
                        onAddEvent = {},
                        onEditEvent = {},
                        onDeleteEvent = { deletedId = it },
                        onSelectSortOption = {},
                        onSelectFilterState = {},
                        onClearFilter = {},
                        onDismissError = {}
                    )
                }
            }
        }

        rule.onNodeWithTag(MAIN_EVENT_LIST_TAG).assertExists()
        rule.onNodeWithTag(MAIN_EVENT_GRID_TAG).assertDoesNotExist()

        val alphaBounds = rule.onNodeWithText("Alpha").fetchSemanticsNode().boundsInRoot
        val bravoBounds = rule.onNodeWithText("Bravo").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Compact cards should stack vertically (Alpha top=${alphaBounds.top}, Bravo top=${bravoBounds.top})",
            bravoBounds.top > alphaBounds.bottom
        )
        assertTrue(
            "Compact swipe backgrounds should be present",
            rule.onAllNodesWithTag(SWIPE_DELETE_BACKGROUND_TAG, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        )

        rule.onNodeWithText("Alpha").performTouchInput { swipeLeft() }
        rule.waitUntil(timeoutMillis = 5_000) { deletedId == 1 }
        assertEquals(1, deletedId)
    }

    @Test
    fun expandedWidthRendersTwoColumnGridWithCappedWidthAndSwipeDisabled() {
        var overriddenDensity = 1f
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(840.dp, 900.dp))) {
                overriddenDensity = LocalDensity.current.density
                SimpleDayCounterTheme(darkTheme = false) {
                    MainScreenContent(
                        uiState = MainScreenUiState(events = sampleEvents(), hasAnyEvents = true),
                        categories = DEFAULT_CATEGORIES,
                        isVibrationEnabled = false,
                        snackbarHostState = remember { SnackbarHostState() },
                        onOpenSettings = {},
                        onAddEvent = {},
                        onEditEvent = {},
                        onDeleteEvent = {},
                        onSelectSortOption = {},
                        onSelectFilterState = {},
                        onClearFilter = {},
                        onDismissError = {}
                    )
                }
            }
        }

        rule.onNodeWithTag(MAIN_EVENT_GRID_TAG).assertExists()
        rule.onNodeWithTag(MAIN_EVENT_LIST_TAG).assertDoesNotExist()
        assertEquals(
            "Swipe backgrounds must be absent on multi-column grid cells",
            0,
            rule.onAllNodesWithTag(SWIPE_DELETE_BACKGROUND_TAG, useUnmergedTree = true)
                .fetchSemanticsNodes().size
        )

        val alphaBounds = rule.onNodeWithText("Alpha").fetchSemanticsNode().boundsInRoot
        val bravoBounds = rule.onNodeWithText("Bravo").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "First two grid cards should share a row (Alpha top=${alphaBounds.top}, Bravo top=${bravoBounds.top})",
            abs(alphaBounds.top - bravoBounds.top) < 2f
        )
        assertTrue(
            "Second card should sit to the right of the first card",
            bravoBounds.left > alphaBounds.right
        )

        val gridWidthDp = rule.onNodeWithTag(MAIN_EVENT_GRID_TAG)
            .fetchSemanticsNode().boundsInRoot.width / overriddenDensity
        assertTrue("Grid width ($gridWidthDp dp) must be capped at 840dp", gridWidthDp <= 841f)
    }

    @Test
    fun tabletWidthCapsGridAt840DpAndRendersCardsSideBySide() {
        var overriddenDensity = 1f
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(1200.dp, 800.dp))) {
                overriddenDensity = LocalDensity.current.density
                SimpleDayCounterTheme(darkTheme = false) {
                    MainScreenContent(
                        uiState = MainScreenUiState(events = sampleEvents(), hasAnyEvents = true),
                        categories = DEFAULT_CATEGORIES,
                        isVibrationEnabled = false,
                        snackbarHostState = remember { SnackbarHostState() },
                        onOpenSettings = {},
                        onAddEvent = {},
                        onEditEvent = {},
                        onDeleteEvent = {},
                        onSelectSortOption = {},
                        onSelectFilterState = {},
                        onClearFilter = {},
                        onDismissError = {}
                    )
                }
            }
        }

        rule.onNodeWithTag(MAIN_EVENT_GRID_TAG).assertExists()
        val gridBounds = rule.onNodeWithTag(MAIN_EVENT_GRID_TAG).fetchSemanticsNode().boundsInRoot
        val gridWidthDp = gridBounds.width / overriddenDensity
        assertTrue(
            "Grid width ($gridWidthDp dp) on 1200dp screen must be capped at <= 840dp",
            gridWidthDp <= 841f
        )

        val alphaBounds = rule.onNodeWithText("Alpha").fetchSemanticsNode().boundsInRoot
        val bravoBounds = rule.onNodeWithText("Bravo").fetchSemanticsNode().boundsInRoot
        assertTrue(abs(alphaBounds.top - bravoBounds.top) < 2f)
        assertTrue(bravoBounds.left > alphaBounds.right)
    }

    @Test
    fun wideWindowPresentsAddEventInCenteredDialogCappedAt560Dp() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(840.dp, 900.dp))) {
                SimpleDayCounterTheme(darkTheme = false) {
                    val layoutSpec = rememberAdaptiveLayoutSpec()
                    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded)
                    AdaptiveEventFormContainer(
                        useDialog = layoutSpec.useDialogForEventForm,
                        sheetState = sheetState,
                        onDismissRequest = {}
                    ) {
                        AddEventBottomSheet(
                            state = EventFormState.defaultNew(),
                            categories = DEFAULT_CATEGORIES,
                            onStateChange = {},
                            onAddCustomCategory = { null },
                            onDismiss = {},
                            onSave = {}
                        )
                    }
                }
            }
        }

        val dialogNode = rule.onNodeWithTag(ADAPTIVE_EVENT_FORM_DIALOG_TAG)
            .assertExists()
            .fetchSemanticsNode()
        rule.onNodeWithTag(ADAPTIVE_EVENT_FORM_SHEET_TAG).assertDoesNotExist()
        rule.onNodeWithText("Add New Event").assertExists()

        // Dialog opens in its own Android window, so normalize pixels by the dialog's own density.
        val dialogWidthDp = dialogNode.boundsInRoot.width / dialogNode.layoutInfo.density.density
        assertTrue(
            "Wide event form dialog ($dialogWidthDp dp) must be capped at <= 560dp",
            dialogWidthDp <= 561f
        )
    }

    @Test
    fun compactPortraitPresentsAddEventInModalBottomSheet() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(360.dp, 800.dp))) {
                SimpleDayCounterTheme(darkTheme = false) {
                    val layoutSpec = rememberAdaptiveLayoutSpec()
                    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded)
                    AdaptiveEventFormContainer(
                        useDialog = layoutSpec.useDialogForEventForm,
                        sheetState = sheetState,
                        onDismissRequest = {}
                    ) {
                        AddEventBottomSheet(
                            state = EventFormState.defaultNew(),
                            categories = DEFAULT_CATEGORIES,
                            onStateChange = {},
                            onAddCustomCategory = { null },
                            onDismiss = {},
                            onSave = {}
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(ADAPTIVE_EVENT_FORM_SHEET_TAG).assertExists()
        rule.onNodeWithTag(ADAPTIVE_EVENT_FORM_DIALOG_TAG).assertDoesNotExist()
        rule.onNodeWithText("Add New Event").assertExists()
    }
}
