package com.happypuppy.daycounter.ui.screens

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.happypuppy.daycounter.BuildConfig
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.navigation.Screen
import com.happypuppy.daycounter.ui.components.AddEventBottomSheet
import com.happypuppy.daycounter.ui.components.EventCard
import com.happypuppy.daycounter.ui.components.FilterChip
import com.happypuppy.daycounter.ui.components.defaultCategories
import com.happypuppy.daycounter.util.calculateDaysLeft
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

enum class SortOption {
    NAME_ASC, NAME_DESC,
    TARGET_DATE_ASC, TARGET_DATE_DESC,
    DATE_ADDED_ASC, DATE_ADDED_DESC,
    CATEGORY_ASC, CATEGORY_DESC
}

// Define filter options
enum class FilterOption {
    ALL,           // Show all events
    FUTURE,        // Show only future events
    TODAY,         // Show only today's events
    PAST,          // Show only past events
    CATEGORY       // Show events of a specific category
}

// Data class to hold the current filter state
data class FilterState(
    val option: FilterOption = FilterOption.ALL,
    val category: String? = null // Used when option is CATEGORY
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController, viewModel: EventViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortMenu by rememberSaveable { mutableStateOf(false) }
    var selectedSortOption by rememberSaveable { mutableStateOf(SortOption.DATE_ADDED_DESC) }

    // Use the BottomSheetStateSaver for managing bottom sheet states
    val (showAddEventSheet, setShowAddEventSheet) = rememberSaveableSheetState("addEventSheet", false)
    val (showEditEventSheet, setShowEditEventSheet) = rememberSaveableSheetState("editEventSheet", false)

    // Use a nullable holder
    var selectedItemData by rememberSaveable { mutableStateOf<SelectedItemData?>(null) }
// Create a computed value for selectedItem based on selectedItemData
    var selectedItem by remember {
        mutableStateOf<Item?>(null)
    }

// Update selectedItem when selectedItemData changes
    LaunchedEffect(selectedItemData) {
        selectedItem = selectedItemData?.toItem()
    }
    var showFilterMenu by rememberSaveable { mutableStateOf(false) }

    var filterOption by rememberSaveable { mutableStateOf(FilterOption.ALL.name) }
    var filterCategory by rememberSaveable { mutableStateOf<String?>(null) }

    var filterState by remember {
        mutableStateOf(
            FilterState(
                option = FilterOption.valueOf(filterOption),
                category = filterCategory
            )
        )
    }
    var showCategoryFilterOptions by rememberSaveable { mutableStateOf(false) }

    // Log orientation changes
    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration) {
        if (BuildConfig.DEBUG) Log.d("MainScreen", "Configuration changed: orientation=${configuration.orientation}")
    }

    // Sort items based on selected option
    val filteredAndSortedItems = remember(uiState.items, selectedSortOption, filterState) {
        // First filter the items
        val filteredItems = filterItems(uiState.items, filterState)

        // Then sort the filtered items
        when (selectedSortOption) {
            SortOption.NAME_ASC -> filteredItems.sortedBy { it.name }
            SortOption.NAME_DESC -> filteredItems.sortedByDescending { it.name }
            SortOption.TARGET_DATE_ASC -> filteredItems.sortedBy { it.targetDate }
            SortOption.TARGET_DATE_DESC -> filteredItems.sortedByDescending { it.targetDate }
            SortOption.DATE_ADDED_ASC -> filteredItems.sortedBy { it.createdAt }
            SortOption.DATE_ADDED_DESC -> filteredItems.sortedByDescending { it.createdAt }
            SortOption.CATEGORY_ASC -> filteredItems.sortedBy { it.itemCategory }
            SortOption.CATEGORY_DESC -> filteredItems.sortedByDescending { it.itemCategory }
        }
    }

    // Get all category names (both default and custom)
    val dataStoreManager = koinInject<DataStoreManager>()
    val customCategories by dataStoreManager.customCategoriesFlow.collectAsState(initial = emptySet())
    val allCategories = remember(customCategories) {
        defaultCategories + customCategories.toList() + "Add Custom..."
    }

    LaunchedEffect(filterOption, filterCategory) {
        filterState = FilterState(
            option = FilterOption.valueOf(filterOption),
            category = filterCategory
        )
    }

    // Show undo snackbar when an item is deleted
    LaunchedEffect(uiState.showUndoSnackbar) {
        if (uiState.showUndoSnackbar && uiState.lastDeletedItem != null) {
            // Short delay to ensure the swipe animation has completed
            delay(200)
            val result = snackbarHostState.showSnackbar(
                message = "\"${uiState.lastDeletedItem?.name}\" deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
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
                title = { Text("Simple Day Counter") },
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
                            contentDescription = "Filter"
                        )

                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            Text(
                                text = "Filter by",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            DropdownMenuItem(
                                text = { Text("All Events") },
                                onClick = {
                                    filterOption = FilterOption.ALL.name
                                    filterCategory = null
                                    showFilterMenu = false
                                },
                                trailingIcon = if (filterState.option == FilterOption.ALL) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else null
                            )

                            DropdownMenuItem(
                                text = { Text("Future Events") },
                                onClick = {
                                    filterOption = FilterOption.FUTURE.name
                                    filterCategory = null
                                    showFilterMenu = false
                                },
                                trailingIcon = if (filterState.option == FilterOption.FUTURE) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else null
                            )

                            DropdownMenuItem(
                                text = { Text("Today's Events") },
                                onClick = {
                                    filterOption = FilterOption.TODAY.name
                                    filterCategory = null
                                    showFilterMenu = false
                                },
                                trailingIcon = if (filterState.option == FilterOption.TODAY) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else null
                            )


                            DropdownMenuItem(
                                text = { Text("Past Events") },
                                onClick = {
                                    filterOption = FilterOption.PAST.name
                                    filterCategory = null
                                    showFilterMenu = false
                                },
                                trailingIcon = if (filterState.option == FilterOption.PAST) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else null
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Filter by Category")
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Show Categories"
                                        )
                                    }
                                },
                                onClick = { showCategoryFilterOptions = true },
                                trailingIcon = if (filterState.option == FilterOption.CATEGORY) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else null
                            )
                        }
                        DropdownMenu(
                            expanded = showCategoryFilterOptions,
                            onDismissRequest = { showCategoryFilterOptions = false }
                        ) {
                            allCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        filterOption = FilterOption.CATEGORY.name
                                        filterCategory = category
                                        showCategoryFilterOptions = false
                                        showFilterMenu = false
                                    },
                                    trailingIcon = if (filterState.option == FilterOption.CATEGORY &&
                                        filterState.category == category
                                    ) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected"
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort"
                        )

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            Text(
                                text = "Sort by",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)") },
                                onClick = {
                                    selectedSortOption = SortOption.NAME_ASC
                                    showSortMenu = false
                                },
                                trailingIcon = if (selectedSortOption == SortOption.NAME_ASC) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z-A)") },
                                onClick = {
                                    selectedSortOption = SortOption.NAME_DESC
                                    showSortMenu = false
                                },
                                trailingIcon = if (selectedSortOption == SortOption.NAME_DESC) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Category (A-Z)") },
                                onClick = {
                                    selectedSortOption = SortOption.CATEGORY_ASC
                                    showSortMenu = false
                                },
                                trailingIcon = if (selectedSortOption == SortOption.CATEGORY_ASC) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Category (Z-A)") },
                                onClick = {
                                    selectedSortOption = SortOption.CATEGORY_DESC
                                    showSortMenu = false
                                },
                                trailingIcon = if (selectedSortOption == SortOption.CATEGORY_DESC) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Target Date (Earliest first)") },
                                onClick = {
                                    selectedSortOption = SortOption.TARGET_DATE_ASC
                                    showSortMenu = false
                                },
                                trailingIcon = if (selectedSortOption == SortOption.TARGET_DATE_ASC) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Target Date (Latest first)") },
                                onClick = {
                                    selectedSortOption = SortOption.TARGET_DATE_DESC
                                    showSortMenu = false
                                },
                                trailingIcon = if (selectedSortOption == SortOption.TARGET_DATE_DESC) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Date Added (Oldest first)") },
                                onClick = {
                                    selectedSortOption = SortOption.DATE_ADDED_ASC
                                    showSortMenu = false
                                },
                                trailingIcon = if (selectedSortOption == SortOption.DATE_ADDED_ASC) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Date Added (Newest first)") },
                                onClick = {
                                    selectedSortOption = SortOption.DATE_ADDED_DESC
                                    showSortMenu = false
                                },
                                trailingIcon = if (selectedSortOption == SortOption.DATE_ADDED_DESC) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else null
                            )
                        }
                    }

                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { setShowAddEventSheet(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text("Add event")
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Show filter chip if filter is active
                if (filterState.option != FilterOption.ALL) {
                    FilterChip(
                        filterState = filterState,
                        onClear = {
                            filterOption = FilterOption.ALL.name
                            filterCategory = null
                        }
                    )
                }

                // Show appropriate content based on filtered/sorted items
                if (filteredAndSortedItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (filterState.option != FilterOption.ALL)
                                "No Events Match Filter" else "No Events Yet",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (filterState.option != FilterOption.ALL)
                                "Try changing your filter settings"
                            else "Tap the + button to add your first event",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredAndSortedItems,
                            key = { item ->
                                val restorationKey = uiState.restoredItemKeys[item.id] ?: 0
                                "${item.id}_$restorationKey"
                            }
                        ) { item ->
                            val restorationKey = uiState.restoredItemKeys[item.id] ?: 0
                            key(item.id, restorationKey) {
                                SwipeToDeleteItem(
                                    item = item,
                                    onDelete = { viewModel.deleteItem(item) },
                                    onClick = {
                                        selectedItem = item
                                        setShowEditEventSheet(true)
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                        }
                    }
                }
            }

            // Add event sheet
            if (showAddEventSheet) {
                ModalBottomSheet(
                    onDismissRequest = { setShowAddEventSheet(false) },
                    sheetState = bottomSheetState
                ) {
                    AddEventBottomSheet(
                        onDismiss = { setShowAddEventSheet(false) },
                        onSave = { name, date, category, emoji ->
                            viewModel.saveItem(
                                name = name,
                                category = category,
                                targetDate = date,
                                emoji = emoji
                            )
                            setShowAddEventSheet(false)
                        },
                        navController = navController
                    )
                }
            }

            // Edit event sheet
            if (showEditEventSheet && selectedItem != null) {
                ModalBottomSheet(
                    onDismissRequest = { setShowEditEventSheet(false) },
                    sheetState = bottomSheetState
                ) {
                    EditEventScreen(
                        item = selectedItem!!,
                        onSave = { item ->
                            viewModel.updateItem(item)
                            setShowEditEventSheet(false)
                        },
                        onCancel = { setShowEditEventSheet(false) },
                        onDelete = { item ->
                            viewModel.deleteItem(item)
                            setShowEditEventSheet(false)
                        },
                        navController = navController
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteItem(
    item: Item,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val viewModel = koinViewModel<EventViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dataStoreManager = koinInject<DataStoreManager>()
    val isVibrationEnabled by dataStoreManager.vibrateEnabledFlow.collectAsState(initial = true)

    // Track if this specific item was recently restored
    val wasUndone = remember(uiState.restoredItemKeys) {
        uiState.restoredItemKeys.containsKey(item.id)
    }

    var isRemoved by remember { mutableStateOf(false) }

    // Reset isRemoved if this item was just restored via undo
    LaunchedEffect(wasUndone) {
        if (wasUndone) {
            isRemoved = false
            if (BuildConfig.DEBUG) Log.d("SwipeDelete", "Reset removal state for undone item ${item.id}")
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.5f },
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                isRemoved = true
                true
            } else {
                false
            }
        }
    )

    LaunchedEffect(isRemoved) {
        if (isRemoved) {
            if (BuildConfig.DEBUG) Log.d("SwipeDelete", "Item ${item.id} marked as removed, animating...")
            // Vibrate if enabled
            if (isVibrationEnabled) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        100, // Duration in milliseconds
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            }
            delay(200)
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
            backgroundContent = {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.Settled -> Color.LightGray
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50) // Green
                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFE91E63) // Pink/Red
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color),
                    contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd)
                        Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        ) {
            EventCard(item = item, onClick = onClick)
        }
    }
}

fun filterItems(items: List<Item>, filterState: FilterState): List<Item> {
    return when (filterState.option) {
        FilterOption.ALL -> items
        FilterOption.FUTURE -> items.filter { calculateDaysLeft(it.targetDate) > 0 }
        FilterOption.TODAY -> items.filter { calculateDaysLeft(it.targetDate) == 0 }
        FilterOption.PAST -> items.filter { calculateDaysLeft(it.targetDate) < 0 }
        FilterOption.CATEGORY -> filterState.category?.let { category ->
            items.filter { it.itemCategory == category }
        } ?: items // If category is null, show all items
    }
}