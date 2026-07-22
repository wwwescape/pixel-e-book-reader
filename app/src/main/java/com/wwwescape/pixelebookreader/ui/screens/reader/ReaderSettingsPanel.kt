package com.wwwescape.pixelebookreader.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wwwescape.pixelebookreader.R
import com.wwwescape.pixelebookreader.data.colorpreset.ColorPreset
import com.wwwescape.pixelebookreader.data.reader.ImageAlignment
import com.wwwescape.pixelebookreader.data.reader.ImageColorEffect
import com.wwwescape.pixelebookreader.data.reader.OrientationLock
import com.wwwescape.pixelebookreader.data.reader.ProgressDisplayMode
import com.wwwescape.pixelebookreader.data.reader.ReaderFontFamily
import com.wwwescape.pixelebookreader.data.reader.ReaderFontWeight
import com.wwwescape.pixelebookreader.data.reader.ReaderSettings
import com.wwwescape.pixelebookreader.data.reader.ReaderTextAlign
import com.wwwescape.pixelebookreader.data.reader.SwipeGestureMode

private val SWATCHES = listOf(
    0xFFFFFFFF, 0xFFF5ECD9, 0xFFE0E0E0, 0xFF121212, 0xFF1B1B1B, 0xFF2B1B12,
    0xFF000000, 0xFF3A2E22, 0xFF5B4636, 0xFFD7C4A3,
)

private enum class SettingsTab { GENERAL, READER, COLORS }

