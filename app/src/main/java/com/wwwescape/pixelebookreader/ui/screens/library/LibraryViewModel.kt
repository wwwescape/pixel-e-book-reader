package com.wwwescape.pixelebookreader.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.pixelebookreader.data.book.Book
import com.wwwescape.pixelebookreader.data.book.BookRepository
import com.wwwescape.pixelebookreader.data.book.CoverStorage
import com.wwwescape.pixelebookreader.data.category.CategoryRepository
import com.wwwescape.pixelebookreader.data.category.LibrarySortOrder
import com.wwwescape.pixelebookreader.data.library.LibraryLayout
import com.wwwescape.pixelebookreader.data.library.LibraryViewRepository
import com.wwwescape.pixelebookreader.data.library.TitlePosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class LibrarySelection(
    val categoryId: Int?,
    val query: String,
    val selectedIds: Set<Int>,
    val tag: String?,
    val minRating: Float?,
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val selectedCategoryId = MutableStateFlow<Int?>(null)
    private val searchQuery = MutableStateFlow("")
    private val selectedBookIds = MutableStateFlow<Set<Int>>(emptySet())
    private val selectedTag = MutableStateFlow<String?>(null)
    private val minRating = MutableStateFlow<Float?>(null)

    private val selection = combine(selectedCategoryId, searchQuery, selectedBookIds, selectedTag, minRating) { categoryId, query, ids, tag, rating ->
        LibrarySelection(categoryId, query, ids, tag, rating)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        BookRepository.getAllBooks(application),
        CategoryRepository.getAllCategories(application),
        LibraryViewRepository.prefsFlow(application),
        selection,
    ) { books, categories, prefs, sel ->
        val category = categories.find { it.id == sel.categoryId }
        val filtered = books
            .filter { sel.categoryId == null || sel.categoryId in it.categories }
            .filter { sel.query.isBlank() || it.title.contains(sel.query, ignoreCase = true) || it.author?.contains(sel.query, ignoreCase = true) == true }
            .filter { sel.tag == null || sel.tag in it.tags }
            .filter { sel.minRating == null || (it.rating ?: 0f) >= sel.minRating }
        val sortOrder = category?.sortOrder ?: prefs.defaultSortOrder
        val sortDescending = category?.sortDescending ?: prefs.defaultSortDescending

        LibraryUiState(
            books = sortBooks(filtered, sortOrder, sortDescending),
            categories = categories,
            selectedCategoryId = sel.categoryId,
            searchQuery = sel.query,
            layout = prefs.layout,
            titlePosition = prefs.titlePosition,
            showProgress = prefs.showProgress,
            showBookCount = prefs.showBookCount,
            sortOrder = sortOrder,
            sortDescending = sortDescending,
            selectedBookIds = sel.selectedIds,
            allTags = books.flatMap { it.tags }.distinct().sorted(),
            selectedTag = sel.tag,
            minRating = sel.minRating,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    private fun sortBooks(books: List<Book>, order: LibrarySortOrder, descending: Boolean): List<Book> {
        val comparator = when (order) {
            LibrarySortOrder.TITLE -> compareBy<Book> { it.title.lowercase() }
            LibrarySortOrder.AUTHOR -> compareBy { it.author?.lowercase() ?: "" }
            LibrarySortOrder.LAST_READ -> compareBy { it.lastOpened ?: 0L }
            LibrarySortOrder.DATE_ADDED -> compareBy { it.id }
            LibrarySortOrder.SERIES -> compareBy<Book> { it.series?.lowercase() ?: "" }.thenBy { it.seriesNumber ?: 0f }
            LibrarySortOrder.RATING -> compareBy { it.rating ?: 0f }
        }
        return books.sortedWith(if (descending) comparator.reversed() else comparator)
    }

    fun setSelectedTag(tag: String?) {
        selectedTag.value = if (selectedTag.value == tag) null else tag
    }

    fun setMinRating(rating: Float?) {
        minRating.value = if (minRating.value == rating) null else rating
    }

    fun selectCategory(id: Int?) {
        selectedCategoryId.value = id
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleBookSelection(bookId: Int) {
        selectedBookIds.value = selectedBookIds.value.let { if (bookId in it) it - bookId else it + bookId }
    }

    fun clearSelection() {
        selectedBookIds.value = emptySet()
    }

    fun deleteSelected() = viewModelScope.launch {
        val books = uiState.value.books.filter { it.id in selectedBookIds.value }
        books.forEach { book ->
            CoverStorage.delete(book.coverImagePath)
            BookRepository.deleteBook(getApplication(), book)
        }
        clearSelection()
    }

    fun addSelectedToCategory(categoryId: Int) = viewModelScope.launch {
        val books = uiState.value.books.filter { it.id in selectedBookIds.value }
        books.forEach { book ->
            if (categoryId !in book.categories) {
                BookRepository.updateBook(getApplication(), book.copy(categories = book.categories + categoryId))
            }
        }
        clearSelection()
    }

    fun setLayout(layout: LibraryLayout) = viewModelScope.launch { LibraryViewRepository.setLayout(getApplication(), layout) }

    fun setTitlePosition(position: TitlePosition) = viewModelScope.launch { LibraryViewRepository.setTitlePosition(getApplication(), position) }

    fun setShowProgress(enabled: Boolean) = viewModelScope.launch { LibraryViewRepository.setShowProgress(getApplication(), enabled) }

    fun setShowBookCount(enabled: Boolean) = viewModelScope.launch { LibraryViewRepository.setShowBookCount(getApplication(), enabled) }

    /** Updates the *effective* sort — the currently selected category's override if one is
     * selected, otherwise the library-wide default (a per-category sort
     * override). */
    fun setSortOrder(order: LibrarySortOrder) = viewModelScope.launch {
        val categoryId = selectedCategoryId.value
        if (categoryId == null) {
            LibraryViewRepository.setDefaultSortOrder(getApplication(), order)
        } else {
            uiState.value.categories.find { it.id == categoryId }?.let {
                CategoryRepository.updateCategory(getApplication(), it.copy(sortOrder = order))
            }
        }
    }

    fun toggleSortDescending() = viewModelScope.launch {
        val categoryId = selectedCategoryId.value
        if (categoryId == null) {
            LibraryViewRepository.setDefaultSortDescending(getApplication(), !uiState.value.sortDescending)
        } else {
            uiState.value.categories.find { it.id == categoryId }?.let {
                CategoryRepository.updateCategory(getApplication(), it.copy(sortDescending = !it.sortDescending))
            }
        }
    }
}
