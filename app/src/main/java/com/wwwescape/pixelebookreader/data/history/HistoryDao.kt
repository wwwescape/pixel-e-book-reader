package com.wwwescape.pixelebookreader.data.history

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entry: HistoryEntry): Long

    @Update
    suspend fun update(entry: HistoryEntry)

    @Delete
    suspend fun delete(entry: HistoryEntry)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("SELECT * FROM history ORDER BY openedAt DESC")
    fun getAll(): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history ORDER BY openedAt DESC LIMIT 1")
    suspend fun getMostRecent(): HistoryEntry?

    /** Collapses into the existing most-recent entry (bumping its timestamp) rather than
     * inserting a new row, when that entry is already for the same book — see
     * [com.wwwescape.pixelebookreader.data.history.HistoryRepository.recordBookOpened]'s doc for
     * the full rationale. `@Transaction` makes the read-then-write atomic against concurrent
     * callers — e.g. a rapid double-tap into the Reader pushing two nav entries in quick
     * succession — which doing this same read-then-write as two separate suspend calls at the
     * repository layer wouldn't guard against. */
    @Transaction
    suspend fun recordBookOpened(bookId: Int, openedAt: Long) {
        val mostRecent = getMostRecent()
        if (mostRecent != null && mostRecent.bookId == bookId) {
            update(mostRecent.copy(openedAt = openedAt))
        } else {
            insert(HistoryEntry(bookId = bookId, openedAt = openedAt))
        }
    }

    @Query(
        "SELECT history.id AS historyId, history.bookId AS bookId, history.openedAt AS openedAt, " +
            "books.title AS title, books.author AS author, books.coverImagePath AS coverImagePath " +
            "FROM history INNER JOIN books ON books.id = history.bookId " +
            "ORDER BY history.openedAt DESC",
    )
    fun getAllWithBooks(): Flow<List<HistoryWithBook>>
}
