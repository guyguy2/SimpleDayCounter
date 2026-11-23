package com.happypuppy.daycounter.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.daycounter.BuildConfig
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

data class MainScreenUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastDeletedItem: Item? = null,
    val showUndoSnackbar: Boolean = false,
    val snackbarToken: Long = 0L,
    val restoredItemKeys: Map<Int, Long> = emptyMap()
)

class EventViewModel(private val repository: ItemRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MainScreenUiState(isLoading = true))
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private val tokenGenerator = AtomicLong(0L)
    private val pendingDeleteJobs = mutableMapOf<Int, Job>()

    init {
        observeItems()
    }

    // Collect the reactive items stream once. Room emits a fresh list on every
    // underlying table change, so mutations (insert/delete/etc.) do not need
    // to manually re-fetch.
    private fun observeItems() {
        viewModelScope.launch {
            repository.getAllItems()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to load events", isLoading = false) }
                }
                .collect { items ->
                    _uiState.update { it.copy(items = items, isLoading = false) }
                }
        }
    }

    fun saveItem(name: String, category: String, targetDate: Date, emoji: String = "") {
        viewModelScope.launch {
            try {
                repository.insertItem(
                    Item(
                        name = name,
                        itemCategory = category,
                        targetDate = targetDate,
                        createdAt = Date(),
                        emoji = emoji
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to save event") }
            }
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.insertItem(item)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update event") }
            }
        }
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
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete event") }
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
        _uiState.update { current ->
            current.copy(
                lastDeletedItem = null,
                showUndoSnackbar = false,
                restoredItemKeys = current.restoredItemKeys + (deletedItem.id to restorationKey),
                error = null
            )
        }

        viewModelScope.launch {
            try {
                repository.insertItem(deletedItem)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to restore event") }
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
        _uiState.update { current ->
            if (!current.restoredItemKeys.containsKey(itemId)) {
                current
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "Cleared restoration key for item $itemId")
                current.copy(restoredItemKeys = current.restoredItemKeys - itemId)
            }
        }
    }

    private fun removeRestorationKeyIfMatches(itemId: Int, expectedKey: Long) {
        _uiState.update { current ->
            if (current.restoredItemKeys[itemId] != expectedKey) {
                current
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "Auto-expired restoration key for item $itemId")
                current.copy(restoredItemKeys = current.restoredItemKeys - itemId)
            }
        }
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

    companion object {
        private const val TAG = "EventViewModel"

        /** Delay before committing delete to Room so the swipe-out animation completes. */
        const val DELETE_DELAY_MS = 200L

        /** How long a restoration key stays in uiState after undo (for LazyColumn re-key). */
        const val RESTORATION_KEY_TTL_MS = 300L
    }
}
