package com.happypuppy.daycounter.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.ui.components.AddCategoryInput
import com.happypuppy.daycounter.ui.components.CategoryColorIndicator
import com.happypuppy.daycounter.ui.components.categoryDisplayName
import com.happypuppy.daycounter.ui.layout.ADAPTIVE_CONTENT_MAX_WIDTH
import com.happypuppy.daycounter.ui.theme.CategoryColors
import com.happypuppy.daycounter.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    onBack: () -> Unit,
    viewModel: CategoryManagementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel, snackbarHostState, resources) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is CategoryUiEvent.ShowUndoSnackbar -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val message = if (event.isAll) {
                        resources.getString(R.string.category_mgmt_all_deleted)
                    } else {
                        resources.getString(R.string.category_mgmt_deleted, event.label)
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = message,
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

    CategoryManagementContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAddCategory = { name -> viewModel.addCustomCategory(name) },
        onDeleteCategory = viewModel::deleteCustomCategory,
        onDeleteAllCategories = viewModel::deleteAllCustomCategories,
        onDismissError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementContent(
    uiState: CategoryManagementUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAddCategory: suspend (String) -> Category?,
    onDeleteCategory: (String) -> Unit,
    onDeleteAllCategories: () -> Unit,
    onDismissError: () -> Unit
) {
    val customCategoryNames = remember(uiState.customCategories) {
        uiState.customCategories.map { it.category.name }.toSet()
    }

    var categoryToDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddCategoryInput by rememberSaveable { mutableStateOf(false) }
    var showDeleteAllConfirmation by rememberSaveable { mutableStateOf(false) }

    val portraitListState = rememberLazyListState()
    val defaultListState = rememberLazyListState()
    val customListState = rememberLazyListState()

    val isDarkTheme = LocalIsDarkTheme.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_mgmt_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            if (isLandscape) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = ADAPTIVE_CONTENT_MAX_WIDTH)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.category_mgmt_default_header),
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                state = defaultListState,
                                modifier = Modifier.weight(1f)
                            ) {
                                items(uiState.defaultCategories, key = { it.id }) { category ->
                                    CategoryItem(
                                        category = category,
                                        eventCount = null,
                                        isDarkTheme = isDarkTheme,
                                        onDelete = null
                                    )
                                }
                            }
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .fillMaxHeight(0.9f)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.category_mgmt_custom_header),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )

                                if (uiState.customCategories.isNotEmpty()) {
                                    IconButton(onClick = { showDeleteAllConfirmation = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.category_mgmt_delete_all_cd),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                IconButton(onClick = { showAddCategoryInput = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.category_mgmt_add_cd)
                                    )
                                }
                            }

                            if (showAddCategoryInput) {
                                AddCategoryInput(
                                    existingCustomCategories = customCategoryNames,
                                    onAddCategory = { name -> onAddCategory(name) },
                                    onCategoryAdded = { showAddCategoryInput = false },
                                    onCancel = { showAddCategoryInput = false },
                                    placeholder = stringResource(R.string.category_name_placeholder)
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (uiState.customCategories.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.category_mgmt_empty_landscape),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = customListState,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(uiState.customCategories, key = { it.category.id }) { entry ->
                                        CategoryItem(
                                            category = entry.category,
                                            eventCount = entry.eventCount,
                                            isDarkTheme = isDarkTheme,
                                            onDelete = { categoryToDeleteId = entry.category.id }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        state = portraitListState,
                        modifier = Modifier
                            .widthIn(max = ADAPTIVE_CONTENT_MAX_WIDTH)
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = innerPadding.calculateBottomPadding() + 16.dp
                        )
                    ) {
                        if (showAddCategoryInput) {
                            item(key = "add_category_input") {
                                AddCategoryInput(
                                    existingCustomCategories = customCategoryNames,
                                    onAddCategory = { name -> onAddCategory(name) },
                                    onCategoryAdded = { showAddCategoryInput = false },
                                    onCancel = { showAddCategoryInput = false },
                                    placeholder = stringResource(R.string.category_name_placeholder)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        item(key = "default_header") {
                            Text(
                                text = stringResource(R.string.category_mgmt_default_header),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(uiState.defaultCategories, key = { "default_${it.id}" }) { category ->
                            CategoryItem(
                                category = category,
                                eventCount = null,
                                isDarkTheme = isDarkTheme,
                                onDelete = null
                            )
                        }

                        item(key = "section_divider") {
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        item(key = "custom_header") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.category_mgmt_custom_header),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )

                                if (uiState.customCategories.isNotEmpty()) {
                                    IconButton(onClick = { showDeleteAllConfirmation = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.category_mgmt_delete_all_cd),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                IconButton(onClick = { showAddCategoryInput = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.category_mgmt_add_cd)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (uiState.customCategories.isEmpty()) {
                            item(key = "custom_empty") {
                                Text(
                                    text = stringResource(R.string.category_mgmt_empty_portrait),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(uiState.customCategories, key = { it.category.id }) { entry ->
                                CategoryItem(
                                    category = entry.category,
                                    eventCount = entry.eventCount,
                                    isDarkTheme = isDarkTheme,
                                    onDelete = { categoryToDeleteId = entry.category.id }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog for a single category
    val pendingDelete = categoryToDeleteId?.let { id ->
        uiState.customCategories.find { it.category.id == id }
    }
    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { categoryToDeleteId = null },
            title = { Text(stringResource(R.string.category_mgmt_delete_title)) },
            text = {
                Text(
                    if (entry.eventCount == 0) {
                        stringResource(R.string.category_mgmt_delete_message, entry.category.name)
                    } else {
                        pluralStringResource(
                            R.plurals.category_mgmt_delete_in_use_message,
                            entry.eventCount,
                            entry.category.name,
                            entry.eventCount
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(entry.category.id)
                        categoryToDeleteId = null
                    }
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDeleteId = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Delete ALL confirmation dialog
    if (showDeleteAllConfirmation) {
        val totalAffected = uiState.totalCustomUsageCount
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmation = false },
            title = { Text(stringResource(R.string.category_mgmt_delete_all_title)) },
            text = {
                Text(
                    if (totalAffected == 0) {
                        stringResource(R.string.category_mgmt_delete_all_message)
                    } else {
                        pluralStringResource(
                            R.plurals.category_mgmt_delete_all_in_use_message,
                            totalAffected,
                            totalAffected
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAllCategories()
                        showDeleteAllConfirmation = false
                    }
                ) {
                    Text(
                        stringResource(R.string.category_mgmt_delete_all_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmation = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    eventCount: Int?,
    isDarkTheme: Boolean,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryColorIndicator(
                color = CategoryColors.getCategoryColor(
                    categoryId = category.id,
                    categoryName = category.name,
                    isDarkTheme = isDarkTheme
                )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = categoryDisplayName(category))
                if (eventCount != null) {
                    val countText = if (eventCount == 0) {
                        stringResource(R.string.category_mgmt_event_count_zero)
                    } else {
                        pluralStringResource(R.plurals.category_mgmt_event_count, eventCount, eventCount)
                    }
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.category_mgmt_delete_cd)
                    )
                }
            }
        }
    }
}
