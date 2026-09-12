package com.happypuppy.daycounter.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.happypuppy.daycounter.BuildConfig
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * A reusable component for adding new custom categories.
 *
 * @param dataStoreManager DataStoreManager instance for storing the new category (passed from screen root)
 * @param onCategoryAdded Callback invoked when a new category is successfully added
 * @param onCancel Callback invoked when the user cancels adding a category
 * @param placeholder Optional placeholder text for the input field
 */
@Composable
fun AddCategoryInput(
    dataStoreManager: DataStoreManager,
    onCategoryAdded: (String) -> Unit = {},
    onCancel: () -> Unit = {},
    placeholder: String = "Enter a new category name",
) {
    var categoryText by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }
    // Lifecycle-bound scope: cancelled when this composable leaves composition,
    // unlike the former unmanaged CoroutineScope(Dispatchers.IO) (IMPROVEMENTS.md P1-4).
    val scope = rememberCoroutineScope()

    OutlinedTextField(
        value = categoryText,
        onValueChange = { categoryText = it },
        label = { Text("New Category") },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words
        ),
        singleLine = true,
        trailingIcon = {
            Row {
                IconButton(onClick = {
                    categoryText = ""
                    onCancel()
                }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Cancel"
                    )
                }
                IconButton(
                    onClick = {
                        if (categoryText.isNotBlank()) {
                            // Format category text
                            val formattedCategory = categoryText.trim().split(" ")
                                .joinToString(" ") { word ->
                                    word.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase() else it.toString()
                                    }
                                }

                            isAdding = true

                            scope.launch {
                                try {
                                    dataStoreManager.addCustomCategory(formattedCategory)
                                    if (BuildConfig.DEBUG) Log.d("CategoryAdd", "Successfully added: $formattedCategory")
                                    onCategoryAdded(formattedCategory)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Log.e("CategoryAdd", "Failed to add category: ${e.message}")
                                    // Could add error handling UI here
                                }
                                isAdding = false
                                categoryText = ""
                            }
                        }
                    },
                    enabled = categoryText.isNotBlank() && !isAdding
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm"
                    )
                }

            }
        }
    )
}