/** The in-reader settings panel — a full-screen overlay rather than a
 * bottom sheet, since the settings surface here is far larger than Library's display-options
 * sheet. General/Reader/Colors tabs (not an accordion — dropped in favor of tabs so all of a
 * tab's ~10-15 controls stay visible and scannable at once) keep the ~50 discrete controls
 * navigable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsPanel(
    uiState: ReaderUiState,
    onDismiss: () -> Unit,
    onUpdateSettings: ((ReaderSettings) -> ReaderSettings) -> Unit,
    onAddColorPreset: (name: String, backgroundColor: Long, fontColor: Long) -> Unit,
    onSelectColorPreset: (Int?) -> Unit,
    onDeleteColorPreset: (ColorPreset) -> Unit,
    onMoveColorPresetUp: (ColorPreset) -> Unit,
    onMoveColorPresetDown: (ColorPreset) -> Unit,
) {
    val settings = uiState.settings
    var selectedTab by remember { mutableStateOf(SettingsTab.GENERAL) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.action_reader_settings)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    Tab(
                        selected = selectedTab == SettingsTab.GENERAL,
                        onClick = { selectedTab = SettingsTab.GENERAL },
                        text = { Text(stringResource(R.string.tab_settings_general)) },
                    )
                    Tab(
                        selected = selectedTab == SettingsTab.READER,
                        onClick = { selectedTab = SettingsTab.READER },
                        text = { Text(stringResource(R.string.tab_settings_reader)) },
                    )
                    Tab(
                        selected = selectedTab == SettingsTab.COLORS,
                        onClick = { selectedTab = SettingsTab.COLORS },
                        text = { Text(stringResource(R.string.tab_settings_colors)) },
                    )
                }
                when (selectedTab) {
                    SettingsTab.GENERAL -> GeneralTabContent(settings, onUpdateSettings)
                    SettingsTab.READER -> ReaderTabContent(settings, onUpdateSettings)
                    SettingsTab.COLORS -> ColorsTabContent(
                        uiState = uiState,
                        settings = settings,
                        onSelectColorPreset = onSelectColorPreset,
                        onDeleteColorPreset = onDeleteColorPreset,
                        onAddColorPreset = onAddColorPreset,
                        onMoveColorPresetUp = onMoveColorPresetUp,
                        onMoveColorPresetDown = onMoveColorPresetDown,
                        onUpdateSettings = onUpdateSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderTabContent(settings: ReaderSettings, onUpdateSettings: ((ReaderSettings) -> ReaderSettings) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
        // Not a bundled dyslexia-friendly font (no such asset exists in this project to bundle)
        // — instead applies the spacing/font-family adjustments most consistently cited as
        // helping readability generally (a sans-serif face, wider letter/line/paragraph
        // spacing), reusing the typography controls that already exist rather than adding a new
        // font dependency.
        OutlinedButton(
            onClick = {
                onUpdateSettings { s ->
                    s.copy(
                        fontFamily = ReaderFontFamily.SANS_SERIF,
                        letterSpacingSp = 1.5f,
                        lineHeightMultiplier = 1.6f,
                        paragraphHeightDp = 24f,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        ) {
            Icon(imageVector = Icons.Rounded.AccessibilityNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.setting_readability_preset), modifier = Modifier.padding(start = 8.dp))
        }

        SettingsSectionLabel(stringResource(R.string.section_font))
        SettingsGroupCard {
            ChoiceRow(stringResource(R.string.setting_font_family), ReaderFontFamily.entries, settings.fontFamily, { fontFamilyLabel(it) }) {
                onUpdateSettings { s -> s.copy(fontFamily = it) }
            }
            ChoiceRow(stringResource(R.string.setting_font_weight), ReaderFontWeight.entries, settings.fontWeight, { fontWeightLabel(it) }) {
                onUpdateSettings { s -> s.copy(fontWeight = it) }
            }
            SwitchRow(stringResource(R.string.setting_italic), settings.italic) { onUpdateSettings { s -> s.copy(italic = it) } }
            SliderRow(stringResource(R.string.setting_font_size), settings.fontSizeSp, 10f..28f, "${settings.fontSizeSp.toInt()}sp") {
                onUpdateSettings { s -> s.copy(fontSizeSp = it) }
            }
            SliderRow(stringResource(R.string.setting_letter_spacing), settings.letterSpacingSp, 0f..4f, "%.1fsp".format(settings.letterSpacingSp)) {
                onUpdateSettings { s -> s.copy(letterSpacingSp = it) }
            }
        }

        SettingsSectionLabel(stringResource(R.string.section_text))
        SettingsGroupCard {
            ChoiceRow(stringResource(R.string.setting_text_align), ReaderTextAlign.entries, settings.textAlign, { textAlignLabel(it) }) {
                onUpdateSettings { s -> s.copy(textAlign = it) }
            }
            ChoiceRow(stringResource(R.string.setting_chapter_title_align), ReaderTextAlign.entries, settings.chapterTitleAlign, { textAlignLabel(it) }) {
                onUpdateSettings { s -> s.copy(chapterTitleAlign = it) }
            }
            SliderRow(stringResource(R.string.setting_line_height), settings.lineHeightMultiplier, 1f..2.2f, "%.1fx".format(settings.lineHeightMultiplier)) {
                onUpdateSettings { s -> s.copy(lineHeightMultiplier = it) }
            }
            SliderRow(stringResource(R.string.setting_paragraph_height), settings.paragraphHeightDp, 0f..40f, "${settings.paragraphHeightDp.toInt()}dp") {
                onUpdateSettings { s -> s.copy(paragraphHeightDp = it) }
            }
            SliderRow(stringResource(R.string.setting_paragraph_indentation), settings.paragraphIndentationDp, 0f..48f, "${settings.paragraphIndentationDp.toInt()}dp") {
                onUpdateSettings { s -> s.copy(paragraphIndentationDp = it) }
            }
            SliderRow(stringResource(R.string.setting_side_padding), settings.sidePaddingDp, 0f..48f, "${settings.sidePaddingDp.toInt()}dp") {
                onUpdateSettings { s -> s.copy(sidePaddingDp = it) }
            }
            SliderRow(stringResource(R.string.setting_vertical_padding), settings.verticalPaddingDp, 0f..32f, "${settings.verticalPaddingDp.toInt()}dp") {
                onUpdateSettings { s -> s.copy(verticalPaddingDp = it) }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun GeneralTabContent(settings: ReaderSettings, onUpdateSettings: ((ReaderSettings) -> ReaderSettings) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
        SettingsSectionLabel(stringResource(R.string.section_horizontal_limiter))
        SettingsGroupCard {
            SwitchRow(stringResource(R.string.setting_limiter_enabled), settings.limiterEnabled) { onUpdateSettings { s -> s.copy(limiterEnabled = it) } }
            SliderRow(stringResource(R.string.setting_limiter_height), settings.limiterHeightDp, 80f..600f, "${settings.limiterHeightDp.toInt()}dp") {
                onUpdateSettings { s -> s.copy(limiterHeightDp = it) }
            }
            SliderRow(stringResource(R.string.setting_limiter_vertical_offset), settings.limiterVerticalOffsetDp, -200f..200f, "${settings.limiterVerticalOffsetDp.toInt()}dp") {
                onUpdateSettings { s -> s.copy(limiterVerticalOffsetDp = it) }
            }
            SwitchRow(stringResource(R.string.setting_limiter_ruler_enabled), settings.limiterRulerEnabled) { onUpdateSettings { s -> s.copy(limiterRulerEnabled = it) } }
            SliderRow(stringResource(R.string.setting_limiter_ruler_thickness), settings.limiterRulerThicknessDp, 1f..8f, "${settings.limiterRulerThicknessDp.toInt()}dp") {
                onUpdateSettings { s -> s.copy(limiterRulerThicknessDp = it) }
            }
            SliderRow(stringResource(R.string.setting_limiter_dimming), settings.limiterDimming, 0f..0.9f, "${(settings.limiterDimming * 100).toInt()}%") {
                onUpdateSettings { s -> s.copy(limiterDimming = it) }
            }
        }

        SettingsSectionLabel(stringResource(R.string.section_perception_expander))
        SettingsGroupCard {
            SwitchRow(stringResource(R.string.setting_expander_enabled), settings.perceptionExpanderEnabled) {
                onUpdateSettings { s -> s.copy(perceptionExpanderEnabled = it) }
            }
            SliderRow(stringResource(R.string.setting_expander_padding), settings.perceptionExpanderPaddingDp, 0f..64f, "${settings.perceptionExpanderPaddingDp.toInt()}dp") {
                onUpdateSettings { s -> s.copy(perceptionExpanderPaddingDp = it) }
            }
            SliderRow(stringResource(R.string.setting_expander_thickness), settings.perceptionExpanderThicknessDp, 1f..8f, "${settings.perceptionExpanderThicknessDp.toInt()}dp") {
                onUpdateSettings { s -> s.copy(perceptionExpanderThicknessDp = it) }
            }
        }

        SettingsSectionLabel(stringResource(R.string.section_highlighted_reading))
        SettingsGroupCard {
            SwitchRow(stringResource(R.string.setting_highlight_enabled), settings.highlightedReadingEnabled) {
                onUpdateSettings { s -> s.copy(highlightedReadingEnabled = it) }
            }
            SliderRow(stringResource(R.string.setting_highlight_thickness), settings.highlightedReadingThicknessDp, 1f..16f, "${settings.highlightedReadingThicknessDp.toInt()}dp") {
                onUpdateSettings { s -> s.copy(highlightedReadingThicknessDp = it) }
            }
        }

        SettingsSectionLabel(stringResource(R.string.section_gestures))
        SettingsGroupCard {
            ChoiceRow(stringResource(R.string.setting_swipe_mode), SwipeGestureMode.entries, settings.swipeGestureMode, { swipeModeLabel(it) }) {
                onUpdateSettings { s -> s.copy(swipeGestureMode = it) }
            }
            SliderRow(stringResource(R.string.setting_swipe_scroll_amount), settings.swipeScrollAmount, 0.2f..3f, "%.1fx".format(settings.swipeScrollAmount)) {
                onUpdateSettings { s -> s.copy(swipeScrollAmount = it) }
            }
            SliderRow(stringResource(R.string.setting_swipe_sensitivity), settings.swipeSensitivity, 0.2f..5f, "%.1fx".format(settings.swipeSensitivity)) {
                onUpdateSettings { s -> s.copy(swipeSensitivity = it) }
            }
            SwitchRow(stringResource(R.string.setting_swipe_alpha_animation), settings.swipeAlphaAnimation) { onUpdateSettings { s -> s.copy(swipeAlphaAnimation = it) } }
            SwitchRow(stringResource(R.string.setting_swipe_pull_animation), settings.swipePullAnimation) { onUpdateSettings { s -> s.copy(swipePullAnimation = it) } }
            SwitchRow(stringResource(R.string.setting_swipe_disable_normal_scroll), settings.swipeDisableNormalScroll) {
                onUpdateSettings { s -> s.copy(swipeDisableNormalScroll = it) }
            }
        }

        SettingsSectionLabel(stringResource(R.string.section_images))
        SettingsGroupCard {
            SwitchRow(stringResource(R.string.setting_show_images), settings.showImages) { onUpdateSettings { s -> s.copy(showImages = it) } }
            SwitchRow(stringResource(R.string.setting_show_image_captions), settings.showImageCaptions) { onUpdateSettings { s -> s.copy(showImageCaptions = it) } }
            SliderRow(stringResource(R.string.setting_image_corner_roundness), settings.imageCornerRoundnessDp, 0f..32f, "${settings.imageCornerRoundnessDp.toInt()}dp") {
                onUpdateSettings { s -> s.copy(imageCornerRoundnessDp = it) }
            }
            ChoiceRow(stringResource(R.string.setting_image_alignment), ImageAlignment.entries, settings.imageAlignment, { alignmentLabel(it) }) {
                onUpdateSettings { s -> s.copy(imageAlignment = it) }
            }
            SliderRow(stringResource(R.string.setting_image_width_percent), settings.imageWidthPercent, 0.2f..1f, "${(settings.imageWidthPercent * 100).toInt()}%") {
                onUpdateSettings { s -> s.copy(imageWidthPercent = it) }
            }
            ChoiceRow(stringResource(R.string.setting_image_color_effect), ImageColorEffect.entries, settings.imageColorEffect, { colorEffectLabel(it) }) {
                onUpdateSettings { s -> s.copy(imageColorEffect = it) }
            }
        }

        SettingsSectionLabel(stringResource(R.string.section_progress_reading))
        SettingsGroupCard {
            SwitchRow(stringResource(R.string.setting_show_progress_bar), settings.showProgressBar) { onUpdateSettings { s -> s.copy(showProgressBar = it) } }
            ChoiceRow(stringResource(R.string.setting_progress_bar_alignment), ImageAlignment.entries, settings.progressBarAlignment, { alignmentLabel(it) }) {
                onUpdateSettings { s -> s.copy(progressBarAlignment = it) }
            }
            SliderRow(stringResource(R.string.setting_progress_bar_padding), settings.progressBarPaddingDp, 0f..32f, "${settings.progressBarPaddingDp.toInt()}dp") {
                onUpdateSettings { s -> s.copy(progressBarPaddingDp = it) }
            }
            SliderRow(stringResource(R.string.setting_progress_bar_font_size), settings.progressBarFontSizeSp, 8f..18f, "${settings.progressBarFontSizeSp.toInt()}sp") {
                onUpdateSettings { s -> s.copy(progressBarFontSizeSp = it) }
            }
            ChoiceRow(stringResource(R.string.setting_progress_display_mode), ProgressDisplayMode.entries, settings.progressDisplayMode, { progressDisplayLabel(it) }) {
                onUpdateSettings { s -> s.copy(progressDisplayMode = it) }
            }
            SwitchRow(stringResource(R.string.setting_double_tap_translate), settings.doubleTapTranslateEnabled) {
                onUpdateSettings { s -> s.copy(doubleTapTranslateEnabled = it) }
            }
            ChoiceRow(stringResource(R.string.setting_orientation_lock), OrientationLock.entries, settings.orientationLock, { orientationLabel(it) }) {
                onUpdateSettings { s -> s.copy(orientationLock = it) }
            }
            SwitchRow(stringResource(R.string.setting_custom_brightness_enabled), settings.customBrightnessEnabled) {
                onUpdateSettings { s -> s.copy(customBrightnessEnabled = it) }
            }
            if (settings.customBrightnessEnabled) {
                SliderRow(stringResource(R.string.setting_custom_brightness), settings.customBrightness, 0.05f..1f, "${(settings.customBrightness * 100).toInt()}%") {
                    onUpdateSettings { s -> s.copy(customBrightness = it) }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ColorsTabContent(
    uiState: ReaderUiState,
    settings: ReaderSettings,
    onSelectColorPreset: (Int?) -> Unit,
    onDeleteColorPreset: (ColorPreset) -> Unit,
    onAddColorPreset: (String, Long, Long) -> Unit,
    onMoveColorPresetUp: (ColorPreset) -> Unit,
    onMoveColorPresetDown: (ColorPreset) -> Unit,
    onUpdateSettings: ((ReaderSettings) -> ReaderSettings) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
        SettingsGroupCard {
            ColorPresetSection(
                presets = uiState.colorPresets,
                selectedId = settings.selectedColorPresetId,
                onSelect = onSelectColorPreset,
                onDelete = onDeleteColorPreset,
                onAdd = onAddColorPreset,
                onMoveUp = onMoveColorPresetUp,
                onMoveDown = onMoveColorPresetDown,
            )
            SwitchRow(stringResource(R.string.setting_fast_color_switch), settings.fastColorPresetSwitch) {
                onUpdateSettings { s -> s.copy(fastColorPresetSwitch = it) }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ColorPresetSection(
    presets: List<ColorPreset>,
    selectedId: Int?,
    onSelect: (Int?) -> Unit,
    onDelete: (ColorPreset) -> Unit,
    onAdd: (String, Long, Long) -> Unit,
    onMoveUp: (ColorPreset) -> Unit,
    onMoveDown: (ColorPreset) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column {
        ListItem(
            headlineContent = { Text(stringResource(R.string.preset_default)) },
            leadingContent = { RadioButton(selected = selectedId == null, onClick = { onSelect(null) }) },
            modifier = Modifier.clickable { onSelect(null) },
        )
        presets.forEachIndexed { index, preset ->
            ListItem(
                headlineContent = { Text(preset.name) },
                leadingContent = { RadioButton(selected = selectedId == preset.id, onClick = { onSelect(preset.id) }) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(24.dp).background(Color(preset.backgroundColor), CircleShape))
                        IconButton(onClick = { onMoveUp(preset) }, enabled = index > 0) {
                            Icon(imageVector = Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.action_move_up))
                        }
                        IconButton(onClick = { onMoveDown(preset) }, enabled = index < presets.lastIndex) {
                            Icon(imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.action_move_down))
                        }
                        IconButton(onClick = { onDelete(preset) }) {
                            Icon(imageVector = Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
                modifier = Modifier.clickable { onSelect(preset.id) },
            )
        }
        TextButton(onClick = { showAddDialog = true }) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
            Text(stringResource(R.string.action_add_preset), modifier = Modifier.padding(start = 4.dp))
        }
    }

    if (showAddDialog) {
        AddColorPresetDialog(onDismiss = { showAddDialog = false }, onAdd = onAdd)
    }
}

@Composable
private fun AddColorPresetDialog(onDismiss: () -> Unit, onAdd: (String, Long, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var background by remember { mutableStateOf(0xFFFFFFFFL) }
    var font by remember { mutableStateOf(0xFF000000L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_preset)) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text(stringResource(R.string.preset_name_placeholder)) }, singleLine = true)
                Text(stringResource(R.string.field_background_color), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                SwatchRow(selected = background) { background = it }
                Text(stringResource(R.string.field_font_color), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                SwatchRow(selected = font) { font = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) { onAdd(name.trim(), background, font); onDismiss() } }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun SwatchRow(selected: Long, onSelect: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        SWATCHES.forEach { swatch ->
            val isSelected = swatch == selected
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(swatch), CircleShape)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(swatch) },
            )
        }
    }
}

@Composable
private fun fontFamilyLabel(value: ReaderFontFamily): String = when (value) {
    ReaderFontFamily.SYSTEM_DEFAULT -> stringResource(R.string.font_family_system)
    ReaderFontFamily.SERIF -> stringResource(R.string.font_family_serif)
    ReaderFontFamily.SANS_SERIF -> stringResource(R.string.font_family_sans_serif)
    ReaderFontFamily.MONOSPACE -> stringResource(R.string.font_family_monospace)
}

@Composable
private fun fontWeightLabel(value: ReaderFontWeight): String = when (value) {
    ReaderFontWeight.LIGHT -> stringResource(R.string.font_weight_light)
    ReaderFontWeight.NORMAL -> stringResource(R.string.font_weight_normal)
    ReaderFontWeight.MEDIUM -> stringResource(R.string.font_weight_medium)
    ReaderFontWeight.SEMI_BOLD -> stringResource(R.string.font_weight_semi_bold)
    ReaderFontWeight.BOLD -> stringResource(R.string.font_weight_bold)
}

@Composable
private fun textAlignLabel(value: ReaderTextAlign): String = when (value) {
    ReaderTextAlign.START -> stringResource(R.string.align_start)
    ReaderTextAlign.CENTER -> stringResource(R.string.align_center)
    ReaderTextAlign.JUSTIFY -> stringResource(R.string.align_justify)
}

@Composable
private fun alignmentLabel(value: ImageAlignment): String = when (value) {
    ImageAlignment.START -> stringResource(R.string.align_start)
    ImageAlignment.CENTER -> stringResource(R.string.align_center)
    ImageAlignment.END -> stringResource(R.string.align_end)
}

@Composable
private fun colorEffectLabel(value: ImageColorEffect): String = when (value) {
    ImageColorEffect.NONE -> stringResource(R.string.effect_none)
    ImageColorEffect.GRAYSCALE -> stringResource(R.string.effect_grayscale)
    ImageColorEffect.SEPIA -> stringResource(R.string.effect_sepia)
}

@Composable
private fun swipeModeLabel(value: SwipeGestureMode): String = when (value) {
    SwipeGestureMode.OFF -> stringResource(R.string.swipe_mode_off)
    SwipeGestureMode.ON -> stringResource(R.string.swipe_mode_on)
    SwipeGestureMode.INVERSE -> stringResource(R.string.swipe_mode_inverse)
}

@Composable
private fun progressDisplayLabel(value: ProgressDisplayMode): String = when (value) {
    ProgressDisplayMode.PERCENTAGE -> stringResource(R.string.display_percentage)
    ProgressDisplayMode.COUNT -> stringResource(R.string.display_count)
}

@Composable
private fun orientationLabel(value: OrientationLock): String = when (value) {
    OrientationLock.DEFAULT -> stringResource(R.string.orientation_default)
    OrientationLock.PORTRAIT -> stringResource(R.string.orientation_portrait)
    OrientationLock.LANDSCAPE -> stringResource(R.string.orientation_landscape)
}
