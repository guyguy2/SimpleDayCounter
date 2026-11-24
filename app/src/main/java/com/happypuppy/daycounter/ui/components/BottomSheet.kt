package com.happypuppy.daycounter.ui.components

import android.util.Log
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.happypuppy.daycounter.BuildConfig
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
    navController: NavController? = null
) {
    val dateFormat = LocalDateFormat.current

    // Use rememberSaveable to preserve state across configuration changes
    var eventName by rememberSaveable { mutableStateOf("") }
    var eventEmoji by rememberSaveable { mutableStateOf("") }
    var targetDate by rememberSaveable { mutableStateOf(Date()) }
    var selectedCategory by rememberSaveable { mutableStateOf("Personal") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showDatePicker) {
        if (BuildConfig.DEBUG) Log.d("DatePicker", "Date picker visibility changed to: $showDatePicker")
    }

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
            text = "Add New Event",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Event name field
        OutlinedTextField(
            value = eventName,
            onValueChange = { eventName = it },
            label = { Text("Event Name") },
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
            label = { Text("Optional Emoji") },
            placeholder = { Text("Enter an emoji character") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Target date field: the read-only field itself is the single focus target and acts as a
        // button that opens the date picker (no overlay).
        OutlinedTextField(
            value = formatDate(targetDate, dateFormat),
            onValueChange = { /* readonly */ },
            label = { Text("Target Date") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    role = Role.Button
                    onClick(label = "Select date") {
                        showDatePicker = true
                        true
                    }
                },
            trailingIcon = {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = "Select Date"
                )
            },
            readOnly = true,
            interactionSource = dateInteractionSource
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Use the shared CategorySelector component with NavController
        CategorySelector(
            selectedCategory = selectedCategory,
            onCategorySelected = { category -> selectedCategory = category },
            dataStoreManager = dataStoreManager,
            modifier = Modifier.fillMaxWidth(),
            navController = navController
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (eventName.isNotBlank()) {
                        if (BuildConfig.DEBUG) Log.d("AddEvent", "Saving event: $eventName, date: $targetDate, category: $selectedCategory, emoji: $eventEmoji")
                        onSave(eventName.trim(), targetDate, selectedCategory, eventEmoji)
                    }
                },
                enabled = eventName.isNotBlank()
            ) {
                Text("Save Event")
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDateToUtcPickerMillis(targetDate)
        )
        if (BuildConfig.DEBUG) Log.d("DatePicker", "Showing date picker dialog")
        DatePickerDialog(
            onDismissRequest = {
                if (BuildConfig.DEBUG) Log.d("DatePicker", "Date picker dismissed")
                showDatePicker = false
            },
            confirmButton = {
                TextButton(onClick = {
                    if (BuildConfig.DEBUG) Log.d("DatePicker", "Date selection confirmed")
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        targetDate = utcPickerMillisToLocalDate(selectedMillis)
                        if (BuildConfig.DEBUG) Log.d("DatePicker", "New date set: $targetDate")
                    }
                    showDatePicker = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (BuildConfig.DEBUG) Log.d("DatePicker", "Date picker cancelled")
                    showDatePicker = false
                }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
