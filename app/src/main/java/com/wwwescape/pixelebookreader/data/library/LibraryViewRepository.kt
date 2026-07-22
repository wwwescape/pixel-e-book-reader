package com.wwwescape.pixelebookreader.data.library

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wwwescape.pixelebookreader.data.category.LibrarySortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.libraryViewDataStore by preferencesDataStore(name = "library_view")

object LibraryViewRepository {

    private val LAYOUT_KEY = stringPreferencesKey("layout")
    private val TITLE_POSITION_KEY = stringPreferencesKey("title_position")
    private val SHOW_PROGRESS_KEY = booleanPreferencesKey("show_progress")
    private val SHOW_BOOK_COUNT_KEY = booleanPreferencesKey("show_book_count")
    private val DEFAULT_SORT_ORDER_KEY = stringPreferencesKey("default_sort_order")
    private val DEFAULT_SORT_DESCENDING_KEY = booleanPreferencesKey("default_sort_descending")

    fun prefsFlow(context: Context): Flow<LibraryViewPrefs> = context.libraryViewDataStore.data.map { prefs ->
        LibraryViewPrefs(
            layout = prefs[LAYOUT_KEY].toEnumOrDefault(LibraryLayout.GRID),
            titlePosition = prefs[TITLE_POSITION_KEY].toEnumOrDefault(TitlePosition.BELOW),
            showProgress = prefs[SHOW_PROGRESS_KEY] ?: true,
            showBookCount = prefs[SHOW_BOOK_COUNT_KEY] ?: true,
            defaultSortOrder = prefs[DEFAULT_SORT_ORDER_KEY].toEnumOrDefault(LibrarySortOrder.LAST_READ),
            defaultSortDescending = prefs[DEFAULT_SORT_DESCENDING_KEY] ?: true,
        )
    }

    suspend fun setLayout(context: Context, layout: LibraryLayout) {
        context.libraryViewDataStore.edit { it[LAYOUT_KEY] = layout.name }
    }

    suspend fun setTitlePosition(context: Context, position: TitlePosition) {
        context.libraryViewDataStore.edit { it[TITLE_POSITION_KEY] = position.name }
    }

    suspend fun setShowProgress(context: Context, enabled: Boolean) {
        context.libraryViewDataStore.edit { it[SHOW_PROGRESS_KEY] = enabled }
    }

    suspend fun setShowBookCount(context: Context, enabled: Boolean) {
        context.libraryViewDataStore.edit { it[SHOW_BOOK_COUNT_KEY] = enabled }
    }

    suspend fun setDefaultSortOrder(context: Context, order: LibrarySortOrder) {
        context.libraryViewDataStore.edit { it[DEFAULT_SORT_ORDER_KEY] = order.name }
    }

    suspend fun setDefaultSortDescending(context: Context, descending: Boolean) {
        context.libraryViewDataStore.edit { it[DEFAULT_SORT_DESCENDING_KEY] = descending }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
        this?.let { name -> runCatching { enumValueOf<T>(name) }.getOrNull() } ?: default
}
