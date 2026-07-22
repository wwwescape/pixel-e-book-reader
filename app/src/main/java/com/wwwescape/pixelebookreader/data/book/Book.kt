package com.wwwescape.pixelebookreader.data.book

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [filePath] is a persisted SAF document Uri string — see
 * [com.wwwescape.pixelebookreader.data.filesystem.FileSystemRepository] — not a filesystem
 * path. [fileName] is the original SAF display name (e.g. "brave-new-world.epub") captured at
 * import time; [com.wwwescape.pixelebookreader.data.parser.BookParsers] dispatches by
 * extension, and a raw content:// uri's `lastPathSegment` often isn't a usable file name
 * (provider-dependent), so this is the only reliable way later code (cover reset, the Reader)
 * can tell which parser a book needs. [scrollIndex]/[scrollOffset] resume a book's
 * [androidx.compose.foundation.lazy.LazyColumn] reading position; [progress] is the same
 * position expressed as 0f..1f for display.
 *
 * Richer book metadata: [series]/
 * [seriesNumber] identify a book's place in a series (number allows "2.5" novellas);
 * [tags] is free-form (genre, shelf, etc. — no fixed taxonomy); [rating] is a user rating in
 * whole stars, 1f..5f (no half-star granularity — a deliberate scope cut, see Book Info's star
 * selector); [publishDate] is stored as the free-form string a source file's metadata carries
 * (EPUB/FB2 publish dates appear in wildly inconsistent formats — year-only, full ISO date, or
 * missing entirely — so parsing to a real date type would silently drop anything that doesn't
 * fit, worse than just keeping the source string as-is).
 *
 * Reading statistics: [chaptersRead] is a monotonic high-water-mark — the
 * count of this book's chapter markers at or before the furthest [scrollIndex] ever reached, so
 * scrolling back up doesn't undo progress already credited (see `ReaderViewModel.saveProgress`).
 * [finishedAt] is set once, the first time [progress] crosses the completion threshold — reading
 * a finished book again doesn't inflate the "books finished" count or move its date. */
@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val filePath: String,
    val fileName: String,
    val coverImagePath: String? = null,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
    val progress: Float = 0f,
    val lastOpened: Long? = null,
    val categories: List<Int> = emptyList(),
    val series: String? = null,
    val seriesNumber: Float? = null,
    val tags: List<String> = emptyList(),
    val rating: Float? = null,
    val isbn: String? = null,
    val publishDate: String? = null,
    val language: String? = null,
    val chaptersRead: Int = 0,
    val finishedAt: Long? = null,
)
