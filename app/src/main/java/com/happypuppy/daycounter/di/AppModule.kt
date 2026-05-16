package com.happypuppy.daycounter.di

import androidx.room.Room
import com.happypuppy.daycounter.data.database.AppDatabase
import com.happypuppy.daycounter.data.database.ItemRepository
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.ui.screens.ViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.jvm.java

// Create a Koin module
val appModule = module {
    // Room database with migration strategy
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "app_database"
        )
            .fallbackToDestructiveMigration() // For simplicity in development
            .build()
    }

    // DAO
    single { get<AppDatabase>().itemDao() }

    // Repository
    single { ItemRepository(get()) }

    // ViewModel
    viewModelOf(::ViewModel)

    single { DataStoreManager(androidContext()) }
}