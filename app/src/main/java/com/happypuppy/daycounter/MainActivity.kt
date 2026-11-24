package com.happypuppy.daycounter

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.happypuppy.daycounter.data.preferences.AppTheme
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.data.preferences.DateFormat
import com.happypuppy.daycounter.navigation.AppNavigation
import com.happypuppy.daycounter.ui.theme.SimpleDayCounterTheme
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (BuildConfig.DEBUG) Log.d("MainActivity", "onCreate: savedInstanceState = $savedInstanceState")

        setContent {
            val dataStoreManager = koinInject<DataStoreManager>()
            val themePreference by dataStoreManager.themeFlow.collectAsState(initial = AppTheme.SYSTEM_DEFAULT)
            val dateFormat by dataStoreManager.dateFormatFlow.collectAsState(initial = DateFormat.SYSTEM)
            val isSystemInDarkTheme = isSystemInDarkTheme()

            val useDarkTheme = themePreference.isDark(isSystemInDarkTheme)

            DisposableEffect(useDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { useDarkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        LIGHT_SCRIM,
                        DARK_SCRIM
                    ) { useDarkTheme }
                )
                onDispose {}
            }

            SimpleDayCounterTheme(
                darkTheme = useDarkTheme,
                dateFormat = dateFormat
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (BuildConfig.DEBUG) Log.d("MainActivity", "onConfigurationChanged: orientation = ${newConfig.orientation}")
    }

    companion object {
        // Default scrims recommended by AndroidX Activity for edge-to-edge 3-button nav bars.
        private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
