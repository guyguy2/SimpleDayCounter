package com.happypuppy.daycounter.ui.theme

import androidx.compose.ui.graphics.Color
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.FINANCE_CATEGORY_ID
import com.happypuppy.daycounter.data.database.HEALTH_CATEGORY_ID
import com.happypuppy.daycounter.data.database.WORK_CATEGORY_ID

/**
 * Predefined colors for default categories and deterministic colors for custom categories.
 */
object CategoryColors {
    // Color map for default categories keyed by stable Category.id - Light Theme
    val defaultCategoryColorsByIdLight = mapOf(
        DEFAULT_CATEGORY_ID to Color(0xFFE57373), // Light Red
        WORK_CATEGORY_ID to Color(0xFF64B5F6),     // Light Blue
        HEALTH_CATEGORY_ID to Color(0xFF81C784),   // Light Green
        FINANCE_CATEGORY_ID to Color(0xFFFFD54F)   // Light Amber
    )

    // Color map for default categories keyed by stable Category.id - Dark Theme
    val defaultCategoryColorsByIdDark = mapOf(
        DEFAULT_CATEGORY_ID to Color(0xFFCF6679), // Dark Red
        WORK_CATEGORY_ID to Color(0xFF90CAF9),     // Dark Blue
        HEALTH_CATEGORY_ID to Color(0xFFA5D6A7),   // Dark Green
        FINANCE_CATEGORY_ID to Color(0xFFFFE082)   // Dark Amber
    )

    // Legacy name-keyed maps retained for compatibility in tests/callers.
    val defaultCategoryColorsLight = mapOf(
        "Personal" to defaultCategoryColorsByIdLight.getValue(DEFAULT_CATEGORY_ID),
        "Work" to defaultCategoryColorsByIdLight.getValue(WORK_CATEGORY_ID),
        "Health" to defaultCategoryColorsByIdLight.getValue(HEALTH_CATEGORY_ID),
        "Finance" to defaultCategoryColorsByIdLight.getValue(FINANCE_CATEGORY_ID)
    )

    val defaultCategoryColorsDark = mapOf(
        "Personal" to defaultCategoryColorsByIdDark.getValue(DEFAULT_CATEGORY_ID),
        "Work" to defaultCategoryColorsByIdDark.getValue(WORK_CATEGORY_ID),
        "Health" to defaultCategoryColorsByIdDark.getValue(HEALTH_CATEGORY_ID),
        "Finance" to defaultCategoryColorsByIdDark.getValue(FINANCE_CATEGORY_ID)
    )

    // Cache for derived custom category colors (deterministic per name).
    private val customCategoryColors = mutableMapOf<String, Pair<Color, Color>>()

    /**
     * Get color for a category based on stable [categoryId], display [categoryName], and theme.
     * Custom categories derive their color deterministically from [categoryName] so migrating to
     * UUID IDs preserves existing user colors.
     */
    fun getCategoryColor(categoryId: String, categoryName: String, isDarkTheme: Boolean): Color {
        return if (isDarkTheme) {
            defaultCategoryColorsByIdDark[categoryId]
                ?: defaultCategoryColorsDark[categoryName]
                ?: getOrCreateCustomCategoryColor(categoryName).second
        } else {
            defaultCategoryColorsByIdLight[categoryId]
                ?: defaultCategoryColorsLight[categoryName]
                ?: getOrCreateCustomCategoryColor(categoryName).first
        }
    }

    /**
     * Convenience overload when only category name or ID is passed.
     */
    fun getCategoryColor(category: String, isDarkTheme: Boolean): Color {
        return getCategoryColor(categoryId = category, categoryName = category, isDarkTheme = isDarkTheme)
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