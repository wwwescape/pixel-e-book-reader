package com.wwwescape.pixelebookreader.data.settings

import com.wwwescape.pixelebookreader.R

enum class ThemeContrast { STANDARD, MEDIUM, HIGH }

/** [seedHue] is a 0–359 HSV hue driving [com.wwwescape.pixelebookreader.ui.theme.generateColorScheme] —
 * see that function's doc for why this app uses one shared, carefully-tuned generator across all
 * 16+ themes rather than hand-authoring ~30 color roles × light/dark × 16 themes independently. */
data class ColorTheme(val id: String, val nameRes: Int, val seedHue: Float)

/** The app's own default palette (Color.kt's hand-authored oxblood scheme) has no entry here —
 * it's `colorThemeId = null`, selected whenever none of these curated themes is chosen. */
val CURATED_COLOR_THEMES = listOf(
    ColorTheme("ocean", R.string.theme_ocean, 205f),
    ColorTheme("forest", R.string.theme_forest, 140f),
    ColorTheme("sunset", R.string.theme_sunset, 25f),
    ColorTheme("grape", R.string.theme_grape, 280f),
    ColorTheme("rose", R.string.theme_rose, 340f),
    ColorTheme("slate", R.string.theme_slate, 220f),
    ColorTheme("gold", R.string.theme_gold, 45f),
    ColorTheme("teal", R.string.theme_teal, 175f),
    ColorTheme("plum", R.string.theme_plum, 300f),
    ColorTheme("moss", R.string.theme_moss, 95f),
    ColorTheme("coral", R.string.theme_coral, 12f),
    ColorTheme("indigo", R.string.theme_indigo, 245f),
    ColorTheme("mustard", R.string.theme_mustard, 55f),
    ColorTheme("crimson", R.string.theme_crimson, 355f),
    ColorTheme("mint", R.string.theme_mint, 160f),
    ColorTheme("periwinkle", R.string.theme_periwinkle, 230f),
)
