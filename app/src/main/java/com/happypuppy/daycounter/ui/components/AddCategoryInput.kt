package com.happypuppy.daycounter.ui.components

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Trims leading/trailing whitespace, collapses internal whitespace runs to a single space,
 * and title-cases each word.
 */
fun normalizeCategoryName(raw: String): String =
    raw.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }

/**
 * Returns true if [candidate] (after normalization) matches any default category or
 * existing custom category case-insensitively.
 */
fun isDuplicateCategory(
    candidate: String,
    existingCustomCategories: Set<String> = emptySet(),
    defaults: List<String> = defaultCategories
): Boolean {
    val normalized = normalizeCategoryName(candidate)
    if (normalized.isEmpty()) return false
    return defaults.any { it.equals(normalized, ignoreCase = true) } ||
        existingCustomCategories.any { it.equals(normalized, ignoreCase = true) }
}

/**
 * A reusable component for adding new custom categories.
 */
@Composable
fun AddCategoryInput(
    dataStoreManager: DataStoreManager,
    onCategoryAdded: (String) -> Unit = {},
    onCancel: () -> Unit = {},
    placeholder: String = stringResource(R.string.category_name_placeholder),
) {
    var categoryText by rememberSaveable { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }
    val existingCustomCategories by dataStoreManager.customCategoriesFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    val isDuplicate = remember(categoryText, existingCustomCategories) {
        isDuplicateCategory(categoryText, existingCustomCategories)
    }

    OutlinedTextField(
        value = categoryText,
        onValueChange = { categoryText = it },
        label = { Text(stringResource(R.string.category_new_label)) },
        placeholder = { Text(placeholder) },
        isError = isDuplicate,
        supportingText = if (isDuplicate) {
            { Text(stringResource(R.string.category_exists)) }
        } else {
            null
        },
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
                        contentDescription = stringResource(R.string.common_cancel)
                    )
                }
                IconButton(
                    onClick = {
                        val formattedCategory = normalizeCategoryName(categoryText)
                        if (formattedCategory.isNotEmpty() && !isDuplicate) {
                            isAdding = true
                            scope.launch {
                                try {
                                    dataStoreManager.addCustomCategory(formattedCategory)
                                    onCategoryAdded(formattedCategory)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                }
                                isAdding = false
                                categoryText = ""
                            }
                        }
                    },
                    enabled = categoryText.isNotBlank() && !isDuplicate && !isAdding
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.common_confirm)
                    )
                }
            }
        }
    )
}
