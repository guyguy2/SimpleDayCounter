package com.happypuppy.daycounter.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.ui.screens.FilterOption
import com.happypuppy.daycounter.ui.screens.FilterState

@Composable
fun FilterMenu(
    expanded: Boolean,
    showCategoryOptions: Boolean,
    filterState: FilterState,
    categories: List<Category>,
    onSelectFilter: (FilterState) -> Unit,
    onDismiss: () -> Unit,
    onOpenCategories: () -> Unit,
    onDismissCategories: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        Text(
            text = stringResource(R.string.filter_by),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        FilterOption.entries.forEach { option ->
            val selectedIcon: (@Composable () -> Unit)? = if (filterState.option == option) {
                {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.common_selected)
                    )
                }
            } else {
                null
            }
            if (option == FilterOption.CATEGORY) {
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(option.labelRes))
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(R.string.main_show_categories)
                            )
                        }
                    },
                    onClick = onOpenCategories,
                    trailingIcon = selectedIcon
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = {
                        onSelectFilter(FilterState(option = option))
                        onDismiss()
                    },
                    trailingIcon = selectedIcon
                )
            }
        }
    }

    DropdownMenu(
        expanded = showCategoryOptions,
        onDismissRequest = onDismissCategories
    ) {
        categories.forEach { category ->
            DropdownMenuItem(
                text = { Text(categoryDisplayName(category)) },
                onClick = {
                    onSelectFilter(
                        FilterState(
                            option = FilterOption.CATEGORY,
                            category = category.id
                        )
                    )
                    onDismissCategories()
                    onDismiss()
                },
                trailingIcon = if (filterState.option == FilterOption.CATEGORY &&
                    filterState.category == category.id
                ) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.common_selected)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}
