package com.happypuppy.daycounter.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.happypuppy.daycounter.data.preferences.AppTheme
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.data.preferences.DateFormat
import com.happypuppy.daycounter.data.preferences.formatDate
import com.happypuppy.daycounter.navigation.Screen
import com.happypuppy.daycounter.util.AppVersionInfo
import com.happypuppy.daycounter.util.vibrate
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Date


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val dataStoreManager = koinInject<DataStoreManager>()
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme setting
            ThemeSelector(dataStoreManager)

            HorizontalDivider()

            // Vibration setting
            VibrationSetting(dataStoreManager)

            HorizontalDivider()

            DateFormatSetting(dataStoreManager)

            HorizontalDivider()

            // Category Management
            CategoryManagementSetting(navController)

            HorizontalDivider()

            // Rate app
            RateAppSetting()

            // About
            AboutSetting(onClick = { showAboutDialog = true })
        }

        // About dialog
        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }
    }
}

@Composable
fun CategoryManagementSetting(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Screen.CategoryManagement) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Category,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Manage Categories",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "View and delete your custom categories",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Other existing functions in SettingsScreen.kt remain the same

@Composable
fun ThemeSelector(dataStoreManager: DataStoreManager) {
    val scope = rememberCoroutineScope()
    val currentTheme by dataStoreManager.themeFlow.collectAsState(initial = AppTheme.SYSTEM_DEFAULT)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // System Default option
        ThemeOption(
            title = "System Default",
            icon = Icons.Outlined.Smartphone,
            selected = currentTheme == AppTheme.SYSTEM_DEFAULT,
            onClick = {
                scope.launch {
                    dataStoreManager.setTheme(AppTheme.SYSTEM_DEFAULT)
                }
            }
        )

        // Light Theme option
        ThemeOption(
            title = "Light",
            icon = Icons.Outlined.LightMode,
            selected = currentTheme == AppTheme.LIGHT,
            onClick = {
                scope.launch {
                    dataStoreManager.setTheme(AppTheme.LIGHT)
                }
            }
        )

        // Dark Theme option
        ThemeOption(
            title = "Dark",
            icon = Icons.Outlined.DarkMode,
            selected = currentTheme == AppTheme.DARK,
            onClick = {
                scope.launch {
                    dataStoreManager.setTheme(AppTheme.DARK)
                }
            }
        )
    }
}

@Composable
fun ThemeOption(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Icon(
            icon, contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(24.dp)
        )

        Text(title, modifier = Modifier.weight(1f))
    }
}

@Composable
fun VibrationSetting(dataStoreManager: DataStoreManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEnabled by dataStoreManager.vibrateEnabledFlow.collectAsState(initial = true)

    SettingSwitch(
        title = "Vibration",
        description = "Vibrate when deleting an event",
        checked = isEnabled,
        icon = Icons.Outlined.Vibration,
        onCheckedChange = { enabled ->
            scope.launch {
                dataStoreManager.setVibrateEnabled(enabled)

                if (enabled) {
                    vibrate(context)
                }
            }
        }
    )
}

@Composable
fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                null
            }
        )
    }
}

@Composable
fun RateAppSetting() {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Open Play Store
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = "market://details?id=${context.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // If Play Store app is not installed, open in browser
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data =
                            "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Rate App",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun AboutSetting(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val versionInfo = AppVersionInfo.getFullVersionInfo(context)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About Simple Day Counter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Count the moments that matter - track your important dates with elegance and simplicity.")

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Text(
                        text = "Version:",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(versionInfo)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("©2025 Simple Day Counter")
                Text("All rights reserved")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun DateFormatSetting(dataStoreManager: DataStoreManager) {
    val scope = rememberCoroutineScope()
    val currentFormat by dataStoreManager.dateFormatFlow.collectAsState(initial = DateFormat.DEFAULT)
    var showFormatOptions by remember { mutableStateOf(false) }
    val sampleDate = remember { Date() } // Define the sample date at function level

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Today, // Using DateRange which is already imported
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Date Display Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Format preview
            val formattedDate = formatDate(sampleDate, currentFormat)

            // DropdownMenu must be a child of this Box so it anchors under the field
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currentFormat.displayName,
                    onValueChange = { },
                    label = { Text("Format") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Format"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = { showFormatOptions = true })
                )

                DropdownMenu(
                    expanded = showFormatOptions,
                    onDismissRequest = { showFormatOptions = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    DateFormat.entries.forEach { format ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(format.displayName)
                                    Text(
                                        text = "Example: ${formatDate(sampleDate, format)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                scope.launch {
                                    dataStoreManager.setDateFormat(format)
                                }
                                showFormatOptions = false
                            },
                            trailingIcon = if (currentFormat == format) {
                                { Icon(Icons.Default.Check, contentDescription = "Selected") }
                            } else null
                        )
                    }
                }
            }

            Text(
                text = "Preview: $formattedDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

