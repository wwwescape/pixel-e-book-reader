package com.wwwescape.pixelebookreader.data.colorpreset

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ColorPresetDao {
    @Insert
    suspend fun insert(preset: ColorPreset): Long

    @Update
    suspend fun update(preset: ColorPreset)

    @Delete
    suspend fun delete(preset: ColorPreset)

    @Query("SELECT * FROM color_presets ORDER BY `order` ASC")
    fun getAll(): Flow<List<ColorPreset>>

    @Query("DELETE FROM color_presets")
    suspend fun deleteAll()
}
