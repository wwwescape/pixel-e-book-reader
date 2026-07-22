package com.wwwescape.pixelebookreader.data.parser.html

import com.wwwescape.pixelebookreader.data.parser.ReaderText
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor

private val BLOCK_TAGS = setOf("p", "div", "li", "blockquote", "pre", "br", "tr")
private val HEADING_TAGS = setOf("h1", "h2", "h3", "h4", "h5", "h6")

/** Walks an (X)HTML body in document order, interleaving [ReaderText.Chapter] (from headings),
 * [ReaderText.Text] (from paragraph-ish block content), and [ReaderText.Image] in the same
 * order they appear — shared by the standalone HTML/HTM parser and each EPUB content document,
 * since both need identical "flatten this (X)HTML body into ReaderText" logic.
 *
 * [resolveImage] resolves an `<img src>` to raw bytes; formats that can't resolve relative
 * image paths (e.g. standalone HTML/HTM) can pass `{ null }` to skip
 * images entirely rather than embed a broken reference. */
fun extractReaderText(body: Element, resolveImage: (src: String) -> ByteArray? = { null }): List<ReaderText> {
    val result = mutableListOf<ReaderText>()
    val textBuffer = StringBuilder()

    fun flushText() {
        val text = textBuffer.toString().trim().replace(Regex("\\s+"), " ")
        if (text.isNotEmpty()) result.add(ReaderText.Text(text))
        textBuffer.clear()
    }

    NodeTraversor.traverse(
        object : NodeVisitor {
            override fun head(node: Node, depth: Int) {
                when {
                    node is TextNode -> textBuffer.append(node.text()).append(' ')
                    node is Element && node.tagName().lowercase() in HEADING_TAGS -> {
                        flushText()
                        val title = node.text().trim()
                        if (title.isNotEmpty()) {
                            result.add(ReaderText.Chapter(title = title, nested = node.tagName().lowercase() != "h1"))
                        }
                    }
                    node is Element && node.tagName().lowercase() == "img" -> {
                        flushText()
                        val src = node.attr("src")
                        if (src.isNotEmpty()) {
                            val caption = node.attr("alt").trim().ifEmpty { null }
                            resolveImage(src)?.let { result.add(ReaderText.Image(it, caption)) }
                        }
                    }
                    node is Element && node.tagName().lowercase() in BLOCK_TAGS -> flushText()
                }
            }

            override fun tail(node: Node, depth: Int) {
                if (node is Element && (node.tagName().lowercase() in BLOCK_TAGS || node.tagName().lowercase() in HEADING_TAGS)) {
                    flushText()
                }
            }
        },
        body,
    )
    flushText()
    return result
}
