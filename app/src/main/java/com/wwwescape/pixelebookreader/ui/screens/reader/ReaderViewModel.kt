package com.wwwescape.pixelebookreader.ui.screens.reader

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.pixelebookreader.data.book.Book
import com.wwwescape.pixelebookreader.data.book.BookRepository
import com.wwwescape.pixelebookreader.data.bookmark.Bookmark
import com.wwwescape.pixelebookreader.data.bookmark.BookmarkRepository
import com.wwwescape.pixelebookreader.data.colorpreset.ColorPreset
import com.wwwescape.pixelebookreader.data.colorpreset.ColorPresetRepository
import com.wwwescape.pixelebookreader.data.highlight.Highlight
import com.wwwescape.pixelebookreader.data.highlight.HighlightRepository
import com.wwwescape.pixelebookreader.data.history.HistoryRepository
import com.wwwescape.pixelebookreader.data.parser.BookParsers
import com.wwwescape.pixelebookreader.data.parser.ReaderText
import com.wwwescape.pixelebookreader.data.reader.ReaderSettings
import com.wwwescape.pixelebookreader.data.reader.ReaderSettingsRepository
import com.wwwescape.pixelebookreader.data.stats.ReadingSessionRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MAX_CHECKPOINTS = 5

/** How many [ReaderText] items on either side of the current position to search when locating a
 * highlight's paragraph — see [ReaderViewModel.findHighlightLocation]. Wide enough to cover a
 * screen or two of nearby text (where a selection actually happened) without scanning an entire
 * long novel for a phrase that also innocuously appears far away. */
private const val HIGHLIGHT_SEARCH_WINDOW = 30

/** How much of a paragraph's text to keep as a bookmark's display snippet. */
private const val BOOKMARK_SNIPPET_LENGTH = 140

/** A session shorter than this is discarded rather than persisted — "debounced" session
 * tracking, so a quick app-switch-and-back doesn't fragment the reading-time log with noise. */
private const val MIN_SESSION_MS = 5_000L
private const val COMPLETION_PROGRESS_THRESHOLD = 0.99f

/** A scroll position saved before a "big" jump (opening the chapters drawer, restoring a
 * checkpoint) — an undo-style safety net, not a persisted history; it resets when the book is
 * closed. */
