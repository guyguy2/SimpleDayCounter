package com.happypuppy.daycounter

import com.happypuppy.daycounter.data.preferences.AppSettings
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Holds the first resolved [AppSettings] for the root of the app. [settings] is null only until the
 * source emits once; MainActivity keeps the system splash screen up while [isResolved] is false and
 * draws nothing until it is true, so the first frame never uses a placeholder theme.
 *
 * A first read that stalls for [FIRST_READ_TIMEOUT_MS] releases the splash with the defaults.
 * The source must not fail on a read error: [com.happypuppy.daycounter.data.preferences.DataStoreManager.settingsFlow]
 * turns an IOException into the defaults, which is what releases the splash on a failed read.
 */
class StartupSettings {
    private val resolved = MutableStateFlow<AppSettings?>(null)

    val settings: StateFlow<AppSettings?> = resolved

    val isResolved: Boolean get() = resolved.value != null

    /**
     * Collects [source] into [settings]. If the first value has not arrived after
     * [FIRST_READ_TIMEOUT_MS] (a stalled read), [AppSettings.Defaults] are used so the splash
     * cannot be held forever; a real value that arrives later still replaces them.
     */
    suspend fun collectFrom(source: Flow<AppSettings>) = coroutineScope {
        val timeout = launch {
            delay(FIRST_READ_TIMEOUT_MS)
            resolved.compareAndSet(null, AppSettings.Defaults)
        }
        source.collect {
            timeout.cancel()
            resolved.value = it
        }
    }

    companion object {
        /** How long the splash may wait for the first settings read before using the defaults. */
        const val FIRST_READ_TIMEOUT_MS = 2_000L
    }
}
