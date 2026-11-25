package com.happypuppy.daycounter.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.daycounter.data.database.AddCategoryResult
import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.data.database.CategoryRepository
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORIES
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemRepository
import com.happypuppy.daycounter.data.preferences.EventListPreferences
import com.happypuppy.daycounter.data.preferences.InMemoryEventListPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import java.util.Date

sealed interface MainUiEvent {
    data class ShowUndoSnackbar(val itemName: String) : MainUiEvent
}

data class MainScreenUiState(
    val events: List<EventUi> = emptyList(),
    val categories: List<Category> = DEFAULT_CATEGORIES,
    val hasAnyEvents: Boolean = false,
    val sortOption: SortOption = SortOption.DATE_ADDED_DESC,
    val filterState: FilterState = FilterState(),
    val isLoading: Boolean = false,
    val error: EventError? = null,
    val canUndo: Boolean = false
)

class EventViewModel(
    private val repository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val listPreferences: EventListPreferences = InMemoryEventListPreferences(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val midnightTicks: Flow<LocalDate> = emptyFlow()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainScreenUiState(isLoading = true))
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private val _events = Channel<MainUiEvent>(Channel.BUFFERED)
    val events: Flow<MainUiEvent> = _events.receiveAsFlow()

    private val _rawItems = MutableStateFlow<List<Item>?>(null)
    private val _categories = MutableStateFlow(DEFAULT_CATEGORIES)
    private val _today = MutableStateFlow(LocalDate.now(clock))
    private val _undoOverlay = MutableStateFlow(UndoUiOverlay())

    private val undoStack = ArrayDeque<Item>()
    private val dbMutex = Mutex()

    init {
        viewModelScope.launch {
            try {
                categoryRepository.ensureInitialized()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Underlying Room flows report LoadFailed if database reads fail.
            }
        }
        observeItems()
        observeCategories()
        observeCombinedState()
        observeMidnightTicks()
    }

    // Collect the reactive items stream once. Room emits a fresh list on every
    // underlying table change, so mutations (insert/delete/etc.) do not need
    // to manually re-fetch.
    private fun observeItems() {
        viewModelScope.launch {
            repository.getAllItems()
                .catch {
                    _uiState.update { state -> state.copy(error = EventError.LoadFailed, isLoading = false) }
                }
                .collect { items ->
                    val presentIds = items.mapTo(mutableSetOf()) { it.id }
                    _undoOverlay.update { overlay ->
                        val pruned = overlay.pendingDeletedIds.intersect(presentIds)
                        if (pruned == overlay.pendingDeletedIds) {
                            overlay
                        } else {
                            overlay.copy(pendingDeletedIds = pruned)
                        }
                    }
                    _rawItems.value = items
                }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            categoryRepository.observeCategories()
                .catch {
                    _uiState.update { state -> state.copy(error = EventError.LoadFailed, isLoading = false) }
                }
                .collect { categories ->
                    _categories.value = categories.ifEmpty { DEFAULT_CATEGORIES }
                }
        }
    }

    private fun observeCombinedState() {
        viewModelScope.launch {
            val dataSnapshot = combine(_rawItems, _categories, _today) { rawItems, categories, today ->
                Triple(rawItems, categories, today)
            }
            val uiPrefsSnapshot = combine(
                listPreferences.filterStateFlow,
                listPreferences.sortOptionFlow,
                _undoOverlay
            ) { filterState, sortOption, overlay ->
                Triple(filterState, sortOption, overlay)
            }
            combine(dataSnapshot, uiPrefsSnapshot) { (rawItems, categories, today), (filterState, sortOption, overlay) ->
                if (rawItems == null) {
                    null
                } else {
                    val categoriesById = categories.associateBy { it.id }
                    val effectiveFilter = resolveFilterState(filterState, categories, categoriesById)
                    val visibleItems = rawItems.filterNot { it.id in overlay.pendingDeletedIds }
                    val mapped = visibleItems.map { item ->
                        item.toEventUi(
                            today = today,
                            categoriesById = categoriesById,
                            restoreVersion = overlay.restoreVersions[item.id] ?: 0
                        )
                    }
                    CombinedEvents(
                        events = sortEvents(filterEvents(mapped, effectiveFilter), sortOption),
                        categories = categories,
                        hasAnyEvents = visibleItems.isNotEmpty(),
                        sortOption = sortOption,
                        filterState = effectiveFilter,
                        canUndo = overlay.canUndo
                    )
                }
            }.collect { combined ->
                if (combined != null) {
                    _uiState.update { current ->
                        current.copy(
                            events = combined.events,
                            categories = combined.categories,
                            hasAnyEvents = combined.hasAnyEvents,
                            sortOption = combined.sortOption,
                            filterState = combined.filterState,
                            canUndo = combined.canUndo,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    private fun resolveFilterState(
        filterState: FilterState,
        categories: List<Category>,
        categoriesById: Map<String, Category>
    ): FilterState {
        if (filterState.option != FilterOption.CATEGORY || filterState.category == null) {
            return filterState
        }
        if (filterState.category in categoriesById) {
            return filterState
        }
        val matchedByName = categories.find { it.name.equals(filterState.category, ignoreCase = true) }
        return if (matchedByName != null) {
            filterState.copy(category = matchedByName.id)
        } else {
            filterState
        }
    }

    private fun observeMidnightTicks() {
        viewModelScope.launch {
            midnightTicks.collect { newDate ->
                _today.value = newDate
            }
        }
    }

    fun refreshToday() {
        _today.value = LocalDate.now(clock)
    }

    fun itemById(id: Int): Item? =
        _rawItems.value?.find { it.id == id && it.id !in _undoOverlay.value.pendingDeletedIds }

    fun setSortOption(option: SortOption) {
        viewModelScope.launch { listPreferences.setSortOption(option) }
    }

    fun setFilterState(state: FilterState) {
        viewModelScope.launch { listPreferences.setFilterState(state) }
    }

    fun clearFilter() {
        setFilterState(FilterState())
    }

    suspend fun addCustomCategory(rawName: String): Category? {
        return try {
            when (val result = categoryRepository.addCustomCategory(rawName)) {
                is AddCategoryResult.Added -> result.category
                is AddCategoryResult.Duplicate -> result.existing
                AddCategoryResult.Invalid -> null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _uiState.update { it.copy(error = EventError.SaveFailed) }
            null
        }
    }

    fun saveItem(name: String, categoryId: String, targetDate: LocalDate, emoji: String = "") {
        viewModelScope.launch {
            try {
                categoryRepository.ensureInitialized()
                val validCategoryId = categoryId.takeIf { id ->
                    id.isNotBlank() && _categories.value.any { it.id == id }
                } ?: DEFAULT_CATEGORY_ID
                repository.insertItem(
                    Item(
                        name = name,
                        categoryId = validCategoryId,
                        targetDate = targetDate,
                        createdAt = Date.from(clock.instant()),
                        emoji = emoji
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(error = EventError.SaveFailed) }
            }
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch {
            try {
                categoryRepository.ensureInitialized()
                val validCategoryId = item.categoryId.takeIf { id ->
                    id.isNotBlank() && _categories.value.any { it.id == id }
                } ?: DEFAULT_CATEGORY_ID
                repository.insertItem(item.copy(categoryId = validCategoryId))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(error = EventError.UpdateFailed) }
            }
        }
    }

    fun deleteEvent(id: Int) {
        itemById(id)?.let(::deleteItem)
    }

    fun deleteItem(item: Item) {
        if (item.id in _undoOverlay.value.pendingDeletedIds) return
        if (_rawItems.value?.none { it.id == item.id } == true) return

        undoStack.addLast(item)
        _undoOverlay.update { overlay ->
            overlay.copy(
                pendingDeletedIds = overlay.pendingDeletedIds + item.id,
                canUndo = true
            )
        }
        _uiState.update { it.copy(error = null) }
        _events.trySend(MainUiEvent.ShowUndoSnackbar(item.name))

        viewModelScope.launch {
            dbMutex.withLock {
                try {
                    repository.deleteItem(item)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    undoStack.remove(item)
                    _undoOverlay.update { overlay ->
                        overlay.copy(
                            pendingDeletedIds = overlay.pendingDeletedIds - item.id,
                            canUndo = undoStack.isNotEmpty()
                        )
                    }
                    _uiState.update { it.copy(error = EventError.DeleteFailed) }
                }
            }
        }
    }

    fun undoDelete() {
        if (undoStack.isEmpty()) return

        viewModelScope.launch {
            dbMutex.withLock {
                val item = undoStack.lastOrNull() ?: return@withLock
                try {
                    repository.insertItem(item)
                    undoStack.removeLast()
                    _undoOverlay.update { overlay ->
                        val nextVersion = (overlay.restoreVersions[item.id] ?: 0) + 1
                        overlay.copy(
                            pendingDeletedIds = overlay.pendingDeletedIds - item.id,
                            restoreVersions = overlay.restoreVersions + (item.id to nextVersion),
                            canUndo = undoStack.isNotEmpty()
                        )
                    }
                    _uiState.update { it.copy(error = null) }
                    undoStack.lastOrNull()?.let { previous ->
                        _events.trySend(MainUiEvent.ShowUndoSnackbar(previous.name))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _uiState.update { it.copy(error = EventError.RestoreFailed) }
                }
            }
        }
    }

    fun dismissUndoSnackbar() {
        undoStack.clear()
        _undoOverlay.update { it.copy(canUndo = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private data class UndoUiOverlay(
        val pendingDeletedIds: Set<Int> = emptySet(),
        val restoreVersions: Map<Int, Int> = emptyMap(),
        val canUndo: Boolean = false
    )

    private data class CombinedEvents(
        val events: List<EventUi>,
        val categories: List<Category>,
        val hasAnyEvents: Boolean,
        val sortOption: SortOption,
        val filterState: FilterState,
        val canUndo: Boolean
    )
}
