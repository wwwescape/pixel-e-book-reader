package com.wwwescape.pixelebookreader.data.parser.md

import android.content.Context
import android.net.Uri
import com.wwwescape.pixelebookreader.data.filesystem.FileSystemRepository
import com.wwwescape.pixelebookreader.data.parser.CoverParser
import com.wwwescape.pixelebookreader.data.parser.FileParser
import com.wwwescape.pixelebookreader.data.parser.ParsedFileMetadata
import com.wwwescape.pixelebookreader.data.parser.ReaderText
import com.wwwescape.pixelebookreader.data.parser.TextParser
import org.commonmark.node.Code
import org.commonmark.node.Heading
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

/** The document's first top-level heading becomes the title, matching how most Markdown books/
 * READMEs open — falls back to the file name if there is none. */
class MarkdownFileParser : FileParser {
    override fun parse(context: Context, uri: Uri): ParsedFileMetadata {
        val bytes = FileSystemRepository.readBytes(context, uri)
        val firstHeading = bytes?.let { findFirstHeadingText(Parser.builder().build().parse(it.toString(Charsets.UTF_8))) }
        return ParsedFileMetadata(title = firstHeading ?: uri.lastPathSegment ?: "Untitled")
    }
}

/** Markdown has no standard cover-image convention. */
class MarkdownCoverParser : CoverParser {
    override fun parseCover(context: Context, uri: Uri): ByteArray? = null
}

class MarkdownTextParser : TextParser {
    override suspend fun parseText(context: Context, uri: Uri): List<ReaderText> {
        val bytes = FileSystemRepository.readBytes(context, uri) ?: return emptyList()
        val document = Parser.builder().build().parse(bytes.toString(Charsets.UTF_8))
        val result = mutableListOf<ReaderText>()
        var child = document.firstChild
        while (child != null) {
            appendNode(child, result)
            child = child.next
        }
        return result
    }
}

private fun appendNode(node: Node, out: MutableList<ReaderText>) {
    when (node) {
        is Heading -> {
            val title = flattenText(node).trim()
            if (title.isNotEmpty()) out.add(ReaderText.Chapter(title = title, nested = node.level > 1))
        }
        is Paragraph -> {
            val text = flattenText(node).trim()
            if (text.isNotEmpty()) out.add(ReaderText.Text(text))
        }
        is ThematicBreak -> out.add(ReaderText.Separator)
        // Images reference relative/external paths this app can't resolve against a standalone
        // SAF document (same limitation as the standalone HTML parser) — skipped rather than
        // embedding a broken reference.
        else -> {
            var child = node.firstChild
            while (child != null) {
                appendNode(child, out)
                child = child.next
            }
        }
    }
}

private fun findFirstHeadingText(document: Node): String? {
    var child = document.firstChild
    while (child != null) {
        if (child is Heading) {
            val text = flattenText(child).trim()
            if (text.isNotEmpty()) return text
        }
        child = child.next
    }
    return null
}

private fun flattenText(node: Node): String {
    val sb = StringBuilder()
    var child = node.firstChild
    while (child != null) {
        when (child) {
            is Text -> sb.append(child.literal)
            is Code -> sb.append(child.literal)
            else -> sb.append(flattenText(child))
        }
        child = child.next
    }
    return sb.toString()
}
