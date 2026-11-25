package com.happypuppy.daycounter.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.daycounter.data.database.AddCategoryResult
import com.happypuppy.daycounter.data.database.AppDatabase
import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.data.database.CategoryDao
import com.happypuppy.daycounter.data.database.CategoryRepository
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORIES
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.FINANCE_CATEGORY_ID
import com.happypuppy.daycounter.data.database.HEALTH_CATEGORY_ID
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemDao
import com.happypuppy.daycounter.data.database.TransactionRunner
import com.happypuppy.daycounter.data.database.WORK_CATEGORY_ID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CategoryDaoInstrumentedTest {

    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var itemDao: ItemDao
    private lateinit var repository: CategoryRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        categoryDao = db.categoryDao()
        itemDao = db.itemDao()
        repository = CategoryRepository(
            categoryDao = categoryDao,
            itemDao = itemDao,
            transactionRunner = object : TransactionRunner {
                override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction { block() }
            }
        )
        repository.ensureInitialized()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun defaultsAreOrderedBeforeCaseInsensitiveCustomCategories() = runBlocking {
        categoryDao.insertCategory(Category(id = "z-id", name = "zeta", isDefault = false, createdAt = 1L))
        categoryDao.insertCategory(Category(id = "a-id", name = "Alpha", isDefault = false, createdAt = 2L))

        val ids = categoryDao.getAllCategories().first().map { it.id }
        assertEquals(
            listOf(DEFAULT_CATEGORY_ID, WORK_CATEGORY_ID, HEALTH_CATEGORY_ID, FINANCE_CATEGORY_ID, "a-id", "z-id"),
            ids
        )
    }

    @Test
    fun usageCountsReflectLinkedItems() = runBlocking {
        val travel = (repository.addCustomCategory("Travel") as AddCategoryResult.Added).category
        val books = (repository.addCustomCategory("Books") as AddCategoryResult.Added).category
        itemDao.insertItem(Item(name = "Flight", categoryId = travel.id, targetDate = LocalDate.of(2026, 7, 1)))
        itemDao.insertItem(Item(name = "Hotel", categoryId = travel.id, targetDate = LocalDate.of(2026, 7, 2)))

        val counts = categoryDao.getCategoriesWithUsageCount().first().associate { it.category.id to it.eventCount }
        assertEquals(2, counts[travel.id])
        assertEquals(0, counts[books.id])
        assertEquals(0, counts[DEFAULT_CATEGORY_ID])
    }

    @Test
    fun deletingInUseCategoryDirectlyThrowsRestrictConstraintViolation() = runBlocking {
        val travel = (repository.addCustomCategory("Travel") as AddCategoryResult.Added).category
        itemDao.insertItem(Item(name = "Flight", categoryId = travel.id, targetDate = LocalDate.of(2026, 7, 1)))

        try {
            categoryDao.deleteCustomCategoryById(travel.id)
            fail("Expected SQLiteConstraintException due to ON DELETE RESTRICT")
        } catch (_: SQLiteConstraintException) {
            // expected
        }
    }

    @Test
    fun defaultCategoriesCannotBeDeletedByDaoOrRepository() = runBlocking {
        assertEquals(0, categoryDao.deleteCustomCategoryById(DEFAULT_CATEGORY_ID))
        assertNull(repository.deleteCustomCategory(WORK_CATEGORY_ID))
        assertEquals(DEFAULT_CATEGORIES.size, categoryDao.getAllCategories().first().size)
    }

    @Test
    fun repositoryDeleteAndRestoreIsTransactionalAndPreservesOnlyAffectedItems() = runBlocking {
        val travel = (repository.addCustomCategory("Travel") as AddCategoryResult.Added).category
        itemDao.insertItem(Item(name = "Flight", categoryId = travel.id, targetDate = LocalDate.of(2026, 7, 1)))
        itemDao.insertItem(Item(name = "Dentist", categoryId = DEFAULT_CATEGORY_ID, targetDate = LocalDate.of(2026, 7, 2)))

        val snapshot = repository.deleteCustomCategory(travel.id)
        assertNotNull(snapshot)
        assertNull(categoryDao.getCategoryById(travel.id))
        assertTrue(itemDao.getAllItems().first().all { it.categoryId == DEFAULT_CATEGORY_ID })

        itemDao.insertItem(Item(name = "Groceries", categoryId = DEFAULT_CATEGORY_ID, targetDate = LocalDate.of(2026, 7, 3)))
        repository.restoreDeletedCategories(snapshot!!)

        val items = itemDao.getAllItems().first()
        assertEquals(travel.id, items.first { it.name == "Flight" }.categoryId)
        assertEquals(DEFAULT_CATEGORY_ID, items.first { it.name == "Dentist" }.categoryId)
        assertEquals(DEFAULT_CATEGORY_ID, items.first { it.name == "Groceries" }.categoryId)
    }
}
