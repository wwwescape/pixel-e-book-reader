package com.wwwescape.pixelebookreader.data.parser.txt

import android.content.Context
import android.net.Uri
import com.wwwescape.pixelebookreader.data.filesystem.FileSystemRepository
import com.wwwescape.pixelebookreader.data.parser.CoverParser
import com.wwwescape.pixelebookreader.data.parser.FileParser
import com.wwwescape.pixelebookreader.data.parser.ParsedFileMetadata
import com.wwwescape.pixelebookreader.data.parser.ReaderText
import com.wwwescape.pixelebookreader.data.parser.TextParser

/** Plain text has no metadata or chapter structure of its own — title falls back to the file
 * name (Browse is expected to pass that in via [ParsedFileMetadata.title] override
 * once it has the SAF display name on hand). */
class TxtFileParser : FileParser {
    override fun parse(context: Context, uri: Uri): ParsedFileMetadata =
        ParsedFileMetadata(title = uri.lastPathSegment ?: "Untitled")
}

class TxtCoverParser : CoverParser {
    override fun parseCover(context: Context, uri: Uri): ByteArray? = null
}

class TxtTextParser : TextParser {
    override suspend fun parseText(context: Context, uri: Uri): List<ReaderText> {
        val bytes = FileSystemRepository.readBytes(context, uri) ?: return emptyList()
        val content = bytes.toString(Charsets.UTF_8)
        return content.split(Regex("\\n\\s*\\n"))
            .map { paragraph -> paragraph.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotEmpty() }
            .map { ReaderText.Text(it) }
    }
}
