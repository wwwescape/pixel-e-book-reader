package com.wwwescape.pixelebookreader.data.history

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wwwescape.pixelebookreader.data.book.Book

/** One row per "opened this book" event. Declares a real Room foreign key with `CASCADE`
 * delete, so removing a book automatically cleans up its history rather than leaving
 * orphaned rows. */
@Entity(
    tableName = "history",
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
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val openedAt: Long,
)
