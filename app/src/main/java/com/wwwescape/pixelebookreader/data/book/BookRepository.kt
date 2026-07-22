package com.wwwescape.pixelebookreader.data.book

import android.content.Context
import com.wwwescape.pixelebookreader.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow

object BookRepository {
    fun getAllBooks(context: Context): Flow<List<Book>> =
        AppDatabase.getInstance(context).bookDao().getAll()

    fun getBook(context: Context, id: Int): Flow<Book?> =
        AppDatabase.getInstance(context).bookDao().getById(id)

    suspend fun addBook(context: Context, book: Book): Long =
        AppDatabase.getInstance(context).bookDao().insert(book)

    suspend fun updateBook(context: Context, book: Book) =
        AppDatabase.getInstance(context).bookDao().update(book)

    suspend fun deleteBook(context: Context, book: Book) =
        AppDatabase.getInstance(context).bookDao().delete(book)

    suspend fun deleteAllBooks(context: Context) =
        AppDatabase.getInstance(context).bookDao().deleteAll()
}
