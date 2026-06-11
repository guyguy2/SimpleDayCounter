package com.happypuppy.daycounter.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.happypuppy.daycounter.data.preferences.AppTheme
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.data.preferences.DateFormat
import org.koin.compose.koinInject

/**
 * Whether the app is currently rendering with the dark color scheme.
 *
 * Resolved once in [SimpleDayCounterTheme] from the user's [AppTheme] preference and the system
 * setting. Leaf composables that need to vary by light/dark should read this instead of collecting
 * the theme flow and recomputing per-call site (see IMPROVEMENTS.md P1-5).
 */
val LocalIsDarkTheme = staticCompositionLocalOf<Boolean> {
    error("LocalIsDarkTheme not provided. Wrap content in SimpleDayCounterTheme.")
}

/**
 * The current date format preference. Provided by [SimpleDayCounterTheme] so individual cards do
 * not each subscribe to [DataStoreManager.dateFormatFlow].
 */
val LocalDateFormat = staticCompositionLocalOf<DateFormat> {
    error("LocalDateFormat not provided. Wrap content in SimpleDayCounterTheme.")
}

// Light Theme Colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Added Light Theme ColorScheme
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// Dark Theme Base Colors
val DarkBackground = Color(0xFF121212)  // Darker background for better contrast
val DarkSurface = Color(0xFF1E1E1E)
val DarkPrimary = Color(0xFFD0BCFF)     // Lighter primary for better visibility (was 0xFFBB86FC)
val DarkSecondary = Color(0xFF03DAC6)
val DarkError = Color(0xFFCF6679)

// Dark Card Colors with improved contrast
val DarkCardPink = Color(0xFF331A24)     // Darker pink for past events cards (was 0xFF2D1A24)
val DarkCardYellow = Color(0xFF332912)   // Darker yellow for future events cards (was 0xFF2D2512)
val DarkCardPurple = Color(0xFF2B1A33)   // Darker purple for today cards (was 0xFF221A2D)

// Dark Card Accent Colors (brighter for better contrast when used for text)
val DarkAccentPink = Color(0xFFEC407A)   // Brighter pink for past events (was 0xFFE91E63)
val DarkAccentYellow = Color(0xFFFFA726) // Brighter orange for future events (was 0xFFFF9800)
val DarkAccentPurple = Color(0xFFAB47BC) // Brighter purple for today (was 0xFF9C27B0)

// Dark Text Colors with improved contrast
val DarkOnSurface = Color(0xFFEEEEEE)      // Whiter primary text for better contrast (was 0xFFE0E0E0)
val DarkOnSurfaceVariant = Color(0xFFBDBDBD) // Lighter secondary text for better contrast (was 0xFFAEAEAE)

// Updated ColorScheme for dark mode with improved contrast
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    primaryContainer = Color(0xFF4E3A71),    // Added for buttons and containers
    onPrimaryContainer = Color(0xFFEADDFF),  // Added for text on primary containers
    secondary = DarkSecondary,
    secondaryContainer = Color(0xFF005047),  // Added for secondary containers
    onSecondaryContainer = Color(0xFFA3F7EF), // Added for text on secondary containers
    tertiary = Color(0xFFD0BCFF),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF323232),      // Added for card backgrounds
    error = DarkError,
    onPrimary = Color(0xFF381E72),           // Darker color for text on primary buttons
    onSecondary = Color(0xFF003731),         // Darker color for text on secondary buttons
    onTertiary = Color(0xFF381E72),
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Color(0xFF857A95)              // Added for borders
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

// Modify the getCounterColor function to respect dark theme
fun getCounterColor(daysLeft: Int, isDarkTheme: Boolean): Color {
    return if (isDarkTheme) {
        when {
            daysLeft > 30 -> Color(0xFF4CAF50) // Keep green
            daysLeft > 7 -> Color(0xFF2196F3)  // Keep blue
            daysLeft > 0 -> DarkAccentYellow   // Brighter orange for dark theme
            daysLeft == 0 -> DarkAccentPurple  // Brighter purple for dark theme
            else -> DarkAccentPink             // Brighter pink for dark theme
        }
    } else {
        when {
            daysLeft > 30 -> Color(0xFF4CAF50) // Green
            daysLeft > 7 -> Color(0xFF2196F3)  // Blue
            daysLeft > 0 -> Color(0xFFFF9800)  // Orange
            daysLeft == 0 -> Color(0xFF9C27B0) // Purple
            else -> Color(0xFFE91E63)          // Pink (past date)
        }
    }
}

@Composable
fun SimpleDayCounterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Get DataStoreManager through Koin injection
    val dataStoreManager = koinInject<DataStoreManager>()

    // Collect theme preference from DataStore
    val themePreferenceState = dataStoreManager.themeFlow.collectAsState(initial = AppTheme.SYSTEM_DEFAULT)
    val themePreference = themePreferenceState.value

    // Collect the date format preference once at the theme level so leaf composables can
    // consume it via LocalDateFormat instead of re-subscribing per card (IMPROVEMENTS.md P1-5).
    val dateFormat by dataStoreManager.dateFormatFlow.collectAsState(initial = DateFormat.DEFAULT)

    // Determine if dark theme should be used based on the saved preference
    val useDarkTheme = when (themePreference) {
        AppTheme.SYSTEM_DEFAULT -> darkTheme
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        // Add an else branch to handle potential future theme values
        else -> darkTheme
    }

    val colorScheme = when {
        dynamicColor && true -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Keep system bars visible (don't use full screen)
            WindowCompat.setDecorFitsSystemWindows(window, true)

            // Set status bar color to match theme
            window.statusBarColor = colorScheme.primary.toArgb()

            // Update status bar appearance based on theme
            WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    // Provide the resolved theme + date format down the tree so leaf composables don't each
    // recompute these from DataStore. Use our Material3 AppTypography.
    CompositionLocalProvider(
        LocalIsDarkTheme provides useDarkTheme,
        LocalDateFormat provides dateFormat
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}