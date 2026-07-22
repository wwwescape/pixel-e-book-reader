package com.wwwescape.pixelebookreader.data.book

import android.content.Context
import android.net.Uri
import com.wwwescape.pixelebookreader.data.parser.BookParsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Import is metadata + cover only — full text parsing (the expensive part) happens lazily
 * when a book is actually opened in the Reader, matching the FileParser/CoverParser/
 * TextParser role split. */
object BookImporter {
    suspend fun importBook(context: Context, uri: Uri, displayName: String): Result<Book> = withContext(Dispatchers.IO) {
        val fileParser = BookParsers.fileParserFor(displayName)
            ?: return@withContext Result.failure(IllegalArgumentException("Unsupported file: $displayName"))
        val coverParser = BookParsers.coverParserFor(displayName)

        val metadata = runCatching { fileParser.parse(context, uri) }.getOrElse {
            return@withContext Result.failure(it)
        }
        val coverPath = runCatching { coverParser?.parseCover(context, uri) }.getOrNull()?.let { CoverStorage.save(context, it) }

        val book = Book(
            title = metadata.title,
            author = metadata.author,
            description = metadata.description,
            filePath = uri.toString(),
            fileName = displayName,
            coverImagePath = coverPath,
            series = metadata.series,
            seriesNumber = metadata.seriesNumber,
            tags = metadata.tags,
            isbn = metadata.isbn,
            publishDate = metadata.publishDate,
            language = metadata.language,
        )
        val id = BookRepository.addBook(context, book)
        Result.success(book.copy(id = id.toInt()))
    }
}
