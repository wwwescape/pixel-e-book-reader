package com.wwwescape.pixelebookreader.data.parser.fb2

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.wwwescape.pixelebookreader.data.filesystem.FileSystemRepository
import com.wwwescape.pixelebookreader.data.parser.CombinedImportParser
import com.wwwescape.pixelebookreader.data.parser.CoverParser
import com.wwwescape.pixelebookreader.data.parser.FileParser
import com.wwwescape.pixelebookreader.data.parser.ParsedFileMetadata
import com.wwwescape.pixelebookreader.data.parser.ReaderText
import com.wwwescape.pixelebookreader.data.parser.TextParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

private fun parseFb2(bytes: ByteArray): Document = Jsoup.parse(bytes.inputStream(), null, "", Parser.xmlParser())

private fun Element.firstChildTag(tag: String): Element? = children().firstOrNull { it.tagName().equals(tag, ignoreCase = true) }

private fun Element.titleInfo(): Element? = selectFirst("description")?.firstChildTag("title-info")
private fun Element.publishInfo(): Element? = selectFirst("description")?.firstChildTag("publish-info")

class Fb2FileParser : FileParser, CombinedImportParser {
    override fun parse(context: Context, uri: Uri): ParsedFileMetadata {
        val bytes = FileSystemRepository.readBytes(context, uri)
            ?: return ParsedFileMetadata(title = uri.lastPathSegment ?: "Untitled")
        return metadataOf(parseFb2(bytes), uri)
    }

    /** One read + XML parse for both metadata and cover — FB2 embeds its images as base64 in
     * the same document, so parsing it twice would double the most expensive part of import. */
    override fun parseWithCover(context: Context, uri: Uri): Pair<ParsedFileMetadata, ByteArray?> {
        val bytes = FileSystemRepository.readBytes(context, uri)
            ?: return ParsedFileMetadata(title = uri.lastPathSegment ?: "Untitled") to null
        val document = parseFb2(bytes)
        return metadataOf(document, uri) to coverOf(document)
    }

    private fun metadataOf(document: Document, uri: Uri): ParsedFileMetadata {
        val titleInfo = document.titleInfo()
        val title = titleInfo?.firstChildTag("book-title")?.text()?.trim().takeUnless { it.isNullOrEmpty() }
        val author = titleInfo?.firstChildTag("author")?.let { authorEl ->
            listOfNotNull(
                authorEl.firstChildTag("first-name")?.text(),
                authorEl.firstChildTag("last-name")?.text(),
            ).joinToString(" ").trim().ifEmpty { null }
        }
        val description = titleInfo?.firstChildTag("annotation")?.text()?.trim()?.ifEmpty { null }
        val language = titleInfo?.firstChildTag("lang")?.text()?.trim()?.ifEmpty { null }
        val tags = titleInfo?.children().orEmpty()
            .filter { it.tagName().equals("genre", ignoreCase = true) }
            .mapNotNull { it.text().trim().ifEmpty { null } }
        val sequence = titleInfo?.firstChildTag("sequence")
        val series = sequence?.attr("name")?.trim()?.ifEmpty { null }
        val seriesNumber = sequence?.attr("number")?.trim()?.toFloatOrNull()

        val publishInfo = document.publishInfo()
        val isbn = publishInfo?.firstChildTag("isbn")?.text()?.trim()?.ifEmpty { null }
        val publishDate = publishInfo?.firstChildTag("year")?.text()?.trim()?.ifEmpty { null }

        return ParsedFileMetadata(
            title = title ?: uri.lastPathSegment ?: "Untitled",
            author = author,
            description = description,
            series = series,
            seriesNumber = seriesNumber,
            tags = tags,
            isbn = isbn,
            publishDate = publishDate,
            language = language,
        )
    }
}

class Fb2CoverParser : CoverParser {
    override fun parseCover(context: Context, uri: Uri): ByteArray? {
        val bytes = FileSystemRepository.readBytes(context, uri) ?: return null
        return coverOf(parseFb2(bytes))
    }
}

/** Decodes only the cover's own `<binary>` — not every embedded image in the book. */
private fun coverOf(document: Document): ByteArray? {
    val coverHref = document.titleInfo()?.firstChildTag("coverpage")?.firstChildTag("image")
        ?.let { it.attr("l:href").ifEmpty { it.attr("xlink:href") }.ifEmpty { it.attr("href") } }
        ?.removePrefix("#")
        ?.ifEmpty { null }
        ?: return null
    val binary = document.select("binary").firstOrNull { it.attr("id") == coverHref } ?: return null
    return runCatching { Base64.decode(binary.text().trim(), Base64.DEFAULT) }.getOrNull()
}

class Fb2TextParser : TextParser {
    override suspend fun parseText(context: Context, uri: Uri): List<ReaderText> = withContext(Dispatchers.IO) {
        parseTextBlocking(context, uri)
    }

    private fun parseTextBlocking(context: Context, uri: Uri): List<ReaderText> {
        val bytes = FileSystemRepository.readBytes(context, uri) ?: return emptyList()
        val document = parseFb2(bytes)
        val binaries = readBinaries(document)
        val body = document.selectFirst("body") ?: return emptyList()
        val result = mutableListOf<ReaderText>()
        body.children()
            .filter { it.tagName().equals("section", ignoreCase = true) }
            .forEach { parseSection(it, depth = 0, binaries, result) }
        return result
    }
}

private fun readBinaries(document: Document): Map<String, ByteArray> =
    document.select("binary").mapNotNull { binary ->
        val id = binary.attr("id").ifEmpty { return@mapNotNull null }
        val bytes = runCatching { Base64.decode(binary.text().trim(), Base64.DEFAULT) }.getOrNull() ?: return@mapNotNull null
        id to bytes
    }.toMap()

private fun parseSection(section: Element, depth: Int, binaries: Map<String, ByteArray>, out: MutableList<ReaderText>) {
    val titleEl = section.firstChildTag("title")
    val title = titleEl?.select("p")?.joinToString(" ") { it.text() }?.trim().orEmpty()
    if (title.isNotEmpty()) {
        out.add(ReaderText.Chapter(title = title, nested = depth > 0))
    }
    for (child in section.children()) {
        when (child.normalName()) {
            "title" -> Unit // already consumed above
            "section" -> parseSection(child, depth + 1, binaries, out)
            "p" -> child.text().trim().takeIf { it.isNotEmpty() }?.let { out.add(ReaderText.Text(it)) }
            "empty-line" -> out.add(ReaderText.Separator)
            "image" -> {
                val href = child.attr("l:href").ifEmpty { child.attr("xlink:href") }.ifEmpty { child.attr("href") }.removePrefix("#")
                binaries[href]?.let { out.add(ReaderText.Image(it)) }
            }
            else -> child.text().trim().takeIf { it.isNotEmpty() }?.let { out.add(ReaderText.Text(it)) }
        }
    }
}
