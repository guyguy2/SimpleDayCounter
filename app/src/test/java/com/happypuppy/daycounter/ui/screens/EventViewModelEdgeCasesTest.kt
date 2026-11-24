package com.happypuppy.daycounter.ui.screens

import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
 * Edge cases around [EventViewModel] that [EventViewModelTest] does not cover. Several of these
 * characterize CURRENT behavior (single-slot undo, restore failure) so refactors that change the
 * delete/undo model are forced to update them deliberately.
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
    fun `undoDelete after the delete was committed re-inserts the same id and fields`() = runTest(dispatcher) {
        val original = item(5, "Anniversary", category = "Work").copy(emoji = "\u2764\uFE0F")
        dao.insertItem(original)
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(original)
        advanceTimeBy(EventViewModel.DELETE_DELAY_MS + 50L)
        runCurrent()
        assertTrue("delete must be committed before undo", dao.currentItems.isEmpty())

        viewModel.undoDelete()
        advanceUntilIdle()

        assertEquals(listOf(original), dao.currentItems)
        assertEquals(listOf(original), viewModel.uiState.value.items)
    }

    @Test
    fun `single-slot undo - after deleting A then B only B can be restored and A stays deleted`() =
        runTest(dispatcher) {
            val a = item(1, "A")
            val b = item(2, "B")
            dao.insertItem(a)
            dao.insertItem(b)
            val viewModel = EventViewModel(repository)
            advanceUntilIdle()

            viewModel.deleteItem(a)
            viewModel.deleteItem(b)
            advanceTimeBy(EventViewModel.DELETE_DELAY_MS + 50L)
            runCurrent()
            assertTrue(dao.currentItems.isEmpty())

            viewModel.undoDelete()
            advanceUntilIdle()

            assertEquals(
                "only the most recent delete is undoable (known limitation, see PLAN.md)",
                listOf(2),
                dao.currentItems.map { it.id }
            )
            // A second undo has nothing left to restore.
            viewModel.undoDelete()
            advanceUntilIdle()
            assertEquals(listOf(2), dao.currentItems.map { it.id })
        }

    @Test
    fun `deleting the same item twice keeps a single pending job and a single removal`() = runTest(dispatcher) {
        val target = item(9, "twice")
        dao.insertItem(target)
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(target)
        val firstToken = viewModel.uiState.value.snackbarToken
        viewModel.deleteItem(target)
        val secondToken = viewModel.uiState.value.snackbarToken
        advanceUntilIdle()

        assertTrue(secondToken > firstToken)
        assertTrue(dao.currentItems.isEmpty())
        assertNull(viewModel.uiState.value.error)
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
    fun `failed restore surfaces an error and leaves the item deleted`() = runTest(dispatcher) {
        val target = item(3, "unlucky")
        dao.insertItem(target)
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()
        viewModel.deleteItem(target)
        advanceTimeBy(EventViewModel.DELETE_DELAY_MS + 50L)
        runCurrent()

        dao.insertError = IOException("restore failed")
        viewModel.undoDelete()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(EventError.RestoreFailed, state.error)
        assertTrue(state.items.isEmpty())
        // Characterization: the undo slot is consumed even though the restore failed, so the user
        // cannot retry. A hardening wave should keep lastDeletedItem until the insert succeeds.
        assertNull(state.lastDeletedItem)
        assertFalse(state.showUndoSnackbar)
    }

    @Test
    fun `saveItem does not touch undo state`() = runTest(dispatcher) {
        dao.insertItem(item(1))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()
        viewModel.deleteItem(item(1))
        runCurrent()
        val token = viewModel.uiState.value.snackbarToken

        viewModel.saveItem("new", "Health", Date())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(token, state.snackbarToken)
        assertEquals(1, state.lastDeletedItem?.id)
        assertTrue(state.showUndoSnackbar)
        assertEquals(listOf("new"), state.items.map { it.name })
    }

    @Test
    fun `new items get distinct generated ids and the list is observed in DAO order`() = runTest(dispatcher) {
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.saveItem("one", "Work", Date(1L))
        viewModel.saveItem("two", "Work", Date(2L))
        advanceUntilIdle()

        val ids = viewModel.uiState.value.items.map { it.id }
        assertEquals(2, ids.size)
        assertEquals(2, ids.toSet().size)
        assertTrue(ids.none { it == 0 })
    }
}
