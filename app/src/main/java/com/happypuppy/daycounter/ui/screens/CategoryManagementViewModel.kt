package com.happypuppy.daycounter.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.database.AddCategoryResult
import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.data.database.CategoryRepository
import com.happypuppy.daycounter.data.database.CategoryWithCount
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORIES
import com.happypuppy.daycounter.data.database.DeletedCategorySnapshot
import com.happypuppy.daycounter.data.preferences.EventListPreferences
import com.happypuppy.daycounter.data.preferences.InMemoryEventListPreferences
import com.happypuppy.daycounter.util.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

sealed interface CategoryUiEvent {
    data class ShowUndoSnackbar(
        val label: String,
        val isAll: Boolean
    ) : CategoryUiEvent
}

sealed interface CategoryError {
    val message: UiText

    data object LoadFailed : CategoryError {
        override val message = UiText.StringResource(R.string.category_mgmt_error_load_failed)
    }

    data object AddFailed : CategoryError {
        override val message = UiText.StringResource(R.string.category_mgmt_error_add_failed)
    }

    data object DuplicateName : CategoryError {
        override val message = UiText.StringResource(R.string.category_mgmt_error_duplicate)
    }

    data object DeleteFailed : CategoryError {
        override val message = UiText.StringResource(R.string.category_mgmt_error_delete_failed)
    }

    data object RestoreFailed : CategoryError {
        override val message = UiText.StringResource(R.string.category_mgmt_error_restore_failed)
    }
}

data class CategoryManagementUiState(
    val defaultCategories: List<Category> = DEFAULT_CATEGORIES,
    val customCategories: List<CategoryWithCount> = emptyList(),
    val isLoading: Boolean = true,
    val error: CategoryError? = null,
    val canUndo: Boolean = false
) {
    val totalCustomUsageCount: Int
        get() = customCategories.sumOf { it.eventCount }
}

class CategoryManagementViewModel(
    private val repository: CategoryRepository,
    private val listPreferences: EventListPreferences = InMemoryEventListPreferences()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryManagementUiState(isLoading = true))
    val uiState: StateFlow<CategoryManagementUiState> = _uiState.asStateFlow()

    private val _events = Channel<CategoryUiEvent>(Channel.BUFFERED)
    val events: Flow<CategoryUiEvent> = _events.receiveAsFlow()

    private val undoStack = ArrayDeque<DeletedCategorySnapshot>()
    private val dbMutex = Mutex()

    init {
        viewModelScope.launch {
            try {
                repository.ensureInitialized()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(error = CategoryError.LoadFailed, isLoading = false) }
            }
        }
        observeCategories()
    }

    private fun observeCategories() {
        viewModelScope.launch {
            repository.observeCategoriesWithUsageCount()
                .catch {
                    _uiState.update { it.copy(error = CategoryError.LoadFailed, isLoading = false) }
                }
                .collect { allWithCounts ->
                    val defaults = allWithCounts
                        .filter { it.category.isDefault }
                        .map { it.category }
                        .ifEmpty { DEFAULT_CATEGORIES }
                    val custom = allWithCounts.filterNot { it.category.isDefault }
                    _uiState.update { current ->
                        current.copy(
                            defaultCategories = defaults,
                            customCategories = custom,
                            isLoading = false
                        )
                    }
                }
        }
    }

    suspend fun addCustomCategory(rawName: String): Category? {
        return dbMutex.withLock {
            try {
                when (val result = repository.addCustomCategory(rawName)) {
                    is AddCategoryResult.Added -> {
                        _uiState.update { it.copy(error = null) }
                        result.category
                    }

                    is AddCategoryResult.Duplicate -> {
                        _uiState.update { it.copy(error = CategoryError.DuplicateName) }
                        null
                    }

                    AddCategoryResult.Invalid -> null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(error = CategoryError.AddFailed) }
                null
            }
        }
    }

    fun deleteCustomCategory(categoryId: String) {
        viewModelScope.launch {
            dbMutex.withLock {
                try {
                    val snapshot = repository.deleteCustomCategory(categoryId) ?: return@withLock
                    val tracked = clearMatchingFilter(snapshot)
                    undoStack.addLast(tracked)
                    _uiState.update { it.copy(error = null, canUndo = true) }
                    _events.trySend(
                        CategoryUiEvent.ShowUndoSnackbar(
                            label = tracked.entries.first().category.name,
                            isAll = false
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _uiState.update { it.copy(error = CategoryError.DeleteFailed) }
                }
            }
        }
    }

    fun deleteAllCustomCategories() {
        viewModelScope.launch {
            dbMutex.withLock {
                try {
                    val snapshot = repository.deleteAllCustomCategories() ?: return@withLock
                    val tracked = clearMatchingFilter(snapshot)
                    undoStack.addLast(tracked)
                    _uiState.update { it.copy(error = null, canUndo = true) }
                    _events.trySend(
                        CategoryUiEvent.ShowUndoSnackbar(
                            label = "",
                            isAll = true
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _uiState.update { it.copy(error = CategoryError.DeleteFailed) }
                }
            }
        }
    }

    fun undoDelete() {
        if (undoStack.isEmpty()) return

        viewModelScope.launch {
            dbMutex.withLock {
                val snapshot = undoStack.lastOrNull() ?: return@withLock
                try {
                    repository.restoreDeletedCategories(snapshot)
                    snapshot.previousFilterState?.let { previousFilter ->
                        listPreferences.setFilterState(previousFilter)
                    }
                    undoStack.removeLast()
                    _uiState.update { it.copy(error = null, canUndo = undoStack.isNotEmpty()) }
                    undoStack.lastOrNull()?.let { previous ->
                        _events.trySend(
                            CategoryUiEvent.ShowUndoSnackbar(
                                label = if (previous.isDeleteAll) "" else previous.entries.first().category.name,
                                isAll = previous.isDeleteAll
                            )
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _uiState.update { it.copy(error = CategoryError.RestoreFailed) }
                }
            }
        }
    }

    fun dismissUndoSnackbar() {
        undoStack.clear()
        _uiState.update { it.copy(canUndo = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun clearMatchingFilter(snapshot: DeletedCategorySnapshot): DeletedCategorySnapshot {
        val currentFilter = listPreferences.filterStateFlow.first()
        val deletedIds = snapshot.entries.map { it.category.id }.toSet()
        val deletedNamesLower = snapshot.entries.map { it.category.name.lowercase(Locale.ROOT) }.toSet()
        val matchesDeleted = currentFilter.option == FilterOption.CATEGORY &&
            currentFilter.category != null &&
            (currentFilter.category in deletedIds ||
                currentFilter.category.lowercase(Locale.ROOT) in deletedNamesLower)
        return if (matchesDeleted) {
            listPreferences.setFilterState(FilterState())
            snapshot.copy(previousFilterState = currentFilter)
        } else {
            snapshot
        }
    }
}
