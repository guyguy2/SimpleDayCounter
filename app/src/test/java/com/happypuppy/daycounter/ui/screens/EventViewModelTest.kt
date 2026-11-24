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

    private fun item(id: Int, name: String = "event-$id", category: String = "Personal", emoji: String = ""): Item =
        Item(id = id, name = name, itemCategory = category, targetDate = Date(1_700_000_000_000L), emoji = emoji)

    @Test
    fun `saveItem inserts new item into repository and updates uiState`() = runTest(dispatcher) {
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        val target = Date(1_800_000_000_000L)
        viewModel.saveItem(name = "Vacation", category = "Personal", targetDate = target, emoji = "✈️")
        advanceUntilIdle()

        val items = viewModel.uiState.value.items
        assertEquals(1, items.size)
        assertEquals("Vacation", items.first().name)
        assertEquals("Personal", items.first().itemCategory)
        assertEquals(target, items.first().targetDate)
        assertEquals("✈️", items.first().emoji)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `updateItem replaces existing item in repository`() = runTest(dispatcher) {
        val original = item(id = 12, name = "Old Name", category = "Work", emoji = "💼")
        dao.insertItem(original)
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        val updated = original.copy(name = "New Name", itemCategory = "Health", emoji = "🏃")
        viewModel.updateItem(updated)
        advanceUntilIdle()

        val items = viewModel.uiState.value.items
        assertEquals(1, items.size)
        assertEquals("New Name", items.first().name)
        assertEquals("Health", items.first().itemCategory)
        assertEquals("🏃", items.first().emoji)
    }

    @Test
    fun `deleteItem sets snackbar and lastDeletedItem before removing from repository`() = runTest(dispatcher) {
        dao.insertItem(item(1, "to-delete"))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(item(1, "to-delete"))
        runCurrent()

        val mid = viewModel.uiState.value
        assertTrue("snackbar should be visible immediately after deleteItem", mid.showUndoSnackbar)
        assertNotNull("lastDeletedItem should be populated immediately", mid.lastDeletedItem)
        assertEquals(1, mid.lastDeletedItem?.id)
        assertTrue("snackbarToken should be positive", mid.snackbarToken > 0L)
        assertEquals(1, mid.items.size)

        advanceTimeBy(EventViewModel.DELETE_DELAY_MS + 50L)
        advanceUntilIdle()
        val final = viewModel.uiState.value
        assertTrue(final.items.isEmpty())
        assertTrue("snackbar remains visible until dismissed or undone", final.showUndoSnackbar)
    }

    @Test
    fun `undoDelete within delete delay cancels pending deletion so item stays present`() = runTest(dispatcher) {
        val target = item(42, "race-test")
        dao.insertItem(target)
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(target)
        // Undo before the 200ms delete delay expires.
        advanceTimeBy(100L)
        runCurrent()
        viewModel.undoDelete()

        // Advance well past both delete delay and restoration key TTL.
        advanceTimeBy(1_000L)
        advanceUntilIdle()

        assertEquals("item must NOT be deleted after rapid undo", 1, dao.currentItems.size)
        assertEquals(42, dao.currentItems.first().id)
        assertEquals(1, viewModel.uiState.value.items.size)
        assertFalse(viewModel.uiState.value.showUndoSnackbar)
        assertNull(viewModel.uiState.value.lastDeletedItem)
    }

    @Test
    fun `undoDelete clears snackbar and adds restoration key for the restored item`() = runTest(dispatcher) {
        dao.insertItem(item(7, "boom"))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(item(7, "boom"))
        advanceTimeBy(EventViewModel.DELETE_DELAY_MS + 50L)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showUndoSnackbar)
        assertTrue(viewModel.uiState.value.items.isEmpty())

        viewModel.undoDelete()
        runCurrent()

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
        advanceTimeBy(EventViewModel.DELETE_DELAY_MS + 50L)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showUndoSnackbar)

        viewModel.dismissUndoSnackbar()

        val state = viewModel.uiState.value
        assertFalse(state.showUndoSnackbar)
        assertNull(state.lastDeletedItem)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `consecutive deletes advance snackbarToken and track most recently deleted item`() = runTest(dispatcher) {
        dao.insertItem(item(10, "first"))
        dao.insertItem(item(20, "second"))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(item(10, "first"))
        runCurrent()
        val firstToken = viewModel.uiState.value.snackbarToken
        assertEquals(10, viewModel.uiState.value.lastDeletedItem?.id)

        // Immediately delete second item while first snackbar is still active.
        viewModel.deleteItem(item(20, "second"))
        runCurrent()
        val secondToken = viewModel.uiState.value.snackbarToken
        assertTrue("consecutive deletes must produce a strictly newer snackbarToken", secondToken > firstToken)
        assertEquals(20, viewModel.uiState.value.lastDeletedItem?.id)
        assertTrue(viewModel.uiState.value.showUndoSnackbar)

        advanceTimeBy(EventViewModel.DELETE_DELAY_MS + 50L)
        advanceUntilIdle()
        assertTrue("both items should be gone from the store", viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun `clearRestorationKey removes only the specified id from restoredItemKeys`() = runTest(dispatcher) {
        dao.insertItem(item(1, "a"))
        dao.insertItem(item(2, "b"))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(item(1, "a"))
        advanceTimeBy(EventViewModel.DELETE_DELAY_MS + 50L)
        runCurrent()
        viewModel.undoDelete()
        runCurrent()

        viewModel.deleteItem(item(2, "b"))
        advanceTimeBy(EventViewModel.DELETE_DELAY_MS + 50L)
        runCurrent()
        viewModel.undoDelete()
        runCurrent()

        assertEquals(setOf(1, 2), viewModel.uiState.value.restoredItemKeys.keys)

        viewModel.clearRestorationKey(1)
        runCurrent()

        assertEquals(setOf(2), viewModel.uiState.value.restoredItemKeys.keys)
    }

    @Test
    fun `clearRestorationKey is a no-op when the id is not present`() = runTest(dispatcher) {
        dao.insertItem(item(1, "only"))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(item(1, "only"))
        advanceTimeBy(EventViewModel.DELETE_DELAY_MS + 50L)
        advanceUntilIdle()
        viewModel.undoDelete()
        runCurrent()

        assertEquals(setOf(1), viewModel.uiState.value.restoredItemKeys.keys)

        viewModel.clearRestorationKey(999)
        runCurrent()

        assertEquals(setOf(1), viewModel.uiState.value.restoredItemKeys.keys)
    }

    @Test
    fun `undoing the same id twice produces strictly increasing monotonic restoration keys`() = runTest(dispatcher) {
        dao.insertItem(item(5, "again"))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(item(5, "again"))
        advanceTimeBy(EventViewModel.DELETE_DELAY_MS + 50L)
        advanceUntilIdle()
        viewModel.undoDelete()
        runCurrent()
        val firstKey = viewModel.uiState.value.restoredItemKeys[5]
        assertNotNull("first undo should set a restoration key", firstKey)

        // No Thread.sleep needed — tokenGenerator is a monotonic AtomicLong.
        viewModel.deleteItem(item(5, "again"))
        advanceTimeBy(EventViewModel.DELETE_DELAY_MS + 50L)
        advanceUntilIdle()
        viewModel.undoDelete()
        runCurrent()
        val secondKey = viewModel.uiState.value.restoredItemKeys[5]
        assertNotNull("second undo should set a restoration key", secondKey)

        assertTrue(
            "subsequent undo must produce a strictly newer restoration key",
            secondKey!! > firstKey!!
        )
        assertEquals(setOf(5), viewModel.uiState.value.restoredItemKeys.keys)
    }

    @Test
    fun `restoration key auto-expires after TTL`() = runTest(dispatcher) {
        dao.insertItem(item(9, "temp-key"))
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteItem(item(9, "temp-key"))
        advanceTimeBy(EventViewModel.DELETE_DELAY_MS + 50L)
        advanceUntilIdle()
        viewModel.undoDelete()
        runCurrent()

        assertTrue(
            "key must be present immediately after undo",
            viewModel.uiState.value.restoredItemKeys.containsKey(9)
        )

        advanceTimeBy(EventViewModel.RESTORATION_KEY_TTL_MS)
        runCurrent()

        assertTrue(
            "key must be cleared after TTL so restoredItemKeys does not leak",
            viewModel.uiState.value.restoredItemKeys.isEmpty()
        )
    }

    @Test
    fun `database read error propagates to uiState error and clears loading`() = runTest(dispatcher) {
        val failingDao = FakeItemDao(flowError = IOException("Disk corrupted"))
        val viewModel = EventViewModel(ItemRepository(failingDao))
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
        val viewModel = EventViewModel(repository)
        advanceUntilIdle()

        dao.insertError = IOException("Insert failed")
        viewModel.saveItem("New", "Work", Date())
        advanceUntilIdle()
        assertEquals(EventError.SaveFailed, viewModel.uiState.value.error)

        viewModel.clearError()
        viewModel.updateItem(existing.copy(name = "Edited"))
        advanceUntilIdle()
        assertEquals(EventError.UpdateFailed, viewModel.uiState.value.error)

        dao.insertError = null
        dao.deleteError = IOException("Delete failed")
        viewModel.deleteItem(existing)
        advanceTimeBy(EventViewModel.DELETE_DELAY_MS + 50L)
        advanceUntilIdle()
        assertEquals(EventError.DeleteFailed, viewModel.uiState.value.error)
    }

    @Test
    fun `uiState emits initial items via Turbine`() = runTest(dispatcher) {
        dao.insertItem(item(42, "seeded"))
        val viewModel = EventViewModel(repository)

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(1, state.items.size)
            assertEquals(42, state.items.first().id)
            assertFalse(state.isLoading)
        }
    }
}
