package com.happypuppy.daycounter.ui.screens

import app.cash.turbine.test
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
import java.util.Date

/**
 * Unit tests for [EventViewModel].
 *
 * `UnconfinedTestDispatcher` is installed as Main so the `viewModelScope` launches
 * complete eagerly and the resulting state can be asserted without explicit
 * `advanceUntilIdle` plumbing for the simple cases. `deleteItem` schedules a
 * `delay(200)` between the optimistic UI update and the DB write, so those tests
 * advance virtual time explicitly to assert the two-phase behavior.
 *
 * The ViewModel is driven through an in-memory [FakeItemDao] wrapped in the real
 * [ItemRepository] (which is a thin pass-through), keeping the test independent of
 * Room and any Android framework dependencies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventViewModelTest {

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

    private fun item(id: Int, name: String = "event-$id"): Item =
        Item(id = id, name = name, itemCategory = "Personal", targetDate = Date())

    @Test
    fun `deleteItem sets snackbar and lastDeletedItem before removing from repository`() = runTest(dispatcher) {
        dao.insertItem(item(1, "to-delete"))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(item(1, "to-delete"))

        // Optimistic UI update happens before the 200ms delay.
        val mid = viewModel.uiState.value
        assertTrue("snackbar should be visible immediately after deleteItem", mid.showUndoSnackbar)
        assertNotNull("lastDeletedItem should be populated immediately", mid.lastDeletedItem)
        assertEquals(1, mid.lastDeletedItem?.id)
        // Repository write is deferred behind delay(200), so the list is still intact.
        assertEquals(1, mid.items.size)

        // After the animation delay, the item is removed from the underlying store.
        advanceTimeBy(250)
        advanceUntilIdle()
        val final = viewModel.uiState.value
        assertTrue(final.items.isEmpty())
        assertTrue("snackbar remains visible until dismissed or undone", final.showUndoSnackbar)
    }

    @Test
    fun `undoDelete clears snackbar and adds restoration key for the restored item`() = runTest(dispatcher) {
        dao.insertItem(item(7, "boom"))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(item(7, "boom"))
        advanceTimeBy(250)
        advanceUntilIdle()
        // Sanity check: snackbar is visible, item is gone from the store.
        assertTrue(viewModel.uiState.value.showUndoSnackbar)
        assertTrue(viewModel.uiState.value.items.isEmpty())

        viewModel.undoDelete()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("snackbar should be cleared after undo", state.showUndoSnackbar)
        assertNull("lastDeletedItem should be cleared after undo", state.lastDeletedItem)
        assertTrue(
            "restoredItemKeys should contain the restored item id",
            state.restoredItemKeys.containsKey(7)
        )
        assertEquals(1, state.items.size)
        assertEquals(7, state.items.first().id)
    }

    @Test
    fun `dismissUndoSnackbar clears snackbar state without touching the repository`() = runTest(dispatcher) {
        dao.insertItem(item(3, "fleeting"))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(item(3, "fleeting"))
        advanceTimeBy(250)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showUndoSnackbar)

        viewModel.dismissUndoSnackbar()

        val state = viewModel.uiState.value
        assertFalse(state.showUndoSnackbar)
        assertNull(state.lastDeletedItem)
        // Dismissal does not restore the deleted item.
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `multiple deleteItem calls track only the most recently deleted item`() = runTest(dispatcher) {
        dao.insertItem(item(10, "first"))
        dao.insertItem(item(20, "second"))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(item(10, "first"))
        advanceTimeBy(250)
        advanceUntilIdle()
        assertEquals(10, viewModel.uiState.value.lastDeletedItem?.id)

        viewModel.deleteItem(item(20, "second"))
        advanceTimeBy(250)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            "lastDeletedItem should track the most recent delete",
            20,
            state.lastDeletedItem?.id
        )
        assertTrue(state.showUndoSnackbar)
        assertTrue("both items should be gone from the store", state.items.isEmpty())
    }

    @Test
    fun `clearRestorationKey removes only the specified id from restoredItemKeys`() = runTest(dispatcher) {
        dao.insertItem(item(1, "a"))
        dao.insertItem(item(2, "b"))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(item(1, "a"))
        advanceTimeBy(250)
        advanceUntilIdle()
        viewModel.undoDelete()
        advanceUntilIdle()

        viewModel.deleteItem(item(2, "b"))
        advanceTimeBy(250)
        advanceUntilIdle()
        viewModel.undoDelete()
        advanceUntilIdle()

        assertEquals(setOf(1, 2), viewModel.uiState.value.restoredItemKeys.keys)

        viewModel.clearRestorationKey(1)
        advanceUntilIdle()

        assertEquals(setOf(2), viewModel.uiState.value.restoredItemKeys.keys)
    }

    @Test
    fun `uiState emits initial items via Turbine`() = runTest(dispatcher) {
        dao.insertItem(item(42, "seeded"))
        val viewModel = EventViewModel(repository)

        viewModel.uiState.test {
            // The init block kicks off getAllItems on the Unconfined dispatcher, so the
            // first emission we observe already contains the seeded item.
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(1, state.items.size)
            assertEquals(42, state.items.first().id)
            assertFalse(state.isLoading)
        }
    }
}
