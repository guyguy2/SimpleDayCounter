package com.happypuppy.daycounter.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> get() = _uiState

    init {
        getAllItems()
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
            getAllItems()
        }
    }

    // Function to update an existing item
    fun updateItem(item: Item) {
        viewModelScope.launch {
            repository.insertItem(item) // Uses Room's REPLACE strategy to update existing item
            getAllItems()
        }
    }

    fun getAllItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val items = repository.getAllItems()
                _uiState.value = _uiState.value.copy(items = items, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            // Update UI state immediately
            Log.d(TAG, "Deleting item ${item.id}")
            _uiState.value = _uiState.value.copy(
                lastDeletedItem = item,
                showUndoSnackbar = true
            )

            // Small delay to ensure animations complete
            delay(200)

            // Then update database
            repository.deleteItem(item)
            Log.d(TAG, "Item deleted, fetching updated list")
            getAllItems()
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            _uiState.value.lastDeletedItem?.let { deletedItem ->
                Log.d(TAG, "Undoing delete for item ${deletedItem.id}")

                // Create a new map with this item's unique restoration key (current time)
                val updatedRestoredKeys = _uiState.value.restoredItemKeys.toMutableMap()
                updatedRestoredKeys[deletedItem.id] = System.currentTimeMillis()

                // Update UI state first with the new key
                _uiState.value = _uiState.value.copy(
                    lastDeletedItem = null,
                    showUndoSnackbar = false,
                    restoredItemKeys = updatedRestoredKeys
                )

                // Insert the item back to the database
                repository.insertItem(deletedItem)
                getAllItems()
            }
        }
    }

    fun clearRestorationKey(itemId: Int) {
        viewModelScope.launch {
            val currentKeys = _uiState.value.restoredItemKeys.toMutableMap()
            currentKeys.remove(itemId)
            _uiState.value = _uiState.value.copy(restoredItemKeys = currentKeys)
            Log.d(TAG, "Cleared restoration key for item $itemId")
        }
    }

    fun dismissUndoSnackbar() {
        _uiState.value = _uiState.value.copy(
            showUndoSnackbar = false,
            lastDeletedItem = null
        )
    }

    fun deleteAllItems() {
        viewModelScope.launch {
            repository.deleteAllItems()
            getAllItems()
        }
    }

    private companion object {
        private const val TAG = "EventViewModel"
    }
}