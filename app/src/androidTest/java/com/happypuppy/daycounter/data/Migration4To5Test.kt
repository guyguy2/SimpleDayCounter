package com.happypuppy.daycounter.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.happypuppy.daycounter.data.database.AppDatabase
import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.FINANCE_CATEGORY_ID
import com.happypuppy.daycounter.data.database.HEALTH_CATEGORY_ID
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.MIGRATION_4_5
import com.happypuppy.daycounter.data.database.WORK_CATEGORY_ID
import com.happypuppy.daycounter.data.database.migration4To5
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class Migration4To5Test {

    private val testDbName = "migration-4-5-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrateEmptyV4ToV5() {
        helper.createDatabase(testDbName, 4).close()

        val db = helper.runMigrationsAndValidate(testDbName, 5, true, MIGRATION_4_5)
        db.query("SELECT `id`, `name`, `is_default` FROM `categories`").use { cursor ->
            val rows = mutableListOf<Triple<String, String, Int>>()
            while (cursor.moveToNext()) {
                rows.add(Triple(cursor.getString(0), cursor.getString(1), cursor.getInt(2)))
            }
            assertEquals(4, rows.size)
            assertTrue(rows.contains(Triple("personal", "Personal", 1)))
            assertTrue(rows.contains(Triple("work", "Work", 1)))
            assertTrue(rows.contains(Triple("health", "Health", 1)))
            assertTrue(rows.contains(Triple("finance", "Finance", 1)))
        }
        db.query("SELECT COUNT(*) FROM `items`").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrateV4WithDefaultsCustomAndOrphanedCategories() {
        val chicago = ZoneId.of("America/Chicago")
        val targetEpoch = LocalDateTime.of(2026, 7, 4, 12, 0).atZone(chicago).toInstant().toEpochMilli()

        helper.createDatabase(testDbName, 4).apply {
            execSQL(
                "INSERT INTO `items` (`id`, `name`, `category`, `created_at`, `target_date`, `emoji`) VALUES (1, 'Dentist', 'Health', 1000, ?, '')",
                arrayOf<Any>(targetEpoch)
            )
            execSQL(
                "INSERT INTO `items` (`id`, `name`, `category`, `created_at`, `target_date`, `emoji`) VALUES (2, 'Standup', 'work', 2000, ?, '')",
                arrayOf<Any>(targetEpoch)
            )
            execSQL(
                "INSERT INTO `items` (`id`, `name`, `category`, `created_at`, `target_date`, `emoji`) VALUES (3, 'Flight', '  Travel  ', 3000, ?, '\u2708\uFE0F')",
                arrayOf<Any>(targetEpoch)
            )
            execSQL(
                "INSERT INTO `items` (`id`, `name`, `category`, `created_at`, `target_date`, `emoji`) VALUES (4, 'Orphaned', 'Legacy Custom', 4000, ?, '')",
                arrayOf<Any>(targetEpoch)
            )
            execSQL(
                "INSERT INTO `items` (`id`, `name`, `category`, `created_at`, `target_date`, `emoji`) VALUES (5, 'Blank Cat', '   ', 5000, ?, '')",
                arrayOf<Any>(targetEpoch)
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 5, true, migration4To5(chicago))

        val categoriesByName = mutableMapOf<String, Pair<String, Int>>()
        db.query("SELECT `id`, `name`, `is_default` FROM `categories`").use { cursor ->
            while (cursor.moveToNext()) {
                categoriesByName[cursor.getString(1)] = cursor.getString(0) to cursor.getInt(2)
            }
        }
        // 4 defaults + Travel + Legacy Custom (no duplicate for lowercase 'work', blank mapped to personal).
        assertEquals(6, categoriesByName.size)
        assertEquals(0, categoriesByName["Travel"]?.second)
        assertEquals(0, categoriesByName["Legacy Custom"]?.second)

        val itemsById = mutableMapOf<Int, Triple<String, String, String>>()
        db.query("SELECT `id`, `category_id`, `target_date`, `emoji` FROM `items`").use { cursor ->
            while (cursor.moveToNext()) {
                itemsById[cursor.getInt(0)] = Triple(cursor.getString(1), cursor.getString(2), cursor.getString(3))
            }
        }
        assertEquals(5, itemsById.size)
        assertEquals(HEALTH_CATEGORY_ID, itemsById[1]?.first)
        assertEquals(WORK_CATEGORY_ID, itemsById[2]?.first)
        assertEquals(categoriesByName["Travel"]?.first, itemsById[3]?.first)
        assertEquals("\u2708\uFE0F", itemsById[3]?.third)
        assertEquals(categoriesByName["Legacy Custom"]?.first, itemsById[4]?.first)
        assertEquals(DEFAULT_CATEGORY_ID, itemsById[5]?.first)
        assertTrue(itemsById.values.all { it.second == "2026-07-04" })
        db.close()
    }

    @Test
    fun migrateV4ConvertsDstAndTimezoneBoundaryDatesDeterministically() {
        val chicago = ZoneId.of("America/Chicago")
        // 2026-03-08 23:30 CDT (spring-forward day) is already 2026-03-09 04:30 UTC.
        val chicagoDstNightMillis = LocalDateTime.of(2026, 3, 8, 23, 30)
            .atZone(chicago)
            .toInstant()
            .toEpochMilli()
        // 2026-11-01 01:30 CST (fall-back day) is 2026-11-01 07:30 UTC.
        val chicagoFallBackMillis = LocalDateTime.of(2026, 11, 1, 1, 30)
            .atZone(chicago)
            .toInstant()
            .toEpochMilli()

        helper.createDatabase(testDbName, 4).apply {
            execSQL(
                "INSERT INTO `items` (`id`, `name`, `category`, `created_at`, `target_date`, `emoji`) VALUES (1, 'Spring', 'Personal', 1000, ?, '')",
                arrayOf<Any>(chicagoDstNightMillis)
            )
            execSQL(
                "INSERT INTO `items` (`id`, `name`, `category`, `created_at`, `target_date`, `emoji`) VALUES (2, 'Fall', 'Finance', 2000, ?, '')",
                arrayOf<Any>(chicagoFallBackMillis)
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 5, true, migration4To5(chicago))
        db.query("SELECT `id`, `target_date`, `category_id` FROM `items` ORDER BY `id` ASC").use { cursor ->
            assertTrue(cursor.moveToNext())
            assertEquals("2026-03-08", cursor.getString(1))
            assertEquals(DEFAULT_CATEGORY_ID, cursor.getString(2))

            assertTrue(cursor.moveToNext())
            assertEquals("2026-11-01", cursor.getString(1))
            assertEquals(FINANCE_CATEGORY_ID, cursor.getString(2))
        }
        db.close()
    }

    @Test
    fun postMigrationDatabaseSupportsRoomCrudAndForeignKeys() = runBlocking {
        val utc = ZoneId.of("UTC")
        val targetMillis = LocalDateTime.of(2026, 12, 25, 12, 0).atZone(utc).toInstant().toEpochMilli()

        helper.createDatabase(testDbName, 4).apply {
            execSQL(
                "INSERT INTO `items` (`id`, `name`, `category`, `created_at`, `target_date`, `emoji`) VALUES (1, 'Holiday', 'Travel', 1000, ?, '')",
                arrayOf<Any>(targetMillis)
            )
            close()
        }

        val migratedSqlite = helper.runMigrationsAndValidate(testDbName, 5, true, migration4To5(utc))
        migratedSqlite.close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, testDbName)
            .addMigrations(migration4To5(utc))
            .build()
        try {
            val categories = roomDb.categoryDao().getAllCategories().first()
            val travel = categories.find { it.name == "Travel" }
            assertNotNull(travel)
            assertFalse(travel!!.isDefault)

            val items = roomDb.itemDao().getAllItems().first()
            assertEquals(1, items.size)
            assertEquals(LocalDate.of(2026, 12, 25), items.single().targetDate)
            assertEquals(travel.id, items.single().categoryId)

            val music = Category(id = "music-id", name = "Music", isDefault = false, createdAt = 999L)
            roomDb.categoryDao().insertCategory(music)
            roomDb.itemDao().insertItem(
                Item(name = "Concert", categoryId = music.id, targetDate = LocalDate.of(2027, 5, 1))
            )
            assertEquals(2, roomDb.itemDao().getAllItems().first().size)
        } finally {
            roomDb.close()
            context.deleteDatabase(testDbName)
        }
    }
}
