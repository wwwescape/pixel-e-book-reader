package com.wwwescape.pixelebookreader.data.stats

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wwwescape.pixelebookreader.data.book.Book

/** One row per debounced reading session — see [com.wwwescape.pixelebookreader.ui.screens.reader.ReaderViewModel]'s
 * pause/resume tracking, which only persists a session once it clears a minimum duration, so a
 * quick app-switch-and-back doesn't fragment the log into noise. Same `CASCADE` foreign-key
 * pattern as [com.wwwescape.pixelebookreader.data.history.HistoryEntry]. */
@Entity(
    tableName = "reading_sessions",
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
data class ReadingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val startedAt: Long,
    val durationMs: Long,
)
