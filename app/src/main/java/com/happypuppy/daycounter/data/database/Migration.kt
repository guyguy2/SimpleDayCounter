package com.happypuppy.daycounter.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

/**
 * Creates the v4 -> v5 schema migration with an injectable [zoneId] for deterministic
 * calendar-date conversion of legacy `target_date` epoch-millisecond timestamps.
 */
fun migration4To5(zoneId: ZoneId = ZoneId.systemDefault()): Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `is_default` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        val categoryIdsByLowerName = mutableMapOf<String, String>()
        for (defaultCategory in DEFAULT_CATEGORIES) {
            db.execSQL(
                "INSERT OR IGNORE INTO `categories` (`id`, `name`, `is_default`, `created_at`) VALUES (?, ?, 1, ?)",
                arrayOf<Any>(defaultCategory.id, defaultCategory.name, defaultCategory.createdAt)
            )
            categoryIdsByLowerName[defaultCategory.name.lowercase(Locale.ROOT)] = defaultCategory.id
        }

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `items_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `category_id` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `target_date` TEXT NOT NULL,
                `emoji` TEXT NOT NULL,
                FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent()
        )

        val migrationTimestamp = System.currentTimeMillis()
        db.query("SELECT `id`, `name`, `category`, `created_at`, `target_date`, `emoji` FROM `items`").use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow("id")
            val nameCol = cursor.getColumnIndexOrThrow("name")
            val categoryCol = cursor.getColumnIndexOrThrow("category")
            val createdAtCol = cursor.getColumnIndexOrThrow("created_at")
            val targetDateCol = cursor.getColumnIndexOrThrow("target_date")
            val emojiCol = cursor.getColumnIndexOrThrow("emoji")

            while (cursor.moveToNext()) {
                val id = cursor.getInt(idCol)
                val name = cursor.getString(nameCol) ?: ""
                val rawCategory = cursor.getString(categoryCol)?.trim().orEmpty()
                val createdAt = cursor.getLong(createdAtCol)
                val targetDateMillis = cursor.getLong(targetDateCol)
                val emoji = cursor.getString(emojiCol) ?: ""

                val categoryId = if (rawCategory.isBlank()) {
                    DEFAULT_CATEGORY_ID
                } else {
                    val lower = rawCategory.lowercase(Locale.ROOT)
                    categoryIdsByLowerName.getOrPut(lower) {
                        val customId = UUID.randomUUID().toString()
                        db.execSQL(
                            "INSERT INTO `categories` (`id`, `name`, `is_default`, `created_at`) VALUES (?, ?, 0, ?)",
                            arrayOf<Any>(customId, rawCategory, migrationTimestamp)
                        )
                        customId
                    }
                }

                val localDateString = Instant.ofEpochMilli(targetDateMillis)
                    .atZone(zoneId)
                    .toLocalDate()
                    .toString()

                db.execSQL(
                    """
                    INSERT INTO `items_new` (`id`, `name`, `category_id`, `created_at`, `target_date`, `emoji`)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(id, name, categoryId, createdAt, localDateString, emoji)
                )
            }
        }

        db.execSQL("DROP TABLE `items`")
        db.execSQL("ALTER TABLE `items_new` RENAME TO `items`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_category_id` ON `items` (`category_id`)")

        db.query("PRAGMA foreign_key_check").use { fkCursor ->
            if (fkCursor.moveToFirst()) {
                throw IllegalStateException("Foreign key check failed during v4 -> v5 migration")
            }
        }
    }
}

val MIGRATION_4_5: Migration = migration4To5()
