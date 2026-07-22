package com.wwwescape.pixelebookreader.ui.screens.reader

import com.wwwescape.pixelebookreader.data.book.Book
import com.wwwescape.pixelebookreader.data.bookmark.Bookmark
import com.wwwescape.pixelebookreader.data.parser.pdf.PdfOutlineEntry
import com.wwwescape.pixelebookreader.data.reader.ReaderSettings

data class PdfPageReaderUiState(
    val book: Book? = null,
    val pageCount: Int = 0,
    val outline: List<PdfOutlineEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val isImmersive: Boolean = false,
    val settings: ReaderSettings = ReaderSettings(),
    val bookmarks: List<Bookmark> = emptyList(),
)
