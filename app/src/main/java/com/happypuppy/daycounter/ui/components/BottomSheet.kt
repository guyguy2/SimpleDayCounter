package com.happypuppy.daycounter.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.database.Category

/**
 * Thin wrapper delegating Add Event sheet content to [EventForm] with hoisted form state.
 */
@Composable
fun AddEventBottomSheet(
    state: EventFormState,
    categories: List<Category>,
    onStateChange: (EventFormState) -> Unit,
    onAddCustomCategory: suspend (String) -> Category?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    EventForm(
        title = stringResource(R.string.add_event_title),
        state = state,
        categories = categories,
        onStateChange = onStateChange,
        onAddCustomCategory = onAddCustomCategory,
        primaryActionLabel = stringResource(R.string.add_event_save),
        onPrimaryAction = onSave,
        onCancel = onDismiss
    )
}
