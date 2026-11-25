package com.happypuppy.daycounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.data.preferences.formatDate
import com.happypuppy.daycounter.ui.components.CategorySelector
import com.happypuppy.daycounter.ui.theme.LocalDateFormat
import com.happypuppy.daycounter.util.isSingleGrapheme
import com.happypuppy.daycounter.util.localDateToUtcPickerMillis
import com.happypuppy.daycounter.util.utcPickerMillisToLocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    item: Item,
    dataStoreManager: DataStoreManager,
    onSave: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    onOpenCategories: () -> Unit = {}
) {
    val dateFormat = LocalDateFormat.current

    // Use rememberSaveable to preserve states across configuration changes
    var name by rememberSaveable(item.id) { mutableStateOf(item.name) }
    var emoji by rememberSaveable(item.id) { mutableStateOf(item.emoji) }
    var targetDate by rememberSaveable(item.id) { mutableStateOf(item.targetDate) }
    var selectedCategory by rememberSaveable(item.id) { mutableStateOf(item.itemCategory) }
    var showDatePicker by rememberSaveable(item.id) { mutableStateOf(false) }

    // Opens the picker on a touch tap of the read-only date field.
    val dateInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(dateInteractionSource) {
        dateInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) showDatePicker = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.edit_event_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Event name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.form_event_name)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Emoji field for custom icon
        OutlinedTextField(
            value = emoji,
            onValueChange = { newValue ->
                if (isSingleGrapheme(newValue)) {
                    emoji = newValue
                }
            },
            label = { Text(stringResource(R.string.form_optional_emoji)) },
            placeholder = { Text(stringResource(R.string.form_emoji_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        val selectDateAction = stringResource(R.string.form_select_date_action)
        // Target date field: the read-only field itself is the single focus target and acts as a
        // button that opens the date picker (no overlay).
        OutlinedTextField(
            value = formatDate(targetDate, dateFormat),
            onValueChange = { /* readonly */ },
            label = { Text(stringResource(R.string.form_target_date)) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    role = Role.Button
                    onClick(label = selectDateAction) {
                        showDatePicker = true
                        true
                    }
                },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = stringResource(R.string.form_select_date)
                )
            },
            readOnly = true,
            interactionSource = dateInteractionSource
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Shared CategorySelector component
        CategorySelector(
            selectedCategory = selectedCategory,
            onCategorySelected = { category -> selectedCategory = category },
            dataStoreManager = dataStoreManager,
            modifier = Modifier.fillMaxWidth(),
            onOpenCategories = onOpenCategories
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { onDelete(item) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.common_delete))
            }

            Spacer(modifier = Modifier.weight(0.5f))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val updatedItem = item.copy(
                            name = name.trim(),
                            itemCategory = selectedCategory,
                            targetDate = targetDate,
                            emoji = emoji
                        )
                        onSave(updatedItem)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.edit_event_save))
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDateToUtcPickerMillis(targetDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        targetDate = utcPickerMillisToLocalDate(selectedMillis)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
