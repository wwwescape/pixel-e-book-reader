package com.wwwescape.pixelebookreader.data.history

/** A history row joined with the book it refers to — the History screen always needs the
 * book's title/author/cover to render anything useful, and a raw [HistoryEntry] doesn't carry
 * them. */
data class HistoryWithBook(
    val historyId: Int,
    val bookId: Int,
    val openedAt: Long,
    val title: String,
    val author: String?,
    val coverImagePath: String?,
)
