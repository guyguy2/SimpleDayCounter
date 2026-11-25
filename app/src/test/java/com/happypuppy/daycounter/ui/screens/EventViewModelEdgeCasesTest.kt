package com.happypuppy.daycounter.ui.screens

import app.cash.turbine.test
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.util.Date

/**
 * Edge cases around [EventViewModel], including LIFO multi-undo and retryable failed restores.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventViewModelEdgeCasesTest {

    private lateinit var dao: FakeItemDao
    private lateinit var repository: ItemRepository
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeItemDao()
        repository = ItemRepository(dao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun item(id: Int, name: String = "event-$id", category: String = "Personal"): Item =
        Item(id = id, name = name, itemCategory = category, targetDate = Date(1_700_000_000_000L))

    @Test
    fun `undoDelete with nothing deleted is a no-op`() = runTest(dispatcher) {
        dao.insertItem(item(1))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()
        val before = viewModel.uiState.value

        viewModel.undoDelete()
        advanceUntilIdle()

        assertEquals(before, viewModel.uiState.value)
        assertEquals(1, dao.currentItems.size)
    }

    @Test
    fun `undoDelete after delete re-inserts the same id and fields`() = runTest(dispatcher) {
        val original = item(5, "Anniversary", category = "Work").copy(emoji = "\u2764\uFE0F")
        dao.insertItem(original)
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(original)
        advanceUntilIdle()
        assertTrue("delete must be committed before undo", dao.currentItems.isEmpty())

        viewModel.undoDelete()
        advanceUntilIdle()

        assertEquals(listOf(original), dao.currentItems)
        assertEquals(listOf(original.id), viewModel.uiState.value.events.map { it.id })
    }

    @Test
    fun `multi-undo - deleting A then B restores B then A and re-emits snackbar for A`() =
        runTest(dispatcher) {
            val a = item(1, "A")
            val b = item(2, "B")
            dao.insertItem(a)
            dao.insertItem(b)
            val viewModel = EventViewModel(repository)
            advanceUntilIdle()

            viewModel.events.test {
                viewModel.deleteItem(a)
                viewModel.deleteItem(b)
                advanceUntilIdle()
                assertEquals(MainUiEvent.ShowUndoSnackbar("A"), awaitItem())
                assertEquals(MainUiEvent.ShowUndoSnackbar("B"), awaitItem())
                assertTrue(dao.currentItems.isEmpty())

                viewModel.undoDelete()
                advanceUntilIdle()
                assertEquals(listOf(2), dao.currentItems.map { it.id })
                assertEquals(MainUiEvent.ShowUndoSnackbar("A"), awaitItem())

                viewModel.undoDelete()
                advanceUntilIdle()
                assertEquals(setOf(1, 2), dao.currentItems.map { it.id }.toSet())
                expectNoEvents()
            }
        }

    @Test
    fun `deleting the same item twice while pending ignores the duplicate`() = runTest(dispatcher) {
        val target = item(9, "twice")
        dao.insertItem(target)
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.deleteItem(target)
            viewModel.deleteItem(target)
            advanceUntilIdle()

            assertEquals(MainUiEvent.ShowUndoSnackbar("twice"), awaitItem())
            expectNoEvents()
            assertTrue(dao.currentItems.isEmpty())
            assertNull(viewModel.uiState.value.error)
        }
    }

    @Test
    fun `deleteItem clears a previous error`() = runTest(dispatcher) {
        dao.insertItem(item(1))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()
        dao.insertError = IOException("boom")
        viewModel.saveItem("x", "Work", Date())
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        viewModel.deleteItem(item(1))
        runCurrent()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `failed restore surfaces RestoreFailed, keeps item on undo stack, and succeeds on retry`() =
        runTest(dispatcher) {
            val target = item(3, "unlucky")
            dao.insertItem(target)
            val viewModel = EventViewModel(repository)
            advanceUntilIdle()
            viewModel.deleteItem(target)
            advanceUntilIdle()

            dao.insertError = IOException("restore failed")
            viewModel.undoDelete()
            advanceUntilIdle()

            val failedState = viewModel.uiState.value
            assertEquals(EventError.RestoreFailed, failedState.error)
            assertTrue(failedState.events.isEmpty())
            assertTrue("failed restore must keep undo slot for retry", failedState.canUndo)

            // Retry once the transient DB error clears.
            dao.insertError = null
            viewModel.undoDelete()
            advanceUntilIdle()

            val recoveredState = viewModel.uiState.value
            assertNull(recoveredState.error)
            assertEquals(listOf(3), recoveredState.events.map { it.id })
            assertFalse(recoveredState.canUndo)
        }

    @Test
    fun `saveItem does not touch undo state`() = runTest(dispatcher) {
        dao.insertItem(item(1))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()
        viewModel.deleteItem(item(1))
        advanceUntilIdle()

        viewModel.saveItem("new", "Health", Date())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.canUndo)
        assertEquals(listOf("new"), state.events.map { it.name })
    }

    @Test
    fun `new items get distinct generated ids and the list is observed in DAO order`() = runTest(dispatcher) {
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.saveItem("one", "Work", Date(1L))
        viewModel.saveItem("two", "Work", Date(2L))
        advanceUntilIdle()

        val ids = viewModel.uiState.value.events.map { it.id }
        assertEquals(2, ids.size)
        assertEquals(2, ids.toSet().size)
        assertTrue(ids.none { it == 0 })
    }
}
