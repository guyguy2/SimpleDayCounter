package com.happypuppy.daycounter.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.happypuppy.daycounter.ui.screens.FilterOption
import com.happypuppy.daycounter.ui.screens.FilterState
import com.happypuppy.daycounter.ui.screens.SortOption
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * DataStoreManager takes the DataStore as a constructor argument (CQ-4), so it runs on the JVM
 * against a real file-backed store in a temp folder. The on-disk contract (file name, key names,
 * value strings) is pinned here because settings and cloud backup depend on it.
 */
class DataStoreManagerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scopes = mutableListOf<Job>()

    @After
    fun cancelScopes() = runBlocking { scopes.forEach { it.cancelAndJoin() } }

    /** The file the production `preferencesDataStore(name = "settings")` delegate opens. */
    private fun settingsFile() = File(tmp.root, "datastore/settings.preferences_pb")

    private fun openStore(file: File = settingsFile()): Pair<DataStore<Preferences>, Job> {
        val job = Job()
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { file }
        )
        scopes += job
        return store to job
    }

    private fun newManager() = DataStoreManager(openStore().first)

    private fun failingStore(error: Throwable) = object : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw error }
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            throw error
    }

    // Defaults

    @Test
    fun `an empty store resolves every setting to its default`() = runBlocking {
        val manager = newManager()

        assertEquals(AppTheme.SYSTEM_DEFAULT, manager.themeFlow.first())
        assertEquals(DateFormat.SYSTEM, manager.dateFormatFlow.first())
        assertTrue(manager.vibrateEnabledFlow.first())
        assertTrue(manager.customCategoriesFlow.first().isEmpty())
    }

    // Round trips

    @Test
    fun `theme date format and vibrate round trip`() = runBlocking {
        val manager = newManager()

        manager.setTheme(AppTheme.DARK)
        manager.setDateFormat(DateFormat.ISO)
        manager.setVibrateEnabled(false)

        assertEquals(AppTheme.DARK, manager.themeFlow.first())
        assertEquals(DateFormat.ISO, manager.dateFormatFlow.first())
        assertFalse(manager.vibrateEnabledFlow.first())
    }

    @Test
    fun `custom categories add remove and clear`() = runBlocking {
        val manager = newManager()

        manager.addCustomCategory("Travel")
        manager.addCustomCategory("Book Club")
        manager.addCustomCategory("Travel")
        assertEquals(setOf("Travel", "Book Club"), manager.customCategoriesFlow.first())

        manager.removeCustomCategory("Travel")
        assertEquals(setOf("Book Club"), manager.customCategoriesFlow.first())

        manager.clearCustomCategories()
        assertTrue(manager.customCategoriesFlow.first().isEmpty())
    }

    @Test
    fun `values written by one manager are read by a new one on the same file`() = runBlocking {
        val (store, job) = openStore()
        DataStoreManager(store).apply {
            setTheme(AppTheme.LIGHT)
            setDateFormat(DateFormat.EU)
            addCustomCategory("Travel")
        }
        job.cancelAndJoin()

        val reopened = DataStoreManager(openStore().first)

        assertEquals(AppTheme.LIGHT, reopened.themeFlow.first())
        assertEquals(DateFormat.EU, reopened.dateFormatFlow.first())
        assertEquals(setOf("Travel"), reopened.customCategoriesFlow.first())
    }

    // Date format key: only a Settings tap writes it

    @Test
    fun `reading never writes the date format key`() = runBlocking {
        val (store, _) = openStore()
        val manager = DataStoreManager(store)

        manager.dateFormatFlow.first()
        manager.themeFlow.first()

        assertNull(store.data.first()[stringPreferencesKey("date_format")])
    }

    @Test
    fun `other setters leave the date format key unwritten`() = runBlocking {
        val (store, _) = openStore()
        val manager = DataStoreManager(store)

        manager.setTheme(AppTheme.DARK)
        manager.setVibrateEnabled(false)
        manager.addCustomCategory("Travel")

        assertNull(store.data.first()[stringPreferencesKey("date_format")])
        assertEquals(DateFormat.SYSTEM, manager.dateFormatFlow.first())
    }

    @Test
    fun `setting a date format writes its frozen value string`() = runBlocking {
        val (store, _) = openStore()
        val manager = DataStoreManager(store)

        for (format in DateFormat.entries) {
            manager.setDateFormat(format)
            assertEquals(format.value, store.data.first()[stringPreferencesKey("date_format")])
        }
    }

    @Test
    fun `every stored date format string is kept including default`() = runBlocking {
        val (store, _) = openStore()
        val manager = DataStoreManager(store)

        for (value in listOf("system", "default", "us", "eu", "iso", "full")) {
            store.edit { it[stringPreferencesKey("date_format")] = value }
            assertEquals(value, manager.dateFormatFlow.first().value)
        }
    }

    @Test
    fun `an unknown stored date format falls back to default`() = runBlocking {
        val (store, _) = openStore()
        store.edit { it[stringPreferencesKey("date_format")] = "mystery" }

        assertEquals(DateFormat.DEFAULT, DataStoreManager(store).dateFormatFlow.first())
    }

    // Same file as before the refactor

    @Test
    fun `a file written under the old key and value names is read back`() = runBlocking {
        // Literal names on purpose: these are what installed apps already have on disk.
        val (oldAccess, oldJob) = openStore()
        oldAccess.edit {
            it[stringPreferencesKey("app_theme")] = "dark"
            it[stringPreferencesKey("date_format")] = "us"
            it[booleanPreferencesKey("vibrate_enabled")] = false
        }
        oldJob.cancelAndJoin()
        assertTrue("settings file must be datastore/settings.preferences_pb", settingsFile().isFile)

        val manager = DataStoreManager(openStore().first)

        assertEquals(AppTheme.DARK, manager.themeFlow.first())
        assertEquals(DateFormat.US, manager.dateFormatFlow.first())
        assertFalse(manager.vibrateEnabledFlow.first())
    }

    @Test
    fun `production file name is settings and the keys are frozen`() {
        assertEquals("settings", SETTINGS_DATASTORE_NAME)
        assertEquals("app_theme", THEME_KEY.name)
        assertEquals("vibrate_enabled", VIBRATE_KEY.name)
        assertEquals("date_format", DATE_FORMAT_KEY.name)
        assertEquals("custom_categories", CUSTOM_CATEGORIES_KEY.name)
    }

    // Read errors resolve to defaults instead of failing the collector

    @Test
    fun `an IOException on read resolves to the defaults`() = runBlocking {
        val manager = DataStoreManager(failingStore(IOException("disk unreadable")))

        assertEquals(AppTheme.SYSTEM_DEFAULT, manager.themeFlow.first())
        assertEquals(DateFormat.SYSTEM, manager.dateFormatFlow.first())
        assertTrue(manager.vibrateEnabledFlow.first())
        assertTrue(manager.customCategoriesFlow.first().isEmpty())
        assertEquals(AppSettings.Defaults, manager.settingsFlow.first())
    }

    @Test
    fun `a failure that is not an IOException is not swallowed`() = runBlocking {
        val manager = DataStoreManager(failingStore(IllegalStateException("bug")))

        try {
            manager.settingsFlow.first()
            fail("expected the IllegalStateException to propagate")
        } catch (expected: IllegalStateException) {
            assertEquals("bug", expected.message)
        }
    }

    // The combined settings flow

    @Test
    fun `settingsFlow first emission is the resolved stored value`() = runBlocking {
        val (store, _) = openStore()
        store.edit {
            it[stringPreferencesKey("app_theme")] = "dark"
            it[stringPreferencesKey("date_format")] = "iso"
        }

        assertEquals(AppSettings(AppTheme.DARK, DateFormat.ISO), DataStoreManager(store).settingsFlow.first())
    }

    @Test
    fun `settingsFlow on an empty store is the defaults`() = runBlocking {
        assertEquals(AppSettings.Defaults, newManager().settingsFlow.first())
        assertEquals(AppSettings(AppTheme.SYSTEM_DEFAULT, DateFormat.SYSTEM), AppSettings.Defaults)
    }

    // Dynamic color (UX-10): an additive boolean, off unless the user turns it on

    @Test
    fun `dynamic color is off by default`() = runBlocking {
        val manager = newManager()

        assertFalse(manager.dynamicColorFlow.first())
        assertFalse(manager.settingsFlow.first().dynamicColor)
        assertFalse(AppSettings.Defaults.dynamicColor)
    }

    @Test
    fun `dynamic color round trips and reaches settingsFlow`() = runBlocking {
        val manager = newManager()

        manager.setDynamicColor(true)
        assertTrue(manager.dynamicColorFlow.first())
        assertEquals(AppSettings(AppTheme.SYSTEM_DEFAULT, DateFormat.SYSTEM, dynamicColor = true), manager.settingsFlow.first())

        manager.setDynamicColor(false)
        assertFalse(manager.dynamicColorFlow.first())
    }

    @Test
    fun `dynamic color key is additive and touches nothing else`() = runBlocking {
        assertEquals("dynamic_color", DYNAMIC_COLOR_KEY.name)
        val (store, _) = openStore()
        val manager = DataStoreManager(store)
        manager.setTheme(AppTheme.DARK)
        manager.setDateFormat(DateFormat.EU)
        val before = store.data.first().asMap().filterKeys { it.name != "dynamic_color" }

        manager.setDynamicColor(true)

        val after = store.data.first().asMap()
        assertEquals(before, after.filterKeys { it.name != "dynamic_color" })
        assertEquals(true, after[booleanPreferencesKey("dynamic_color")])
        assertEquals(AppTheme.DARK, manager.themeFlow.first())
        assertEquals(DateFormat.EU, manager.dateFormatFlow.first())
    }

    @Test
    fun `an existing settings file without the key reads dynamic color as off`() = runBlocking {
        val (store, _) = openStore()
        store.edit {
            it[stringPreferencesKey("app_theme")] = "dark"
            it[stringPreferencesKey("date_format")] = "iso"
        }

        assertEquals(AppSettings(AppTheme.DARK, DateFormat.ISO, dynamicColor = false), DataStoreManager(store).settingsFlow.first())
    }

    // Sort and filter persistence (UX-11)

    @Test
    fun `sort option and filter state resolve to defaults on empty store`() = runBlocking {
        val manager = newManager()

        assertEquals(SortOption.DATE_ADDED_DESC, manager.sortOptionFlow.first())
        assertEquals(FilterState(FilterOption.ALL, null), manager.filterStateFlow.first())
    }

    @Test
    fun `sort option and category filter round trip across manager instances`() = runBlocking {
        val (store, job) = openStore()
        DataStoreManager(store).apply {
            setSortOption(SortOption.NAME_ASC)
            setFilterState(FilterState(FilterOption.CATEGORY, "Work"))
        }
        job.cancelAndJoin()

        val reopened = DataStoreManager(openStore().first)
        assertEquals(SortOption.NAME_ASC, reopened.sortOptionFlow.first())
        assertEquals(FilterState(FilterOption.CATEGORY, "Work"), reopened.filterStateFlow.first())
    }

    @Test
    fun `switching to a non-category filter clears stored filter category key`() = runBlocking {
        val (store, _) = openStore()
        val manager = DataStoreManager(store)

        manager.setFilterState(FilterState(FilterOption.CATEGORY, "Health"))
        assertEquals("Health", store.data.first()[FILTER_CATEGORY_KEY])

        manager.setFilterState(FilterState(FilterOption.FUTURE))
        assertEquals(FilterState(FilterOption.FUTURE, null), manager.filterStateFlow.first())
        assertNull(store.data.first()[FILTER_CATEGORY_KEY])
    }

    @Test
    fun `unknown stored sort and filter values fall back safely to defaults`() = runBlocking {
        val (store, _) = openStore()
        store.edit {
            it[SORT_OPTION_KEY] = "REMOVED_SORT"
            it[FILTER_OPTION_KEY] = "REMOVED_FILTER"
            it[FILTER_CATEGORY_KEY] = "Work"
        }

        val manager = DataStoreManager(store)
        assertEquals(SortOption.DATE_ADDED_DESC, manager.sortOptionFlow.first())
        assertEquals(FilterState(FilterOption.ALL, null), manager.filterStateFlow.first())
    }
}
