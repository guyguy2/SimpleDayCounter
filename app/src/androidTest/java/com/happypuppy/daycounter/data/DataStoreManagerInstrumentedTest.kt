package com.happypuppy.daycounter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.daycounter.data.preferences.AppTheme
import com.happypuppy.daycounter.data.preferences.DATE_FORMAT_KEY
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.data.preferences.DateFormat
import com.happypuppy.daycounter.data.preferences.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DataStoreManager uses the process-wide `settings` DataStore, so these tests snapshot and restore
 * the values they touch. Run them on an emulator, not on a device with data you care about.
 */
@RunWith(AndroidJUnit4::class)
class DataStoreManagerInstrumentedTest {

    private lateinit var manager: DataStoreManager
    private lateinit var savedTheme: AppTheme
    private var savedFormatValue: String? = null
    private lateinit var context: Context
    private var savedVibrate = true
    private var savedCategories = emptySet<String>()

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext<Context>()
        manager = DataStoreManager(context)
        savedTheme = manager.themeFlow.first()
        savedFormatValue = context.dataStore.data.first()[DATE_FORMAT_KEY]
        savedVibrate = manager.vibrateEnabledFlow.first()
        savedCategories = manager.customCategoriesFlow.first()
        manager.clearCustomCategories()
    }

    @After
    fun tearDown() = runBlocking {
        manager.setTheme(savedTheme)
        // Restore the raw stored value, including "nothing stored".
        context.dataStore.edit { prefs ->
            savedFormatValue?.let { prefs[DATE_FORMAT_KEY] = it } ?: prefs.remove(DATE_FORMAT_KEY)
        }
        manager.setVibrateEnabled(savedVibrate)
        manager.clearCustomCategories()
        savedCategories.forEach { manager.addCustomCategory(it) }
    }

    @Test
    fun themeAndDateFormatRoundTrip() = runBlocking {
        manager.setTheme(AppTheme.DARK)
        manager.setDateFormat(DateFormat.ISO)

        assertEquals(AppTheme.DARK, manager.themeFlow.first())
        assertEquals(DateFormat.ISO, manager.dateFormatFlow.first())
    }

    @Test
    fun noStoredDateFormatMeansTheSystemDefault() = runBlocking {
        context.dataStore.edit { it.remove(DATE_FORMAT_KEY) }

        assertEquals(DateFormat.SYSTEM, manager.dateFormatFlow.first())
        // Reading never writes a choice for the user.
        assertEquals(null, context.dataStore.data.first()[DATE_FORMAT_KEY])
    }

    @Test
    fun everyStoredDateFormatIsKept() = runBlocking {
        // Includes "default": users who picked it before the system option existed keep it.
        for (format in DateFormat.entries) {
            context.dataStore.edit { it[DATE_FORMAT_KEY] = format.value }
            assertEquals(format, manager.dateFormatFlow.first())
        }
    }

    @Test
    fun vibrateRoundTrips() = runBlocking {
        manager.setVibrateEnabled(false)
        assertEquals(false, manager.vibrateEnabledFlow.first())
        manager.setVibrateEnabled(true)
        assertEquals(true, manager.vibrateEnabledFlow.first())
    }

    @Test
    fun customCategoriesAddRemoveAndClear() = runBlocking {
        assertTrue(manager.customCategoriesFlow.first().isEmpty())

        manager.addCustomCategory("Travel")
        manager.addCustomCategory("Book Club")
        manager.addCustomCategory("Travel") // set semantics, no duplicate
        assertEquals(setOf("Travel", "Book Club"), manager.customCategoriesFlow.first())

        manager.removeCustomCategory("Travel")
        assertEquals(setOf("Book Club"), manager.customCategoriesFlow.first())

        manager.clearCustomCategories()
        assertTrue(manager.customCategoriesFlow.first().isEmpty())
    }
}
