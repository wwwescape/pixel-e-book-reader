package com.wwwescape.pixelebookreader.data.parser.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.core.graphics.createBitmap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.wwwescape.pixelebookreader.data.parser.CombinedImportParser
import com.wwwescape.pixelebookreader.data.parser.CoverParser
import com.wwwescape.pixelebookreader.data.parser.FileParser
import com.wwwescape.pixelebookreader.data.parser.ParsedFileMetadata
import com.wwwescape.pixelebookreader.data.parser.ReaderText
import com.wwwescape.pixelebookreader.data.parser.TextParser
import com.wwwescape.pixelebookreader.data.parser.copyToCacheFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

private val pdfBoxInitialized = AtomicBoolean(false)

/** Width a PDF's first page is rendered at for its cover — matches `CoverStorage`'s own size cap,
 * so there's no point rendering any larger. */
private const val COVER_RENDER_WIDTH_PX = 600

private val PARAGRAPH_BREAK = Regex("\\n\\s*\\n")
private val WHITESPACE = Regex("\\s+")

/** PDFBox-Android needs its font/encoding resources loaded once per process before any
 * [PDDocument] operation — safe to call repeatedly, but only actually does the work once. */
private fun ensurePdfBoxInitialized(context: Context) {
    if (pdfBoxInitialized.compareAndSet(false, true)) {
        PDFBoxResourceLoader.init(context.applicationContext)
    }
}

/** Renders a page with the platform's native [PdfRenderer] (Pdfium) — many times faster, and so
 * far cheaper on battery, than PDFBox's pure-Java renderer. Not thread-safe: callers must
 * serialize access to [renderer]. */
private fun renderNative(renderer: PdfRenderer, index: Int, targetWidthPx: Int): Bitmap {
    val page = renderer.openPage(index)
    try {
        val scale = if (page.width > 0) targetWidthPx.toFloat() / page.width else 1f
        val width = (page.width * scale).roundToInt().coerceAtLeast(1)
        val height = (page.height * scale).roundToInt().coerceAtLeast(1)
        val bitmap = createBitmap(width, height)
        // PdfRenderer draws onto whatever is already there — pages without their own background
        // would otherwise come out transparent (i.e. black on a dark theme).
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    } finally {
        page.close()
    }
}

/** First page as JPEG bytes, via [PdfRenderer] — or null if the platform renderer can't open the
 * file (e.g. it's encrypted), in which case callers fall back to PDFBox. */
private fun renderCoverNative(file: File): ByteArray? = runCatching {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
        val renderer = PdfRenderer(fd)
        try {
            if (renderer.pageCount == 0) return@runCatching null
            renderNative(renderer, 0, COVER_RENDER_WIDTH_PX).toJpegBytes()
        } finally {
            renderer.close()
        }
    }
}.getOrNull()

private fun renderCoverPdfBox(document: PDDocument): ByteArray? {
    if (document.numberOfPages == 0) return null
    val nativeWidthPt = document.getPage(0).mediaBox.width
    val scale = if (nativeWidthPt > 0f) COVER_RENDER_WIDTH_PX / nativeWidthPt else 1f
    return PDFRenderer(document).renderImage(0, scale).toJpegBytes()
}

private fun Bitmap.toJpegBytes(): ByteArray {
    val output = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 95, output)
    recycle()
    return output.toByteArray()
}

private fun PDDocument.toMetadata(uri: Uri): ParsedFileMetadata {
    val info = documentInformation
    return ParsedFileMetadata(
        title = info?.title?.trim().takeUnless { it.isNullOrEmpty() } ?: (uri.lastPathSegment ?: "Untitled"),
        author = info?.author?.trim()?.ifEmpty { null },
    )
}

class PdfFileParser : FileParser, CombinedImportParser {
    override fun parse(context: Context, uri: Uri): ParsedFileMetadata {
        ensurePdfBoxInitialized(context)
        val temp = copyToCacheFile(context, uri, "pdf_", ".pdf")
            ?: return ParsedFileMetadata(title = uri.lastPathSegment ?: "Untitled")
        try {
            return PDDocument.load(temp).use { it.toMetadata(uri) }
        } finally {
            temp.delete()
        }
    }

    /** One cache copy for both metadata and cover, instead of copying the whole PDF twice. */
    override fun parseWithCover(context: Context, uri: Uri): Pair<ParsedFileMetadata, ByteArray?> {
        ensurePdfBoxInitialized(context)
        val temp = copyToCacheFile(context, uri, "pdf_", ".pdf")
            ?: return ParsedFileMetadata(title = uri.lastPathSegment ?: "Untitled") to null
        try {
            return PDDocument.load(temp).use { document ->
                val cover = renderCoverNative(temp) ?: runCatching { renderCoverPdfBox(document) }.getOrNull()
                document.toMetadata(uri) to cover
            }
        } finally {
            temp.delete()
        }
    }
}

