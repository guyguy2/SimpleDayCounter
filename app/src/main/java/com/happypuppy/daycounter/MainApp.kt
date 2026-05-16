package com.happypuppy.daycounter

import android.app.Application
import android.content.res.Configuration
import com.happypuppy.daycounter.di.appModule
import com.happypuppy.daycounter.ui.theme.CategoryColors
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.core.logger.Level

class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MainApp)
            androidLogger(Level.INFO)
            modules(appModule)
        }
    }

    override fun onTerminate() {
        // Clear category color cache when app terminates
        CategoryColors.clearCustomColorCache()
        stopKoin()
        super.onTerminate()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // This is a good place to handle theme changes if needed
    }
}