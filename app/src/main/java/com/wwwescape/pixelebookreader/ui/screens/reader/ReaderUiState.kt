package com.wwwescape.pixelebookreader.ui.screens.reader

import com.wwwescape.pixelebookreader.data.book.Book
import com.wwwescape.pixelebookreader.data.bookmark.Bookmark
import com.wwwescape.pixelebookreader.data.colorpreset.ColorPreset
import com.wwwescape.pixelebookreader.data.highlight.Highlight
import com.wwwescape.pixelebookreader.data.parser.ReaderText
import com.wwwescape.pixelebookreader.data.reader.ReaderSettings

/** A chapter marker's position within the flat [ReaderUiState.content] list — the same list
 * both the scrollable reader and the chapters drawer read from. */
data class ChapterEntry(val contentIndex: Int, val chapter: ReaderText.Chapter)

data class ReaderUiState(
    val book: Book? = null,
    val content: List<ReaderText> = emptyList(),
    val chapters: List<ChapterEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val isImmersive: Boolean = false,
    val canUndoCheckpoint: Boolean = false,
    val settings: ReaderSettings = ReaderSettings(),
    val colorPresets: List<ColorPreset> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val highlights: List<Highlight> = emptyList(),
) {
    val activeColorPreset: ColorPreset? get() = colorPresets.find { it.id == settings.selectedColorPresetId }
}
