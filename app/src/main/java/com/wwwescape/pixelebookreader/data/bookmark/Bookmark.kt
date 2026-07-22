package com.wwwescape.pixelebookreader.data.bookmark

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wwwescape.pixelebookreader.data.book.Book

/** A saved position within a book — [contentIndex] is the same flat-list coordinate space as
 * [com.wwwescape.pixelebookreader.data.parser.ReaderText]/`Book.scrollIndex`/chapter markers, so
 * jumping to a bookmark reuses the exact mechanism chapters already use
 * (`listState.scrollToItem`). [snippet] captures the paragraph text at save time purely for
 * display in the Bookmarks tab — it isn't re-derived from [contentIndex] later, so it stays
 * stable even if editing/re-importing ever changes a book's parsed content. Same `CASCADE`
 * foreign-key pattern as [com.wwwescape.pixelebookreader.data.stats.ReadingSession]. */
@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId")],
)
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val contentIndex: Int,
    val snippet: String,
    val createdAt: Long = System.currentTimeMillis(),
)
