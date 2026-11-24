package com.happypuppy.daycounter.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.happypuppy.daycounter.R

/**
 * Display-name lookup for the default categories. The stored category string stays the English
 * key ("Personal", ...) because colors, icons and saved events match on it; only what the user
 * reads goes through a string resource. Custom categories have no resource and show as stored.
 */
@StringRes
fun defaultCategoryNameRes(key: String): Int? = when (key) {
    "Personal" -> R.string.category_personal
    "Work" -> R.string.category_work
    "Health" -> R.string.category_health
    "Finance" -> R.string.category_finance
    else -> null
}

@Composable
fun categoryDisplayName(key: String): String =
    defaultCategoryNameRes(key)?.let { stringResource(it) } ?: key
