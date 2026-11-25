package com.happypuppy.daycounter.ui.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORIES
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.util.DayLabel
import com.happypuppy.daycounter.util.calculateDaysLeft
import com.happypuppy.daycounter.util.dayLabelFor
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class EventUi(
    val id: Int,
    val name: String,
    val categoryId: String,
    val categoryName: String,
    val targetDate: LocalDate,
    val daysLeft: Int,
    val dayLabel: DayLabel,
    val emoji: String,
    val createdAtMillis: Long,
    val restoreVersion: Int = 0
)

fun Item.toEventUi(
    today: LocalDate,
    categoriesById: Map<String, Category> = DEFAULT_CATEGORIES.associateBy { it.id },
    restoreVersion: Int = 0
): EventUi {
    val resolvedCategory = categoriesById[categoryId] ?: DEFAULT_CATEGORIES.first()
    val daysLeft = ChronoUnit.DAYS.between(today, targetDate).toInt()
    return EventUi(
        id = id,
        name = name,
        categoryId = resolvedCategory.id,
        categoryName = resolvedCategory.name,
        targetDate = targetDate,
        daysLeft = daysLeft,
        dayLabel = dayLabelFor(daysLeft),
        emoji = emoji,
        createdAtMillis = createdAt.time,
        restoreVersion = restoreVersion
    )
}

enum class SortOption(@StringRes val labelRes: Int) {
    NAME_ASC(R.string.main_sort_name_asc),
    NAME_DESC(R.string.main_sort_name_desc),
    CATEGORY_ASC(R.string.main_sort_category_asc),
    CATEGORY_DESC(R.string.main_sort_category_desc),
    TARGET_DATE_ASC(R.string.main_sort_target_date_asc),
    TARGET_DATE_DESC(R.string.main_sort_target_date_desc),
    DATE_ADDED_ASC(R.string.main_sort_date_added_asc),
    DATE_ADDED_DESC(R.string.main_sort_date_added_desc);

    companion object {
        fun fromName(value: String?): SortOption =
            entries.find { it.name == value } ?: DATE_ADDED_DESC
    }
}

enum class FilterOption(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    FUTURE(R.string.filter_future),
    TODAY(R.string.filter_today),
    PAST(R.string.filter_past),
    CATEGORY(R.string.filter_by_category);

    companion object {
        fun fromName(value: String?): FilterOption =
            entries.find { it.name == value } ?: ALL
    }
}

data class FilterState(
    val option: FilterOption = FilterOption.ALL,
    val category: String? = null
)

private const val FILTER_OPTION_KEY = "option"
private const val FILTER_CATEGORY_KEY = "category"

/** Lets [FilterState] live directly in `rememberSaveable` (enum stored by name). */
val FilterStateSaver: Saver<FilterState, Any> = mapSaver(
    save = { mapOf(FILTER_OPTION_KEY to it.option.name, FILTER_CATEGORY_KEY to it.category) },
    restore = { saved ->
        val option = FilterOption.fromName(saved[FILTER_OPTION_KEY] as? String)
        val category = if (option == FilterOption.CATEGORY) {
            saved[FILTER_CATEGORY_KEY] as? String
        } else {
            null
        }
        FilterState(option = option, category = category)
    }
)

fun filterItems(
    items: List<Item>,
    filterState: FilterState,
    today: LocalDate = LocalDate.now()
): List<Item> {
    return when (filterState.option) {
        FilterOption.ALL -> items
        FilterOption.FUTURE -> items.filter { calculateDaysLeft(it.targetDate, today) > 0 }
        FilterOption.TODAY -> items.filter { calculateDaysLeft(it.targetDate, today) == 0 }
        FilterOption.PAST -> items.filter { calculateDaysLeft(it.targetDate, today) < 0 }
        FilterOption.CATEGORY -> filterState.category?.let { categoryId ->
            items.filter { it.categoryId == categoryId }
        } ?: items // If category is null, show all items
    }
}

fun sortItems(items: List<Item>, option: SortOption): List<Item> {
    return when (option) {
        SortOption.NAME_ASC -> items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        SortOption.NAME_DESC -> items.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
        SortOption.TARGET_DATE_ASC -> items.sortedBy { it.targetDate }
        SortOption.TARGET_DATE_DESC -> items.sortedByDescending { it.targetDate }
        SortOption.DATE_ADDED_ASC -> items.sortedBy { it.createdAt }
        SortOption.DATE_ADDED_DESC -> items.sortedByDescending { it.createdAt }
        SortOption.CATEGORY_ASC -> items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.categoryId })
        SortOption.CATEGORY_DESC -> items.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.categoryId })
    }
}

fun filterEvents(events: List<EventUi>, filterState: FilterState): List<EventUi> {
    return when (filterState.option) {
        FilterOption.ALL -> events
        FilterOption.FUTURE -> events.filter { it.daysLeft > 0 }
        FilterOption.TODAY -> events.filter { it.daysLeft == 0 }
        FilterOption.PAST -> events.filter { it.daysLeft < 0 }
        FilterOption.CATEGORY -> filterState.category?.let { categoryId ->
            events.filter { it.categoryId == categoryId }
        } ?: events
    }
}

fun sortEvents(events: List<EventUi>, option: SortOption): List<EventUi> {
    return when (option) {
        SortOption.NAME_ASC -> events.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        SortOption.NAME_DESC -> events.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
        SortOption.TARGET_DATE_ASC -> events.sortedBy { it.targetDate }
        SortOption.TARGET_DATE_DESC -> events.sortedByDescending { it.targetDate }
        SortOption.DATE_ADDED_ASC -> events.sortedBy { it.createdAtMillis }
        SortOption.DATE_ADDED_DESC -> events.sortedByDescending { it.createdAtMillis }
        SortOption.CATEGORY_ASC -> events.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.categoryName })
        SortOption.CATEGORY_DESC -> events.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.categoryName })
    }
}
