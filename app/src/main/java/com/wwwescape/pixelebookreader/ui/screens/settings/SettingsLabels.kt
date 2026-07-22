package com.wwwescape.pixelebookreader.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wwwescape.pixelebookreader.R
import com.wwwescape.pixelebookreader.data.backup.ImportMode
import com.wwwescape.pixelebookreader.data.settings.ColorTheme
import com.wwwescape.pixelebookreader.data.settings.ThemeContrast
import com.wwwescape.pixelebookreader.data.settings.ThemeMode
import java.util.Locale

/** `null` means "follow the system language" ([androidx.appcompat.app.AppCompatDelegate]'s empty
 * locale list) — see the Settings screen's Language row. Scaffolded for the languages sibling
 * apps already support; English is listed explicitly too since the system
 * default might not be English even though that's this app's base `values/strings.xml`. */
val SUPPORTED_APP_LANGUAGES = listOf(null, "en", "es", "fr", "hi", "pt")

@Composable
fun appLanguageLabel(languageTag: String?): String =
    if (languageTag == null) {
        stringResource(R.string.language_system_default)
    } else {
        val locale = Locale.forLanguageTag(languageTag)
        locale.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }
    }

@Composable
fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
        ThemeMode.SYSTEM -> R.string.theme_mode_system
    },
)

@Composable
fun ThemeContrast.label(): String = stringResource(
    when (this) {
        ThemeContrast.STANDARD -> R.string.contrast_standard
        ThemeContrast.MEDIUM -> R.string.contrast_medium
        ThemeContrast.HIGH -> R.string.contrast_high
    },
)

@Composable
fun ColorTheme.label(): String = stringResource(nameRes)

@Composable
fun ImportMode.label(): String = stringResource(
    when (this) {
        ImportMode.REPLACE -> R.string.import_mode_replace
        ImportMode.MERGE -> R.string.import_mode_merge
    },
)

@Composable
fun ImportMode.description(): String = stringResource(
    when (this) {
        ImportMode.REPLACE -> R.string.import_mode_replace_description
        ImportMode.MERGE -> R.string.import_mode_merge_description
    },
)
