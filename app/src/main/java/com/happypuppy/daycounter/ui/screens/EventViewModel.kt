package com.happypuppy.daycounter.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.daycounter.BuildConfig
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Date

data class MainScreenUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastDeletedItem: Item? = null,
    val showUndoSnackbar: Boolean = false,
    val restoredItemKeys: Map<Int, Long> = mapOf() // Map of item ID to a timestamp "key"
)

class EventViewModel(private val repository: ItemRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MainScreenUiState(isLoading = true))
    val uiState: StateFlow<MainScreenUiState> get() = _uiState

    init {
        observeItems()
    }

    // Collect the reactive items stream once. Room emits a fresh list on every
    // underlying table change, so mutations (insert/delete/etc.) no longer need
    // to manually re-fetch.
    private fun observeItems() {
        viewModelScope.launch {
            repository.getAllItems()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
                .collect { items ->
                    _uiState.value = _uiState.value.copy(items = items, isLoading = false)
                }
        }
    }

    // Function to create a new item with emoji support
    fun saveItem(name: String, category: String, targetDate: Date, emoji: String = "") {
        viewModelScope.launch {
            repository.insertItem(
                Item(
                    name = name,
                    itemCategory = category,
                    targetDate = targetDate,
                    createdAt = Date(),
                    emoji = emoji
                )
            )
        }
    }

    // Function to update an existing item
    fun updateItem(item: Item) {
        viewModelScope.launch {
            repository.insertItem(item) // Uses Room's REPLACE strategy to update existing item
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            // Update UI state immediately
            if (BuildConfig.DEBUG) Log.d(TAG, "Deleting item ${item.id}")
            _uiState.value = _uiState.value.copy(
                lastDeletedItem = item,
                showUndoSnackbar = true
            )

            // Small delay to ensure animations complete
            delay(200)

            // Then update database; the items Flow will emit the new list.
            repository.deleteItem(item)
            if (BuildConfig.DEBUG) Log.d(TAG, "Item deleted, fetching updated list")
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            _uiState.value.lastDeletedItem?.let { deletedItem ->
                if (BuildConfig.DEBUG) Log.d(TAG, "Undoing delete for item ${deletedItem.id}")

                // Fresh timestamp forces LazyColumn to remount the row so swipe state resets.
                val restorationKey = System.currentTimeMillis()
                val updatedRestoredKeys = _uiState.value.restoredItemKeys.toMutableMap()
                updatedRestoredKeys[deletedItem.id] = restorationKey

                _uiState.value = _uiState.value.copy(
                    lastDeletedItem = null,
                    showUndoSnackbar = false,
                    restoredItemKeys = updatedRestoredKeys
                )

                repository.insertItem(deletedItem)

                // Auto-expire so the map does not grow for the process lifetime.
                // Only remove if this exact key is still present (a later undo of the
                // same id would have replaced it with a newer timestamp).
                delay(RESTORATION_KEY_TTL_MS)
                val stillCurrent = _uiState.value.restoredItemKeys[deletedItem.id]
                if (stillCurrent == restorationKey) {
                    removeRestorationKey(deletedItem.id)
                }
            }
        }
    }

    fun clearRestorationKey(itemId: Int) {
        viewModelScope.launch {
            removeRestorationKey(itemId)
        }
    }

    private fun removeRestorationKey(itemId: Int) {
        if (!_uiState.value.restoredItemKeys.containsKey(itemId)) return
        val currentKeys = _uiState.value.restoredItemKeys.toMutableMap()
        currentKeys.remove(itemId)
        _uiState.value = _uiState.value.copy(restoredItemKeys = currentKeys)
        if (BuildConfig.DEBUG) Log.d(TAG, "Cleared restoration key for item $itemId")
    }

    fun dismissUndoSnackbar() {
        _uiState.value = _uiState.value.copy(
            showUndoSnackbar = false,
            lastDeletedItem = null
        )
    }

    companion object {
        private const val TAG = "EventViewModel"

        /** How long a restoration key stays in uiState after undo (for LazyColumn re-key). */
        const val RESTORATION_KEY_TTL_MS = 300L
    }
}
