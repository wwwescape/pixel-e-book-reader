package com.wwwescape.pixelebookreader.ui.screens.settings

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.pixelebookreader.R
import com.wwwescape.pixelebookreader.data.backup.ImportMode
import com.wwwescape.pixelebookreader.data.backup.ImportSummary
import com.wwwescape.pixelebookreader.data.settings.AppSettings
import com.wwwescape.pixelebookreader.data.settings.CURATED_COLOR_THEMES
import com.wwwescape.pixelebookreader.data.settings.ThemeContrast
import com.wwwescape.pixelebookreader.data.settings.ThemeMode
import com.wwwescape.pixelebookreader.util.openUrl
import java.util.Locale

private val SettingsRowHeight = 72.dp

private const val PRIVACY_POLICY_URL = "https://www.ericppereira.co.in/apps/pixel-e-book-reader/privacy-policy.html"

@Composable
fun SettingsScreen(
    onNavigateToLicenses: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsScreenContent(
        settings = settings,
        onThemeModeSelected = viewModel::setThemeMode,
        onDynamicColorChanged = viewModel::setDynamicColor,
        onColorThemeSelected = viewModel::setColorThemeId,
        onThemeContrastSelected = viewModel::setThemeContrast,
        onPureDarkChanged = viewModel::setPureDark,
        onAbsoluteDarkChanged = viewModel::setAbsoluteDark,
        onShowStartScreenChanged = viewModel::setShowStartScreen,
        onDoublePressBackToExitChanged = viewModel::setDoublePressBackToExit,
        onExportBackup = viewModel::exportBackup,
        onImportBackup = viewModel::importBackup,
        onNavigateToLicenses = onNavigateToLicenses,
        onNavigateToStatistics = onNavigateToStatistics,
        modifier = modifier,
    )
}

