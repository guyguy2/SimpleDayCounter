package com.happypuppy.daycounter.data.preferences

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.happypuppy.daycounter.R
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// DataStore for preferences
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Preference keys
val THEME_KEY = stringPreferencesKey("app_theme")
val VIBRATE_KEY = booleanPreferencesKey("vibrate_enabled")
val DATE_FORMAT_KEY = stringPreferencesKey("date_format")
val CUSTOM_CATEGORIES_KEY = stringSetPreferencesKey("custom_categories")

// Theme options
enum class AppTheme(val value: String) {
    SYSTEM_DEFAULT("system_default"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromString(value: String): AppTheme {
            return entries.find { it.value == value } ?: SYSTEM_DEFAULT
        }
    }
}

// Add an enum for date format options
enum class DateFormat(val value: String, val formatPattern: String?, @StringRes val displayNameRes: Int) {
    // No stored choice: follow the device locale (null pattern, see formatDate).
    SYSTEM("system", null, R.string.date_format_system),
    DEFAULT("default", "MMM d, yyyy", R.string.date_format_default),
    US("us", "MM/dd/yyyy", R.string.date_format_us),
    EU("eu", "dd/MM/yyyy", R.string.date_format_eu),
    ISO("iso", "yyyy-MM-dd", R.string.date_format_iso),
    FULL("full", "MMMM d, yyyy", R.string.date_format_full);

    companion object {
        fun fromString(value: String): DateFormat {
            return entries.find { it.value == value } ?: DEFAULT
        }
    }
}

class DataStoreManager(private val context: Context) {
    // Theme preference
    val themeFlow: Flow<AppTheme> = context.dataStore.data
        .map { preferences ->
            val themeString = preferences[THEME_KEY] ?: AppTheme.SYSTEM_DEFAULT.value
            AppTheme.fromString(themeString)
        }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { settings ->
            settings[THEME_KEY] = theme.value
        }
    }

    // Vibrate preference (default: true)
    val vibrateEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[VIBRATE_KEY] != false
        }

    suspend fun setVibrateEnabled(enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[VIBRATE_KEY] = enabled
        }
    }

    val dateFormatFlow: Flow<DateFormat> = context.dataStore.data
        .map { preferences ->
            // Nothing stored means the user never chose: follow the device locale. Reading must
            // never write a choice, and an explicitly stored value (including "default") is kept.
            preferences[DATE_FORMAT_KEY]?.let { DateFormat.fromString(it) } ?: DateFormat.SYSTEM
        }

    suspend fun setDateFormat(format: DateFormat) {
        context.dataStore.edit { settings ->
            settings[DATE_FORMAT_KEY] = format.value
        }
    }

    // Custom Categories
    val customCategoriesFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[CUSTOM_CATEGORIES_KEY] ?: emptySet()
        }

    suspend fun addCustomCategory(category: String) {
        context.dataStore.edit { settings ->
            val currentCategories = settings[CUSTOM_CATEGORIES_KEY] ?: emptySet()
            settings[CUSTOM_CATEGORIES_KEY] = currentCategories + category
        }
    }

    suspend fun removeCustomCategory(category: String) {
        context.dataStore.edit { settings ->
            val currentCategories = settings[CUSTOM_CATEGORIES_KEY] ?: emptySet()
            settings[CUSTOM_CATEGORIES_KEY] = currentCategories - category
        }
    }

    suspend fun clearCustomCategories() {
        context.dataStore.edit { settings ->
            settings[CUSTOM_CATEGORIES_KEY] = emptySet()
        }
    }

}

/**
 * Pure date formatter - no DataStore dependency (IMPROVEMENTS.md P1-1). [DateFormat.SYSTEM] uses
 * the locale's own medium date style; every other option is a fixed pattern with locale month names.
 */
fun formatDate(date: Date, format: DateFormat, locale: Locale = Locale.getDefault()): String {
    val pattern = format.formatPattern
        ?: return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
            .format(date.toInstant().atZone(ZoneId.systemDefault()))
    return SimpleDateFormat(pattern, locale).format(date)
}