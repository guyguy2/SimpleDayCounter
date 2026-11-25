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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORIES
import com.happypuppy.daycounter.ui.theme.CategoryColors
import com.happypuppy.daycounter.ui.theme.LocalIsDarkTheme

// Predefined default category names
val defaultCategories = DEFAULT_CATEGORIES.map { it.name }

/**
 * Stateless category selector dropdown listing default and custom [Category] rows,
 * plus an entry to trigger inline custom-category creation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    selectedCategoryId: String,
    categories: List<Category>,
    onCategorySelected: (String) -> Unit,
    onAddCustomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val isDarkTheme = LocalIsDarkTheme.current
    val resolvedCategories = categories.ifEmpty { DEFAULT_CATEGORIES }
    val selectedCategory = remember(selectedCategoryId, resolvedCategories) {
        resolvedCategories.find { it.id == selectedCategoryId } ?: resolvedCategories.first()
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
                    CategoryColorIndicator(
                        color = CategoryColors.getCategoryColor(
                            categoryId = selectedCategory.id,
                            categoryName = selectedCategory.name,
                            isDarkTheme = isDarkTheme
                        )
                    )
                },
                readOnly = true
            )

            ExposedDropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false }
            ) {
                resolvedCategories.forEach { category ->
                    DropdownMenuItem(
                        leadingIcon = {
                            CategoryColorIndicator(
                                color = CategoryColors.getCategoryColor(
                                    categoryId = category.id,
                                    categoryName = category.name,
                                    isDarkTheme = isDarkTheme
                                )
                            )
                        },
                        text = {
                            Text(categoryDisplayName(category))
                        },
                        onClick = {
                            onCategorySelected(category.id)
                            showCategoryDropdown = false
                        }
                    )
                }

                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.category_add_custom))
                    },
                    onClick = {
                        onAddCustomClick()
                        showCategoryDropdown = false
                    }
                )
            }
        }
    }
}
