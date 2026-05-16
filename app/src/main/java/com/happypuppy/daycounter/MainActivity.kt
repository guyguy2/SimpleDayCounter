package com.happypuppy.daycounter

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.happypuppy.daycounter.data.preferences.AppTheme
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.navigation.AppNavigation
import com.happypuppy.daycounter.ui.theme.SimpleDayCounterTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate: savedInstanceState = $savedInstanceState")

        setContent {
            AppContent()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("MainActivity", "onConfigurationChanged: orientation = ${newConfig.orientation}")
    }
}

@Composable
fun AppContent() {
    val dataStoreManager = koinInject<DataStoreManager>()
    val themePreference = dataStoreManager.themeFlow.collectAsState(initial = AppTheme.SYSTEM_DEFAULT)
    val isSystemInDarkTheme = isSystemInDarkTheme()

    Log.d("AppTheme", "themePreference: ${themePreference.value}")
    Log.d("AppTheme", "isSystemInDarkTheme: $isSystemInDarkTheme")

    val selectedTheme = when (themePreference.value) {
        AppTheme.SYSTEM_DEFAULT -> isSystemInDarkTheme
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    Log.d("AppTheme", "Final darkTheme setting: $selectedTheme")

    SimpleDayCounterTheme(
        darkTheme = selectedTheme
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation()
        }
    }
}