@Composable
private fun SettingsScreenContent(
    settings: AppSettings,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onColorThemeSelected: (String?) -> Unit,
    onThemeContrastSelected: (ThemeContrast) -> Unit,
    onPureDarkChanged: (Boolean) -> Unit,
    onAbsoluteDarkChanged: (Boolean) -> Unit,
    onShowStartScreenChanged: (Boolean) -> Unit,
    onDoublePressBackToExitChanged: (Boolean) -> Unit,
    onExportBackup: (Uri, (Result<Unit>) -> Unit) -> Unit,
    onImportBackup: (Uri, ImportMode, (Result<ImportSummary>) -> Unit) -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "—"
    }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showColorThemeDialog by remember { mutableStateOf(false) }
    var showContrastDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val currentLanguageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { null }
    val exportSuccessMessage = stringResource(R.string.backup_export_success)
    val exportFailureMessage = stringResource(R.string.backup_export_failure)
    // Resolved as a raw %1$d/%2$d template here (composition time) rather than via
    // stringResource(id, args) at the point of use, since the args (import counts) aren't known
    // until the import finishes asynchronously inside a non-composable callback — see
    // LocalContextGetResourceValueCall's rationale against calling context.getString() there.
    val importSuccessTemplate = stringResource(R.string.backup_import_success)
    val importFailureMessage = stringResource(R.string.backup_import_failure)
    val isDarkTheme = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        onExportBackup(uri) { result ->
            Toast.makeText(context, if (result.isSuccess) exportSuccessMessage else exportFailureMessage, Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingImportUri = it }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionLabel(stringResource(R.string.section_appearance))
        SettingsGroupCard {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ToggleRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.setting_dynamic_color),
                    subtitle = stringResource(R.string.setting_dynamic_color_subtitle),
                    checked = settings.useDynamicColor,
                    onCheckedChange = onDynamicColorChanged,
                )
                RowDivider()
            }
            NavigationRow(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.setting_color_theme),
                subtitle = settings.colorThemeId?.let { id ->
                    CURATED_COLOR_THEMES.find { it.id == id }?.label()
                } ?: stringResource(R.string.theme_app_default),
                onClick = { showColorThemeDialog = true },
            )
            RowDivider()
            NavigationRow(
                icon = Icons.Rounded.DarkMode,
                title = stringResource(R.string.setting_theme_mode),
                subtitle = settings.themeMode.label(),
                onClick = { showThemeModeDialog = true },
            )
            // Contrast/pure dark/absolute dark all only affect (or only make sense for) dark
            // theme — hidden entirely rather than shown-but-inert when the effective theme
            // (accounting for "System default" actually resolving to light) is light.
            if (isDarkTheme) {
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.Contrast,
                    title = stringResource(R.string.setting_theme_contrast),
                    subtitle = settings.themeContrast.label(),
                    onClick = { showContrastDialog = true },
                )
                RowDivider()
                ToggleRow(
                    icon = Icons.Rounded.Brightness6,
                    title = stringResource(R.string.setting_pure_dark),
                    subtitle = stringResource(R.string.setting_pure_dark_subtitle),
                    checked = settings.pureDark,
                    onCheckedChange = onPureDarkChanged,
                )
                RowDivider()
                ToggleRow(
                    icon = Icons.Rounded.NightsStay,
                    title = stringResource(R.string.setting_absolute_dark),
                    subtitle = stringResource(R.string.setting_absolute_dark_subtitle),
                    checked = settings.absoluteDark,
                    onCheckedChange = onAbsoluteDarkChanged,
                )
            }
        }

        SectionLabel(stringResource(R.string.section_general))
        SettingsGroupCard {
            ToggleRow(
                icon = Icons.Rounded.RocketLaunch,
                title = stringResource(R.string.setting_show_start_screen),
                subtitle = null,
                checked = settings.showStartScreen,
                onCheckedChange = onShowStartScreenChanged,
            )
            RowDivider()
            ToggleRow(
                icon = Icons.AutoMirrored.Rounded.ExitToApp,
                title = stringResource(R.string.setting_double_press_back),
                subtitle = null,
                checked = settings.doublePressBackToExit,
                onCheckedChange = onDoublePressBackToExitChanged,
            )
            RowDivider()
            NavigationRow(
                icon = Icons.Rounded.Language,
                title = stringResource(R.string.setting_language),
                subtitle = appLanguageLabel(currentLanguageTag),
                onClick = { showLanguageDialog = true },
            )
        }

        SectionLabel(stringResource(R.string.section_backup))
        SettingsGroupCard {
            NavigationRow(
                icon = Icons.Rounded.CloudUpload,
                title = stringResource(R.string.action_export_backup),
                subtitle = stringResource(R.string.backup_export_subtitle),
                onClick = { exportLauncher.launch("pixel-e-book-reader-backup.json") },
            )
            RowDivider()
            NavigationRow(
                icon = Icons.Rounded.CloudDownload,
                title = stringResource(R.string.action_import_backup),
                subtitle = stringResource(R.string.backup_import_subtitle),
                onClick = { importLauncher.launch(arrayOf("*/*")) },
            )
            Text(
                text = stringResource(R.string.backup_files_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        SectionLabel(stringResource(R.string.section_statistics))
        SettingsGroupCard {
            NavigationRow(
                icon = Icons.Rounded.QueryStats,
                title = stringResource(R.string.title_statistics),
                subtitle = stringResource(R.string.settings_row_statistics_subtitle),
                onClick = onNavigateToStatistics,
            )
        }

        SectionLabel(stringResource(R.string.section_about))
        SettingsGroupCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo_mark),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp)),
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = stringResource(R.string.about_version, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RowDivider()
            Text(
                text = stringResource(R.string.about_app_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            RowDivider()
            NavigationRow(
                icon = Icons.Rounded.Shield,
                title = stringResource(R.string.section_privacy_policy),
                subtitle = null,
                trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                onClick = { openUrl(context, PRIVACY_POLICY_URL) },
            )
            RowDivider()
            NavigationRow(
                icon = Icons.Rounded.Code,
                title = stringResource(R.string.section_open_source_licenses),
                subtitle = stringResource(R.string.settings_row_licenses_subtitle),
                onClick = onNavigateToLicenses,
            )
        }
    }

    if (showThemeModeDialog) {
        SettingsPickerDialog(
            title = stringResource(R.string.setting_theme_mode),
            options = ThemeMode.entries,
            selected = settings.themeMode,
            label = { it.label() },
            onSelect = onThemeModeSelected,
            onDismiss = { showThemeModeDialog = false },
        )
    }
    if (showColorThemeDialog) {
        ThemePickerDialog(
            selected = settings.colorThemeId,
            onSelect = onColorThemeSelected,
            onDismiss = { showColorThemeDialog = false },
        )
    }
    if (showContrastDialog) {
        SettingsPickerDialog(
            title = stringResource(R.string.setting_theme_contrast),
            options = ThemeContrast.entries,
            selected = settings.themeContrast,
            label = { it.label() },
            onSelect = onThemeContrastSelected,
            onDismiss = { showContrastDialog = false },
        )
    }
    if (showLanguageDialog) {
        SettingsPickerDialog(
            title = stringResource(R.string.setting_language),
            options = SUPPORTED_APP_LANGUAGES,
            selected = currentLanguageTag,
            label = { appLanguageLabel(it) },
            onSelect = { tag ->
                AppCompatDelegate.setApplicationLocales(
                    if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
                )
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
    pendingImportUri?.let { uri ->
        ImportModeDialog(
            onSelect = { mode ->
                pendingImportUri = null
                onImportBackup(uri, mode) { result ->
                    val message = result.fold(
                        onSuccess = { String.format(Locale.getDefault(), importSuccessTemplate, it.booksImported, it.categoriesImported) },
                        onFailure = { importFailureMessage },
                    )
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { pendingImportUri = null },
        )
    }
}

@Composable
private fun ImportModeDialog(onSelect: (ImportMode) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(ImportMode.REPLACE) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_import_backup)) },
        text = {
            Column {
                ImportMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected == mode, onClick = { selected = mode })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == mode, onClick = null)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(mode.label(), style = MaterialTheme.typography.bodyLarge)
                            Text(mode.description(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSelect(selected) }) { Text(stringResource(R.string.action_import_backup)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun RowIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SettingsRowHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            RowIcon(icon)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailingIcon: ImageVector = Icons.Rounded.ChevronRight,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(SettingsRowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
