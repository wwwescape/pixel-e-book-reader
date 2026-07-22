package com.wwwescape.pixelebookreader.data.category

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LibrarySortOrder { TITLE, AUTHOR, LAST_READ, DATE_ADDED, SERIES, RATING }

/** [order] is the user's manual reorder position among categories (see the up/down controls
 * on the Manage Categories screen — this skips full drag-and-drop to avoid a new dependency
 * for what's otherwise a rarely-reordered list). [sortOrder]/[sortDescending] override the
 * Library screen's default book sort whenever this category's tab is selected. */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val order: Int = 0,
    val sortOrder: LibrarySortOrder = LibrarySortOrder.LAST_READ,
    val sortDescending: Boolean = true,
)
