package com.wwwescape.pixelebookreader.data.reader

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.readerSettingsDataStore by preferencesDataStore(name = "reader_settings")

/** DataStore-backed, matching every other scalar-settings repository in the app; reading color
 * presets themselves are Room data (see `ColorPresetRepository`) — this only tracks which one
 * is selected. One [settingsFlow] + one setter per field, same shape as `SettingsRepository`
 * and `LibraryViewRepository`, just with many more fields given the size of this settings
 * surface. */
object ReaderSettingsRepository {

    private val FONT_FAMILY = stringPreferencesKey("font_family")
    private val FONT_WEIGHT = stringPreferencesKey("font_weight")
    private val ITALIC = booleanPreferencesKey("italic")
    private val FONT_SIZE = floatPreferencesKey("font_size_sp")
    private val LINE_HEIGHT = floatPreferencesKey("line_height_multiplier")
    private val PARAGRAPH_HEIGHT = floatPreferencesKey("paragraph_height_dp")
    private val PARAGRAPH_INDENTATION = floatPreferencesKey("paragraph_indentation_dp")
    private val SIDE_PADDING = floatPreferencesKey("side_padding_dp")
    private val VERTICAL_PADDING = floatPreferencesKey("vertical_padding_dp")
    private val LETTER_SPACING = floatPreferencesKey("letter_spacing_sp")
    private val TEXT_ALIGN = stringPreferencesKey("text_align")
    private val CHAPTER_TITLE_ALIGN = stringPreferencesKey("chapter_title_align")

    private val SELECTED_COLOR_PRESET_ID = intPreferencesKey("selected_color_preset_id")
    private val FAST_COLOR_PRESET_SWITCH = booleanPreferencesKey("fast_color_preset_switch")

    private val LIMITER_ENABLED = booleanPreferencesKey("limiter_enabled")
    private val LIMITER_HEIGHT = floatPreferencesKey("limiter_height_dp")
    private val LIMITER_VERTICAL_OFFSET = floatPreferencesKey("limiter_vertical_offset_dp")
    private val LIMITER_RULER_ENABLED = booleanPreferencesKey("limiter_ruler_enabled")
    private val LIMITER_RULER_THICKNESS = floatPreferencesKey("limiter_ruler_thickness_dp")
    private val LIMITER_DIMMING = floatPreferencesKey("limiter_dimming")

    private val EXPANDER_ENABLED = booleanPreferencesKey("perception_expander_enabled")
    private val EXPANDER_PADDING = floatPreferencesKey("perception_expander_padding_dp")
    private val EXPANDER_THICKNESS = floatPreferencesKey("perception_expander_thickness_dp")

    private val HIGHLIGHT_ENABLED = booleanPreferencesKey("highlighted_reading_enabled")
    private val HIGHLIGHT_THICKNESS = floatPreferencesKey("highlighted_reading_thickness_dp")

    private val SWIPE_MODE = stringPreferencesKey("swipe_gesture_mode")
    private val SWIPE_SCROLL_AMOUNT = floatPreferencesKey("swipe_scroll_amount")
    private val SWIPE_SENSITIVITY = floatPreferencesKey("swipe_sensitivity")
    private val SWIPE_ALPHA_ANIMATION = booleanPreferencesKey("swipe_alpha_animation")
    private val SWIPE_PULL_ANIMATION = booleanPreferencesKey("swipe_pull_animation")
    private val SWIPE_DISABLE_NORMAL_SCROLL = booleanPreferencesKey("swipe_disable_normal_scroll")

    private val SHOW_IMAGES = booleanPreferencesKey("show_images")
    private val SHOW_IMAGE_CAPTIONS = booleanPreferencesKey("show_image_captions")
    private val IMAGE_CORNER_ROUNDNESS = floatPreferencesKey("image_corner_roundness_dp")
    private val IMAGE_ALIGNMENT = stringPreferencesKey("image_alignment")
    private val IMAGE_WIDTH_PERCENT = floatPreferencesKey("image_width_percent")
    private val IMAGE_COLOR_EFFECT = stringPreferencesKey("image_color_effect")

