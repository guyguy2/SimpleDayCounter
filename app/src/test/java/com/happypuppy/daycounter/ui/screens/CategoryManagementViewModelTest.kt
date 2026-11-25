package com.happypuppy.daycounter.ui.screens

import app.cash.turbine.test
import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.data.database.CategoryDao
import com.happypuppy.daycounter.data.database.CategoryRepository
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.preferences.InMemoryEventListPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryManagementViewModelTest {

    private lateinit var itemDao: FakeItemDao
    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var listPreferences: InMemoryEventListPreferences
    private lateinit var repository: CategoryRepository
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        itemDao = FakeItemDao()
        categoryDao = FakeCategoryDao(itemDao)
        listPreferences = InMemoryEventListPreferences()
        var nextId = 1
        repository = CategoryRepository(
            categoryDao = categoryDao,
            itemDao = itemDao,
            idGenerator = { "custom-${nextId++}" }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exposes default categories and custom categories with exact usage counts`() = runTest(dispatcher) {
        val viewModel = CategoryManagementViewModel(repository, listPreferences)
        advanceUntilIdle()

        val travel = viewModel.addCustomCategory("Travel")!!
        val books = viewModel.addCustomCategory("Books")!!
        itemDao.insertItem(Item(id = 1, name = "Trip 1", categoryId = travel.id, targetDate = LocalDate.of(2026, 7, 1)))
        itemDao.insertItem(Item(id = 2, name = "Trip 2", categoryId = travel.id, targetDate = LocalDate.of(2026, 7, 2)))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(4, state.defaultCategories.size)
        assertEquals(2, state.customCategories.size)
        assertEquals(0, state.customCategories.first { it.category.id == books.id }.eventCount)
        assertEquals(2, state.customCategories.first { it.category.id == travel.id }.eventCount)
        assertEquals(2, state.totalCustomUsageCount)
    }

    @Test
    fun `deleteCustomCategory reassigns events, clears matching filter, emits snackbar, and Undo restores all`() =
        runTest(dispatcher) {
            val viewModel = CategoryManagementViewModel(repository, listPreferences)
            advanceUntilIdle()

            val travel = viewModel.addCustomCategory("Travel")!!
            itemDao.insertItem(Item(id = 1, name = "Trip", categoryId = travel.id, targetDate = LocalDate.of(2026, 7, 1)))
            listPreferences.setFilterState(FilterState(FilterOption.CATEGORY, travel.id))
            advanceUntilIdle()

            viewModel.events.test {
                viewModel.deleteCustomCategory(travel.id)
                advanceUntilIdle()

                assertEquals(CategoryUiEvent.ShowUndoSnackbar("Travel", isAll = false), awaitItem())
                assertTrue(viewModel.uiState.value.customCategories.isEmpty())
                assertTrue(viewModel.uiState.value.canUndo)
                assertEquals(DEFAULT_CATEGORY_ID, itemDao.currentItems.single().categoryId)
                assertEquals(FilterState(FilterOption.ALL, null), listPreferences.filterStateFlow.first())

                viewModel.undoDelete()
                advanceUntilIdle()

                assertFalse(viewModel.uiState.value.canUndo)
                assertEquals(1, viewModel.uiState.value.customCategories.size)
                assertEquals(travel.id, itemDao.currentItems.single().categoryId)
                assertEquals(FilterState(FilterOption.CATEGORY, travel.id), listPreferences.filterStateFlow.first())
                expectNoEvents()
            }
        }

    @Test
    fun `deleteAllCustomCategories and Undo restores all categories and item assignments`() = runTest(dispatcher) {
        val viewModel = CategoryManagementViewModel(repository, listPreferences)
        advanceUntilIdle()

        val travel = viewModel.addCustomCategory("Travel")!!
        val music = viewModel.addCustomCategory("Music")!!
        itemDao.insertItem(Item(id = 1, name = "Flight", categoryId = travel.id, targetDate = LocalDate.of(2026, 7, 1)))
        itemDao.insertItem(Item(id = 2, name = "Concert", categoryId = music.id, targetDate = LocalDate.of(2026, 7, 2)))
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.deleteAllCustomCategories()
            advanceUntilIdle()

            assertEquals(CategoryUiEvent.ShowUndoSnackbar("", isAll = true), awaitItem())
            assertTrue(viewModel.uiState.value.customCategories.isEmpty())
            assertTrue(itemDao.currentItems.all { it.categoryId == DEFAULT_CATEGORY_ID })

            viewModel.undoDelete()
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.customCategories.size)
            assertEquals(travel.id, itemDao.currentItems.first { it.id == 1 }.categoryId)
            assertEquals(music.id, itemDao.currentItems.first { it.id == 2 }.categoryId)
            expectNoEvents()
        }
    }

    @Test
    fun `failed undo keeps snapshot on stack for retry`() = runTest(dispatcher) {
        val viewModel = CategoryManagementViewModel(repository, listPreferences)
        advanceUntilIdle()

        val travel = viewModel.addCustomCategory("Travel")!!
        itemDao.insertItem(Item(id = 1, name = "Flight", categoryId = travel.id, targetDate = LocalDate.of(2026, 7, 1)))
        advanceUntilIdle()

        viewModel.deleteCustomCategory(travel.id)
        advanceUntilIdle()

        categoryDao.insertError = IOException("Disk full")
        viewModel.undoDelete()
        advanceUntilIdle()

        assertEquals(CategoryError.RestoreFailed, viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.canUndo)

        categoryDao.insertError = null
        viewModel.undoDelete()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.canUndo)
        assertEquals(travel.id, itemDao.currentItems.single().categoryId)
    }

    @Test
    fun `database load error surfaces LoadFailed`() = runTest(dispatcher) {
        val failingCategoryDao = FakeCategoryDao(itemDao, flowError = IOException("DB corrupt"))
        val failingRepo = CategoryRepository(failingCategoryDao, itemDao)
        val viewModel = CategoryManagementViewModel(failingRepo, listPreferences)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertEquals(CategoryError.LoadFailed, viewModel.uiState.value.error)
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `dismissUndoSnackbar during suspended undoDelete does not throw or set RestoreFailed`() =
        runTest(dispatcher) {
            val restoreStarted = CompletableDeferred<Unit>()
            val allowRestoreFinish = CompletableDeferred<Unit>()
            val suspendingDao = object : CategoryDao by categoryDao {
                override suspend fun insertCategories(categories: List<Category>) {
                    if (categories.any { !it.isDefault }) {
                        restoreStarted.complete(Unit)
                        allowRestoreFinish.await()
                    }
                    categoryDao.insertCategories(categories)
                }
            }
            var nextId = 1
            val repo = CategoryRepository(suspendingDao, itemDao, idGenerator = { "custom-${nextId++}" })
            val viewModel = CategoryManagementViewModel(repo, listPreferences)
            advanceUntilIdle()

            val travel = viewModel.addCustomCategory("Travel")!!
            itemDao.insertItem(Item(id = 1, name = "Trip", categoryId = travel.id, targetDate = LocalDate.of(2026, 7, 1)))
            viewModel.deleteCustomCategory(travel.id)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.canUndo)

            viewModel.undoDelete()
            restoreStarted.await()

            viewModel.dismissUndoSnackbar()
            allowRestoreFinish.complete(Unit)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.canUndo)
            assertNotNull(categoryDao.getCategoryById(travel.id))
            assertEquals(travel.id, itemDao.currentItems.single().categoryId)
        }

    @Test
    fun `dismissUndoSnackbar during suspended deleteCustomCategory prevents stale snackbar and canUndo`() =
        runTest(dispatcher) {
            val deleteStarted = CompletableDeferred<Unit>()
            val allowDeleteFinish = CompletableDeferred<Unit>()
            val suspendingDao = object : CategoryDao by categoryDao {
                override suspend fun deleteCustomCategoryById(id: String): Int {
                    deleteStarted.complete(Unit)
                    allowDeleteFinish.await()
                    return categoryDao.deleteCustomCategoryById(id)
                }
            }
            var nextId = 1
            val repo = CategoryRepository(suspendingDao, itemDao, idGenerator = { "custom-${nextId++}" })
            val viewModel = CategoryManagementViewModel(repo, listPreferences)
            advanceUntilIdle()

            val travel = viewModel.addCustomCategory("Travel")!!
            advanceUntilIdle()

            viewModel.events.test {
                viewModel.deleteCustomCategory(travel.id)
                deleteStarted.await()

                viewModel.dismissUndoSnackbar()
                allowDeleteFinish.complete(Unit)
                advanceUntilIdle()

                assertFalse(viewModel.uiState.value.canUndo)
                assertNull(viewModel.uiState.value.error)
                expectNoEvents()
            }
        }

    @Test
    fun `re-adding deleted category before Undo merges items into replacement and updates filter without duplicates`() =
        runTest(dispatcher) {
            val viewModel = CategoryManagementViewModel(repository, listPreferences)
            advanceUntilIdle()

            val oldTravel = viewModel.addCustomCategory("Travel")!!
            itemDao.insertItem(Item(id = 1, name = "Old Trip", categoryId = oldTravel.id, targetDate = LocalDate.of(2026, 7, 1)))
            listPreferences.setFilterState(FilterState(FilterOption.CATEGORY, oldTravel.id))
            advanceUntilIdle()

            viewModel.deleteCustomCategory(oldTravel.id)
            advanceUntilIdle()
            assertEquals(FilterState(FilterOption.ALL, null), listPreferences.filterStateFlow.first())

            val newTravel = viewModel.addCustomCategory("Travel")!!
            itemDao.insertItem(Item(id = 2, name = "New Trip", categoryId = newTravel.id, targetDate = LocalDate.of(2026, 7, 2)))
            advanceUntilIdle()

            viewModel.undoDelete()
            advanceUntilIdle()

            val customCategories = viewModel.uiState.value.customCategories
            assertEquals(1, customCategories.size)
            assertEquals(newTravel.id, customCategories.single().category.id)
            assertEquals(2, customCategories.single().eventCount)
            assertEquals(newTravel.id, itemDao.currentItems.first { it.id == 1 }.categoryId)
            assertEquals(newTravel.id, itemDao.currentItems.first { it.id == 2 }.categoryId)
            assertEquals(FilterState(FilterOption.CATEGORY, newTravel.id), listPreferences.filterStateFlow.first())
            assertFalse(viewModel.uiState.value.canUndo)
            assertNull(viewModel.uiState.value.error)
        }
}
