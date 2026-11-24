package com.happypuppy.daycounter.util

/** What the label under the day-count badge says, as data. Counts are always positive. */
sealed interface DayLabel {
    data object Today : DayLabel
    data class Left(val days: Int) : DayLabel
    data class Past(val days: Int) : DayLabel
}

/** Maps the signed day count from [calculateDaysLeft] to a [DayLabel]. */
fun dayLabelFor(daysLeft: Int): DayLabel = when {
    daysLeft > 0 -> DayLabel.Left(daysLeft)
    daysLeft < 0 -> DayLabel.Past(-daysLeft)
    else -> DayLabel.Today
}
