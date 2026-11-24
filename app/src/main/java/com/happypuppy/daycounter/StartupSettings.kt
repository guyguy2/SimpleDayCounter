package com.happypuppy.daycounter

import com.happypuppy.daycounter.data.preferences.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the first resolved [AppSettings] for the root of the app. [settings] is null only until the
 * source emits once; MainActivity keeps the system splash screen up while [isResolved] is false and
 * draws nothing until it is true, so the first frame never uses a placeholder theme.
 *
 * The source must not fail on a read error: [com.happypuppy.daycounter.data.preferences.DataStoreManager.settingsFlow]
 * turns an IOException into the defaults, which is what releases the splash on a failed read.
 */
class StartupSettings {
    private val resolved = MutableStateFlow<AppSettings?>(null)

    val settings: StateFlow<AppSettings?> = resolved

    val isResolved: Boolean get() = resolved.value != null

    suspend fun collectFrom(source: Flow<AppSettings>) {
        source.collect { resolved.value = it }
    }
}
