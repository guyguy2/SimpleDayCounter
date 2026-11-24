package com.happypuppy.daycounter.ui.screens

import android.content.res.Configuration
import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.happypuppy.daycounter.BuildConfig
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.ui.components.AddCategoryInput
import com.happypuppy.daycounter.ui.components.CategoryColorIndicator
import com.happypuppy.daycounter.ui.components.categoryDisplayName
import com.happypuppy.daycounter.ui.components.defaultCategories
import com.happypuppy.daycounter.ui.theme.CategoryColors
import com.happypuppy.daycounter.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(onBack: () -> Unit) {
    val dataStoreManager = koinInject<DataStoreManager>()
    val customCategories by dataStoreManager.customCategoriesFlow.collectAsState(initial = emptySet())
    val sortedCustomCategories = remember(customCategories) {
        customCategories.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
    val scope = rememberCoroutineScope()

    // Use rememberSaveable to preserve state during configuration changes
    var categoryToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddCategoryInput by rememberSaveable { mutableStateOf(false) }
    var showDeleteAllConfirmation by rememberSaveable { mutableStateOf(false) }

    // Remember scroll positions
    val portraitListState = rememberLazyListState()
    val defaultListState = rememberLazyListState()
    val customListState = rememberLazyListState()

    val isDarkTheme = LocalIsDarkTheme.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (BuildConfig.DEBUG) Log.d("CategoryManagementScreen", "Orientation: ${if (isLandscape) "Landscape" else "Portrait"}")

    Scaffold(
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
        if (isLandscape) {
            // Landscape layout (two columns)
            Row(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Left column: Default categories
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
                        items(defaultCategories, key = { it }) { category ->
                            CategoryItem(
                                category = category,
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

                // Right column: Custom categories
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

                        if (sortedCustomCategories.isNotEmpty()) {
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
                            dataStoreManager = dataStoreManager,
                            onCategoryAdded = { showAddCategoryInput = false },
                            onCancel = { showAddCategoryInput = false },
                            placeholder = stringResource(R.string.category_name_placeholder)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (sortedCustomCategories.isEmpty()) {
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
                            items(sortedCustomCategories, key = { it }) { category ->
                                CategoryItem(
                                    category = category,
                                    isDarkTheme = isDarkTheme,
                                    onDelete = { categoryToDelete = category }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Portrait layout: single LazyColumn so neither default nor custom categories get clipped.
            LazyColumn(
                state = portraitListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection)
                    ),
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
                            dataStoreManager = dataStoreManager,
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

                items(defaultCategories, key = { "default_$it" }) { category ->
                    CategoryItem(
                        category = category,
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

                        if (sortedCustomCategories.isNotEmpty()) {
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

                if (sortedCustomCategories.isEmpty()) {
                    item(key = "custom_empty") {
                        Text(
                            text = stringResource(R.string.category_mgmt_empty_portrait),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(sortedCustomCategories, key = { it }) { category ->
                        CategoryItem(
                            category = category,
                            isDarkTheme = isDarkTheme,
                            onDelete = { categoryToDelete = category }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog for a single category
    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text(stringResource(R.string.category_mgmt_delete_title)) },
            text = { Text(stringResource(R.string.category_mgmt_delete_message, category)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            dataStoreManager.removeCustomCategory(category)
                        }
                        categoryToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Delete ALL confirmation dialog
    if (showDeleteAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmation = false },
            title = { Text(stringResource(R.string.category_mgmt_delete_all_title)) },
            text = { Text(stringResource(R.string.category_mgmt_delete_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            dataStoreManager.clearCustomCategories()
                        }
                        showDeleteAllConfirmation = false
                    }
                ) {
                    Text(stringResource(R.string.category_mgmt_delete_all_confirm), color = MaterialTheme.colorScheme.error)
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
    category: String,
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
                color = CategoryColors.getCategoryColor(category, isDarkTheme)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = categoryDisplayName(category),
                modifier = Modifier.weight(1f)
            )

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
