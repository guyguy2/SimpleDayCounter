package com.happypuppy.daycounter.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.happypuppy.daycounter.BuildConfig
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.data.preferences.DateFormat
import com.happypuppy.daycounter.ui.screens.rememberSaveableSheetState
import org.koin.compose.koinInject
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventBottomSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, targetDate: Date, category: String, emoji: String) -> Unit,
    navController: NavController? = null
) {
    val dataStoreManager = koinInject<DataStoreManager>()
    val dateFormat by dataStoreManager.dateFormatFlow.collectAsState(initial = DateFormat.DEFAULT)

    // Use rememberSaveable to preserve state across configuration changes
    var eventName by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var eventEmoji by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    val calendar = Calendar.getInstance()
    val initialTargetDate = calendar.time
    var targetDate by rememberSaveable { androidx.compose.runtime.mutableStateOf(initialTargetDate) }
    var selectedCategory by rememberSaveable { androidx.compose.runtime.mutableStateOf("Personal") }

    // Use BottomSheetStateSaver for the date picker state
    val (showDatePicker, setShowDatePicker) = rememberSaveableSheetState("addEventDatePicker", false)
    var isAdding by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

    // Debug statement to confirm when date picker state changes
    LaunchedEffect(showDatePicker) {
        if (BuildConfig.DEBUG) Log.d("DatePicker", "Date picker visibility changed to: $showDatePicker")
    }

    // Initialize date picker with current time
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = targetDate.time
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                // Only take the first emoji/character or clear it
                if (newValue.isEmpty() || newValue.length == 1 || newValue.codePointCount(0, newValue.length) == 1) {
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

        // Target date field with the OutlinedTextField but fully clickable
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = dataStoreManager.formatDate(targetDate, dateFormat),
                onValueChange = { /* readonly */ },
                label = { Text("Target Date") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = "Select Date"
                    )
                },
                readOnly = true
            )

            // Invisible clickable overlay that sits on top of the field
            Box(
                modifier = Modifier
                    .matchParentSize() // This makes it exactly the same size as its parent Box
                    .clickable(
                        onClick = {
                            if (BuildConfig.DEBUG) Log.d("DatePicker", "Date field clicked, opening picker")
                            setShowDatePicker(true)
                        }
                    )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Use the shared CategorySelector component with NavController
        CategorySelector(
            selectedCategory = selectedCategory,
            onCategorySelected = { category -> selectedCategory = category },
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
                        onSave(eventName, targetDate, selectedCategory, eventEmoji)
                        onDismiss()
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
        if (BuildConfig.DEBUG) Log.d("DatePicker", "Showing date picker dialog")
        DatePickerDialog(
            onDismissRequest = {
                if (BuildConfig.DEBUG) Log.d("DatePicker", "Date picker dismissed")
                setShowDatePicker(false)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (BuildConfig.DEBUG) Log.d("DatePicker", "Date selection confirmed")
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        // This fixes the timezone issue by extracting just the year, month, and day
                        // from the selected timestamp and building a new date with those components

                        // Get UTC calendar
                        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        utcCalendar.timeInMillis = selectedMillis

                        // Extract year, month, day
                        val year = utcCalendar.get(Calendar.YEAR)
                        val month = utcCalendar.get(Calendar.MONTH)
                        val day = utcCalendar.get(Calendar.DAY_OF_MONTH)

                        // Create local calendar with the same date components
                        val localCalendar = Calendar.getInstance()
                        localCalendar.set(Calendar.YEAR, year)
                        localCalendar.set(Calendar.MONTH, month)
                        localCalendar.set(Calendar.DAY_OF_MONTH, day)
                        // Set to noon to avoid any timezone issues
                        localCalendar.set(Calendar.HOUR_OF_DAY, 12)
                        localCalendar.set(Calendar.MINUTE, 0)
                        localCalendar.set(Calendar.SECOND, 0)
                        localCalendar.set(Calendar.MILLISECOND, 0)

                        targetDate = localCalendar.time
                        if (BuildConfig.DEBUG) Log.d("DatePicker", "New date set: $targetDate")
                    }
                    setShowDatePicker(false)
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (BuildConfig.DEBUG) Log.d("DatePicker", "Date picker cancelled")
                    setShowDatePicker(false)
                }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}