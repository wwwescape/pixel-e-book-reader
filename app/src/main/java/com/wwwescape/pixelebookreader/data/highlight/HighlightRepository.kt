package com.wwwescape.pixelebookreader.data.highlight

import android.content.Context
import com.wwwescape.pixelebookreader.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow

object HighlightRepository {
    fun getForBook(context: Context, bookId: Int): Flow<List<Highlight>> =
        AppDatabase.getInstance(context).highlightDao().getForBook(bookId)

    suspend fun addHighlight(context: Context, highlight: Highlight): Long =
        AppDatabase.getInstance(context).highlightDao().insert(highlight)

    suspend fun deleteHighlight(context: Context, highlight: Highlight) =
        AppDatabase.getInstance(context).highlightDao().delete(highlight)
}
