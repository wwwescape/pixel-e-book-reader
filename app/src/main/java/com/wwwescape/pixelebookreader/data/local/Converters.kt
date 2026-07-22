package com.wwwescape.pixelebookreader.data.local

import androidx.room.TypeConverter
import com.wwwescape.pixelebookreader.data.category.LibrarySortOrder

// Unit separator (U+001F): can't appear in an Int's string form, so no escaping is needed.
private const val SEPARATOR = ''

/** Category IDs are stored as a delimiter-joined string — simple and dependency-free, matching
 * the app family's preference for hand-rolled persistence over pulling in a JSON library for a
 * single list field. */
class IntListConverter {
    @TypeConverter
    fun fromList(value: List<Int>): String = value.joinToString(SEPARATOR.toString())

    @TypeConverter
    fun toList(value: String): List<Int> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR).mapNotNull { it.toIntOrNull() }
}

/** Tags are free-form user/metadata strings (see [com.wwwescape.pixelebookreader.data.book.Book.tags]),
 * stored the same delimiter-joined way as [IntListConverter] for the same reason — no JSON
 * dependency needed for a single list field. */
class StringListConverter {
    @TypeConverter
    fun fromList(value: List<String>): String = value.joinToString(SEPARATOR.toString())

    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR).filter { it.isNotEmpty() }
}

class LibrarySortOrderConverter {
    @TypeConverter
    fun fromSortOrder(value: LibrarySortOrder): String = value.name

    @TypeConverter
    fun toSortOrder(value: String): LibrarySortOrder =
        runCatching { LibrarySortOrder.valueOf(value) }.getOrDefault(LibrarySortOrder.LAST_READ)
}