class PdfCoverParser : CoverParser {
    override fun parseCover(context: Context, uri: Uri): ByteArray? {
        val temp = copyToCacheFile(context, uri, "pdf_", ".pdf") ?: return null
        try {
            renderCoverNative(temp)?.let { return it }
            ensurePdfBoxInitialized(context)
            return PDDocument.load(temp).use { renderCoverPdfBox(it) }
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
                    pageText.split(PARAGRAPH_BREAK)
                        .map { it.replace(WHITESPACE, " ").trim() }
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

/** Keeps one PDF open (a temp local copy, since both renderers need a real file rather than a
 * content `Uri`) for the lifetime of a page-mode reading session, rendering individual pages as
 * bitmaps on demand — unlike [PdfTextParser]/[PdfCoverParser]'s load-use-close-immediately
 * pattern, which doesn't fit a reader that needs to keep jumping between arbitrary pages. Call
 * [close] when the reading session ends (e.g. `onCleared()`) to release the document and delete
 * the temp file.
 *
 * Pages are drawn by the platform's native [PdfRenderer] whenever it can open the file; PDFBox
 * is only kept open as a fallback renderer for files it can't (e.g. encrypted ones). Neither
 * renderer is thread-safe, and the pager asks for neighboring pages concurrently, so every
 * render is serialized through [lock]. Recently rendered pages are kept in a small
 * memory-bounded [cache], so paging back and forth doesn't re-render (and re-burn CPU on) the
 * same page. */
class PdfPageSource private constructor(
    private val tempFile: File,
    private val nativeFd: ParcelFileDescriptor?,
    private val nativeRenderer: PdfRenderer?,
    private val fallbackDocument: PDDocument?,
    val pageCount: Int,
    val outline: List<PdfOutlineEntry>,
) {
    private val lock = Mutex()
    private val fallbackRenderer = fallbackDocument?.let { PDFRenderer(it) }

    @Volatile
    private var closed = false
    private var released = false

    private val cache = object : LruCache<Long, Bitmap>((Runtime.getRuntime().maxMemory() / 8).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.allocationByteCount
    }

    /** Renders [index] (0-based) scaled so its native page width maps to [targetWidthPx] — a
     * PDF's own point-space size has no inherent relationship to a phone screen's pixel density,
     * so rendering at a fixed DPI regardless of screen size would either waste memory (too high
     * for a small screen) or look soft (too low for a dense one). Returns null once [close]d or
     * if the page can't be rendered. */
    suspend fun renderPage(index: Int, targetWidthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (index !in 0 until pageCount || targetWidthPx <= 0) return@withContext null
        val key = (index.toLong() shl 32) or targetWidthPx.toLong()
        cache.get(key)?.let { return@withContext it }
        lock.withLock {
            if (closed) {
                releaseLocked()
                return@withLock null
            }
            cache.get(key) ?: runCatching { renderLocked(index, targetWidthPx) }.getOrNull()?.also { cache.put(key, it) }
        }
    }

    private fun renderLocked(index: Int, targetWidthPx: Int): Bitmap? {
        nativeRenderer?.let { return renderNative(it, index, targetWidthPx) }
        val document = fallbackDocument ?: return null
        val renderer = fallbackRenderer ?: return null
        val nativeWidthPt = document.getPage(index).mediaBox.width
        val scale = if (nativeWidthPt > 0f) targetWidthPx / nativeWidthPt else 1f
        return renderer.renderImage(index, scale)
    }

    /** Safe to call while a render is still in flight — the document is then released by that
     * render as soon as it finishes, instead of being pulled out from under it. */
    fun close() {
        closed = true
        cache.evictAll()
        if (lock.tryLock()) {
            try {
                releaseLocked()
            } finally {
                lock.unlock()
            }
        }
    }

    private fun releaseLocked() {
        if (released) return
        released = true
        runCatching { nativeRenderer?.close() }
        runCatching { nativeFd?.close() }
        runCatching { fallbackDocument?.close() }
        tempFile.delete()
    }

    companion object {
        suspend fun open(context: Context, uri: Uri): PdfPageSource? = withContext(Dispatchers.IO) {
            ensurePdfBoxInitialized(context)
            val temp = copyToCacheFile(context, uri, "pdf_", ".pdf") ?: return@withContext null

            // PDFBox is still what reads the outline (PdfRenderer has no outline API below
            // Android 15); once that's read it's closed again unless it's needed for rendering.
            val document = runCatching { PDDocument.load(temp) }.getOrNull()
            val outline = document?.let { runCatching { buildOutline(it) }.getOrNull() }.orEmpty()

            val fd = runCatching { ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY) }.getOrNull()
            val nativeRenderer = fd?.let { runCatching { PdfRenderer(it) }.getOrNull() }
            if (nativeRenderer == null) runCatching { fd?.close() }

            if (nativeRenderer != null) {
                runCatching { document?.close() }
                return@withContext PdfPageSource(temp, fd, nativeRenderer, null, nativeRenderer.pageCount, outline)
            }
            if (document == null) {
                temp.delete()
                return@withContext null
            }
            PdfPageSource(temp, null, null, document, document.numberOfPages, outline)
        }
    }
}
