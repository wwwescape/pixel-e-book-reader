package com.wwwescape.pixelebookreader.data.parser.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.wwwescape.pixelebookreader.data.parser.CoverParser
import com.wwwescape.pixelebookreader.data.parser.FileParser
import com.wwwescape.pixelebookreader.data.parser.ParsedFileMetadata
import com.wwwescape.pixelebookreader.data.parser.ReaderText
import com.wwwescape.pixelebookreader.data.parser.TextParser
import com.wwwescape.pixelebookreader.data.parser.copyToCacheFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private val pdfBoxInitialized = AtomicBoolean(false)

/** PDFBox-Android needs its font/encoding resources loaded once per process before any
 * [PDDocument] operation — safe to call repeatedly, but only actually does the work once. */
private fun ensurePdfBoxInitialized(context: Context) {
    if (pdfBoxInitialized.compareAndSet(false, true)) {
        PDFBoxResourceLoader.init(context.applicationContext)
    }
}

class PdfFileParser : FileParser {
    override fun parse(context: Context, uri: Uri): ParsedFileMetadata {
        ensurePdfBoxInitialized(context)
        val temp = copyToCacheFile(context, uri, "pdf_", ".pdf")
            ?: return ParsedFileMetadata(title = uri.lastPathSegment ?: "Untitled")
        try {
            PDDocument.load(temp).use { document ->
                val info = document.documentInformation
                return ParsedFileMetadata(
                    title = info?.title?.trim().takeUnless { it.isNullOrEmpty() } ?: (uri.lastPathSegment ?: "Untitled"),
                    author = info?.author?.trim()?.ifEmpty { null },
                )
            }
        } finally {
            temp.delete()
        }
    }
}

class PdfCoverParser : CoverParser {
    override fun parseCover(context: Context, uri: Uri): ByteArray? {
        ensurePdfBoxInitialized(context)
        val temp = copyToCacheFile(context, uri, "pdf_", ".pdf") ?: return null
        try {
            PDDocument.load(temp).use { document ->
                if (document.numberOfPages == 0) return null
                val bitmap = PDFRenderer(document).renderImage(0)
                val output = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                return output.toByteArray()
            }
        } finally {
            temp.delete()
        }
    }
}

class PdfTextParser : TextParser {
    override suspend fun parseText(context: Context, uri: Uri): List<ReaderText> = withContext(Dispatchers.IO) {
        ensurePdfBoxInitialized(context)
        val temp = copyToCacheFile(context, uri, "pdf_", ".pdf") ?: return@withContext emptyList()
        try {
            PDDocument.load(temp).use { document ->
                val chaptersByPage = buildOutline(document).groupBy({ it.pageIndex }, { it.title to it.depth })
                val stripper = PDFTextStripper()
                val result = mutableListOf<ReaderText>()
                for (pageIndex in 0 until document.numberOfPages) {
                    chaptersByPage[pageIndex]?.forEach { (title, depth) ->
                        result.add(ReaderText.Chapter(title = title, nested = depth > 0))
                    }
                    stripper.startPage = pageIndex + 1
                    stripper.endPage = pageIndex + 1
                    val pageText = runCatching { stripper.getText(document) }.getOrDefault("")
                    pageText.split(Regex("\n\\s*\n"))
                        .map { it.replace(Regex("\\s+"), " ").trim() }
                        .filter { it.isNotEmpty() }
                        .forEach { result.add(ReaderText.Text(it)) }
                    if (pageIndex != document.numberOfPages - 1) result.add(ReaderText.Separator)
                }
                result
            }
        } finally {
            temp.delete()
        }
    }
}

/** One outline (bookmark) entry anchored at [pageIndex] (0-based), in document order, with
 * nesting [depth] — shared by [PdfTextParser] (which turns these into inline `ReaderText.Chapter`
 * markers for the continuous reader) and [PdfPageSource] (which exposes them directly, keyed by
 * page, for the page-mode reader's chapters list). */
data class PdfOutlineEntry(val pageIndex: Int, val title: String, val depth: Int)

/** Plain PDFs with no outline produce an empty list, and readers fall back to a chapterless
 * document — there's no reliable way to invent chapter boundaries in an arbitrary PDF that
 * doesn't declare its own. */
private fun buildOutline(document: PDDocument): List<PdfOutlineEntry> {
    val outline = document.documentCatalog?.documentOutline ?: return emptyList()
    val result = mutableListOf<PdfOutlineEntry>()

    fun walk(item: PDOutlineItem?, depth: Int) {
        var current = item
        while (current != null) {
            val title = current.title?.trim()
            val pageNumber = runCatching { (current.destination as? PDPageDestination)?.retrievePageNumber() }.getOrNull()
            if (!title.isNullOrEmpty() && pageNumber != null && pageNumber >= 0) {
                result.add(PdfOutlineEntry(pageNumber, title, depth))
            }
            walk(current.firstChild, depth + 1)
            current = current.nextSibling
        }
    }

    walk(outline.firstChild, 0)
    return result.sortedBy { it.pageIndex }
}

/** Keeps one PDF open (a temp local copy, since PDFBox needs a [File] not a content `Uri`) for
 * the lifetime of a page-mode reading session, rendering individual pages as bitmaps on demand —
 * unlike [PdfTextParser]/[PdfCoverParser]'s load-use-close-immediately pattern, which doesn't fit
 * a reader that needs to keep jumping between arbitrary pages. Call [close] when the reading
 * session ends (e.g. `onCleared()`) to release the document and delete the temp file. */
class PdfPageSource private constructor(private val tempFile: File, private val document: PDDocument) {
    private val renderer = PDFRenderer(document)

    val pageCount: Int get() = document.numberOfPages

    val outline: List<PdfOutlineEntry> by lazy { buildOutline(document) }

    /** Renders [index] (0-based) scaled so its native page width maps to [targetWidthPx] — a
     * PDF's own point-space size has no inherent relationship to a phone screen's pixel density,
     * so rendering at a fixed DPI regardless of screen size would either waste memory (too high
     * for a small screen) or look soft (too low for a dense one). */
    suspend fun renderPage(index: Int, targetWidthPx: Int): Bitmap = withContext(Dispatchers.IO) {
        val nativeWidthPt = document.getPage(index).mediaBox.width
        val scale = if (nativeWidthPt > 0f) targetWidthPx / nativeWidthPt else 1f
        renderer.renderImage(index, scale)
    }

    fun close() {
        runCatching { document.close() }
        tempFile.delete()
    }

    companion object {
        suspend fun open(context: Context, uri: Uri): PdfPageSource? = withContext(Dispatchers.IO) {
            ensurePdfBoxInitialized(context)
            val temp = copyToCacheFile(context, uri, "pdf_", ".pdf") ?: return@withContext null
            val document = runCatching { PDDocument.load(temp) }.getOrNull()
            if (document == null) {
                temp.delete()
                return@withContext null
            }
            PdfPageSource(temp, document)
        }
    }
}
