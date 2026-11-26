package com.happypuppy.daycounter.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORIES
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.FINANCE_CATEGORY_ID
import com.happypuppy.daycounter.data.database.HEALTH_CATEGORY_ID
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.ui.components.AddEventBottomSheet
import com.happypuppy.daycounter.ui.components.EmptyEventsState
import com.happypuppy.daycounter.ui.components.EventCard
import com.happypuppy.daycounter.ui.components.EventFormState
import com.happypuppy.daycounter.ui.components.EventFormStateSaver
import com.happypuppy.daycounter.ui.components.FilterChip
import com.happypuppy.daycounter.ui.components.FilterMenu
import com.happypuppy.daycounter.ui.components.SortMenu
import com.happypuppy.daycounter.ui.components.SwipeToDeleteItem
import com.happypuppy.daycounter.ui.layout.ADAPTIVE_CONTENT_MAX_WIDTH
import com.happypuppy.daycounter.ui.layout.ADAPTIVE_GRID_MIN_CELL_WIDTH
import com.happypuppy.daycounter.ui.layout.AdaptiveEventFormContainer
import com.happypuppy.daycounter.ui.layout.AdaptiveLayoutSpec
import com.happypuppy.daycounter.ui.layout.rememberAdaptiveLayoutSpec
import com.happypuppy.daycounter.ui.theme.SimpleDayCounterTheme
import com.happypuppy.daycounter.util.DayLabel
import com.happypuppy.daycounter.util.vibrate
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.LocalDate

