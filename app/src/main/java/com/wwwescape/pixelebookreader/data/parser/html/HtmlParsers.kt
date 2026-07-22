package com.wwwescape.pixelebookreader.data.parser.html

import android.content.Context
import android.net.Uri
import com.wwwescape.pixelebookreader.data.filesystem.FileSystemRepository
import com.wwwescape.pixelebookreader.data.parser.CoverParser
import com.wwwescape.pixelebookreader.data.parser.FileParser
import com.wwwescape.pixelebookreader.data.parser.ParsedFileMetadata
import com.wwwescape.pixelebookreader.data.parser.ReaderText
import com.wwwescape.pixelebookreader.data.parser.TextParser
import org.jsoup.Jsoup

class HtmlFileParser : FileParser {
    override fun parse(context: Context, uri: Uri): ParsedFileMetadata {
        val bytes = FileSystemRepository.readBytes(context, uri)
        val title = bytes?.let { Jsoup.parse(it.toString(Charsets.UTF_8)).title().ifEmpty { null } }
        return ParsedFileMetadata(title = title ?: uri.lastPathSegment ?: "Untitled")
    }
}

/** Standalone HTML files carry no standard cover-image convention (unlike EPUB/FB2/PDF). */
class HtmlCoverParser : CoverParser {
    override fun parseCover(context: Context, uri: Uri): ByteArray? = null
}

class HtmlTextParser : TextParser {
    override suspend fun parseText(context: Context, uri: Uri): List<ReaderText> {
        val bytes = FileSystemRepository.readBytes(context, uri) ?: return emptyList()
        val document = Jsoup.parse(bytes.toString(Charsets.UTF_8))
        // Relative image paths (img src) can't be resolved against a standalone SAF document
        // without walking its sibling files, so images are skipped here rather than embedding
        // broken references — resolveImage stays the default no-op.
        return extractReaderText(document.body())
    }
}
