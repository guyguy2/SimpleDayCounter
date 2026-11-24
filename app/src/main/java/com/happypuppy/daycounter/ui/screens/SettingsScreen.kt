package com.happypuppy.daycounter.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.preferences.AppTheme
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.data.preferences.DateFormat
import com.happypuppy.daycounter.data.preferences.formatDate
import com.happypuppy.daycounter.util.AppVersionInfo
import com.happypuppy.daycounter.util.vibrate
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenCategories: () -> Unit) {
    val dataStoreManager = koinInject<DataStoreManager>()
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
            SettingsSectionHeader(stringResource(R.string.settings_section_appearance))

            ThemeSelector(dataStoreManager)

            DynamicColorSetting(dataStoreManager)

            HorizontalDivider()

            SettingsSectionHeader(stringResource(R.string.settings_section_general))

            VibrationSetting(dataStoreManager)

            HorizontalDivider()

            SettingsSectionHeader(stringResource(R.string.settings_section_dates))

            DateFormatSetting(dataStoreManager)

            HorizontalDivider()

            SettingsSectionHeader(stringResource(R.string.settings_section_categories))

            CategoryManagementSetting(onOpenCategories)

            HorizontalDivider()

            SettingsSectionHeader(stringResource(R.string.settings_section_support))

            RateAppSetting()

            AboutSetting(onClick = { showAboutDialog = true })
        }

        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }
    }
}

/** Section title for a group of settings rows; announced as a heading by screen readers. */
@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
fun CategoryManagementSetting(onOpenCategories: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenCategories() }
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
                text = stringResource(R.string.settings_manage_categories),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.settings_manage_categories_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ThemeSelector(dataStoreManager: DataStoreManager) {
    val scope = rememberCoroutineScope()
    val currentTheme by dataStoreManager.themeFlow.collectAsState(initial = AppTheme.SYSTEM_DEFAULT)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ThemeOption(
            title = stringResource(R.string.settings_theme_system),
            icon = Icons.Outlined.Smartphone,
            selected = currentTheme == AppTheme.SYSTEM_DEFAULT,
            onClick = {
                scope.launch {
                    dataStoreManager.setTheme(AppTheme.SYSTEM_DEFAULT)
                }
            }
        )

        ThemeOption(
            title = stringResource(R.string.settings_theme_light),
            icon = Icons.Outlined.LightMode,
            selected = currentTheme == AppTheme.LIGHT,
            onClick = {
                scope.launch {
                    dataStoreManager.setTheme(AppTheme.LIGHT)
                }
            }
        )

        ThemeOption(
            title = stringResource(R.string.settings_theme_dark),
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
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(24.dp)
        )

        Text(title, modifier = Modifier.weight(1f))
    }
}

@Composable
fun DynamicColorSetting(dataStoreManager: DataStoreManager) {
    val scope = rememberCoroutineScope()
    val isEnabled by dataStoreManager.dynamicColorFlow.collectAsState(initial = false)

    SettingSwitch(
        title = stringResource(R.string.settings_dynamic_color),
        description = stringResource(R.string.settings_dynamic_color_description),
        checked = isEnabled,
        icon = Icons.Outlined.Palette,
        onCheckedChange = { enabled -> scope.launch { dataStoreManager.setDynamicColor(enabled) } }
    )
}

@Composable
fun VibrationSetting(dataStoreManager: DataStoreManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEnabled by dataStoreManager.vibrateEnabledFlow.collectAsState(initial = true)

    SettingSwitch(
        title = stringResource(R.string.settings_vibration),
        description = stringResource(R.string.settings_vibration_description),
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
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch
            )
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
            onCheckedChange = null,
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
                val marketIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = "market://details?id=${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(marketIntent)
                } catch (_: ActivityNotFoundException) {
                    val webIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(webIntent)
                    } catch (_: ActivityNotFoundException) {
                        // No Play Store or browser installed; ignore safely.
                    }
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
            text = stringResource(R.string.settings_rate_app),
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
            text = stringResource(R.string.settings_about),
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
        title = { Text(stringResource(R.string.settings_about_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_about_description))

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Text(
                        text = stringResource(R.string.settings_about_version),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(versionInfo)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(stringResource(R.string.settings_about_copyright))
                Text(stringResource(R.string.settings_about_rights))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_about_ok))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFormatSetting(dataStoreManager: DataStoreManager) {
    val scope = rememberCoroutineScope()
    val currentFormat by dataStoreManager.dateFormatFlow.collectAsState(initial = DateFormat.SYSTEM)
    var showFormatOptions by remember { mutableStateOf(false) }
    val sampleDate = remember { Date() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Today,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_date_format_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val formattedDate = formatDate(sampleDate, currentFormat)

            ExposedDropdownMenuBox(
                expanded = showFormatOptions,
                onExpandedChange = { showFormatOptions = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = stringResource(currentFormat.displayNameRes),
                    onValueChange = { },
                    label = { Text(stringResource(R.string.settings_date_format_label)) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(R.string.settings_date_format_select)
                        )
                    },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    readOnly = true
                )

                ExposedDropdownMenu(
                    expanded = showFormatOptions,
                    onDismissRequest = { showFormatOptions = false }
                ) {
                    DateFormat.entries.forEach { format ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(stringResource(format.displayNameRes))
                                    Text(
                                        text = stringResource(R.string.settings_date_format_example, formatDate(sampleDate, format)),
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
                                { Icon(Icons.Default.Check, contentDescription = stringResource(R.string.common_selected)) }
                            } else null
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.settings_date_format_preview, formattedDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}
