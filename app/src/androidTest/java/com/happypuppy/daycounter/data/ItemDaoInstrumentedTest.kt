package com.happypuppy.daycounter.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.daycounter.data.database.AppDatabase
import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.data.database.CategoryDao
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORIES
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemDao
import com.happypuppy.daycounter.data.database.WORK_CATEGORY_ID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.Date

/** Real Room (in-memory) coverage for the v5 ItemDao & foreign-key contract. */
@RunWith(AndroidJUnit4::class)
class ItemDaoInstrumentedTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.itemDao()
        categoryDao = db.categoryDao()
        categoryDao.insertCategories(DEFAULT_CATEGORIES)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun item(
        name: String,
        createdAt: Long,
        id: Int = 0,
        categoryId: String = WORK_CATEGORY_ID,
        targetDate: LocalDate = LocalDate.of(2027, 1, 15)
    ) = Item(
        id = id,
        name = name,
        categoryId = categoryId,
        createdAt = Date(createdAt),
        targetDate = targetDate,
        emoji = "\u2764\uFE0F"
    )

    @Test
    fun emptyDatabaseEmitsEmptyList() = runBlocking {
        assertTrue(dao.getAllItems().first().isEmpty())
    }

    @Test
    fun itemsAreOrderedByCreatedAtDescending() = runBlocking {
        dao.insertItem(item("old", createdAt = 1_000))
        dao.insertItem(item("newest", createdAt = 3_000))
        dao.insertItem(item("middle", createdAt = 2_000))

        assertEquals(listOf("newest", "middle", "old"), dao.getAllItems().first().map { it.name })
    }

    @Test
    fun insertWithExistingIdReplacesTheRow() = runBlocking {
        dao.insertItem(item("first", createdAt = 1))
        val stored = dao.getAllItems().first().single()

        dao.insertItem(stored.copy(name = "renamed"))

        val after = dao.getAllItems().first()
        assertEquals(1, after.size)
        assertEquals("renamed", after.single().name)
        assertEquals(stored.id, after.single().id)
    }

    @Test
    fun fieldsRoundTripIncludingEmojiAndLocalDate() = runBlocking {
        val original = item("trip", createdAt = 42, targetDate = LocalDate.of(2028, 2, 29))
        dao.insertItem(original)

        val loaded = dao.getAllItems().first().single()
        assertEquals(original.copy(id = loaded.id), loaded)
    }

    @Test
    fun deleteRemovesOnlyThatRowAndAllowsReinsertWithSameId() = runBlocking {
        dao.insertItem(item("a", createdAt = 1))
        dao.insertItem(item("b", createdAt = 2))
        val (b, a) = dao.getAllItems().first()

        dao.deleteItem(b)
        assertEquals(listOf("a"), dao.getAllItems().first().map { it.name })

        // Undo path in EventViewModel re-inserts the original object, id included.
        dao.insertItem(b)
        assertEquals(setOf(a.id, b.id), dao.getAllItems().first().map { it.id }.toSet())
    }

    @Test
    fun insertingItemWithUnknownCategoryIdViolatesForeignKey() = runBlocking {
        try {
            dao.insertItem(item("orphan", createdAt = 1, categoryId = "does-not-exist"))
            fail("Expected SQLiteConstraintException for missing category_id FK")
        } catch (_: SQLiteConstraintException) {
            // expected
        }
    }

    @Test
    fun reassignCategoryAndReassignItemsMoveExpectedRows() = runBlocking {
        val travel = Category(id = "travel-id", name = "Travel", isDefault = false, createdAt = 10L)
        categoryDao.insertCategory(travel)
        dao.insertItem(item("Flight", createdAt = 1, categoryId = travel.id))
        dao.insertItem(item("Hotel", createdAt = 2, categoryId = travel.id))
        dao.insertItem(item("Desk", createdAt = 3, categoryId = WORK_CATEGORY_ID))

        val affectedIds = categoryDao.getItemIdsForCategory(travel.id)
        assertEquals(2, affectedIds.size)

        dao.reassignCategory(travel.id, DEFAULT_CATEGORY_ID)
        val afterDeleteMove = dao.getAllItems().first()
        assertEquals(DEFAULT_CATEGORY_ID, afterDeleteMove.first { it.name == "Flight" }.categoryId)
        assertEquals(DEFAULT_CATEGORY_ID, afterDeleteMove.first { it.name == "Hotel" }.categoryId)
        assertEquals(WORK_CATEGORY_ID, afterDeleteMove.first { it.name == "Desk" }.categoryId)

        dao.reassignItemsToCategory(affectedIds, travel.id)
        val afterUndoMove = dao.getAllItems().first()
        assertEquals(travel.id, afterUndoMove.first { it.name == "Flight" }.categoryId)
        assertEquals(travel.id, afterUndoMove.first { it.name == "Hotel" }.categoryId)
        assertEquals(WORK_CATEGORY_ID, afterUndoMove.first { it.name == "Desk" }.categoryId)
    }
}
