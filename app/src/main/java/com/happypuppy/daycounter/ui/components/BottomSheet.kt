package com.happypuppy.daycounter.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.data.preferences.formatDate
import com.happypuppy.daycounter.ui.theme.LocalDateFormat
import com.happypuppy.daycounter.util.isSingleGrapheme
import com.happypuppy.daycounter.util.localDateToUtcPickerMillis
import com.happypuppy.daycounter.util.utcPickerMillisToLocalDate
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventBottomSheet(
    dataStoreManager: DataStoreManager,
    onDismiss: () -> Unit,
    onSave: (name: String, targetDate: Date, category: String, emoji: String) -> Unit,
    onOpenCategories: () -> Unit = {}
) {
    val dateFormat = LocalDateFormat.current

    // Use rememberSaveable to preserve state across configuration changes
    var eventName by rememberSaveable { mutableStateOf("") }
    var eventEmoji by rememberSaveable { mutableStateOf("") }
    var targetDate by rememberSaveable { mutableStateOf(Date()) }
    var selectedCategory by rememberSaveable { mutableStateOf("Personal") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

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
            text = stringResource(R.string.add_event_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Event name field
        OutlinedTextField(
            value = eventName,
            onValueChange = { eventName = it },
            label = { Text(stringResource(R.string.form_event_name)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Emoji field for custom icon
        OutlinedTextField(
            value = eventEmoji,
            onValueChange = { newValue ->
                if (isSingleGrapheme(newValue)) {
                    eventEmoji = newValue
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
                    Icons.Outlined.CalendarMonth,
                    contentDescription = stringResource(R.string.form_select_date)
                )
            },
            readOnly = true,
            interactionSource = dateInteractionSource
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Use the shared CategorySelector component
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
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (eventName.isNotBlank()) {
                        onSave(eventName.trim(), targetDate, selectedCategory, eventEmoji)
                    }
                },
                enabled = eventName.isNotBlank()
            ) {
                Text(stringResource(R.string.add_event_save))
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
