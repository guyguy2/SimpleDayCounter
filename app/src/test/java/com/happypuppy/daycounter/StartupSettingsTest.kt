package com.happypuppy.daycounter

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.happypuppy.daycounter.data.preferences.AppSettings
import com.happypuppy.daycounter.data.preferences.AppTheme
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.data.preferences.DateFormat
import com.happypuppy.daycounter.data.preferences.THEME_KEY
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The system splash screen stays up while `!StartupSettings.isResolved` (MainActivity's
 * setKeepOnScreenCondition), so this is the contract that decides whether the splash can hang.
 */
class StartupSettingsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `the keep condition holds until the first emission`() = runBlocking {
        val startup = StartupSettings()
        val gate = CompletableDeferred<AppSettings>()
        val job = launch { startup.collectFrom(flow { emit(gate.await()) }) }
        // Let the collector start and suspend on the gate; only then is "not resolved" meaningful.
        yield()

        assertFalse(startup.isResolved)
        assertNull(startup.settings.value)

        gate.complete(AppSettings(AppTheme.DARK, DateFormat.ISO))
        job.join()

        assertTrue(startup.isResolved)
        assertEquals(AppSettings(AppTheme.DARK, DateFormat.ISO), startup.settings.value)
    }

    @Test
    fun `the first resolved value is the stored one not a placeholder`() = runBlocking {
        val job = Job()
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { tmp.newFile("settings.preferences_pb").also { it.delete() } }
        )
        store.edit { it[THEME_KEY] = AppTheme.DARK.value }
        val startup = StartupSettings()

        startup.collectFrom(DataStoreManager(store).settingsFlow.take(1))

        assertEquals(AppTheme.DARK, startup.settings.value?.theme)
        job.cancelAndJoin()
    }

    @Test
    fun `the keep condition clears when the DataStore read fails`() = runBlocking {
        val failing = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("disk unreadable") }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                throw IOException("disk unreadable")
        }
        val startup = StartupSettings()

        withTimeout(5_000) { startup.collectFrom(DataStoreManager(failing).settingsFlow.take(1)) }

        assertTrue("splash would hang on a failed read", startup.isResolved)
        assertEquals(AppSettings.Defaults, startup.settings.value)
    }

    @Test
    fun `later emissions replace the resolved value`() = runBlocking {
        val startup = StartupSettings()
        startup.collectFrom(
            flowOf(AppSettings(AppTheme.LIGHT, DateFormat.US), AppSettings(AppTheme.DARK, DateFormat.EU))
        )

        assertEquals(AppSettings(AppTheme.DARK, DateFormat.EU), startup.settings.value)
    }
}
