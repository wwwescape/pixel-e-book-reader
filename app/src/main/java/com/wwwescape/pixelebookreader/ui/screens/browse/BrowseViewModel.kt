package com.wwwescape.pixelebookreader.ui.screens.browse

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.pixelebookreader.data.book.BookImporter
import com.wwwescape.pixelebookreader.data.browse.BrowseSource
import com.wwwescape.pixelebookreader.data.browse.BrowseSourceRepository
import com.wwwescape.pixelebookreader.data.filesystem.BrowsableFile
import com.wwwescape.pixelebookreader.data.filesystem.FileSystemRepository
import com.wwwescape.pixelebookreader.data.parser.BookParsers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class BrowseFilters(
    val query: String = "",
    val sortOrder: BrowseSortOrder = BrowseSortOrder.NAME,
    val sortDescending: Boolean = false,
    val layout: BrowseLayout = BrowseLayout.LIST,
)

/** [navStack] holds the path from a source's root to the current folder — empty means "show
 * the sources list" (Browse's root). Not saved across process death; re-picking a source is
 * cheap since the SAF grant itself is already persisted. */
class BrowseViewModel(application: Application) : AndroidViewModel(application) {

    private val navStack = MutableStateFlow<List<DocumentFile>>(emptyList())
    private val filters = MutableStateFlow(BrowseFilters())
    private val _importingFileName = MutableStateFlow<String?>(null)
    private val _importResult = MutableStateFlow<ImportOutcome?>(null)
    val importResult: StateFlow<ImportOutcome?> = _importResult.asStateFlow()

    val uiState: StateFlow<BrowseUiState> = combine(
        BrowseSourceRepository.sourcesFlow(application),
        navStack,
        filters,
        _importingFileName,
    ) { sources, stack, currentFilters, importingFileName ->
        val currentDir = stack.lastOrNull()
        val rawEntries = currentDir?.let { FileSystemRepository.listChildren(it) }.orEmpty()
        val visible = rawEntries
            .filter { it.isDirectory || BookParsers.isSupported(it.name) }
            .filter { currentFilters.query.isBlank() || it.name.contains(currentFilters.query, ignoreCase = true) }
            .let { sortEntries(it, currentFilters.sortOrder, currentFilters.sortDescending) }

        BrowseUiState(
            sources = sources,
            breadcrumbs = stack.mapNotNull { it.name },
            entries = visible,
            atRoot = stack.isEmpty(),
            query = currentFilters.query,
            sortOrder = currentFilters.sortOrder,
            sortDescending = currentFilters.sortDescending,
            layout = currentFilters.layout,
            importingFileName = importingFileName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseUiState())

    /** Folders always precede files; within each group, the chosen field/direction applies
     * uniformly. */
    private fun sortEntries(entries: List<BrowsableFile>, order: BrowseSortOrder, descending: Boolean): List<BrowsableFile> {
        val comparator = when (order) {
            BrowseSortOrder.NAME -> compareBy<BrowsableFile> { it.name.lowercase() }
            BrowseSortOrder.DATE -> compareBy { it.lastModified }
            BrowseSortOrder.SIZE -> compareBy { it.size }
        }
        val directed = if (descending) comparator.reversed() else comparator
        val (folders, files) = entries.partition { it.isDirectory }
        return folders.sortedWith(directed) + files.sortedWith(directed)
    }

    fun addSource(treeUri: Uri) = viewModelScope.launch { BrowseSourceRepository.addSource(getApplication(), treeUri) }

    fun removeSource(source: BrowseSource) = viewModelScope.launch { BrowseSourceRepository.removeSource(getApplication(), source) }

    fun openSource(source: BrowseSource) {
        val root = FileSystemRepository.rootDocument(getApplication(), source.uri) ?: return
        navStack.value = listOf(root)
    }

    fun openFolder(entry: BrowsableFile) {
        val doc = FileSystemRepository.documentFor(getApplication(), entry.uri) ?: return
        navStack.value = navStack.value + doc
    }

    /** True if consumed (popped a folder or returned to the sources list); false if Browse was
     * already at its root, so the caller should let the platform back gesture proceed. */
    fun navigateUp(): Boolean {
        if (navStack.value.isEmpty()) return false
        navStack.value = navStack.value.dropLast(1)
        return true
    }

    fun setQuery(query: String) {
        filters.value = filters.value.copy(query = query)
    }

    fun setSortOrder(order: BrowseSortOrder) {
        filters.value = filters.value.copy(sortOrder = order)
    }

    fun toggleSortDescending() {
        filters.value = filters.value.copy(sortDescending = !filters.value.sortDescending)
    }

    fun setLayout(layout: BrowseLayout) {
        filters.value = filters.value.copy(layout = layout)
    }

    fun importFile(entry: BrowsableFile) {
        viewModelScope.launch {
            _importingFileName.value = entry.name
            val result = BookImporter.importBook(getApplication(), entry.uri, entry.name)
            _importingFileName.value = null
            _importResult.value = result.fold(
                onSuccess = { ImportOutcome.Success(it.id, it.title) },
                onFailure = { ImportOutcome.Failure(entry.name) },
            )
        }
    }

    /** For a loose single file added via [androidx.activity.result.contract.ActivityResultContracts.OpenDocument]
     * rather than browsed from an already-granted source tree — needs its own persistable
     * permission (see [FileSystemRepository.persistPermission]). */
    fun importLooseFile(uri: Uri, displayName: String) {
        viewModelScope.launch {
            _importingFileName.value = displayName
            FileSystemRepository.persistPermission(getApplication(), uri)
            val result = BookImporter.importBook(getApplication(), uri, displayName)
            _importingFileName.value = null
            _importResult.value = result.fold(
                onSuccess = { ImportOutcome.Success(it.id, it.title) },
                onFailure = { ImportOutcome.Failure(displayName) },
            )
        }
    }

    fun consumeImportResult() {
        _importResult.value = null
    }
}
