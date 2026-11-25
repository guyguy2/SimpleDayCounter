package com.happypuppy.daycounter.di

import androidx.room.Room
import androidx.room.withTransaction
import com.happypuppy.daycounter.data.database.AppDatabase
import com.happypuppy.daycounter.data.database.CategoryRepository
import com.happypuppy.daycounter.data.database.ItemRepository
import com.happypuppy.daycounter.data.database.MIGRATION_4_5
import com.happypuppy.daycounter.data.database.TransactionRunner
import com.happypuppy.daycounter.data.preferences.CategoryMigrationPreferences
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.data.preferences.EventListPreferences
import com.happypuppy.daycounter.data.preferences.dataStore
import com.happypuppy.daycounter.ui.screens.CategoryManagementViewModel
import com.happypuppy.daycounter.ui.screens.EventViewModel
import com.happypuppy.daycounter.util.midnightDateFlow
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.time.Clock

// Create a Koin module
val appModule = module {
    // Room database with migration strategy
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "app_database"
        )
            .addMigrations(MIGRATION_4_5)
            // Allow destructive upgrade only from legacy pre-v4 dev schemas;
            // v4 -> v5+ uses explicit Room Migrations.
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3)
            .build()
    }

    // DAOs
    single { get<AppDatabase>().itemDao() }
    single { get<AppDatabase>().categoryDao() }

    // Repositories
    single { ItemRepository(get()) }
    single {
        val database = get<AppDatabase>()
        CategoryRepository(
            categoryDao = get(),
            itemDao = get(),
            migrationPreferences = get(),
            transactionRunner = object : TransactionRunner {
                override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction { block() }
            },
            clock = get()
        )
    }

    // Clock
    single<Clock> { Clock.systemDefaultZone() }

    // ViewModels
    viewModel {
        EventViewModel(
            repository = get(),
            categoryRepository = get(),
            listPreferences = get(),
            clock = get(),
            midnightTicks = midnightDateFlow(get())
        )
    }
    viewModel {
        CategoryManagementViewModel(
            repository = get(),
            listPreferences = get()
        )
    }

    single { DataStoreManager(androidContext().dataStore) }
    single<EventListPreferences> { get<DataStoreManager>() }
    single<CategoryMigrationPreferences> { get<DataStoreManager>() }
}