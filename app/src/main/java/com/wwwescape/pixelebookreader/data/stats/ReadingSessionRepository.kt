package com.wwwescape.pixelebookreader.data.stats

import android.content.Context
import com.wwwescape.pixelebookreader.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow

object ReadingSessionRepository {
    fun getAllSessions(context: Context): Flow<List<ReadingSession>> =
        AppDatabase.getInstance(context).readingSessionDao().getAll()

    suspend fun recordSession(context: Context, bookId: Int, startedAt: Long, durationMs: Long) {
        AppDatabase.getInstance(context).readingSessionDao().insert(ReadingSession(bookId = bookId, startedAt = startedAt, durationMs = durationMs))
    }

    suspend fun deleteAllSessions(context: Context) =
        AppDatabase.getInstance(context).readingSessionDao().deleteAll()
}
