package com.happypuppy.daycounter.data.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.happypuppy.daycounter.data.repository.DateConverters
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.Date

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["category_id"])]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "category_id") val categoryId: String = DEFAULT_CATEGORY_ID,
    @ColumnInfo(name = "created_at") val createdAt: Date = Date(),
    @ColumnInfo(name = "target_date") val targetDate: LocalDate,
    @ColumnInfo(name = "emoji") val emoji: String = ""
)

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY created_at DESC")
    fun getAllItems(): Flow<List<Item>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("UPDATE items SET category_id = :toCategoryId WHERE category_id = :fromCategoryId")
    suspend fun reassignCategory(fromCategoryId: String, toCategoryId: String)

    @Query("UPDATE items SET category_id = :toCategoryId WHERE id IN (:itemIds)")
    suspend fun reassignItemsToCategory(itemIds: List<Int>, toCategoryId: String)

    @Query("UPDATE items SET category_id = :defaultCategoryId WHERE category_id IN (SELECT id FROM categories WHERE is_default = 0)")
    suspend fun reassignAllCustomCategoriesToDefault(defaultCategoryId: String = DEFAULT_CATEGORY_ID)
}

@Dao
interface CategoryDao {
    @Query(
        """
        SELECT * FROM categories
        ORDER BY is_default DESC,
            CASE id
                WHEN 'personal' THEN 0
                WHEN 'work' THEN 1
                WHEN 'health' THEN 2
                WHEN 'finance' THEN 3
                ELSE 4
            END,
            name COLLATE NOCASE ASC
        """
    )
    fun getAllCategories(): Flow<List<Category>>

    @Query(
        """
        SELECT categories.*, COUNT(items.id) AS event_count
        FROM categories
        LEFT JOIN items ON categories.id = items.category_id
        GROUP BY categories.id
        ORDER BY categories.is_default DESC,
            CASE categories.id
                WHEN 'personal' THEN 0
                WHEN 'work' THEN 1
                WHEN 'health' THEN 2
                WHEN 'finance' THEN 3
                ELSE 4
            END,
            categories.name COLLATE NOCASE ASC
        """
    )
    fun getCategoriesWithUsageCount(): Flow<List<CategoryWithCount>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): Category?

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesList(): List<Category>

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByNameIgnoreCase(name: String): Category?

    @Query("SELECT * FROM categories WHERE is_default = 0 ORDER BY name COLLATE NOCASE ASC")
    suspend fun getCustomCategories(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<Category>)

    @Query("DELETE FROM categories WHERE id = :id AND is_default = 0")
    suspend fun deleteCustomCategoryById(id: String): Int

    @Query("DELETE FROM categories WHERE is_default = 0")
    suspend fun deleteAllCustomCategories(): Int

    @Query("SELECT id FROM items WHERE category_id = :categoryId")
    suspend fun getItemIdsForCategory(categoryId: String): List<Int>

    @Query("SELECT id, category_id FROM items WHERE category_id IN (SELECT id FROM categories WHERE is_default = 0)")
    suspend fun getItemIdsForCustomCategories(): List<ItemCategoryRef>
}

@Database(entities = [Item::class, Category::class], version = 5, exportSchema = true)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
}

class ItemRepository(private val itemDao: ItemDao) {
    fun getAllItems(): Flow<List<Item>> = itemDao.getAllItems()
    suspend fun insertItem(item: Item) = itemDao.insertItem(item)
    suspend fun deleteItem(item: Item) = itemDao.deleteItem(item)
}
