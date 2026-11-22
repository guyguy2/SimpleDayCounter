package com.happypuppy.daycounter

import android.app.Application
import com.happypuppy.daycounter.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MainApp)
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            modules(appModule)
        }
    }
}
