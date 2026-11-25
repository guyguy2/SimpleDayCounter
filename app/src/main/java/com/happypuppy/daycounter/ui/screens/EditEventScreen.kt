package com.happypuppy.daycounter.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.ui.components.EventForm
import com.happypuppy.daycounter.ui.components.EventFormState

/**
 * Thin wrapper delegating Edit Event sheet content to [EventForm] with hoisted form state.
 */
@Composable
fun EditEventScreen(
    item: Item,
    state: EventFormState,
    categories: List<Category>,
    onStateChange: (EventFormState) -> Unit,
    onAddCustomCategory: suspend (String) -> Category?,
    onSave: (Item) -> Unit,
    onDelete: (Item) -> Unit
) {
    EventForm(
        title = stringResource(R.string.edit_event_title),
        state = state,
        categories = categories,
        onStateChange = onStateChange,
        onAddCustomCategory = onAddCustomCategory,
        primaryActionLabel = stringResource(R.string.edit_event_save),
        onPrimaryAction = {
            if (state.canSave) {
                onSave(
                    item.copy(
                        name = state.trimmedName,
                        categoryId = state.selectedCategoryId,
                        targetDate = state.targetDate,
                        emoji = state.emoji
                    )
                )
            }
        },
        onDelete = { onDelete(item) }
    )
}
