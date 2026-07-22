package com.wwwescape.pixelebookreader.data.settings

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** Grows as each owning build phase lands — only the settings every screen needs from day one
 * live here for now. [colorThemeId] is `null` for the app's own hand-authored default palette
 * (see `Color.kt`); a non-null value picks one of [CURATED_COLOR_THEMES]. Both are ignored
 * while [useDynamicColor] is on and the device supports it (API 31+). [pureDark]/[absoluteDark]
 * only affect dark theme. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val colorThemeId: String? = null,
    val themeContrast: ThemeContrast = ThemeContrast.STANDARD,
    val pureDark: Boolean = false,
    val absoluteDark: Boolean = false,
    val showStartScreen: Boolean = true,
    val doublePressBackToExit: Boolean = false,
)
