package com.happypuppy.daycounter.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.ui.screens.SortOption

@Composable
fun SortMenu(
    expanded: Boolean,
    selectedSortOption: SortOption,
    onSelectOption: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        Text(
            text = stringResource(R.string.main_sort_by),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        SortOption.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(stringResource(option.labelRes)) },
                onClick = {
                    onSelectOption(option)
                    onDismiss()
                },
                trailingIcon = if (selectedSortOption == option) {
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
