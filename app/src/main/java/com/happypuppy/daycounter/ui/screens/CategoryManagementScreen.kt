package com.happypuppy.daycounter.ui.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.happypuppy.daycounter.BuildConfig
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.ui.components.AddCategoryInput
import com.happypuppy.daycounter.ui.components.CategoryColorIndicator
import com.happypuppy.daycounter.ui.components.defaultCategories
import com.happypuppy.daycounter.ui.theme.CategoryColors
import com.happypuppy.daycounter.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(navController: NavHostController) {
    val dataStoreManager = koinInject<DataStoreManager>()
    val customCategories by dataStoreManager.customCategoriesFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    // Use rememberSaveable to preserve state during configuration changes
    var categoryToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddCategoryInput by rememberSaveable { mutableStateOf(false) }
    var showDeleteAllConfirmation by rememberSaveable { mutableStateOf(false) }

    // Remember scroll positions for both lists
    val defaultListState = rememberLazyListState()
    val customListState = rememberLazyListState()

    // Theme is resolved once at SimpleDayCounterTheme (IMPROVEMENTS.md P1-5).
    val isDarkTheme = LocalIsDarkTheme.current

    // Get the current orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (BuildConfig.DEBUG) Log.d("CategoryManagementScreen", "Orientation: ${if (isLandscape) "Landscape" else "Portrait"}")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
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
                        text = "Default Categories",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Default categories list
                    LazyColumn(state = defaultListState) {
                        items(defaultCategories) { category ->
                            CategoryItem(
                                category = category,
                                isDarkTheme = isDarkTheme,
                                onDelete = null // No delete option for default categories
                            )
                        }
                    }
                }

                // Add a vertical divider between columns
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
                    // Custom categories header with Add and Delete All buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom Categories",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )

                        // Delete All button (in red)
                        if (customCategories.isNotEmpty()) {
                            IconButton(onClick = { showDeleteAllConfirmation = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete All Categories",
                                    tint = MaterialTheme.colorScheme.error // Red color
                                )
                            }
                        }

                        // Add button
                        IconButton(onClick = { showAddCategoryInput = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Custom Category"
                            )
                        }
                    }

                    // Add category input field when shown
                    if (showAddCategoryInput) {
                        AddCategoryInput(
                            dataStoreManager = dataStoreManager,
                            onCategoryAdded = { newCategory ->
                                // Category was successfully added
                                showAddCategoryInput = false
                            },
                            onCancel = {
                                showAddCategoryInput = false
                            },
                            placeholder = "Enter a new category name"
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Custom categories list or empty state message
                    if (customCategories.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No custom categories yet.\nAdd one using the + button.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(state = customListState) {
                            items(customCategories.toList()) { category ->
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
            // Portrait layout (original single column)
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Show the add category input if enabled
                if (showAddCategoryInput) {
                    AddCategoryInput(
                        dataStoreManager = dataStoreManager,
                        onCategoryAdded = { newCategory ->
                            // Category was successfully added
                            showAddCategoryInput = false
                        },
                        onCancel = {
                            showAddCategoryInput = false
                        },
                        placeholder = "Enter a new category name"
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "Default Categories",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Display default categories with colors
                LazyColumn(state = defaultListState) {
                    items(defaultCategories) { category ->
                        CategoryItem(
                            category = category,
                            isDarkTheme = isDarkTheme,
                            onDelete = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Custom categories section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom Categories",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )

                    // Delete All button (in red)
                    if (customCategories.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete All Categories",
                                tint = MaterialTheme.colorScheme.error // Red color
                            )
                        }
                    }

                    // Add button next to the section title
                    IconButton(onClick = { showAddCategoryInput = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Custom Category"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (customCategories.isEmpty()) {
                    Text(
                        text = "No custom categories yet. Add one using the + button.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(state = customListState) {
                        items(customCategories.toList()) { category ->
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
    }

    // Delete confirmation dialog for a single category
    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete \"$category\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            dataStoreManager.removeCustomCategory(category)
                        }
                        categoryToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete ALL confirmation dialog
    if (showDeleteAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmation = false },
            title = { Text("Delete All Custom Categories") },
            text = { Text("Are you sure you want to delete all custom categories? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            // Remove all custom categories
                            customCategories.forEach { category ->
                                dataStoreManager.removeCustomCategory(category)
                            }
                        }
                        showDeleteAllConfirmation = false
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error) // Red text for emphasis
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Reusable category item component
 *
 * @param category The category name
 * @param isDarkTheme Whether dark theme is active
 * @param onDelete Optional callback when delete is clicked (null for default categories)
 */
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
            // Display color indicator
            CategoryColorIndicator(
                color = CategoryColors.getCategoryColor(category, isDarkTheme)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = category,
                modifier = Modifier.weight(1f)
            )

            // Show delete button only for custom categories
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Category"
                    )
                }
            }
        }
    }
}