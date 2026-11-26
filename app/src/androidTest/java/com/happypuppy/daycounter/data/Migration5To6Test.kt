package com.happypuppy.daycounter.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.happypuppy.daycounter.data.database.AppDatabase
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.MIGRATION_4_5
import com.happypuppy.daycounter.data.database.MIGRATION_5_6
import com.happypuppy.daycounter.data.database.WORK_CATEGORY_ID
import com.happypuppy.daycounter.data.database.Item
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class Migration5To6Test {

    private val testDbName = "migration-5-6-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrateEmptyV5ToV6() {
        helper.createDatabase(testDbName, 5).close()

        val db = helper.runMigrationsAndValidate(testDbName, 6, true, MIGRATION_5_6)
        db.query("SELECT COUNT(*) FROM `items`").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrateV5WithExistingRowsDefaultsRepeatsYearlyToZero() {
        helper.createDatabase(testDbName, 5).apply {
            execSQL(
                "INSERT INTO `categories` (`id`, `name`, `is_default`, `created_at`) VALUES ('personal', 'Personal', 1, 1)"
            )
            execSQL(
                "INSERT INTO `categories` (`id`, `name`, `is_default`, `created_at`) VALUES ('work', 'Work', 1, 2)"
            )
            execSQL(
                "INSERT INTO `items` (`id`, `name`, `category_id`, `created_at`, `target_date`, `emoji`) VALUES (1, 'Birthday', 'personal', 1000, '2000-05-20', '\uD83C\uDF82')"
            )
            execSQL(
                "INSERT INTO `items` (`id`, `name`, `category_id`, `created_at`, `target_date`, `emoji`) VALUES (2, 'Deadline', 'work', 2000, '2026-09-01', '')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 6, true, MIGRATION_5_6)
        db.query("SELECT `id`, `name`, `category_id`, `target_date`, `emoji`, `repeats_yearly` FROM `items` ORDER BY `id` ASC").use { cursor ->
            assertTrue(cursor.moveToNext())
            assertEquals(1, cursor.getInt(0))
            assertEquals("Birthday", cursor.getString(1))
            assertEquals(DEFAULT_CATEGORY_ID, cursor.getString(2))
            assertEquals("2000-05-20", cursor.getString(3))
            assertEquals("\uD83C\uDF82", cursor.getString(4))
            assertEquals(0, cursor.getInt(5))

            assertTrue(cursor.moveToNext())
            assertEquals(2, cursor.getInt(0))
            assertEquals("Deadline", cursor.getString(1))
            assertEquals(WORK_CATEGORY_ID, cursor.getString(2))
            assertEquals("2026-09-01", cursor.getString(3))
            assertEquals("", cursor.getString(4))
            assertEquals(0, cursor.getInt(5))
        }
        db.close()
    }

    @Test
    fun postMigrationV6DatabaseSupportsRoomCrudWithRepeatsYearly() = runBlocking {
        helper.createDatabase(testDbName, 5).apply {
            execSQL(
                "INSERT INTO `categories` (`id`, `name`, `is_default`, `created_at`) VALUES ('personal', 'Personal', 1, 1)"
            )
            execSQL(
                "INSERT INTO `items` (`id`, `name`, `category_id`, `created_at`, `target_date`, `emoji`) VALUES (1, 'One-off', 'personal', 1000, '2026-07-04', '')"
            )
            close()
        }

        helper.runMigrationsAndValidate(testDbName, 6, true, MIGRATION_5_6).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, testDbName)
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
            .build()
        try {
            val migrated = roomDb.itemDao().getAllItems().first()
            assertEquals(1, migrated.size)
            assertFalse(migrated.single().repeatsYearly)

            roomDb.itemDao().insertItem(
                Item(
                    name = "Anniversary",
                    categoryId = DEFAULT_CATEGORY_ID,
                    targetDate = LocalDate.of(2020, 8, 15),
                    emoji = "\u2764\uFE0F",
                    repeatsYearly = true
                )
            )
            val all = roomDb.itemDao().getAllItems().first()
            assertEquals(2, all.size)
            val anniversary = all.first { it.name == "Anniversary" }
            assertTrue(anniversary.repeatsYearly)
            assertEquals(LocalDate.of(2020, 8, 15), anniversary.targetDate)
            assertFalse(all.first { it.name == "One-off" }.repeatsYearly)
        } finally {
            roomDb.close()
            context.deleteDatabase(testDbName)
        }
    }
}
