package com.wwwescape.pixelebookreader.ui.screens.bookinfo

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.pixelebookreader.data.book.Book
import com.wwwescape.pixelebookreader.data.book.BookRepository
import com.wwwescape.pixelebookreader.data.book.CoverStorage
import com.wwwescape.pixelebookreader.data.category.Category
import com.wwwescape.pixelebookreader.data.category.CategoryRepository
import com.wwwescape.pixelebookreader.data.filesystem.FileSystemRepository
import com.wwwescape.pixelebookreader.data.parser.BookParsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val bookId = MutableStateFlow<Int?>(null)
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    @Suppress("OPT_IN_USAGE")
    val book: StateFlow<Book?> = bookId.flatMapLatest { id ->
        if (id == null) flowOf(null) else BookRepository.getBook(application, id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val categories: StateFlow<List<Category>> = CategoryRepository.getAllCategories(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setBookId(id: Int) {
        bookId.value = id
    }

    fun save(
        title: String,
        author: String,
        description: String,
        categoryIds: Set<Int>,
        series: String,
        seriesNumber: String,
        tags: List<String>,
        rating: Float?,
        isbn: String,
        publishDate: String,
        language: String,
    ) = viewModelScope.launch {
        val current = book.value ?: return@launch
        BookRepository.updateBook(
            getApplication(),
            current.copy(
                title = title.trim().ifEmpty { current.title },
                author = author.trim().ifEmpty { null },
                description = description.trim().ifEmpty { null },
                categories = categoryIds.toList(),
                series = series.trim().ifEmpty { null },
                seriesNumber = seriesNumber.trim().toFloatOrNull(),
                tags = tags,
                rating = rating,
                isbn = isbn.trim().ifEmpty { null },
                publishDate = publishDate.trim().ifEmpty { null },
                language = language.trim().ifEmpty { null },
            ),
        )
    }

    fun relink(uri: Uri, displayName: String) = viewModelScope.launch {
        val current = book.value ?: return@launch
        FileSystemRepository.persistPermission(getApplication(), uri)
        BookRepository.updateBook(getApplication(), current.copy(filePath = uri.toString(), fileName = displayName))
    }

    fun replaceCover(bytes: ByteArray) = viewModelScope.launch {
        val current = book.value ?: return@launch
        CoverStorage.delete(current.coverImagePath)
        val path = CoverStorage.save(getApplication(), bytes)
        BookRepository.updateBook(getApplication(), current.copy(coverImagePath = path))
    }

    fun resetCover() = viewModelScope.launch {
        val current = book.value ?: return@launch
        val coverParser = BookParsers.coverParserFor(current.fileName) ?: return@launch
        val bytes = withContext(Dispatchers.IO) {
            runCatching { coverParser.parseCover(getApplication(), current.filePath.toUri()) }.getOrNull()
        } ?: return@launch
        CoverStorage.delete(current.coverImagePath)
        val path = CoverStorage.save(getApplication(), bytes)
        BookRepository.updateBook(getApplication(), current.copy(coverImagePath = path))
    }

    fun deleteCover() = viewModelScope.launch {
        val current = book.value ?: return@launch
        CoverStorage.delete(current.coverImagePath)
        BookRepository.updateBook(getApplication(), current.copy(coverImagePath = null))
    }

    fun deleteBook() = viewModelScope.launch {
        val current = book.value ?: return@launch
        CoverStorage.delete(current.coverImagePath)
        BookRepository.deleteBook(getApplication(), current)
        _deleted.value = true
    }
}
