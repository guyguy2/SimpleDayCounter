package com.happypuppy.daycounter.ui.screens

import app.cash.turbine.test
import com.happypuppy.daycounter.data.database.CategoryRepository
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.HEALTH_CATEGORY_ID
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemRepository
import com.happypuppy.daycounter.data.database.WORK_CATEGORY_ID
import com.happypuppy.daycounter.util.DayLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class EventViewModelTest {

    private lateinit var dao: FakeItemDao
    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var repository: ItemRepository
    private lateinit var categoryRepository: CategoryRepository
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeItemDao()
        categoryDao = FakeCategoryDao(dao)
        repository = ItemRepository(dao)
        categoryRepository = CategoryRepository(categoryDao, dao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun item(
        id: Int,
        name: String = "event-$id",
        categoryId: String = DEFAULT_CATEGORY_ID,
        emoji: String = ""
    ): Item = Item(
        id = id,
        name = name,
        categoryId = categoryId,
        targetDate = LocalDate.of(2026, 11, 14),
        emoji = emoji
    )

    @Test
    fun `saveItem inserts new item into repository and updates uiState`() = runTest(dispatcher) {
        val viewModel = EventViewModel(repository, categoryRepository)
        advanceUntilIdle()

        val target = LocalDate.of(2027, 1, 15)
        viewModel.saveItem(name = "Vacation", categoryId = DEFAULT_CATEGORY_ID, targetDate = target, emoji = "\u2708\uFE0F")
        advanceUntilIdle()

        val events = viewModel.uiState.value.events
        assertEquals(1, events.size)
        assertEquals("Vacation", events.first().name)
        assertEquals(DEFAULT_CATEGORY_ID, events.first().categoryId)
        assertEquals("Personal", events.first().categoryName)
        assertEquals(target, events.first().targetDate)
        assertEquals("\u2708\uFE0F", events.first().emoji)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `addCustomCategory inserts into Room and exposes in uiState categories`() = runTest(dispatcher) {
        val viewModel = EventViewModel(repository, categoryRepository)
        advanceUntilIdle()

        val created = viewModel.addCustomCategory("  travel plans ")
        advanceUntilIdle()

        assertNotNull(created)
        assertEquals("Travel Plans", created!!.name)
        assertTrue(viewModel.uiState.value.categories.any { it.id == created.id && it.name == "Travel Plans" })
    }

    @Test
    fun `updateItem replaces existing item in repository`() = runTest(dispatcher) {
        val original = item(id = 12, name = "Old Name", categoryId = WORK_CATEGORY_ID, emoji = "\uD83D\uDCBC")
        dao.insertItem(original)
        val viewModel = EventViewModel(repository, categoryRepository)
        advanceUntilIdle()

        val updated = original.copy(name = "New Name", categoryId = HEALTH_CATEGORY_ID, emoji = "\uD83C\uDFC3")
        viewModel.updateItem(updated)
        advanceUntilIdle()

        val events = viewModel.uiState.value.events
        assertEquals(1, events.size)
        assertEquals("New Name", events.first().name)
        assertEquals(HEALTH_CATEGORY_ID, events.first().categoryId)
        assertEquals("Health", events.first().categoryName)
        assertEquals("\uD83C\uDFC3", events.first().emoji)
    }

    @Test
    fun `deleteItem hides event immediately, emits Channel snackbar event, and deletes from repository`() =
        runTest(dispatcher) {
            dao.insertItem(item(1, "to-delete"))
            val viewModel = EventViewModel(repository, categoryRepository)
            advanceUntilIdle()

            viewModel.events.test {
                viewModel.deleteItem(item(1, "to-delete"))
                advanceUntilIdle()

                assertEquals(MainUiEvent.ShowUndoSnackbar("to-delete"), awaitItem())
                assertTrue(viewModel.uiState.value.events.isEmpty())
                assertFalse(viewModel.uiState.value.hasAnyEvents)
                assertTrue(viewModel.uiState.value.canUndo)
                assertTrue(dao.currentItems.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `undoDelete restores item, increments restoreVersion, and clears canUndo`() = runTest(dispatcher) {
        dao.insertItem(item(7, "boom"))
        val viewModel = EventViewModel(repository, categoryRepository)
        advanceUntilIdle()

        viewModel.deleteItem(item(7, "boom"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.events.isEmpty())
        assertTrue(viewModel.uiState.value.canUndo)

        viewModel.undoDelete()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.canUndo)
        assertEquals(1, state.events.size)
        assertEquals(7, state.events.first().id)
        assertEquals(1, state.events.first().restoreVersion)
    }

    @Test
    fun `consecutive deletes emit Channel events and undo restores in LIFO order`() = runTest(dispatcher) {
        dao.insertItem(item(10, "first"))
        dao.insertItem(item(20, "second"))
        val viewModel = EventViewModel(repository, categoryRepository)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.deleteItem(item(10, "first"))
            viewModel.deleteItem(item(20, "second"))
            advanceUntilIdle()

            assertEquals(MainUiEvent.ShowUndoSnackbar("first"), awaitItem())
            assertEquals(MainUiEvent.ShowUndoSnackbar("second"), awaitItem())
            assertTrue(viewModel.uiState.value.events.isEmpty())
            assertTrue(viewModel.uiState.value.canUndo)

            viewModel.undoDelete()
            advanceUntilIdle()
            assertEquals(MainUiEvent.ShowUndoSnackbar("first"), awaitItem())
            assertEquals(listOf(20), viewModel.uiState.value.events.map { it.id })
            assertTrue(viewModel.uiState.value.canUndo)

            viewModel.undoDelete()
            advanceUntilIdle()
            assertEquals(setOf(10, 20), viewModel.uiState.value.events.map { it.id }.toSet())
            assertFalse(viewModel.uiState.value.canUndo)
            expectNoEvents()
        }
    }

    @Test
    fun `dismissUndoSnackbar clears undo stack without touching the repository`() = runTest(dispatcher) {
        dao.insertItem(item(3, "fleeting"))
        val viewModel = EventViewModel(repository, categoryRepository)
        advanceUntilIdle()

        viewModel.deleteItem(item(3, "fleeting"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canUndo)

        viewModel.dismissUndoSnackbar()
        viewModel.undoDelete()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canUndo)
        assertTrue(viewModel.uiState.value.events.isEmpty())
        assertTrue(dao.currentItems.isEmpty())
    }

    @Test
    fun `undoing the same id twice produces strictly increasing restoreVersion`() = runTest(dispatcher) {
        dao.insertItem(item(5, "again"))
        val viewModel = EventViewModel(repository, categoryRepository)
        advanceUntilIdle()

        viewModel.deleteItem(item(5, "again"))
        advanceUntilIdle()
        viewModel.undoDelete()
        advanceUntilIdle()
        val firstVersion = viewModel.uiState.value.events.single().restoreVersion
        assertEquals(1, firstVersion)

        viewModel.deleteItem(item(5, "again"))
        advanceUntilIdle()
        viewModel.undoDelete()
        advanceUntilIdle()
        val secondVersion = viewModel.uiState.value.events.single().restoreVersion
        assertEquals(2, secondVersion)
        assertTrue(secondVersion > firstVersion)
    }

    @Test
    fun `database read error propagates to uiState error and clears loading`() = runTest(dispatcher) {
        val failingDao = FakeItemDao(flowError = IOException("Disk corrupted"))
        val viewModel = EventViewModel(ItemRepository(failingDao), categoryRepository)
        advanceUntilIdle()

        assertEquals(EventError.LoadFailed, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `database write errors on save, update, and delete propagate to uiState error`() = runTest(dispatcher) {
        val existing = item(1, "existing")
        dao.insertItem(existing)
        val viewModel = EventViewModel(repository, categoryRepository)
        advanceUntilIdle()

        dao.insertError = IOException("Insert failed")
        viewModel.saveItem("New", WORK_CATEGORY_ID, LocalDate.of(2026, 6, 15))
        advanceUntilIdle()
        assertEquals(EventError.SaveFailed, viewModel.uiState.value.error)

        viewModel.clearError()
        viewModel.updateItem(existing.copy(name = "Edited"))
        advanceUntilIdle()
        assertEquals(EventError.UpdateFailed, viewModel.uiState.value.error)

        dao.insertError = null
        dao.deleteError = IOException("Delete failed")
        viewModel.deleteItem(existing)
        advanceUntilIdle()
        assertEquals(EventError.DeleteFailed, viewModel.uiState.value.error)
        assertEquals(listOf(1), viewModel.uiState.value.events.map { it.id })
        assertFalse(viewModel.uiState.value.canUndo)
    }

    @Test
    fun `uiState emits initial items via Turbine`() = runTest(dispatcher) {
        dao.insertItem(item(42, "seeded"))
        val viewModel = EventViewModel(repository, categoryRepository)

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(1, state.events.size)
            assertEquals(42, state.events.first().id)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `midnightTicks updates daysLeft, dayLabel, and date-based filters across midnight`() = runTest(dispatcher) {
        val zone = ZoneId.of("UTC")
        var currentInstant = ZonedDateTime.of(2026, 6, 15, 23, 59, 0, 0, zone).toInstant()
        val fakeClock = object : Clock() {
            override fun getZone(): ZoneId = zone
            override fun withZone(zone: ZoneId): Clock = this
            override fun instant(): Instant = currentInstant
        }
        val midnightTicks = MutableSharedFlow<LocalDate>()

        val targetDate = LocalDate.of(2026, 6, 16)
        dao.insertItem(Item(id = 1, name = "Midnight Event", categoryId = WORK_CATEGORY_ID, targetDate = targetDate))

        val viewModel = EventViewModel(repository, categoryRepository, clock = fakeClock, midnightTicks = midnightTicks)
        advanceUntilIdle()

        val beforeMidnight = viewModel.uiState.value.events.single()
        assertEquals(1, beforeMidnight.daysLeft)
        assertEquals(DayLabel.Left(1), beforeMidnight.dayLabel)

        viewModel.setFilterState(FilterState(FilterOption.FUTURE))
        assertEquals(1, viewModel.uiState.value.events.size)
        viewModel.setFilterState(FilterState(FilterOption.TODAY))
        assertTrue(viewModel.uiState.value.events.isEmpty())

        currentInstant = ZonedDateTime.of(2026, 6, 16, 0, 0, 1, 0, zone).toInstant()
        midnightTicks.emit(LocalDate.of(2026, 6, 16))
        runCurrent()

        assertEquals(1, viewModel.uiState.value.events.size)
        val onTargetDay = viewModel.uiState.value.events.single()
        assertEquals(0, onTargetDay.daysLeft)
        assertEquals(DayLabel.Today, onTargetDay.dayLabel)

        currentInstant = ZonedDateTime.of(2026, 6, 17, 9, 0, 0, 0, zone).toInstant()
        viewModel.refreshToday()
        runCurrent()

        assertTrue(viewModel.uiState.value.events.isEmpty())
        viewModel.setFilterState(FilterState(FilterOption.PAST))
        val pastEvent = viewModel.uiState.value.events.single()
        assertEquals(-1, pastEvent.daysLeft)
        assertEquals(DayLabel.Past(1), pastEvent.dayLabel)
    }
}
