package com.wwwescape.pixelebookreader.data.colorpreset

import android.content.Context
import com.wwwescape.pixelebookreader.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow

object ColorPresetRepository {
    fun getAllPresets(context: Context): Flow<List<ColorPreset>> =
        AppDatabase.getInstance(context).colorPresetDao().getAll()

    suspend fun addPreset(context: Context, preset: ColorPreset): Long =
        AppDatabase.getInstance(context).colorPresetDao().insert(preset)

    suspend fun updatePreset(context: Context, preset: ColorPreset) =
        AppDatabase.getInstance(context).colorPresetDao().update(preset)

    suspend fun deletePreset(context: Context, preset: ColorPreset) =
        AppDatabase.getInstance(context).colorPresetDao().delete(preset)

    suspend fun deleteAllPresets(context: Context) =
        AppDatabase.getInstance(context).colorPresetDao().deleteAll()
}
