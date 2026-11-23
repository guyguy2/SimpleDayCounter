package com.happypuppy.daycounter.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.daycounter.data.database.AppDatabase
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/** Real Room (in-memory) coverage for the DAO contract the ViewModel relies on. */
@RunWith(AndroidJUnit4::class)
class ItemDaoInstrumentedTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.itemDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun item(name: String, createdAt: Long, id: Int = 0) = Item(
        id = id,
        name = name,
        itemCategory = "Work",
        createdAt = Date(createdAt),
        targetDate = Date(1_800_000_000_000L),
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
    fun fieldsRoundTripIncludingEmojiAndDates() = runBlocking {
        val original = item("trip", createdAt = 42)
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
}
