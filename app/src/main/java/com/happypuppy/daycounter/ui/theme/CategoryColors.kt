package com.happypuppy.daycounter.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

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

    // Store custom category colors
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
     * Get or create a custom category color
     *
     * @param category The category name
     * @return A pair of (light theme color, dark theme color)
     */
    private fun getOrCreateCustomCategoryColor(category: String): Pair<Color, Color> {
        return customCategoryColors.getOrPut(category) {
            // Generate random pastel color for light theme
            val hue = Random.nextFloat() * 360f
            val lightSaturation = 0.5f + Random.nextFloat() * 0.3f // 0.5-0.8
            val lightValue = 0.85f + Random.nextFloat() * 0.1f    // 0.85-0.95

            val lightColor = Color.hsl(hue, lightSaturation, lightValue)

            // Create a darker version for dark theme
            val darkSaturation = lightSaturation + 0.05f
            val darkValue = lightValue - 0.3f
            val darkColor = Color.hsl(hue, darkSaturation, darkValue)

            Pair(lightColor, darkColor)
        }
    }

    /**
     * Clears the cached custom colors
     * Should be called when app is closed to prevent memory leaks
     */
    fun clearCustomColorCache() {
        customCategoryColors.clear()
    }
}