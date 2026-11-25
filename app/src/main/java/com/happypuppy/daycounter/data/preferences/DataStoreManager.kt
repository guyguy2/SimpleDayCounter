package com.happypuppy.daycounter.data.preferences

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.happypuppy.daycounter.R
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// DataStore for preferences
const val SETTINGS_DATASTORE_NAME = "settings"

// One instance per file per process: production passes this to DataStoreManager.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SETTINGS_DATASTORE_NAME)

// Preference keys
val THEME_KEY = stringPreferencesKey("app_theme")
val VIBRATE_KEY = booleanPreferencesKey("vibrate_enabled")
val DATE_FORMAT_KEY = stringPreferencesKey("date_format")
val CUSTOM_CATEGORIES_KEY = stringSetPreferencesKey("custom_categories")
val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")

// Theme options
enum class AppTheme(val value: String) {
    SYSTEM_DEFAULT("system_default"),
    LIGHT("light"),
    DARK("dark");

    /** Whether this choice renders dark, given whether the device is currently in dark mode. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM_DEFAULT -> systemDark
        LIGHT -> false
        DARK -> true
    }

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

/** The settings that decide the first frame: resolved once, never a placeholder. */
data class AppSettings(
    val theme: AppTheme,
    val dateFormat: DateFormat,
    val dynamicColor: Boolean = false
) {
    companion object {
        val Defaults = AppSettings(AppTheme.SYSTEM_DEFAULT, DateFormat.SYSTEM)
    }
}

class DataStoreManager(private val dataStore: DataStore<Preferences>) {
    private fun themeOf(preferences: Preferences): AppTheme =
        AppTheme.fromString(preferences[THEME_KEY] ?: AppTheme.SYSTEM_DEFAULT.value)

    // Nothing stored means the user never chose: follow the device locale. Reading must
    // never write a choice, and an explicitly stored value (including "default") is kept.
    private fun dateFormatOf(preferences: Preferences): DateFormat =
        preferences[DATE_FORMAT_KEY]?.let { DateFormat.fromString(it) } ?: DateFormat.SYSTEM

    // Brand palette unless the user turned dynamic color on: nothing stored means off.
    private fun dynamicColorOf(preferences: Preferences): Boolean = preferences[DYNAMIC_COLOR_KEY] == true

    // A failed read (including a corrupt file) resolves to the defaults instead of failing the
    // collector, so the splash keep condition and the first frame can never wait on a dead flow.
    // Anything that is not an IOException is a bug and still propagates.
    private val data: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val settingsFlow: Flow<AppSettings> = data
        .map { AppSettings(themeOf(it), dateFormatOf(it), dynamicColorOf(it)) }
        .distinctUntilChanged()

    // Theme preference
    val themeFlow: Flow<AppTheme> = data.map(::themeOf)

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { settings ->
            settings[THEME_KEY] = theme.value
        }
    }

    val dynamicColorFlow: Flow<Boolean> = data.map(::dynamicColorOf)

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    // Vibrate preference (default: true)
    val vibrateEnabledFlow: Flow<Boolean> = data
        .map { preferences ->
            preferences[VIBRATE_KEY] != false
        }

    suspend fun setVibrateEnabled(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[VIBRATE_KEY] = enabled
        }
    }

    val dateFormatFlow: Flow<DateFormat> = data.map(::dateFormatOf)

    suspend fun setDateFormat(format: DateFormat) {
        dataStore.edit { settings ->
            settings[DATE_FORMAT_KEY] = format.value
        }
    }

    // Custom Categories
    val customCategoriesFlow: Flow<Set<String>> = data
        .map { preferences ->
            preferences[CUSTOM_CATEGORIES_KEY] ?: emptySet()
        }

    suspend fun addCustomCategory(category: String) {
        dataStore.edit { settings ->
            val currentCategories = settings[CUSTOM_CATEGORIES_KEY] ?: emptySet()
            settings[CUSTOM_CATEGORIES_KEY] = currentCategories + category
        }
    }

    suspend fun removeCustomCategory(category: String) {
        dataStore.edit { settings ->
            val currentCategories = settings[CUSTOM_CATEGORIES_KEY] ?: emptySet()
            settings[CUSTOM_CATEGORIES_KEY] = currentCategories - category
        }
    }

    suspend fun clearCustomCategories() {
        dataStore.edit { settings ->
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