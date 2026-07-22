package com.wwwescape.pixelebookreader.data.highlight

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {
    @Insert
    suspend fun insert(highlight: Highlight): Long

    @Delete
    suspend fun delete(highlight: Highlight)

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY contentIndex ASC")
    fun getForBook(bookId: Int): Flow<List<Highlight>>
}
