package com.wwwescape.pixelebookreader.data.stats

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Insert
    suspend fun insert(session: ReadingSession): Long

    @Query("SELECT * FROM reading_sessions ORDER BY startedAt DESC")
    fun getAll(): Flow<List<ReadingSession>>

    @Query("DELETE FROM reading_sessions")
    suspend fun deleteAll()
}