private data class Checkpoint(val index: Int, val offset: Int)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val bookId = MutableStateFlow<Int?>(null)
    private val content = MutableStateFlow<List<ReaderText>>(emptyList())
    private val isLoading = MutableStateFlow(true)
    private val isError = MutableStateFlow(false)
    private val isImmersive = MutableStateFlow(false)
    private val checkpoints = ArrayDeque<Checkpoint>()
    private val canUndoCheckpoint = MutableStateFlow(false)
    private var sessionStartedAt: Long? = null

    @Suppress("OPT_IN_USAGE")
    val book: StateFlow<Book?> = bookId.flatMapLatest { id ->
        if (id == null) flowOf(null) else BookRepository.getBook(application, id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private data class ReaderFlags(val isLoading: Boolean, val isError: Boolean, val isImmersive: Boolean, val canUndoCheckpoint: Boolean)

    private val flags = combine(isLoading, isError, isImmersive, canUndoCheckpoint) { loading, error, immersive, canUndo ->
        ReaderFlags(loading, error, immersive, canUndo)
    }

    private data class Personalization(val settings: ReaderSettings, val colorPresets: List<ColorPreset>)

    private val personalization = combine(
        ReaderSettingsRepository.settingsFlow(application),
        ColorPresetRepository.getAllPresets(application),
    ) { settings, presets -> Personalization(settings, presets) }

    private data class Annotations(val bookmarks: List<Bookmark>, val highlights: List<Highlight>)

    @Suppress("OPT_IN_USAGE")
    private val annotations: Flow<Annotations> = bookId.flatMapLatest { id ->
        if (id == null) {
            flowOf(Annotations(emptyList(), emptyList()))
        } else {
            combine(
                BookmarkRepository.getForBook(application, id),
                HighlightRepository.getForBook(application, id),
            ) { bookmarks, highlights -> Annotations(bookmarks, highlights) }
        }
    }

    val uiState: StateFlow<ReaderUiState> = combine(book, content, flags, personalization, annotations) { currentBook, currentContent, currentFlags, p, a ->
        ReaderUiState(
            book = currentBook,
            content = currentContent,
            chapters = currentContent.withIndex()
                .filter { (_, item) -> item is ReaderText.Chapter }
                .map { (index, item) -> ChapterEntry(index, item as ReaderText.Chapter) },
            isLoading = currentFlags.isLoading,
            isError = currentFlags.isError,
            isImmersive = currentFlags.isImmersive,
            canUndoCheckpoint = currentFlags.canUndoCheckpoint,
            settings = p.settings,
            colorPresets = p.colorPresets,
            bookmarks = a.bookmarks,
            highlights = a.highlights,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    /** Loads (and caches) a book's full text — a no-op if already loaded for this [id], so
     * recomposition/navigation churn doesn't re-parse. */
    fun open(id: Int) {
        if (bookId.value == id) return
        bookId.value = id
        checkpoints.clear()
        canUndoCheckpoint.value = false
        viewModelScope.launch {
            isLoading.value = true
            isError.value = false
            val currentBook = BookRepository.getBook(getApplication(), id).first()
            val textParser = currentBook?.let { BookParsers.textParserFor(it.fileName) }
            if (currentBook == null || textParser == null) {
                isError.value = true
                isLoading.value = false
                return@launch
            }
            val parsed = runCatching { textParser.parseText(getApplication(), currentBook.filePath.toUri()) }.getOrNull()
            if (parsed.isNullOrEmpty()) {
                isError.value = true
                isLoading.value = false
                return@launch
            }
            content.value = parsed
            isLoading.value = false
            HistoryRepository.recordBookOpened(getApplication(), id)
            BookRepository.updateBook(getApplication(), currentBook.copy(lastOpened = System.currentTimeMillis()))
            resumeSession()
        }
    }

    /** Starts (or continues) the current reading session's clock — called once the book has
     * loaded, and again on `ON_RESUME` after the app was backgrounded mid-read. A no-op if a
     * session is already running. */
    fun resumeSession() {
        if (sessionStartedAt == null) sessionStartedAt = System.currentTimeMillis()
    }

    /** Stops the clock and persists the session — but only if it cleared [MIN_SESSION_MS], see
     * that constant's doc. Called on `ON_PAUSE` (app backgrounded) and when leaving the Reader
     * (see `onCleared`), so both "switched apps" and "closed the book" end a session the same
     * way.
     *
     * Deliberately doesn't use [viewModelScope]: `androidx.lifecycle.ViewModel.clear()` closes
     * the scope *before* calling `onCleared()`, so a `viewModelScope.launch` invoked from there
     * (the "closed the book" path) would silently never run. This is a short, one-shot write
     * that needs to outlive the ViewModel either way. */
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
        super.onCleared()
    }

    fun toggleImmersive() {
        isImmersive.value = !isImmersive.value
    }

    /** Called before a "big" jump (opening the chapters drawer, restoring a previous
     * checkpoint) so it can be undone. */
    fun pushCheckpoint(index: Int, offset: Int) {
        if (checkpoints.size >= MAX_CHECKPOINTS) checkpoints.removeFirst()
        checkpoints.addLast(Checkpoint(index, offset))
        canUndoCheckpoint.value = true
    }

    /** Returns the position to restore to, or null if there's nothing to undo. */
    fun popCheckpoint(): Pair<Int, Int>? {
        val checkpoint = checkpoints.removeLastOrNull() ?: return null
        canUndoCheckpoint.value = checkpoints.isNotEmpty()
        return checkpoint.index to checkpoint.offset
    }

    fun saveProgress(index: Int, offset: Int) = viewModelScope.launch {
        val current = book.value ?: return@launch
        val total = content.value.size.coerceAtLeast(1)
        val progress = (index.toFloat() / total).coerceIn(0f, 1f)
        val reachedChapters = content.value.withIndex().count { (contentIndex, item) -> item is ReaderText.Chapter && contentIndex <= index }
        val chaptersRead = maxOf(current.chaptersRead, reachedChapters)
        val finishedAt = current.finishedAt ?: System.currentTimeMillis().takeIf { progress >= COMPLETION_PROGRESS_THRESHOLD }
        if (current.scrollIndex == index && current.scrollOffset == offset && chaptersRead == current.chaptersRead && finishedAt == current.finishedAt) return@launch
        BookRepository.updateBook(
            getApplication(),
            current.copy(scrollIndex = index, scrollOffset = offset, progress = progress, chaptersRead = chaptersRead, finishedAt = finishedAt),
        )
    }

    fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) = viewModelScope.launch {
        ReaderSettingsRepository.update(getApplication(), transform)
    }

    fun addColorPreset(name: String, backgroundColor: Long, fontColor: Long) = viewModelScope.launch {
        val nextOrder = (uiState.value.colorPresets.maxOfOrNull { it.order } ?: -1) + 1
        val id = ColorPresetRepository.addPreset(getApplication(), ColorPreset(name = name, backgroundColor = backgroundColor, fontColor = fontColor, order = nextOrder))
        ReaderSettingsRepository.update(getApplication()) { it.copy(selectedColorPresetId = id.toInt()) }
    }

    fun deleteColorPreset(preset: ColorPreset) = viewModelScope.launch {
        ColorPresetRepository.deletePreset(getApplication(), preset)
        if (uiState.value.settings.selectedColorPresetId == preset.id) {
            ReaderSettingsRepository.update(getApplication()) { it.copy(selectedColorPresetId = null) }
        }
    }

    fun selectColorPreset(id: Int?) = updateSettings { it.copy(selectedColorPresetId = id) }

    fun moveColorPresetUp(preset: ColorPreset) = swapColorPresetOrder(preset, -1)

    fun moveColorPresetDown(preset: ColorPreset) = swapColorPresetOrder(preset, 1)

    private fun swapColorPresetOrder(preset: ColorPreset, delta: Int) = viewModelScope.launch {
        val list = uiState.value.colorPresets
        val index = list.indexOfFirst { it.id == preset.id }
        val targetIndex = index + delta
        if (index < 0 || targetIndex < 0 || targetIndex >= list.size) return@launch
        val other = list[targetIndex]
        ColorPresetRepository.updatePreset(getApplication(), preset.copy(order = other.order))
        ColorPresetRepository.updatePreset(getApplication(), other.copy(order = preset.order))
    }

    /** Cycles to the next reading color preset in order — the "fast switch" behavior, bound to
     * a top-bar action when [ReaderSettings.fastColorPresetSwitch] is on. */
    fun cycleColorPreset() {
        val presets = uiState.value.colorPresets
        if (presets.isEmpty()) return
        val currentId = uiState.value.settings.selectedColorPresetId
        val currentIndex = presets.indexOfFirst { it.id == currentId }
        val next = presets[(currentIndex + 1).mod(presets.size)]
        selectColorPreset(next.id)
    }

    /** Toggles a bookmark at [contentIndex] — removes it if one already exists there exactly,
     * otherwise adds one, capturing a display snippet at save time (see [snippetFor]). */
    fun toggleBookmarkAtCurrentPosition(contentIndex: Int) = viewModelScope.launch {
        val id = bookId.value ?: return@launch
        val existing = uiState.value.bookmarks.find { it.contentIndex == contentIndex }
        if (existing != null) {
            BookmarkRepository.deleteBookmark(getApplication(), existing)
        } else {
            BookmarkRepository.addBookmark(
                getApplication(),
                Bookmark(bookId = id, contentIndex = contentIndex, snippet = snippetFor(contentIndex)),
            )
        }
    }

    fun deleteBookmark(bookmark: Bookmark) = viewModelScope.launch {
        BookmarkRepository.deleteBookmark(getApplication(), bookmark)
    }

    /** Finds the paragraph the currently selected [selectedText] came from and saves a highlight
     * there in [color]. Returns `false` (and saves nothing) when the text can't be unambiguously
     * located — see [findHighlightLocation]. */
    fun addHighlight(fromContentIndex: Int, selectedText: String, color: Long): Boolean {
        val id = bookId.value ?: return false
        val (index, startOffset, endOffset) = findHighlightLocation(fromContentIndex, selectedText) ?: return false
        viewModelScope.launch {
            HighlightRepository.addHighlight(
                getApplication(),
                Highlight(bookId = id, contentIndex = index, startOffset = startOffset, endOffset = endOffset, text = selectedText, color = color),
            )
        }
        return true
    }

    fun deleteHighlight(highlight: Highlight) = viewModelScope.launch {
        HighlightRepository.deleteHighlight(getApplication(), highlight)
    }

    /** Searches a window of paragraphs around [fromContentIndex] (see [HIGHLIGHT_SEARCH_WINDOW])
     * for a single unambiguous occurrence of [selectedText], returning its content index and
     * character offsets. Returns null if the text isn't found, or is found more than once —
     * either in more than one paragraph, or more than once within the same paragraph — since
     * either case means the highlight's true location can't be determined reliably (see
     * `Highlight`'s doc for why this search-based approach is needed at all). */
    private fun findHighlightLocation(fromContentIndex: Int, selectedText: String): Triple<Int, Int, Int>? {
        if (selectedText.isEmpty()) return null
        val list = content.value
        val start = (fromContentIndex - HIGHLIGHT_SEARCH_WINDOW).coerceAtLeast(0)
        val end = (fromContentIndex + HIGHLIGHT_SEARCH_WINDOW).coerceAtMost(list.size - 1)
        var found: Triple<Int, Int, Int>? = null
        for (index in start..end) {
            val item = list[index] as? ReaderText.Text ?: continue
            val offset = item.line.indexOf(selectedText)
            if (offset < 0) continue
            val hasAnotherOccurrence = item.line.indexOf(selectedText, offset + 1) >= 0
            if (hasAnotherOccurrence || found != null) return null
            found = Triple(index, offset, offset + selectedText.length)
        }
        return found
    }

    /** The display text shown for a bookmark in the Bookmarks tab — the first non-blank
     * paragraph at or after [contentIndex], truncated, falling back to a chapter title if
     * [contentIndex] itself lands on one. */
    private fun snippetFor(contentIndex: Int): String {
        val list = content.value
        for (index in contentIndex until list.size) {
            when (val item = list[index]) {
                is ReaderText.Text -> if (item.line.isNotBlank()) return item.line.take(BOOKMARK_SNIPPET_LENGTH)
                is ReaderText.Chapter -> return item.title
                else -> Unit
            }
        }
        return ""
    }
}
