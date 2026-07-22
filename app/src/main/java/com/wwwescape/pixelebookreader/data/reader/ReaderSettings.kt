package com.wwwescape.pixelebookreader.data.reader

enum class ReaderFontFamily { SYSTEM_DEFAULT, SERIF, SANS_SERIF, MONOSPACE }
enum class ReaderFontWeight { LIGHT, NORMAL, MEDIUM, SEMI_BOLD, BOLD }
enum class ReaderTextAlign { START, CENTER, JUSTIFY }
enum class ImageAlignment { START, CENTER, END }
enum class ImageColorEffect { NONE, GRAYSCALE, SEPIA }
enum class SwipeGestureMode { OFF, ON, INVERSE }
enum class ProgressDisplayMode { PERCENTAGE, COUNT }
enum class OrientationLock { DEFAULT, PORTRAIT, LANDSCAPE }

/** Every reading-experience setting exposed by the in-reader settings panel —
 * deliberately one flat data class rather than nested groups, since it's persisted
 * as one DataStore object and read as a single unit by the reader on every recomposition.
 * Reading color presets are a separate, structured list (see `ColorPreset`, stored in Room) —
 * [selectedColorPresetId] here just tracks which one is active. */
data class ReaderSettings(
    // Typography
    val fontFamily: ReaderFontFamily = ReaderFontFamily.SYSTEM_DEFAULT,
    val fontWeight: ReaderFontWeight = ReaderFontWeight.NORMAL,
    val italic: Boolean = false,
    val fontSizeSp: Float = 16f,
    val lineHeightMultiplier: Float = 1.4f,
    val paragraphHeightDp: Float = 12f,
    val paragraphIndentationDp: Float = 0f,
    val sidePaddingDp: Float = 16f,
    val verticalPaddingDp: Float = 4f,
    val letterSpacingSp: Float = 0f,
    val textAlign: ReaderTextAlign = ReaderTextAlign.START,
    val chapterTitleAlign: ReaderTextAlign = ReaderTextAlign.START,

    // Reading color presets
    val selectedColorPresetId: Int? = null,
    val fastColorPresetSwitch: Boolean = false,

    // Horizontal Limiter (reading ruler)
    val limiterEnabled: Boolean = false,
    val limiterHeightDp: Float = 220f,
    val limiterVerticalOffsetDp: Float = 0f,
    val limiterRulerEnabled: Boolean = true,
    val limiterRulerThicknessDp: Float = 2f,
    val limiterDimming: Float = 0.5f,

    // Perception Expander
    val perceptionExpanderEnabled: Boolean = false,
    val perceptionExpanderPaddingDp: Float = 24f,
    val perceptionExpanderThicknessDp: Float = 2f,

    // Highlighted Reading
    val highlightedReadingEnabled: Boolean = false,
    val highlightedReadingThicknessDp: Float = 4f,

    // Horizontal swipe gesture mode
    val swipeGestureMode: SwipeGestureMode = SwipeGestureMode.OFF,
    val swipeScrollAmount: Float = 1f,
    val swipeSensitivity: Float = 1f,
    val swipeAlphaAnimation: Boolean = true,
    val swipePullAnimation: Boolean = true,
    val swipeDisableNormalScroll: Boolean = false,

    // Inline images
    val showImages: Boolean = true,
    val showImageCaptions: Boolean = true,
    val imageCornerRoundnessDp: Float = 0f,
    val imageAlignment: ImageAlignment = ImageAlignment.CENTER,
    val imageWidthPercent: Float = 1f,
    val imageColorEffect: ImageColorEffect = ImageColorEffect.NONE,

    // Progress bar
    val showProgressBar: Boolean = true,
    val progressBarAlignment: ImageAlignment = ImageAlignment.START,
    val progressBarPaddingDp: Float = 8f,
    val progressBarFontSizeSp: Float = 11f,
    val progressDisplayMode: ProgressDisplayMode = ProgressDisplayMode.PERCENTAGE,

    // Translate, orientation & brightness
    val doubleTapTranslateEnabled: Boolean = true,
    val orientationLock: OrientationLock = OrientationLock.DEFAULT,
    val customBrightnessEnabled: Boolean = false,
    val customBrightness: Float = 0.5f,

    // PDF page-mode reader only
    val pageTurnEffectEnabled: Boolean = false,
)
