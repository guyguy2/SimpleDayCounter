package com.happypuppy.daycounter.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.happypuppy.daycounter.BuildConfig
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.data.preferences.formatDate
import com.happypuppy.daycounter.ui.components.CategorySelector
import com.happypuppy.daycounter.ui.theme.LocalDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    item: Item,
    dataStoreManager: DataStoreManager,
    onSave: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    navController: NavController? = null
) {
    val dateFormat = LocalDateFormat.current

    // Use rememberSaveable to preserve states across configuration changes
    var name by rememberSaveable(item.id) { mutableStateOf(item.name) }
    var emoji by rememberSaveable(item.id) { mutableStateOf(item.emoji) }
    var targetDate by rememberSaveable(item.id) { mutableStateOf(item.targetDate) }
    var selectedCategory by rememberSaveable(item.id) { mutableStateOf(item.itemCategory) }
    var showDatePicker by rememberSaveable(item.id) { mutableStateOf(false) }

    // Log state for debugging
    LaunchedEffect(showDatePicker) {
        if (BuildConfig.DEBUG) Log.d("EditEventScreen", "Date picker visibility for item ${item.id}: $showDatePicker")
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = targetDate.time
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Edit Event",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Event name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Event Name") },
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
                // Only take the first emoji/character or clear it
                if (newValue.isEmpty() || newValue.length == 1 || newValue.codePointCount(
                        0,
                        newValue.length
                    ) == 1
                ) {
                    emoji = newValue
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

        // Target date field - using the same approach as in AddEventBottomSheet
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = formatDate(targetDate, dateFormat),
                onValueChange = { /* readonly */ },
                label = { Text("Target Date") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "Select Date"
                    )
                },
                readOnly = true
            )

            // Invisible clickable overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        onClick = {
                            if (BuildConfig.DEBUG) Log.d("DatePicker", "Edit screen date field clicked, opening picker")
                            showDatePicker = true
                        }
                    )
            )
        }

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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { onDelete(item) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Delete")
            }

            Spacer(modifier = Modifier.weight(0.5f))

            Button(
                onClick = {
                    // Create updated item with the emoji
                    val updatedItem = item.copy(
                        name = name,
                        itemCategory = selectedCategory,
                        targetDate = targetDate,
                        emoji = emoji
                    )
                    onSave(updatedItem)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        // Fix timezone issue by extracting just the year, month, and day
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
                        if (BuildConfig.DEBUG) Log.d("DatePicker", "Edit screen: New date set: $targetDate")
                    }
                    showDatePicker = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}