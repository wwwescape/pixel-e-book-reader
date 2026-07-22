package com.wwwescape.pixelebookreader.data.bookmark

import android.content.Context
import com.wwwescape.pixelebookreader.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow

object BookmarkRepository {
    fun getForBook(context: Context, bookId: Int): Flow<List<Bookmark>> =
        AppDatabase.getInstance(context).bookmarkDao().getForBook(bookId)

    suspend fun addBookmark(context: Context, bookmark: Bookmark): Long =
        AppDatabase.getInstance(context).bookmarkDao().insert(bookmark)

    suspend fun deleteBookmark(context: Context, bookmark: Bookmark) =
        AppDatabase.getInstance(context).bookmarkDao().delete(bookmark)
}
