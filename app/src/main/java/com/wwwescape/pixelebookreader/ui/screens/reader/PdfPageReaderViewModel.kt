package com.wwwescape.pixelebookreader.ui.screens.reader

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.pixelebookreader.data.book.Book
import com.wwwescape.pixelebookreader.data.book.BookRepository
import com.wwwescape.pixelebookreader.data.bookmark.Bookmark
import com.wwwescape.pixelebookreader.data.bookmark.BookmarkRepository
import com.wwwescape.pixelebookreader.data.history.HistoryRepository
import com.wwwescape.pixelebookreader.data.parser.pdf.PdfPageSource
import com.wwwescape.pixelebookreader.data.reader.ReaderSettings
import com.wwwescape.pixelebookreader.data.reader.ReaderSettingsRepository
import com.wwwescape.pixelebookreader.data.stats.ReadingSessionRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Same debounce reasoning as [ReaderViewModel.MIN_SESSION_MS] — a quick app-switch-and-back
 * shouldn't fragment the reading-time log with noise. */
private const val MIN_SESSION_MS = 5_000L
private const val COMPLETION_PROGRESS_THRESHOLD = 0.99f

/** Drives the PDF page-mode reader ([PdfPageReaderScreen]) — a sibling to [ReaderViewModel], not
 * a subclass, since page-based navigation (current page, page count, an outline keyed by page)
 * has no `List<ReaderText>`/`contentIndex` to share with the continuous-scroll reader. Progress
 * and bookmarks still reuse `Book.scrollIndex`/`Bookmark.contentIndex` as a plain "page number"
 * though — that's a safe, migration-free reuse. */
class PdfPageReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val bookId = MutableStateFlow<Int?>(null)
    private val isLoading = MutableStateFlow(true)
    private val isError = MutableStateFlow(false)
    private val isImmersive = MutableStateFlow(false)
    private val pageSourceState = MutableStateFlow<PdfPageSource?>(null)
    private var sessionStartedAt: Long? = null

    @Suppress("OPT_IN_USAGE")
    val book: StateFlow<Book?> = bookId.flatMapLatest { id ->
        if (id == null) flowOf(null) else BookRepository.getBook(application, id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @Suppress("OPT_IN_USAGE")
    private val bookmarks: StateFlow<List<Bookmark>> = bookId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else BookmarkRepository.getForBook(application, id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private data class Flags(val isLoading: Boolean, val isError: Boolean, val isImmersive: Boolean)
    private val flags = combine(isLoading, isError, isImmersive) { l, e, i -> Flags(l, e, i) }

    private val settings = ReaderSettingsRepository.settingsFlow(application)

    val uiState: StateFlow<PdfPageReaderUiState> = combine(book, flags, settings, bookmarks, pageSourceState) { currentBook, f, s, bm, source ->
        PdfPageReaderUiState(
            book = currentBook,
            pageCount = source?.pageCount ?: 0,
            outline = source?.outline ?: emptyList(),
            isLoading = f.isLoading,
            isError = f.isError,
            isImmersive = f.isImmersive,
            settings = s,
            bookmarks = bm,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PdfPageReaderUiState())

    /** Loads (and caches) a PDF's page source — a no-op if already loaded for this [id]. */
    fun open(id: Int) {
        if (bookId.value == id) return
        bookId.value = id
        viewModelScope.launch {
            isLoading.value = true
            isError.value = false
            val currentBook = BookRepository.getBook(getApplication(), id).first()
            if (currentBook == null) {
                isError.value = true
                isLoading.value = false
                return@launch
            }
            val source = PdfPageSource.open(getApplication(), currentBook.filePath.toUri())
            if (source == null) {
                isError.value = true
                isLoading.value = false
                return@launch
            }
            pageSourceState.value = source
            isLoading.value = false
            HistoryRepository.recordBookOpened(getApplication(), id)
            BookRepository.updateBook(getApplication(), currentBook.copy(lastOpened = System.currentTimeMillis()))
            resumeSession()
        }
    }

    /** Renders one page on demand — see [PdfPageSource.renderPage] for the scale-to-screen-width
     * reasoning. Returns null if the page source isn't ready yet. */
    suspend fun renderPage(index: Int, targetWidthPx: Int) = pageSourceState.value?.renderPage(index, targetWidthPx)

    fun resumeSession() {
        if (sessionStartedAt == null) sessionStartedAt = System.currentTimeMillis()
    }

    /** Deliberately doesn't use [viewModelScope] — see [ReaderViewModel.pauseSession]'s doc for
     * why (`onCleared()` closes the scope before running, so a `viewModelScope.launch` there
     * would silently never execute). */
    @OptIn(DelicateCoroutinesApi::class)
    fun pauseSession() {
        val startedAt = sessionStartedAt ?: return
        sessionStartedAt = null
        val id = bookId.value ?: return
        val duration = System.currentTimeMillis() - startedAt
        if (duration < MIN_SESSION_MS) return
        val context = getApplication<Application>()
        GlobalScope.launch(Dispatchers.IO) { ReadingSessionRepository.recordSession(context, id, startedAt, duration) }
    }

    override fun onCleared() {
        pauseSession()
        pageSourceState.value?.close()
        super.onCleared()
    }

    fun toggleImmersive() {
        isImmersive.value = !isImmersive.value
    }

    fun saveProgress(currentPage: Int) = viewModelScope.launch {
        val current = book.value ?: return@launch
        val total = uiState.value.pageCount.coerceAtLeast(1)
        val progress = (currentPage.toFloat() / total).coerceIn(0f, 1f)
        val reachedChapters = uiState.value.outline.count { it.pageIndex <= currentPage }
        val chaptersRead = maxOf(current.chaptersRead, reachedChapters)
        val finishedAt = current.finishedAt ?: System.currentTimeMillis().takeIf { progress >= COMPLETION_PROGRESS_THRESHOLD }
        if (current.scrollIndex == currentPage && chaptersRead == current.chaptersRead && finishedAt == current.finishedAt) return@launch
        BookRepository.updateBook(
            getApplication(),
            current.copy(scrollIndex = currentPage, scrollOffset = 0, progress = progress, chaptersRead = chaptersRead, finishedAt = finishedAt),
        )
    }

    fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) = viewModelScope.launch {
        ReaderSettingsRepository.update(getApplication(), transform)
    }

    /** Toggles a bookmark at [page] — same "exists exactly there? delete : add" logic as
     * [ReaderViewModel.toggleBookmarkAtCurrentPosition], just keyed on a page number instead of
     * a content index, and with a "Page N" snippet since there's no extracted text to show. */
    fun toggleBookmarkAtCurrentPosition(page: Int, snippet: String) = viewModelScope.launch {
        val id = bookId.value ?: return@launch
        val existing = uiState.value.bookmarks.find { it.contentIndex == page }
        if (existing != null) {
            BookmarkRepository.deleteBookmark(getApplication(), existing)
        } else {
            BookmarkRepository.addBookmark(getApplication(), Bookmark(bookId = id, contentIndex = page, snippet = snippet))
        }
    }

    fun deleteBookmark(bookmark: Bookmark) = viewModelScope.launch {
        BookmarkRepository.deleteBookmark(getApplication(), bookmark)
    }
}
