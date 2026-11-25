package com.happypuppy.daycounter.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.daycounter.ui.components.AddEventBottomSheet
import com.happypuppy.daycounter.ui.components.EventCard
import com.happypuppy.daycounter.ui.components.EventFormState
import com.happypuppy.daycounter.ui.layout.AdaptiveEventFormContainer
import com.happypuppy.daycounter.ui.screens.EventUi
import com.happypuppy.daycounter.ui.theme.SimpleDayCounterTheme
import com.happypuppy.daycounter.util.dayLabelFor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Verifies hardware keyboard and D-pad shortcuts (Wave 6 UX-19):
 * - Enter / NumPadEnter / DirectionCenter on a focused [EventCard] triggers edit.
 * - Delete / Backspace on a focused [EventCard] triggers delete without opening edit.
 * - Escape dismisses open Add/Edit dialog and sheet containers without swallowing text input.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class KeyboardNavigationInstrumentedTest {

    @get:Rule
    val rule = createComposeRule()

    private fun sampleEvent(name: String = "Keyboard Event"): EventUi = EventUi(
        id = 7,
        name = name,
        category = "Work",
        targetDate = LocalDate.of(2026, 6, 20),
        daysLeft = 5,
        dayLabel = dayLabelFor(5),
        emoji = "",
        createdAtMillis = 1_000L
    )

    @Test
    fun enterAndDpadCenterOnFocusedEventCardTriggerEdit() {
        var editCount = 0
        var deleteCount = 0
        rule.setContent {
            SimpleDayCounterTheme(darkTheme = false) {
                EventCard(
                    event = sampleEvent(),
                    onClick = { editCount++ },
                    onDelete = { deleteCount++ }
                )
            }
        }

        val card = rule.onNodeWithText("Keyboard Event")
        card.performSemanticsAction(SemanticsActions.RequestFocus)
        card.assertIsFocused()

        card.performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        assertEquals(1, editCount)

        card.performKeyInput { pressKey(Key.NumPadEnter) }
        rule.waitForIdle()
        assertEquals(2, editCount)

        card.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitForIdle()
        assertEquals(3, editCount)
        assertEquals(0, deleteCount)
    }

    @Test
    fun deleteKeyOnFocusedEventCardTriggersDelete() {
        var editCount = 0
        var deleteCount = 0
        rule.setContent {
            SimpleDayCounterTheme(darkTheme = false) {
                EventCard(
                    event = sampleEvent("Delete Me"),
                    onClick = { editCount++ },
                    onDelete = { deleteCount++ }
                )
            }
        }

        val card = rule.onNodeWithText("Delete Me")
        card.performSemanticsAction(SemanticsActions.RequestFocus)
        card.assertIsFocused()

        card.performKeyInput { pressKey(Key.Delete) }
        rule.waitForIdle()

        assertEquals(1, deleteCount)
        assertEquals(0, editCount)
    }

    @Test
    fun escapeDismissesAddEventDialogWithoutSwallowingTextEditing() {
        var dismissedCount = 0
        rule.setContent {
            SimpleDayCounterTheme(darkTheme = false) {
                var open by remember { mutableStateOf(true) }
                var formState by remember { mutableStateOf(EventFormState.defaultNew()) }
                val sheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded)
                if (open) {
                    AdaptiveEventFormContainer(
                        useDialog = true,
                        sheetState = sheetState,
                        onDismissRequest = {
                            dismissedCount++
                            open = false
                        }
                    ) {
                        AddEventBottomSheet(
                            state = formState,
                            customCategories = emptySet(),
                            onStateChange = { formState = it },
                            onAddCustomCategory = {},
                            onDismiss = {
                                dismissedCount++
                                open = false
                            },
                            onSave = {}
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        val nameField = rule.onNodeWithText("Event Name")
        nameField.performTextInput("Summit")
        rule.onNodeWithText("Summit").assertExists()

        nameField.performKeyInput { pressKey(Key.Escape) }
        rule.waitForIdle()

        assertEquals(1, dismissedCount)
        rule.onNodeWithText("Add New Event").assertDoesNotExist()
    }

    @Test
    fun escapeDismissesAddEventSheet() {
        var dismissedCount = 0
        rule.setContent {
            SimpleDayCounterTheme(darkTheme = false) {
                var open by remember { mutableStateOf(true) }
                var formState by remember { mutableStateOf(EventFormState.defaultNew()) }
                val sheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded)
                if (open) {
                    AdaptiveEventFormContainer(
                        useDialog = false,
                        sheetState = sheetState,
                        onDismissRequest = {
                            dismissedCount++
                            open = false
                        }
                    ) {
                        AddEventBottomSheet(
                            state = formState,
                            customCategories = emptySet(),
                            onStateChange = { formState = it },
                            onAddCustomCategory = {},
                            onDismiss = {
                                dismissedCount++
                                open = false
                            },
                            onSave = {}
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        val nameField = rule.onNodeWithText("Event Name")
        nameField.performSemanticsAction(SemanticsActions.RequestFocus)
        nameField.performKeyInput { pressKey(Key.Escape) }
        rule.waitForIdle()

        assertEquals(1, dismissedCount)
        rule.onNodeWithText("Add New Event").assertDoesNotExist()
    }
}
