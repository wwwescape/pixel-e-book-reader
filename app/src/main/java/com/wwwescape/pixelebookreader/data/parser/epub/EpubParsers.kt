package com.wwwescape.pixelebookreader.data.parser.epub

import android.content.Context
import android.net.Uri
import com.wwwescape.pixelebookreader.data.parser.CoverParser
import com.wwwescape.pixelebookreader.data.parser.FileParser
import com.wwwescape.pixelebookreader.data.parser.ParsedFileMetadata
import com.wwwescape.pixelebookreader.data.parser.ReaderText
import com.wwwescape.pixelebookreader.data.parser.TextParser
import com.wwwescape.pixelebookreader.data.parser.html.extractReaderText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** Chapter title per spine href, and its nesting depth, read from the NCX `navMap` — falls
 * back gracefully (no title) when the EPUB has no NCX, falling back to using the first visible
 * line of chapter text as a synthetic title in that case. */
private fun parseNcxTitles(ncx: Document): Map<String, Pair<String, Int>> {
    val result = mutableMapOf<String, Pair<String, Int>>()

    fun childNavPoints(el: Element) = el.children().filter { it.tagName().substringAfter(':').equals("navPoint", ignoreCase = true) }

    fun walk(navPoint: Element, depth: Int) {
        val label = navPoint.firstByLocalName("navLabel")?.firstByLocalName("text")?.text()?.trim()
        val src = navPoint.firstByLocalName("content")?.attr("src")?.substringBefore('#')
        if (!label.isNullOrEmpty() && !src.isNullOrEmpty() && src !in result) {
            result[src] = label to depth
        }
        childNavPoints(navPoint).forEach { walk(it, depth + 1) }
    }

    ncx.select("navMap").firstOrNull()?.let { navMap -> childNavPoints(navMap).forEach { walk(it, 0) } }
    return result
}

/** ISBN-13/10, optionally hyphenated — used to spot which `<dc:identifier>` (a book can carry
 * several: ISBN, a publisher UUID, a URN…) is the ISBN when no scheme attribute says so. */
private val ISBN_PATTERN = Regex("^[0-9][0-9Xx-]{8,16}[0-9Xx]$")

class EpubFileParser : FileParser {
    override fun parse(context: Context, uri: Uri): ParsedFileMetadata {
        EpubDocument.open(context, uri)?.use { epub ->
            val metadata = epub.opf.select("metadata").firstOrNull()
            val title = metadata?.firstByLocalName("title")?.text()?.trim()
            val author = metadata?.firstByLocalName("creator")?.text()?.trim()
            val description = metadata?.firstByLocalName("description")?.text()?.trim()
            val language = metadata?.firstByLocalName("language")?.text()?.trim()?.ifEmpty { null }
            val publishDate = metadata?.firstByLocalName("date")?.text()?.trim()?.ifEmpty { null }
            val tags = metadata?.allByLocalName("subject").orEmpty()
                .mapNotNull { it.text().trim().ifEmpty { null } }

            // No scheme resolution (Jsoup's XML parser doesn't resolve the opf: namespace prefix
            // on attributes either), so an ISBN identifier is spotted by an attribute mentioning
            // "isbn" or, failing that, by looking like one.
            val identifiers = metadata?.allByLocalName("identifier").orEmpty()
            val isbn = identifiers.firstOrNull { el -> el.attributes().any { it.value.contains("isbn", ignoreCase = true) } }?.text()?.trim()
                ?: identifiers.map { it.text().trim() }.firstOrNull { ISBN_PATTERN.matches(it) }

            // Calibre's de facto series convention — the only one with any real-world prevalence
            // in EPUBs found in the wild; EPUB3's own `belongs-to-collection`/`refines` mechanism
            // is rare enough in practice not to be worth the extra parsing complexity here.
            val series = metadata?.select("meta[name=\"calibre:series\"]")?.firstOrNull()?.attr("content")?.trim()?.ifEmpty { null }
            val seriesNumber = metadata?.select("meta[name=\"calibre:series_index\"]")?.firstOrNull()?.attr("content")?.trim()?.toFloatOrNull()

            return ParsedFileMetadata(
                title = title.takeUnless { it.isNullOrEmpty() } ?: (uri.lastPathSegment ?: "Untitled"),
                author = author?.ifEmpty { null },
                description = description?.ifEmpty { null },
                series = series,
                seriesNumber = seriesNumber,
                tags = tags,
                isbn = isbn,
                publishDate = publishDate,
                language = language,
            )
        }
        return ParsedFileMetadata(title = uri.lastPathSegment ?: "Untitled")
    }
}

class EpubCoverParser : CoverParser {
    override fun parseCover(context: Context, uri: Uri): ByteArray? =
        EpubDocument.open(context, uri)?.use { epub -> epub.findCoverEntryHref()?.let { epub.readEntry(it) } }
}

class EpubTextParser : TextParser {
    override suspend fun parseText(context: Context, uri: Uri): List<ReaderText> =
        withContext(Dispatchers.IO) {
            val epub = EpubDocument.open(context, uri) ?: return@withContext emptyList()
            epub.use {
                val ncxTitles = it.readNcx()?.let(::parseNcxTitles).orEmpty()
                val limitedDispatcher = Dispatchers.IO.limitedParallelism(3)

                val perChapter = coroutineScope {
                    it.spineHrefs.map { href ->
                        async(limitedDispatcher) { parseSpineItem(it, href, ncxTitles[href]) }
                    }.awaitAll()
                }
                perChapter.flatten()
            }
        }

    private fun parseSpineItem(epub: EpubDocument, href: String, ncxEntry: Pair<String, Int>?): List<ReaderText> {
        val bytes = epub.readEntry(href) ?: return emptyList()
        val document = Jsoup.parse(bytes.inputStream(), null, "", org.jsoup.parser.Parser.htmlParser())
        val baseDir = href.substringBeforeLast('/', "")

        val content = extractReaderText(document.body()) { src ->
            val resolved = if (baseDir.isEmpty()) src else "$baseDir/$src"
            epub.readEntry(resolved)
        }

        val (ncxTitle, depth) = ncxEntry ?: (null to 0)
        val chapterTitle = ncxTitle ?: content.firstOrNull { it is ReaderText.Text }?.let { (it as ReaderText.Text).line }
        val chapterMarker = chapterTitle?.let { ReaderText.Chapter(title = it, nested = depth > 0) }

        // If content already opens with its own heading-derived Chapter (from extractReaderText),
        // don't add a second, redundant one on top of it.
        return if (chapterMarker != null && content.firstOrNull() !is ReaderText.Chapter) {
            listOf(chapterMarker) + content
        } else {
            content
        }
    }
}
