package com.happypuppy.daycounter.data.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

const val DEFAULT_CATEGORY_ID = "personal"
const val WORK_CATEGORY_ID = "work"
const val HEALTH_CATEGORY_ID = "health"
const val FINANCE_CATEGORY_ID = "finance"

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L
)

val DEFAULT_CATEGORIES: List<Category> = listOf(
    Category(id = DEFAULT_CATEGORY_ID, name = "Personal", isDefault = true, createdAt = 0L),
    Category(id = WORK_CATEGORY_ID, name = "Work", isDefault = true, createdAt = 0L),
    Category(id = HEALTH_CATEGORY_ID, name = "Health", isDefault = true, createdAt = 0L),
    Category(id = FINANCE_CATEGORY_ID, name = "Finance", isDefault = true, createdAt = 0L)
)

data class CategoryWithCount(
    @Embedded val category: Category,
    @ColumnInfo(name = "event_count") val eventCount: Int
)

data class ItemCategoryRef(
    @ColumnInfo(name = "id") val itemId: Int,
    @ColumnInfo(name = "category_id") val categoryId: String
)

/**
 * Trims leading/trailing whitespace, collapses internal whitespace runs to a single space,
 * and title-cases each word.
 */
fun normalizeCategoryNameValue(raw: String): String =
    raw.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }
