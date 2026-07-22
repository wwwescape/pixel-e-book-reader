package com.wwwescape.pixelebookreader.data.highlight

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wwwescape.pixelebookreader.data.book.Book

/** A highlighted passage within one paragraph — [contentIndex] identifies the paragraph (same
 * coordinate space as [Bookmark.contentIndex]/`Book.scrollIndex`), [startOffset]/[endOffset] are
 * character offsets within that paragraph's text. Deliberately single-paragraph only: Compose's
 * [androidx.compose.ui.platform.TextToolbar] doesn't expose selection offsets directly (see
 * `ReaderTextToolbar.kt`'s clipboard-bridge trick), so a highlight's location is recovered by
 * searching nearby paragraphs for the selected string — a search that only stays unambiguous
 * within a single paragraph. [color] is packed ARGB, same convention as
 * [com.wwwescape.pixelebookreader.data.colorpreset.ColorPreset]. Same `CASCADE` foreign-key
 * pattern as [com.wwwescape.pixelebookreader.data.stats.ReadingSession]. */
@Entity(
    tableName = "highlights",
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
data class Highlight(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val contentIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val text: String,
    val color: Long,
    val createdAt: Long = System.currentTimeMillis(),
)
