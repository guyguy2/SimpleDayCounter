package com.happypuppy.daycounter.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.happypuppy.daycounter.R

/**
 * Thin wrapper delegating Add Event sheet content to [EventForm] with hoisted form state.
 */
@Composable
fun AddEventBottomSheet(
    state: EventFormState,
    customCategories: Set<String>,
    onStateChange: (EventFormState) -> Unit,
    onAddCustomCategory: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    EventForm(
        title = stringResource(R.string.add_event_title),
        state = state,
        customCategories = customCategories,
        onStateChange = onStateChange,
        onAddCustomCategory = onAddCustomCategory,
        primaryActionLabel = stringResource(R.string.add_event_save),
        onPrimaryAction = onSave,
        onCancel = onDismiss
    )
}