    private val SHOW_PROGRESS_BAR = booleanPreferencesKey("show_progress_bar")
    private val PROGRESS_BAR_ALIGNMENT = stringPreferencesKey("progress_bar_alignment")
    private val PROGRESS_BAR_PADDING = floatPreferencesKey("progress_bar_padding_dp")
    private val PROGRESS_BAR_FONT_SIZE = floatPreferencesKey("progress_bar_font_size_sp")
    private val PROGRESS_DISPLAY_MODE = stringPreferencesKey("progress_display_mode")

    private val DOUBLE_TAP_TRANSLATE_ENABLED = booleanPreferencesKey("double_tap_translate_enabled")
    private val ORIENTATION_LOCK = stringPreferencesKey("orientation_lock")
    private val CUSTOM_BRIGHTNESS_ENABLED = booleanPreferencesKey("custom_brightness_enabled")
    private val CUSTOM_BRIGHTNESS = floatPreferencesKey("custom_brightness")

    private val PAGE_TURN_EFFECT_ENABLED = booleanPreferencesKey("page_turn_effect_enabled")

    fun settingsFlow(context: Context): Flow<ReaderSettings> = context.readerSettingsDataStore.data.map { prefs ->
        val defaults = ReaderSettings()
        ReaderSettings(
            fontFamily = prefs[FONT_FAMILY].toEnumOrDefault(defaults.fontFamily),
            fontWeight = prefs[FONT_WEIGHT].toEnumOrDefault(defaults.fontWeight),
            italic = prefs[ITALIC] ?: defaults.italic,
            fontSizeSp = prefs[FONT_SIZE] ?: defaults.fontSizeSp,
            lineHeightMultiplier = prefs[LINE_HEIGHT] ?: defaults.lineHeightMultiplier,
            paragraphHeightDp = prefs[PARAGRAPH_HEIGHT] ?: defaults.paragraphHeightDp,
            paragraphIndentationDp = prefs[PARAGRAPH_INDENTATION] ?: defaults.paragraphIndentationDp,
            sidePaddingDp = prefs[SIDE_PADDING] ?: defaults.sidePaddingDp,
            verticalPaddingDp = prefs[VERTICAL_PADDING] ?: defaults.verticalPaddingDp,
            letterSpacingSp = prefs[LETTER_SPACING] ?: defaults.letterSpacingSp,
            textAlign = prefs[TEXT_ALIGN].toEnumOrDefault(defaults.textAlign),
            chapterTitleAlign = prefs[CHAPTER_TITLE_ALIGN].toEnumOrDefault(defaults.chapterTitleAlign),
            selectedColorPresetId = prefs[SELECTED_COLOR_PRESET_ID],
            fastColorPresetSwitch = prefs[FAST_COLOR_PRESET_SWITCH] ?: defaults.fastColorPresetSwitch,
            limiterEnabled = prefs[LIMITER_ENABLED] ?: defaults.limiterEnabled,
            limiterHeightDp = prefs[LIMITER_HEIGHT] ?: defaults.limiterHeightDp,
            limiterVerticalOffsetDp = prefs[LIMITER_VERTICAL_OFFSET] ?: defaults.limiterVerticalOffsetDp,
            limiterRulerEnabled = prefs[LIMITER_RULER_ENABLED] ?: defaults.limiterRulerEnabled,
            limiterRulerThicknessDp = prefs[LIMITER_RULER_THICKNESS] ?: defaults.limiterRulerThicknessDp,
            limiterDimming = prefs[LIMITER_DIMMING] ?: defaults.limiterDimming,
            perceptionExpanderEnabled = prefs[EXPANDER_ENABLED] ?: defaults.perceptionExpanderEnabled,
            perceptionExpanderPaddingDp = prefs[EXPANDER_PADDING] ?: defaults.perceptionExpanderPaddingDp,
            perceptionExpanderThicknessDp = prefs[EXPANDER_THICKNESS] ?: defaults.perceptionExpanderThicknessDp,
            highlightedReadingEnabled = prefs[HIGHLIGHT_ENABLED] ?: defaults.highlightedReadingEnabled,
            highlightedReadingThicknessDp = prefs[HIGHLIGHT_THICKNESS] ?: defaults.highlightedReadingThicknessDp,
            swipeGestureMode = prefs[SWIPE_MODE].toEnumOrDefault(defaults.swipeGestureMode),
            swipeScrollAmount = prefs[SWIPE_SCROLL_AMOUNT] ?: defaults.swipeScrollAmount,
            swipeSensitivity = prefs[SWIPE_SENSITIVITY] ?: defaults.swipeSensitivity,
            swipeAlphaAnimation = prefs[SWIPE_ALPHA_ANIMATION] ?: defaults.swipeAlphaAnimation,
            swipePullAnimation = prefs[SWIPE_PULL_ANIMATION] ?: defaults.swipePullAnimation,
            swipeDisableNormalScroll = prefs[SWIPE_DISABLE_NORMAL_SCROLL] ?: defaults.swipeDisableNormalScroll,
            showImages = prefs[SHOW_IMAGES] ?: defaults.showImages,
            showImageCaptions = prefs[SHOW_IMAGE_CAPTIONS] ?: defaults.showImageCaptions,
            imageCornerRoundnessDp = prefs[IMAGE_CORNER_ROUNDNESS] ?: defaults.imageCornerRoundnessDp,
            imageAlignment = prefs[IMAGE_ALIGNMENT].toEnumOrDefault(defaults.imageAlignment),
            imageWidthPercent = prefs[IMAGE_WIDTH_PERCENT] ?: defaults.imageWidthPercent,
            imageColorEffect = prefs[IMAGE_COLOR_EFFECT].toEnumOrDefault(defaults.imageColorEffect),
            showProgressBar = prefs[SHOW_PROGRESS_BAR] ?: defaults.showProgressBar,
            progressBarAlignment = prefs[PROGRESS_BAR_ALIGNMENT].toEnumOrDefault(defaults.progressBarAlignment),
            progressBarPaddingDp = prefs[PROGRESS_BAR_PADDING] ?: defaults.progressBarPaddingDp,
            progressBarFontSizeSp = prefs[PROGRESS_BAR_FONT_SIZE] ?: defaults.progressBarFontSizeSp,
            progressDisplayMode = prefs[PROGRESS_DISPLAY_MODE].toEnumOrDefault(defaults.progressDisplayMode),
            doubleTapTranslateEnabled = prefs[DOUBLE_TAP_TRANSLATE_ENABLED] ?: defaults.doubleTapTranslateEnabled,
            orientationLock = prefs[ORIENTATION_LOCK].toEnumOrDefault(defaults.orientationLock),
            customBrightnessEnabled = prefs[CUSTOM_BRIGHTNESS_ENABLED] ?: defaults.customBrightnessEnabled,
            customBrightness = prefs[CUSTOM_BRIGHTNESS] ?: defaults.customBrightness,
            pageTurnEffectEnabled = prefs[PAGE_TURN_EFFECT_ENABLED] ?: defaults.pageTurnEffectEnabled,
        )
    }

