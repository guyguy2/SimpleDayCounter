package com.happypuppy.daycounter.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.navigation.Screen
import com.happypuppy.daycounter.ui.theme.CategoryColors
import com.happypuppy.daycounter.ui.theme.LocalIsDarkTheme

// Predefined default categories
val defaultCategories = listOf("Personal", "Work", "Health", "Finance")

/** Menu sentinel for the "Add Custom..." entry; shown through a string resource, never stored. */
private const val ADD_CUSTOM_CATEGORY_KEY = "Add Custom..."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    dataStoreManager: DataStoreManager,
    modifier: Modifier = Modifier,
    navController: NavController? = null,
) {
    var showCategoryDropdown by remember { mutableStateOf(false) }

    // Get custom categories from DataStore (injected at screen root - P1-1)
    val customCategories by dataStoreManager.customCategoriesFlow.collectAsState(initial = emptySet())

    // Theme is resolved once at SimpleDayCounterTheme (IMPROVEMENTS.md P1-5).
    val isDarkTheme = LocalIsDarkTheme.current

    // Combine default and alphabetically sorted custom categories for the dropdown
    val allCategories = remember(customCategories) {
        defaultCategories + customCategories.sortedWith(String.CASE_INSENSITIVE_ORDER) + ADD_CUSTOM_CATEGORY_KEY
    }

    Column(modifier = modifier) {
        // Category selection field: the field itself is the single focus target and menu anchor.
        ExposedDropdownMenuBox(
            expanded = showCategoryDropdown,
            onExpandedChange = { showCategoryDropdown = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = categoryDisplayName(selectedCategory),
                onValueChange = { /* readonly */ },
                label = { Text(stringResource(R.string.category_label)) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.category_select)
                    )
                },
                leadingIcon = {
                    // Show color indicator for the selected category
                    CategoryColorIndicator(
                        color = CategoryColors.getCategoryColor(selectedCategory, isDarkTheme)
                    )
                },
                readOnly = true
            )

            // Category dropdown
            ExposedDropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false }
            ) {
                allCategories.forEach { category ->
                    DropdownMenuItem(
                        leadingIcon = {
                            if (category != ADD_CUSTOM_CATEGORY_KEY) {
                                // Show color indicator for each category in dropdown
                                CategoryColorIndicator(
                                    color = CategoryColors.getCategoryColor(category, isDarkTheme)
                                )
                            }
                        },
                        text = {
                            Text(if (category == ADD_CUSTOM_CATEGORY_KEY) stringResource(R.string.category_add_custom) else categoryDisplayName(category))
                        },
                        onClick = {
                            if (category == ADD_CUSTOM_CATEGORY_KEY) {
                                // Navigate to the Category Management screen instead of showing input
                                navController?.navigate(Screen.CategoryManagement)
                            } else {
                                onCategorySelected(category)
                            }
                            showCategoryDropdown = false
                        }
                    )
                }
            }
        }
    }
}
