package com.happypuppy.daycounter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.happypuppy.daycounter.data.preferences.DateFormat

/**
 * Whether the app is currently rendering with the dark color scheme.
 *
 * Resolved once at the app root and provided via [SimpleDayCounterTheme].
 */
val LocalIsDarkTheme = staticCompositionLocalOf<Boolean> {
    error("LocalIsDarkTheme not provided. Wrap content in SimpleDayCounterTheme.")
}

/**
 * The current date format preference. Provided by [SimpleDayCounterTheme] so individual cards do
 * not each subscribe to DataStore.
 */
val LocalDateFormat = staticCompositionLocalOf<DateFormat> {
    error("LocalDateFormat not provided. Wrap content in SimpleDayCounterTheme.")
}

// Light Theme Colors
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// Dark Theme Base Colors
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkPrimary = Color(0xFFD0BCFF)
val DarkSecondary = Color(0xFF03DAC6)
val DarkError = Color(0xFFCF6679)

// Dark Card Colors with improved contrast
val DarkCardPink = Color(0xFF331A24)
val DarkCardYellow = Color(0xFF332912)
val DarkCardPurple = Color(0xFF2B1A33)

// Dark Text Colors with improved contrast
val DarkOnSurface = Color(0xFFEEEEEE)
val DarkOnSurfaceVariant = Color(0xFFBDBDBD)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    primaryContainer = Color(0xFF4E3A71),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = DarkSecondary,
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFFA3F7EF),
    tertiary = Color(0xFFD0BCFF),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF323232),
    error = DarkError,
    onPrimary = Color(0xFF381E72),
    onSecondary = Color(0xFF003731),
    onTertiary = Color(0xFF381E72),
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Color(0xFF857A95)
)

// Define Material3 Typography
val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )
)

@Composable
fun SimpleDayCounterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dateFormat: DateFormat = DateFormat.DEFAULT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalDateFormat provides dateFormat
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
