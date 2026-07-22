package com.wwwescape.pixelebookreader.data.book

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert
    suspend fun insert(book: Book): Long

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)

    @Query("SELECT * FROM books ORDER BY lastOpened DESC")
    fun getAll(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getById(id: Int): Flow<Book?>

    @Query("DELETE FROM books")
    suspend fun deleteAll()
}
