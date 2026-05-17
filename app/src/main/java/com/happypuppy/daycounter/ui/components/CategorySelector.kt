package com.happypuppy.daycounter.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.happypuppy.daycounter.BuildConfig
import com.happypuppy.daycounter.data.preferences.AppTheme
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.navigation.Screen
import com.happypuppy.daycounter.ui.theme.CategoryColors
import org.koin.compose.koinInject

// Predefined default categories
val defaultCategories = listOf("Personal", "Work", "Health", "Finance")

@Composable
fun CategorySelector(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    dataStoreManager: DataStoreManager = koinInject()
) {
    var showCategoryDropdown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Get custom categories from DataStore
    val customCategories by dataStoreManager.customCategoriesFlow.collectAsState(initial = emptySet())

    // Determine if dark theme is active
    val themePreference by dataStoreManager.themeFlow.collectAsState(initial = AppTheme.SYSTEM_DEFAULT)
    val systemInDarkTheme = isSystemInDarkTheme()
    val isDarkTheme = when (themePreference) {
        AppTheme.SYSTEM_DEFAULT -> systemInDarkTheme
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    // Combine default and custom categories for the dropdown
    val allCategories = remember(customCategories) {
        defaultCategories + customCategories.toList() + "Add Custom..."
    }

    Column(modifier = modifier) {
        // Category selection field with clickable overlay
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = { /* readonly */ },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Category"
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

            // Invisible clickable overlay that covers the whole field
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        onClick = {
                            if (BuildConfig.DEBUG) Log.d("CategorySelector", "Category field clicked, opening dropdown")
                            showCategoryDropdown = true
                        }
                    )
            )

            // Category dropdown
            DropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                allCategories.forEach { category ->
                    DropdownMenuItem(
                        leadingIcon = {
                            if (category != "Add Custom...") {
                                // Show color indicator for each category in dropdown
                                CategoryColorIndicator(
                                    color = CategoryColors.getCategoryColor(category, isDarkTheme)
                                )
                            }
                        },
                        text = { Text(category) },
                        onClick = {
                            if (category == "Add Custom...") {
                                // Navigate to the Category Management screen instead of showing input
                                navController?.navigate(Screen.CategoryManagement.route)
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