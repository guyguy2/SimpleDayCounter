package com.happypuppy.daycounter.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.daycounter.BuildConfig
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemRepository
import com.happypuppy.daycounter.data.preferences.EventListPreferences
import com.happypuppy.daycounter.data.preferences.InMemoryEventListPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

data class MainScreenUiState(
    val events: List<EventUi> = emptyList(),
    val hasAnyEvents: Boolean = false,
    val sortOption: SortOption = SortOption.DATE_ADDED_DESC,
    val filterState: FilterState = FilterState(),
    val isLoading: Boolean = false,
    val error: EventError? = null,
    val lastDeletedItem: Item? = null,
    val showUndoSnackbar: Boolean = false,
    val snackbarToken: Long = 0L,
    val restoredItemKeys: Map<Int, Long> = emptyMap()
)

class EventViewModel(
    private val repository: ItemRepository,
    private val listPreferences: EventListPreferences = InMemoryEventListPreferences(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val midnightTicks: Flow<LocalDate> = emptyFlow()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainScreenUiState(isLoading = true))
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private val _rawItems = MutableStateFlow<List<Item>?>(null)
    private val _today = MutableStateFlow(LocalDate.now(clock))
    private val _restoredItemKeys = MutableStateFlow<Map<Int, Long>>(emptyMap())

    private val tokenGenerator = AtomicLong(0L)
    private val pendingDeleteJobs = mutableMapOf<Int, Job>()

    init {
        observeItems()
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
                    _rawItems.value = items
                }
        }
    }

    private fun observeCombinedState() {
        viewModelScope.launch {
            combine(
                _rawItems,
                _today,
                listPreferences.filterStateFlow,
                listPreferences.sortOptionFlow,
                _restoredItemKeys
            ) { rawItems, today, filterState, sortOption, restoredKeys ->
                if (rawItems == null) {
                    null
                } else {
                    val mapped = rawItems.map { item ->
                        item.toEventUi(
                            today = today,
                            zoneId = clock.zone,
                            restoreVersion = (restoredKeys[item.id] ?: 0L).toInt()
                        )
                    }
                    CombinedEvents(
                        events = sortEvents(filterEvents(mapped, filterState), sortOption),
                        hasAnyEvents = rawItems.isNotEmpty(),
                        sortOption = sortOption,
                        filterState = filterState,
                        restoredKeys = restoredKeys
                    )
                }
            }.collect { combined ->
                if (combined != null) {
                    _uiState.update { current ->
                        current.copy(
                            events = combined.events,
                            hasAnyEvents = combined.hasAnyEvents,
                            sortOption = combined.sortOption,
                            filterState = combined.filterState,
                            restoredItemKeys = combined.restoredKeys,
                            isLoading = false
                        )
                    }
                }
            }
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

    fun itemById(id: Int): Item? = _rawItems.value?.find { it.id == id }

    fun setSortOption(option: SortOption) {
        viewModelScope.launch { listPreferences.setSortOption(option) }
    }

    fun setFilterState(state: FilterState) {
        viewModelScope.launch { listPreferences.setFilterState(state) }
    }

    fun clearFilter() {
        setFilterState(FilterState())
    }

    fun saveItem(name: String, category: String, targetDate: Date, emoji: String = "") {
        viewModelScope.launch {
            try {
                repository.insertItem(
                    Item(
                        name = name,
                        itemCategory = category,
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
                repository.insertItem(item)
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
        pendingDeleteJobs.remove(item.id)?.cancel()
        val token = tokenGenerator.incrementAndGet()
        if (BuildConfig.DEBUG) Log.d(TAG, "Deleting item ${item.id} (token=$token)")

        _uiState.update {
            it.copy(
                lastDeletedItem = item,
                showUndoSnackbar = true,
                snackbarToken = token,
                error = null
            )
        }

        val job = viewModelScope.launch {
            delay(DELETE_DELAY_MS)
            try {
                repository.deleteItem(item)
                if (BuildConfig.DEBUG) Log.d(TAG, "Item ${item.id} deleted from repository")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(error = EventError.DeleteFailed) }
            } finally {
                pendingDeleteJobs.remove(item.id)
            }
        }
        pendingDeleteJobs[item.id] = job
    }

    fun undoDelete() {
        val deletedItem = _uiState.value.lastDeletedItem ?: return
        // Cancel any in-flight delayed delete for this item so it cannot delete after restore.
        pendingDeleteJobs.remove(deletedItem.id)?.cancel()

        if (BuildConfig.DEBUG) Log.d(TAG, "Undoing delete for item ${deletedItem.id}")

        // Monotonic key forces LazyColumn to remount the row so swipe state resets.
        val restorationKey = tokenGenerator.incrementAndGet()
        _restoredItemKeys.update { it + (deletedItem.id to restorationKey) }
        _uiState.update { current ->
            current.copy(
                lastDeletedItem = null,
                showUndoSnackbar = false,
                restoredItemKeys = _restoredItemKeys.value,
                error = null
            )
        }

        viewModelScope.launch {
            try {
                repository.insertItem(deletedItem)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(error = EventError.RestoreFailed) }
                return@launch
            }

            // Auto-expire so the map does not grow for the process lifetime.
            // Only remove if this exact key is still present (a later undo of the
            // same id would have replaced it with a newer token).
            delay(RESTORATION_KEY_TTL_MS)
            removeRestorationKeyIfMatches(deletedItem.id, restorationKey)
        }
    }

    fun clearRestorationKey(itemId: Int) {
        _restoredItemKeys.update { current ->
            if (!current.containsKey(itemId)) {
                current
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "Cleared restoration key for item $itemId")
                current - itemId
            }
        }
        _uiState.update { it.copy(restoredItemKeys = _restoredItemKeys.value) }
    }

    private fun removeRestorationKeyIfMatches(itemId: Int, expectedKey: Long) {
        _restoredItemKeys.update { current ->
            if (current[itemId] != expectedKey) {
                current
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "Auto-expired restoration key for item $itemId")
                current - itemId
            }
        }
        _uiState.update { it.copy(restoredItemKeys = _restoredItemKeys.value) }
    }

    fun dismissUndoSnackbar() {
        _uiState.update {
            it.copy(
                showUndoSnackbar = false,
                lastDeletedItem = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private data class CombinedEvents(
        val events: List<EventUi>,
        val hasAnyEvents: Boolean,
        val sortOption: SortOption,
        val filterState: FilterState,
        val restoredKeys: Map<Int, Long>
    )

    companion object {
        private const val TAG = "EventViewModel"

        /** Delay before committing delete to Room so the swipe-out animation completes. */
        const val DELETE_DELAY_MS = 200L

        /** How long a restoration key stays in uiState after undo (for LazyColumn re-key). */
        const val RESTORATION_KEY_TTL_MS = 300L
    }
}
