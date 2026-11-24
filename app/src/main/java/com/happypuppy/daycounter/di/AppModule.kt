package com.happypuppy.daycounter.di

import androidx.room.Room
import com.happypuppy.daycounter.data.database.AppDatabase
import com.happypuppy.daycounter.data.database.ItemRepository
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.data.preferences.dataStore
import com.happypuppy.daycounter.ui.screens.EventViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Create a Koin module
val appModule = module {
    // Room database with migration strategy
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "app_database"
        )
            // Allow destructive upgrade only from legacy pre-v4 dev schemas;
            // future schema bumps (v4 -> v5+) must provide explicit Room Migrations.
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3)
            .build()
    }

    // DAO
    single { get<AppDatabase>().itemDao() }

    // Repository
    single { ItemRepository(get()) }

    // ViewModel
    viewModelOf(::EventViewModel)

    single { DataStoreManager(androidContext().dataStore) }
}