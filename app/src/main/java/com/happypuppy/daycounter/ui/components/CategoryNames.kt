package com.happypuppy.daycounter.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.FINANCE_CATEGORY_ID
import com.happypuppy.daycounter.data.database.HEALTH_CATEGORY_ID
import com.happypuppy.daycounter.data.database.WORK_CATEGORY_ID

/**
 * Display-name lookup for the default categories keyed by stable [Category.id]
 * ("personal", "work", "health", "finance"). Custom categories have no resource
 * and show their stored [Category.name].
 */
@StringRes
fun defaultCategoryNameRes(categoryId: String): Int? = when (categoryId) {
    DEFAULT_CATEGORY_ID -> R.string.category_personal
    WORK_CATEGORY_ID -> R.string.category_work
    HEALTH_CATEGORY_ID -> R.string.category_health
    FINANCE_CATEGORY_ID -> R.string.category_finance
    else -> null
}

@Composable
fun categoryDisplayName(categoryId: String, categoryName: String = categoryId): String =
    defaultCategoryNameRes(categoryId)?.let { stringResource(it) } ?: categoryName

@Composable
fun categoryDisplayName(category: Category): String =
    categoryDisplayName(category.id, category.name)
