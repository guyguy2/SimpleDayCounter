package com.happypuppy.daycounter.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.happypuppy.daycounter.data.database.Item
import java.util.Date

// Create a temporary data holder for selected item properties
data class SelectedItemData(
    val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val createdAtMillis: Long = 0,
    val targetDateMillis: Long = 0,
    val emoji: String = ""
)

// Convert Item to/from SelectedItemData
fun Item.toData() = SelectedItemData(
    id = id,
    name = name,
    category = itemCategory,
    createdAtMillis = createdAt.time,
    targetDateMillis = targetDate.time,
    emoji = emoji
)

fun SelectedItemData.toItem() = Item(
    id = id,
    name = name,
    itemCategory = category,
    createdAt = Date(createdAtMillis),
    targetDate = Date(targetDateMillis),
    emoji = emoji
)

