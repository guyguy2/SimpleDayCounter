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

/**
 * Dynamic color: past, today and future map to the tertiary, primary and secondary containers of
 * the wallpaper scheme, with text from the matching on-container roles. Those pairs are tonal by
 * design (container tone 90 or 30 against on-container tone 10 or 90), so no brand tints are used.
 */
fun dynamicEventCardColors(scheme: ColorScheme): EventCardColors {
    fun state(container: Color, onContainer: Color) =
        EventStateColors(container, onContainer, onContainer.copy(alpha = DYNAMIC_VARIANT_ALPHA))
    return EventCardColors(
        past = state(scheme.tertiaryContainer, scheme.onTertiaryContainer),
        today = state(scheme.primaryContainer, scheme.onPrimaryContainer),
        future = state(scheme.secondaryContainer, scheme.onSecondaryContainer)
    )
}

/** Secondary text (date, category) is the on-container color at this alpha. */
const val DYNAMIC_VARIANT_ALPHA = 0.8f
