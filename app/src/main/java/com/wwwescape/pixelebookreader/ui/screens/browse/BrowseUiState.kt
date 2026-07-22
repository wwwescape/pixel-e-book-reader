package com.wwwescape.pixelebookreader.ui.screens.browse

import com.wwwescape.pixelebookreader.data.browse.BrowseSource
import com.wwwescape.pixelebookreader.data.filesystem.BrowsableFile

enum class BrowseSortOrder { NAME, DATE, SIZE }
enum class BrowseLayout { LIST, GRID }

sealed class ImportOutcome {
    data class Success(val bookId: Int, val title: String) : ImportOutcome()
    data class Failure(val fileName: String) : ImportOutcome()
}

data class BrowseUiState(
    val sources: List<BrowseSource> = emptyList(),
    val breadcrumbs: List<String> = emptyList(),
    val entries: List<BrowsableFile> = emptyList(),
    val atRoot: Boolean = true,
    val query: String = "",
    val sortOrder: BrowseSortOrder = BrowseSortOrder.NAME,
    val sortDescending: Boolean = false,
    val layout: BrowseLayout = BrowseLayout.LIST,
    val importingFileName: String? = null,
)
