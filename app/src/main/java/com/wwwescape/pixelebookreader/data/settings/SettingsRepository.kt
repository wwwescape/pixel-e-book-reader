package com.wwwescape.pixelebookreader.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Mirrors [ThemeMode] outside DataStore, which is Flow-only/async. [MainActivity] reads this
 * synchronously in `attachBaseContext`, before any Compose or DataStore code can run, so the
 * window's initial (pre-Compose) theme resolution honors the user's choice instead of always
 * following the raw system day/night setting. */
const val THEME_PREFS_NAME = "theme_prefs_sync"
const val THEME_MODE_PREF_KEY = "theme_mode"

object SettingsRepository {

    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    private val COLOR_THEME_ID_KEY = stringPreferencesKey("color_theme_id")
    private val THEME_CONTRAST_KEY = stringPreferencesKey("theme_contrast")
    private val PURE_DARK_KEY = booleanPreferencesKey("pure_dark")
    private val ABSOLUTE_DARK_KEY = booleanPreferencesKey("absolute_dark")
    private val SHOW_START_SCREEN_KEY = booleanPreferencesKey("show_start_screen")
    private val DOUBLE_PRESS_BACK_TO_EXIT_KEY = booleanPreferencesKey("double_press_back_to_exit")

    fun settingsFlow(context: Context): Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            themeMode = prefs[THEME_MODE_KEY].toEnumOrDefault(ThemeMode.SYSTEM),
            useDynamicColor = prefs[DYNAMIC_COLOR_KEY] ?: defaults.useDynamicColor,
            colorThemeId = prefs[COLOR_THEME_ID_KEY],
            themeContrast = prefs[THEME_CONTRAST_KEY].toEnumOrDefault(ThemeContrast.STANDARD),
            pureDark = prefs[PURE_DARK_KEY] ?: defaults.pureDark,
            absoluteDark = prefs[ABSOLUTE_DARK_KEY] ?: defaults.absoluteDark,
            showStartScreen = prefs[SHOW_START_SCREEN_KEY] ?: defaults.showStartScreen,
            doublePressBackToExit = prefs[DOUBLE_PRESS_BACK_TO_EXIT_KEY] ?: defaults.doublePressBackToExit,
        )
    }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.settingsDataStore.edit { it[THEME_MODE_KEY] = mode.name }
        context.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(THEME_MODE_PREF_KEY, mode.name).apply()
    }

    suspend fun setDynamicColor(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }

    suspend fun setColorThemeId(context: Context, id: String?) {
        context.settingsDataStore.edit { prefs ->
            if (id == null) prefs.remove(COLOR_THEME_ID_KEY) else prefs[COLOR_THEME_ID_KEY] = id
        }
    }

    suspend fun setThemeContrast(context: Context, contrast: ThemeContrast) {
        context.settingsDataStore.edit { it[THEME_CONTRAST_KEY] = contrast.name }
    }

    suspend fun setPureDark(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[PURE_DARK_KEY] = enabled }
    }

    suspend fun setAbsoluteDark(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[ABSOLUTE_DARK_KEY] = enabled }
    }

    suspend fun setShowStartScreen(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[SHOW_START_SCREEN_KEY] = enabled }
    }

    suspend fun setDoublePressBackToExit(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[DOUBLE_PRESS_BACK_TO_EXIT_KEY] = enabled }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
        this?.let { name -> runCatching { enumValueOf<T>(name) }.getOrNull() } ?: default
}