internal const val MAIN_EVENT_LIST_TAG = "main_event_list"
internal const val MAIN_EVENT_GRID_TAG = "main_event_grid"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    viewModel: EventViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val layoutSpec = rememberAdaptiveLayoutSpec()
    val snackbarHostState = remember { SnackbarHostState() }
    val addSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val editSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    var showAddEventSheet by rememberSaveable { mutableStateOf(false) }
    var showEditEventSheet by rememberSaveable { mutableStateOf(false) }
    var addFormState by rememberSaveable(stateSaver = EventFormStateSaver) {
        mutableStateOf(EventFormState.defaultNew())
    }
    var editFormState by rememberSaveable(stateSaver = EventFormStateSaver) {
        mutableStateOf(EventFormState.defaultNew())
    }

    // Save only the selected item's id; resolve the Item against the current list so the
    // edit sheet survives configuration changes and process death (IMPROVEMENTS.md F-1).
    var selectedItemId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedItem = remember(uiState.events, selectedItemId) {
        selectedItemId?.let(viewModel::itemById)
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(context, viewModel) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                viewModel.refreshToday()
            }
        }
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshToday()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val dataStoreManager = koinInject<DataStoreManager>()
    val isVibrationEnabled by dataStoreManager.vibrateEnabledFlow.collectAsState(initial = true)
    val categories = uiState.categories

    // Show undo snackbar for one-shot delete/undo Channel events. collectLatest cancels any
    // superseded snackbar with CancellationException so SnackbarResult.Dismissed only runs
    // on real timeout/dismissal, preserving the multi-undo stack across consecutive deletes.
    val resources = LocalResources.current
    LaunchedEffect(viewModel, snackbarHostState, resources) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is MainUiEvent.ShowUndoSnackbar -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.main_event_deleted, event.itemName),
                        actionLabel = resources.getString(R.string.main_undo),
                        duration = SnackbarDuration.Long
                    )
                    when (result) {
                        SnackbarResult.ActionPerformed -> viewModel.undoDelete()
                        SnackbarResult.Dismissed -> viewModel.dismissUndoSnackbar()
                    }
                }
            }
        }
    }

    MainScreenContent(
        uiState = uiState,
        categories = categories,
        isVibrationEnabled = isVibrationEnabled,
        snackbarHostState = snackbarHostState,
        onOpenSettings = onOpenSettings,
        onAddEvent = {
            addFormState = EventFormState.defaultNew()
            showAddEventSheet = true
        },
        onEditEvent = { id ->
            viewModel.itemById(id)?.let { item ->
                editFormState = EventFormState.fromItem(item)
                selectedItemId = id
                showEditEventSheet = true
            }
        },
        onDeleteEvent = viewModel::deleteEvent,
        onSelectSortOption = viewModel::setSortOption,
        onSelectFilterState = viewModel::setFilterState,
        onClearFilter = viewModel::clearFilter,
        onDismissError = viewModel::clearError,
        layoutSpec = layoutSpec
    )

    // Add event sheet / adaptive wide dialog
    if (showAddEventSheet) {
        AdaptiveEventFormContainer(
            useDialog = layoutSpec.useDialogForEventForm,
            sheetState = addSheetState,
            onDismissRequest = {
                showAddEventSheet = false
                addFormState = EventFormState.defaultNew()
            }
        ) {
            AddEventBottomSheet(
                state = addFormState,
                categories = categories,
                onStateChange = { addFormState = it },
                onAddCustomCategory = viewModel::addCustomCategory,
                onDismiss = {
                    showAddEventSheet = false
                    addFormState = EventFormState.defaultNew()
                },
                onSave = {
                    if (addFormState.canSave) {
                        viewModel.saveItem(
                            name = addFormState.trimmedName,
                            categoryId = addFormState.selectedCategoryId,
                            targetDate = addFormState.targetDate,
                            emoji = addFormState.emoji,
                            repeatsYearly = addFormState.repeatsYearly
                        )
                        showAddEventSheet = false
                        addFormState = EventFormState.defaultNew()
                    }
                }
            )
        }
    }

    // Edit event sheet / adaptive wide dialog
    if (showEditEventSheet && selectedItem != null) {
        AdaptiveEventFormContainer(
            useDialog = layoutSpec.useDialogForEventForm,
            sheetState = editSheetState,
            onDismissRequest = {
                showEditEventSheet = false
                selectedItemId = null
            }
        ) {
            EditEventScreen(
                item = selectedItem,
                state = editFormState,
                categories = categories,
                onStateChange = { editFormState = it },
                onAddCustomCategory = viewModel::addCustomCategory,
                onSave = { item ->
                    viewModel.updateItem(item)
                    showEditEventSheet = false
                    selectedItemId = null
                },
                onDelete = { item ->
                    viewModel.deleteItem(item)
                    showEditEventSheet = false
                    selectedItemId = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    uiState: MainScreenUiState,
    categories: List<Category>,
    isVibrationEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    onOpenSettings: () -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (Int) -> Unit,
    onDeleteEvent: (Int) -> Unit,
    onSelectSortOption: (SortOption) -> Unit,
    onSelectFilterState: (FilterState) -> Unit,
    onClearFilter: () -> Unit,
    onDismissError: () -> Unit,
    layoutSpec: AdaptiveLayoutSpec = rememberAdaptiveLayoutSpec()
) {
    var showSortMenu by rememberSaveable { mutableStateOf(false) }
    var showFilterMenu by rememberSaveable { mutableStateOf(false) }
    var showCategoryFilterOptions by rememberSaveable { mutableStateOf(false) }

    val filterState = uiState.filterState
    val selectedSortOption = uiState.sortOption

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
                            imageVector = if (filterState.option != FilterOption.ALL) {
                                Icons.Default.FilterAlt
                            } else {
                                Icons.Default.FilterList
                            },
                            contentDescription = stringResource(R.string.main_filter)
                        )

                        FilterMenu(
                            expanded = showFilterMenu,
                            showCategoryOptions = showCategoryFilterOptions,
                            filterState = filterState,
                            categories = categories,
                            onSelectFilter = onSelectFilterState,
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
                            onSelectOption = onSelectSortOption,
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
                onClick = onAddEvent,
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
        val context = LocalContext.current
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
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error banner if any database operation failed
                uiState.error?.let { error ->
                    Surface(
                        modifier = Modifier
                            .widthIn(max = ADAPTIVE_CONTENT_MAX_WIDTH)
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
                            TextButton(onClick = onDismissError) {
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
                        categories = categories,
                        onClear = onClearFilter,
                        modifier = Modifier.widthIn(max = ADAPTIVE_CONTENT_MAX_WIDTH)
                    )
                }

                // Show loading, empty state, or adaptive event list / grid
                when {
                    uiState.isLoading && !uiState.hasAnyEvents -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding()),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    uiState.events.isEmpty() -> {
                        EmptyEventsState(
                            isFiltered = filterState.option != FilterOption.ALL,
                            onAddEvent = onAddEvent,
                            onClearFilter = onClearFilter,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = innerPadding.calculateBottomPadding() + 16.dp
                            )
                        )
                    }

                    layoutSpec.useGrid -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = ADAPTIVE_GRID_MIN_CELL_WIDTH),
                            modifier = Modifier
                                .widthIn(max = ADAPTIVE_CONTENT_MAX_WIDTH)
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag(MAIN_EVENT_GRID_TAG),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = innerPadding.calculateBottomPadding() + 88.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.events,
                                key = { event -> "${event.id}_${event.restoreVersion}" }
                            ) { event ->
                                // Multi-column grids disable horizontal swipe-to-dismiss so cells
                                // stay uncluttered; edit sheet Delete, hardware Delete key, and
                                // TalkBack custom Delete action remain available.
                                var deleteDispatched by remember(event.id, event.restoreVersion) {
                                    mutableStateOf(false)
                                }
                                val deleteOnce = {
                                    if (!deleteDispatched) {
                                        deleteDispatched = true
                                        if (isVibrationEnabled) {
                                            vibrate(context)
                                        }
                                        onDeleteEvent(event.id)
                                    }
                                }
                                EventCard(
                                    event = event,
                                    onClick = { onEditEvent(event.id) },
                                    onDelete = deleteOnce
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(MAIN_EVENT_LIST_TAG),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = innerPadding.calculateBottomPadding() + 88.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.events,
                                // Include restoreVersion so undo remounts the row (resets swipe state).
                                key = { event -> "${event.id}_${event.restoreVersion}" }
                            ) { event ->
                                SwipeToDeleteItem(
                                    event = event,
                                    isVibrationEnabled = isVibrationEnabled,
                                    onDelete = { onDeleteEvent(event.id) },
                                    onClick = { onEditEvent(event.id) }
                                )
                            }
                        }
                    }
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

private fun previewEvents(): List<EventUi> {
    val today = LocalDate.of(2026, 6, 15)
    return listOf(
        EventUi(
            id = 1,
            name = "Vacation",
            categoryId = DEFAULT_CATEGORY_ID,
            categoryName = "Personal",
            targetDate = today.plusDays(14),
            daysLeft = 14,
            dayLabel = DayLabel.Left(14),
            emoji = "\u2708\uFE0F",
            createdAtMillis = 3_000L
        ),
        EventUi(
            id = 2,
            name = "Dentist",
            categoryId = HEALTH_CATEGORY_ID,
            categoryName = "Health",
            targetDate = today,
            daysLeft = 0,
            dayLabel = DayLabel.Today,
            emoji = "",
            createdAtMillis = 2_000L
        ),
        EventUi(
            id = 3,
            name = "Tax Filing",
            categoryId = FINANCE_CATEGORY_ID,
            categoryName = "Finance",
            targetDate = today.minusDays(3),
            daysLeft = -3,
            dayLabel = DayLabel.Past(3),
            emoji = "",
            createdAtMillis = 1_000L
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun MainScreenEmptyLightPreview() {
    SimpleDayCounterTheme(darkTheme = false) {
        MainScreenContent(
            uiState = MainScreenUiState(),
            categories = DEFAULT_CATEGORIES,
            isVibrationEnabled = true,
            snackbarHostState = remember { SnackbarHostState() },
            onOpenSettings = {},
            onAddEvent = {},
            onEditEvent = {},
            onDeleteEvent = {},
            onSelectSortOption = {},
            onSelectFilterState = {},
            onClearFilter = {},
            onDismissError = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenEmptyDarkPreview() {
    SimpleDayCounterTheme(darkTheme = true) {
        MainScreenContent(
            uiState = MainScreenUiState(),
            categories = DEFAULT_CATEGORIES,
            isVibrationEnabled = true,
            snackbarHostState = remember { SnackbarHostState() },
            onOpenSettings = {},
            onAddEvent = {},
            onEditEvent = {},
            onDeleteEvent = {},
            onSelectSortOption = {},
            onSelectFilterState = {},
            onClearFilter = {},
            onDismissError = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPopulatedLightPreview() {
    SimpleDayCounterTheme(darkTheme = false) {
        MainScreenContent(
            uiState = MainScreenUiState(
                events = previewEvents(),
                hasAnyEvents = true
            ),
            categories = DEFAULT_CATEGORIES,
            isVibrationEnabled = true,
            snackbarHostState = remember { SnackbarHostState() },
            onOpenSettings = {},
            onAddEvent = {},
            onEditEvent = {},
            onDeleteEvent = {},
            onSelectSortOption = {},
            onSelectFilterState = {},
            onClearFilter = {},
            onDismissError = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPopulatedDarkPreview() {
    SimpleDayCounterTheme(darkTheme = true) {
        MainScreenContent(
            uiState = MainScreenUiState(
                events = previewEvents(),
                hasAnyEvents = true
            ),
            categories = DEFAULT_CATEGORIES,
            isVibrationEnabled = true,
            snackbarHostState = remember { SnackbarHostState() },
            onOpenSettings = {},
            onAddEvent = {},
            onEditEvent = {},
            onDeleteEvent = {},
            onSelectSortOption = {},
            onSelectFilterState = {},
            onClearFilter = {},
            onDismissError = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600)
@Composable
private fun MainScreenTabletLightPreview() {
    SimpleDayCounterTheme(darkTheme = false) {
        MainScreenContent(
            uiState = MainScreenUiState(
                events = previewEvents(),
                hasAnyEvents = true
            ),
            categories = DEFAULT_CATEGORIES,
            isVibrationEnabled = true,
            snackbarHostState = remember { SnackbarHostState() },
            onOpenSettings = {},
            onAddEvent = {},
            onEditEvent = {},
            onDeleteEvent = {},
            onSelectSortOption = {},
            onSelectFilterState = {},
            onClearFilter = {},
            onDismissError = {},
            layoutSpec = AdaptiveLayoutSpec(
                useGrid = true,
                enableSwipeToDelete = false,
                useDialogForEventForm = true
            )
        )
    }
}
