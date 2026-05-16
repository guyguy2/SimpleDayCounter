package com.happypuppy.daycounter.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Predefined colors for default categories
 */
object CategoryColors {
    // Color map for default categories - Light Theme
    val defaultCategoryColorsLight = mapOf(
        "Personal" to Color(0xFFE57373), // Light Red
        "Work" to Color(0xFF64B5F6),     // Light Blue
        "Health" to Color(0xFF81C784),   // Light Green
        "Finance" to Color(0xFFFFD54F)   // Light Amber
    )

    // Color map for default categories - Dark Theme
    val defaultCategoryColorsDark = mapOf(
        "Personal" to Color(0xFFCF6679), // Dark Red
        "Work" to Color(0xFF90CAF9),     // Dark Blue
        "Health" to Color(0xFFA5D6A7),   // Dark Green
        "Finance" to Color(0xFFFFE082)   // Dark Amber
    )

    // Cache for derived custom category colors (deterministic per name).
    private val customCategoryColors = mutableMapOf<String, Pair<Color, Color>>()

    /**
     * Get color for a category based on theme
     *
     * @param category The category name
     * @param isDarkTheme Whether dark theme is active
     * @return The color for the category
     */
    fun getCategoryColor(category: String, isDarkTheme: Boolean): Color {
        // Check if category is a default category
        return if (isDarkTheme) {
            defaultCategoryColorsDark[category] ?: getOrCreateCustomCategoryColor(category).second
        } else {
            defaultCategoryColorsLight[category] ?: getOrCreateCustomCategoryColor(category).first
        }
    }

    /**
     * Get or create a custom category color.
     *
     * Colors are derived deterministically from the category name so they stay
     * stable across app launches and cache clears. The hue comes from the string
     * hash mapped onto [0, 360); saturation/value live in a fixed pastel band.
     *
     * @param category The category name
     * @return A pair of (light theme color, dark theme color)
     */
    private fun getOrCreateCustomCategoryColor(category: String): Pair<Color, Color> {
        return customCategoryColors.getOrPut(category) {
            // Derive a stable hue in [0, 360) from the category name. Using
            // Math.floorMod keeps the result non-negative for any hash value.
            val hue = Math.floorMod(category.hashCode(), 360).toFloat()

            // Fixed pastel band so colors stay readable and consistent.
            val lightSaturation = 0.65f
            val lightValue = 0.9f
            val lightColor = Color.hsl(hue, lightSaturation, lightValue)

            // Slightly more saturated, noticeably darker variant for dark theme.
            val darkSaturation = 0.7f
            val darkValue = 0.6f
            val darkColor = Color.hsl(hue, darkSaturation, darkValue)

            Pair(lightColor, darkColor)
        }
    }

    /**
     * Clears the cached custom colors.
     *
     * Colors are derived deterministically from the category name, so clearing
     * the cache only frees memory; the next lookup will reproduce the same
     * colors.
     */
    fun clearCustomColorCache() {
        customCategoryColors.clear()
    }
}