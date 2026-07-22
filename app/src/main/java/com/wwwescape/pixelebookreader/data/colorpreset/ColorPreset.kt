package com.wwwescape.pixelebookreader.data.colorpreset

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A reading-surface background+font color pair — independent of the app's own Material 3
 * theme, e.g. a sepia/paper look for the
 * reader specifically. [order] drives "fast switch" cycling through the list;
 * [backgroundColor]/[fontColor] are packed ARGB, matching how Room already stores other simple
 * value types with no need for a dedicated converter. */
@Entity(tableName = "color_presets")
data class ColorPreset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val backgroundColor: Long,
    val fontColor: Long,
    val order: Int = 0,
)
