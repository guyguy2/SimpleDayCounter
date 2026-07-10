package com.happypuppy.daycounter.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// DataStore for preferences
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Preference keys
val THEME_KEY = stringPreferencesKey("app_theme")
val VIBRATE_KEY = booleanPreferencesKey("vibrate_enabled")
val SOUND_KEY = booleanPreferencesKey("sound_enabled")
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
enum class DateFormat(val value: String, val formatPattern: String, val displayName: String) {
    DEFAULT("default", "MMM d, yyyy", "Default (Dec 31, 2020)"),
    US("us", "MM/dd/yyyy", "US (12/31/2020)"),
    EU("eu", "dd/MM/yyyy", "EU (31/12/2020)"),
    ISO("iso", "yyyy-MM-dd", "ISO (2020-12-31)"),
    FULL("full", "MMMM d, yyyy", "Full (December 31, 2020)");

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

    // Sound preference (default: false)
    val soundEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SOUND_KEY] ?: false
        }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[SOUND_KEY] = enabled
        }
    }

    val dateFormatFlow: Flow<DateFormat> = context.dataStore.data
        .map { preferences ->
            val formatString = preferences[DATE_FORMAT_KEY] ?: DateFormat.DEFAULT.value
            DateFormat.fromString(formatString)
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

    fun addCategoryAsync(category: String, onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                addCustomCategory(category)
                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                Log.e("CategoryAdd", "Failed to add category: ${e.message}")
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    suspend fun removeCustomCategory(category: String) {
        context.dataStore.edit { settings ->
            val currentCategories = settings[CUSTOM_CATEGORIES_KEY] ?: emptySet()
            settings[CUSTOM_CATEGORIES_KEY] = currentCategories - category
        }
    }

}

/** Pure date formatter - no DataStore dependency (IMPROVEMENTS.md P1-1). */
fun formatDate(date: Date, format: DateFormat): String {
    return SimpleDateFormat(format.formatPattern, Locale.getDefault()).format(date)
}