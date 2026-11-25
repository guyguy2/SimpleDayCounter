package com.happypuppy.daycounter.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.happypuppy.daycounter.BuildConfig
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.ui.components.AddEventBottomSheet
import com.happypuppy.daycounter.ui.components.EventCard
import com.happypuppy.daycounter.ui.components.FilterChip
import com.happypuppy.daycounter.ui.components.FilterMenu
import com.happypuppy.daycounter.ui.components.SortMenu
import com.happypuppy.daycounter.ui.components.defaultCategories
import com.happypuppy.daycounter.util.vibrate
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    onOpenCategories: () -> Unit,
    viewModel: EventViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val addSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val editSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    var showSortMenu by rememberSaveable { mutableStateOf(false) }
    var selectedSortOption by rememberSaveable { mutableStateOf(SortOption.DATE_ADDED_DESC) }

    var showAddEventSheet by rememberSaveable { mutableStateOf(false) }
    var showEditEventSheet by rememberSaveable { mutableStateOf(false) }

    // Save only the selected item's id; resolve the Item against the current list so the
    // edit sheet survives configuration changes and process death (IMPROVEMENTS.md F-1).
    var selectedItemId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedItem = remember(uiState.items, selectedItemId) {
        uiState.items.find { it.id == selectedItemId }
    }
    var showFilterMenu by rememberSaveable { mutableStateOf(false) }

    var filterState by rememberSaveable(stateSaver = FilterStateSaver) { mutableStateOf(FilterState()) }
    var showCategoryFilterOptions by rememberSaveable { mutableStateOf(false) }

    // Log orientation changes
    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration) {
        if (BuildConfig.DEBUG) Log.d("MainScreen", "Configuration changed: orientation=${configuration.orientation}")
    }

    // Sort items based on selected option (case-insensitive for name/category)
    val filteredAndSortedItems = remember(uiState.items, selectedSortOption, filterState) {
        sortItems(filterItems(uiState.items, filterState), selectedSortOption)
    }

    // Get all category names (both default and custom); inject at screen root (P1-1)
    val dataStoreManager = koinInject<DataStoreManager>()
    val customCategories by dataStoreManager.customCategoriesFlow.collectAsState(initial = emptySet())
    val isVibrationEnabled by dataStoreManager.vibrateEnabledFlow.collectAsState(initial = true)
    // Filter menu lists real categories only; the "Add Custom..." entry belongs to CategorySelector.
    val allCategories = remember(customCategories) {
        defaultCategories + customCategories.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    // Show undo snackbar whenever a new delete event occurs (keyed on monotonic token so
    // consecutive deletes replace the previous snackbar cleanly).
    val resources = LocalResources.current
    LaunchedEffect(uiState.snackbarToken) {
        val deletedItem = uiState.lastDeletedItem
        if (uiState.showUndoSnackbar && deletedItem != null) {
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.main_event_deleted, deletedItem.name),
                actionLabel = resources.getString(R.string.main_undo),
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.undoDelete()
                SnackbarResult.Dismissed -> viewModel.dismissUndoSnackbar()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { MainTopBarTitle() },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                },
                actions = {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            imageVector = if (filterState.option != FilterOption.ALL)
                                Icons.Default.FilterAlt else Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.main_filter)
                        )

                        FilterMenu(
                            expanded = showFilterMenu,
                            showCategoryOptions = showCategoryFilterOptions,
                            filterState = filterState,
                            categories = allCategories,
                            onSelectFilter = { filterState = it },
                            onDismiss = { showFilterMenu = false },
                            onOpenCategories = { showCategoryFilterOptions = true },
                            onDismissCategories = { showCategoryFilterOptions = false }
                        )
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.main_sort)
                        )

                        SortMenu(
                            expanded = showSortMenu,
                            selectedSortOption = selectedSortOption,
                            onSelectOption = { selectedSortOption = it },
                            onDismiss = { showSortMenu = false }
                        )
                    }

                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.main_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            val addEventLabel = stringResource(R.string.add_event)
            ExtendedFloatingActionButton(
                onClick = { showAddEventSheet = true },
                modifier = Modifier.semantics { contentDescription = addEventLabel },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text(addEventLabel)
                }
            )
        }
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection)
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Error banner if any database operation failed
                uiState.error?.let { error ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error.message.asString(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text(
                                    text = stringResource(R.string.main_dismiss),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Show filter chip if filter is active
                if (filterState.option != FilterOption.ALL) {
                    FilterChip(
                        filterState = filterState,
                        onClear = { filterState = FilterState() }
                    )
                }

                // Show loading, empty state, or event list
                when {
                    uiState.isLoading && uiState.items.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding()),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    filteredAndSortedItems.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = innerPadding.calculateBottomPadding() + 16.dp
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (filterState.option != FilterOption.ALL)
                                    stringResource(R.string.main_empty_filtered_title) else stringResource(R.string.main_empty_title),
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (filterState.option != FilterOption.ALL)
                                    stringResource(R.string.main_empty_filtered_hint)
                                else stringResource(R.string.main_empty_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = innerPadding.calculateBottomPadding() + 88.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = filteredAndSortedItems,
                                // Include restoration token so undo remounts the row
                                // (resets swipe state).
                                key = { item ->
                                    val restorationKey = uiState.restoredItemKeys[item.id] ?: 0L
                                    "${item.id}_$restorationKey"
                                }
                            ) { item ->
                                SwipeToDeleteItem(
                                    item = item,
                                    wasUndone = uiState.restoredItemKeys.containsKey(item.id),
                                    isVibrationEnabled = isVibrationEnabled,
                                    onDelete = { viewModel.deleteItem(item) },
                                    onClick = {
                                        selectedItemId = item.id
                                        showEditEventSheet = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Add event sheet
            if (showAddEventSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAddEventSheet = false },
                    sheetState = addSheetState
                ) {
                    AddEventBottomSheet(
                        dataStoreManager = dataStoreManager,
                        onDismiss = { showAddEventSheet = false },
                        onSave = { name, date, category, emoji ->
                            viewModel.saveItem(
                                name = name,
                                category = category,
                                targetDate = date,
                                emoji = emoji
                            )
                            showAddEventSheet = false
                        },
                        onOpenCategories = onOpenCategories
                    )
                }
            }

            // Edit event sheet
            if (showEditEventSheet && selectedItem != null) {
                ModalBottomSheet(
                    onDismissRequest = { showEditEventSheet = false },
                    sheetState = editSheetState
                ) {
                    EditEventScreen(
                        item = selectedItem,
                        dataStoreManager = dataStoreManager,
                        onSave = { item ->
                            viewModel.updateItem(item)
                            showEditEventSheet = false
                        },
                        onDelete = { item ->
                            viewModel.deleteItem(item)
                            showEditEventSheet = false
                        },
                        onOpenCategories = onOpenCategories
                    )
                }
            }
        }
    }
}

@Composable
internal fun MainTopBarTitle() {
    Text(
        text = stringResource(R.string.app_name),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteItem(
    item: Item,
    wasUndone: Boolean,
    isVibrationEnabled: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var isRemoved by remember { mutableStateOf(false) }

    // Reset isRemoved if this item was just restored via undo
    LaunchedEffect(wasUndone) {
        if (wasUndone) {
            isRemoved = false
            if (BuildConfig.DEBUG) Log.d("SwipeDelete", "Reset removal state for undone item ${item.id}")
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.5f }
    )

    // Shared by the swipe gesture and the card's "Delete" accessibility action.
    val deleteOnce = {
        if (!isRemoved) {
            isRemoved = true
            if (isVibrationEnabled) {
                vibrate(context)
            }
            if (BuildConfig.DEBUG) Log.d("SwipeDelete", "Executing onDelete for item ${item.id}")
            onDelete()
        }
    }

    AnimatedVisibility(
        visible = !isRemoved,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        SwipeToDismissBox(
            state = dismissState,
            onDismiss = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                    deleteOnce()
                }
            },
            backgroundContent = {
                val color by animateColorAsState(
                    targetValue = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                        SwipeToDismissBoxValue.StartToEnd,
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    },
                    label = "SwipeDeleteBackground"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(color),
                    contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd)
                        Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        ) {
            EventCard(item = item, onClick = onClick, onDelete = deleteOnce)
        }
    }
}
