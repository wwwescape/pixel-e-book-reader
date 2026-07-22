package com.wwwescape.pixelebookreader.ui.screens.reader

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.wwwescape.pixelebookreader.data.reader.ImageAlignment
import com.wwwescape.pixelebookreader.data.reader.ImageColorEffect
import com.wwwescape.pixelebookreader.data.reader.ReaderFontFamily
import com.wwwescape.pixelebookreader.data.reader.ReaderFontWeight
import com.wwwescape.pixelebookreader.data.reader.ReaderSettings
import com.wwwescape.pixelebookreader.data.reader.ReaderTextAlign

/** Builds the [TextStyle] every reader paragraph/chapter-heading renders with, straight from
 * [ReaderSettings] — the full typography surface (font family/weight/italic/size/line
 * height/letter spacing/alignment/indentation) in one place. [forChapterTitle] swaps in
 * [ReaderSettings.chapterTitleAlign] instead of the body [ReaderSettings.textAlign], matching
 * the checklist's "separate chapter-title alignment". */
fun readerTextStyle(settings: ReaderSettings, forChapterTitle: Boolean = false, indent: TextUnit = 0.sp): TextStyle {
    val fontFamily = when (settings.fontFamily) {
        ReaderFontFamily.SYSTEM_DEFAULT -> FontFamily.Default
        ReaderFontFamily.SERIF -> FontFamily.Serif
        ReaderFontFamily.SANS_SERIF -> FontFamily.SansSerif
        ReaderFontFamily.MONOSPACE -> FontFamily.Monospace
    }
    val fontWeight = when (settings.fontWeight) {
        ReaderFontWeight.LIGHT -> FontWeight.Light
        ReaderFontWeight.NORMAL -> FontWeight.Normal
        ReaderFontWeight.MEDIUM -> FontWeight.Medium
        ReaderFontWeight.SEMI_BOLD -> FontWeight.SemiBold
        ReaderFontWeight.BOLD -> FontWeight.Bold
    }
    val align = when (if (forChapterTitle) settings.chapterTitleAlign else settings.textAlign) {
        ReaderTextAlign.START -> TextAlign.Start
        ReaderTextAlign.CENTER -> TextAlign.Center
        ReaderTextAlign.JUSTIFY -> TextAlign.Justify
    }
    val fontSize = if (forChapterTitle) (settings.fontSizeSp * 1.4f).sp else settings.fontSizeSp.sp
    return TextStyle(
        fontFamily = fontFamily,
        fontWeight = if (forChapterTitle) FontWeight.Bold else fontWeight,
        fontStyle = if (!forChapterTitle && settings.italic) FontStyle.Italic else FontStyle.Normal,
        fontSize = fontSize,
        lineHeight = fontSize * settings.lineHeightMultiplier,
        letterSpacing = settings.letterSpacingSp.sp,
        textAlign = align,
        textIndent = if (!forChapterTitle) TextIndent(firstLine = indent) else TextIndent.None,
    )
}

/** Grayscale/sepia via a hand-specified [ColorMatrix] — no image-processing dependency needed
 * for two fixed effects. */
fun imageColorFilter(effect: ImageColorEffect): ColorFilter? = when (effect) {
    ImageColorEffect.NONE -> null
    ImageColorEffect.GRAYSCALE -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    ImageColorEffect.SEPIA -> ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}

fun imageAlignmentToHorizontal(alignment: ImageAlignment): Alignment.Horizontal = when (alignment) {
    ImageAlignment.START -> Alignment.Start
    ImageAlignment.CENTER -> Alignment.CenterHorizontally
    ImageAlignment.END -> Alignment.End
}