    /** Read-modify-write, e.g. `update(context) { it.copy(fontSizeSp = 18f) }` — far more
     * ergonomic than a setter per field given how many fields this settings surface has. */
    suspend fun update(context: Context, transform: (ReaderSettings) -> ReaderSettings) {
        val current = settingsFlow(context).first()
        writeAll(context, transform(current))
    }

    private suspend fun writeAll(context: Context, s: ReaderSettings) {
        context.readerSettingsDataStore.edit { prefs ->
            prefs[FONT_FAMILY] = s.fontFamily.name
            prefs[FONT_WEIGHT] = s.fontWeight.name
            prefs[ITALIC] = s.italic
            prefs[FONT_SIZE] = s.fontSizeSp
            prefs[LINE_HEIGHT] = s.lineHeightMultiplier
            prefs[PARAGRAPH_HEIGHT] = s.paragraphHeightDp
            prefs[PARAGRAPH_INDENTATION] = s.paragraphIndentationDp
            prefs[SIDE_PADDING] = s.sidePaddingDp
            prefs[VERTICAL_PADDING] = s.verticalPaddingDp
            prefs[LETTER_SPACING] = s.letterSpacingSp
            prefs[TEXT_ALIGN] = s.textAlign.name
            prefs[CHAPTER_TITLE_ALIGN] = s.chapterTitleAlign.name
            s.selectedColorPresetId?.let { prefs[SELECTED_COLOR_PRESET_ID] = it } ?: prefs.remove(SELECTED_COLOR_PRESET_ID)
            prefs[FAST_COLOR_PRESET_SWITCH] = s.fastColorPresetSwitch
            prefs[LIMITER_ENABLED] = s.limiterEnabled
            prefs[LIMITER_HEIGHT] = s.limiterHeightDp
            prefs[LIMITER_VERTICAL_OFFSET] = s.limiterVerticalOffsetDp
            prefs[LIMITER_RULER_ENABLED] = s.limiterRulerEnabled
            prefs[LIMITER_RULER_THICKNESS] = s.limiterRulerThicknessDp
            prefs[LIMITER_DIMMING] = s.limiterDimming
            prefs[EXPANDER_ENABLED] = s.perceptionExpanderEnabled
            prefs[EXPANDER_PADDING] = s.perceptionExpanderPaddingDp
            prefs[EXPANDER_THICKNESS] = s.perceptionExpanderThicknessDp
            prefs[HIGHLIGHT_ENABLED] = s.highlightedReadingEnabled
            prefs[HIGHLIGHT_THICKNESS] = s.highlightedReadingThicknessDp
            prefs[SWIPE_MODE] = s.swipeGestureMode.name
            prefs[SWIPE_SCROLL_AMOUNT] = s.swipeScrollAmount
            prefs[SWIPE_SENSITIVITY] = s.swipeSensitivity
            prefs[SWIPE_ALPHA_ANIMATION] = s.swipeAlphaAnimation
            prefs[SWIPE_PULL_ANIMATION] = s.swipePullAnimation
            prefs[SWIPE_DISABLE_NORMAL_SCROLL] = s.swipeDisableNormalScroll
            prefs[SHOW_IMAGES] = s.showImages
            prefs[SHOW_IMAGE_CAPTIONS] = s.showImageCaptions
            prefs[IMAGE_CORNER_ROUNDNESS] = s.imageCornerRoundnessDp
            prefs[IMAGE_ALIGNMENT] = s.imageAlignment.name
            prefs[IMAGE_WIDTH_PERCENT] = s.imageWidthPercent
            prefs[IMAGE_COLOR_EFFECT] = s.imageColorEffect.name
            prefs[SHOW_PROGRESS_BAR] = s.showProgressBar
            prefs[PROGRESS_BAR_ALIGNMENT] = s.progressBarAlignment.name
            prefs[PROGRESS_BAR_PADDING] = s.progressBarPaddingDp
            prefs[PROGRESS_BAR_FONT_SIZE] = s.progressBarFontSizeSp
            prefs[PROGRESS_DISPLAY_MODE] = s.progressDisplayMode.name
            prefs[DOUBLE_TAP_TRANSLATE_ENABLED] = s.doubleTapTranslateEnabled
            prefs[ORIENTATION_LOCK] = s.orientationLock.name
            prefs[CUSTOM_BRIGHTNESS_ENABLED] = s.customBrightnessEnabled
            prefs[CUSTOM_BRIGHTNESS] = s.customBrightness
            prefs[PAGE_TURN_EFFECT_ENABLED] = s.pageTurnEffectEnabled
        }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
        this?.let { name -> runCatching { enumValueOf<T>(name) }.getOrNull() } ?: default
}
