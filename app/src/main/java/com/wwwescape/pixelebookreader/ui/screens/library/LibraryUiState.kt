package com.wwwescape.pixelebookreader.ui.screens.library

import com.wwwescape.pixelebookreader.data.book.Book
import com.wwwescape.pixelebookreader.data.category.Category
import com.wwwescape.pixelebookreader.data.category.LibrarySortOrder
import com.wwwescape.pixelebookreader.data.library.LibraryLayout
import com.wwwescape.pixelebookreader.data.library.TitlePosition

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Int? = null,
    val searchQuery: String = "",
    val layout: LibraryLayout = LibraryLayout.GRID,
    val titlePosition: TitlePosition = TitlePosition.BELOW,
    val showProgress: Boolean = true,
    val showBookCount: Boolean = true,
    val sortOrder: LibrarySortOrder = LibrarySortOrder.LAST_READ,
    val sortDescending: Boolean = true,
    val selectedBookIds: Set<Int> = emptySet(),
    val allTags: List<String> = emptyList(),
    val selectedTag: String? = null,
    val minRating: Float? = null,
) {
    val isSelectionMode: Boolean get() = selectedBookIds.isNotEmpty()
}
