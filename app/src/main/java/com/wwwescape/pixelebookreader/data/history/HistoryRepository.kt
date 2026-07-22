package com.wwwescape.pixelebookreader.data.history

import android.content.Context
import com.wwwescape.pixelebookreader.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow

object HistoryRepository {
    fun getAllHistory(context: Context): Flow<List<HistoryEntry>> =
        AppDatabase.getInstance(context).historyDao().getAll()

    fun getAllHistoryWithBooks(context: Context): Flow<List<HistoryWithBook>> =
        AppDatabase.getInstance(context).historyDao().getAllWithBooks()

    suspend fun recordOpen(context: Context, bookId: Int, openedAt: Long = System.currentTimeMillis()): Long =
        AppDatabase.getInstance(context).historyDao().insert(HistoryEntry(bookId = bookId, openedAt = openedAt))

    /** Called whenever a book is opened from the Reader. Collapses into the existing most-recent
     * entry (bumping its timestamp) rather than inserting a new row, when that entry is already
     * for the same book — so rapidly reopening the same book (navigating back and forth, testing)
     * doesn't spam History with near-duplicate rows for what's really one reading session. A
     * genuinely new session — a different book was opened in between — still gets its own row.
     * [recordOpen] itself stays a plain insert since [HistoryViewModel.undoDelete] needs an exact
     * re-insert, not a collapse. The actual read-then-write lives in [HistoryDao.recordBookOpened]
     * as a `@Transaction`, not here, so it's atomic against concurrent callers. */
    suspend fun recordBookOpened(context: Context, bookId: Int, openedAt: Long = System.currentTimeMillis()) {
        AppDatabase.getInstance(context).historyDao().recordBookOpened(bookId, openedAt)
    }

    suspend fun deleteEntry(context: Context, entry: HistoryEntry) =
        AppDatabase.getInstance(context).historyDao().delete(entry)

    suspend fun deleteAllHistory(context: Context) =
        AppDatabase.getInstance(context).historyDao().deleteAll()
}
