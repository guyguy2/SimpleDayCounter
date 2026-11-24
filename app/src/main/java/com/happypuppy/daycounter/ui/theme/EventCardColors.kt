package com.happypuppy.daycounter.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Container and text colors for one EventCard state. */
@Immutable
class EventStateColors(
    val container: Color,
    val content: Color,
    val contentVariant: Color
)

/**
 * The three EventCard states (past, today, future). Provided by [SimpleDayCounterTheme] as
 * [LocalEventCardColors] so EventCard holds no literal colors.
 */
@Immutable
class EventCardColors(
    val past: EventStateColors,
    val today: EventStateColors,
    val future: EventStateColors
) {
    fun forDaysLeft(daysLeft: Int): EventStateColors = when {
        daysLeft < 0 -> past
        daysLeft == 0 -> today
        else -> future
    }
}

val LocalEventCardColors = staticCompositionLocalOf<EventCardColors> {
    error("LocalEventCardColors not provided. Wrap content in SimpleDayCounterTheme.")
}

// Light brand card tints (past pink, today purple, future yellow).
val LightCardPink = Color(0xFFFFF0F6)
val LightCardPurple = Color(0xFFF9F0FF)
val LightCardYellow = Color(0xFFFFFBE6)

/** Brand palette: the tinted containers the app always used, with the scheme's own text roles. */
fun brandEventCardColors(scheme: ColorScheme, darkTheme: Boolean): EventCardColors {
    fun state(container: Color) = EventStateColors(container, scheme.onSurface, scheme.onSurfaceVariant)
    return if (darkTheme) {
        EventCardColors(state(DarkCardPink), state(DarkCardPurple), state(DarkCardYellow))
    } else {
        EventCardColors(state(LightCardPink), state(LightCardPurple), state(LightCardYellow))
    }
}
