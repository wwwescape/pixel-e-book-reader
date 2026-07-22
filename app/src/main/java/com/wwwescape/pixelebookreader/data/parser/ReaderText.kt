package com.wwwescape.pixelebookreader.data.parser

import java.util.UUID

/** The shared in-memory representation every format parser produces: chapters interleaved with
 * paragraphs/images/separators in reading order — this flat list is what backs both the
 * scrollable reader content and the chapters drawer/TOC. [Image] carries raw bytes
 * rather than a decoded bitmap so the data layer stays free of Android UI/graphics types —
 * decoding happens where the reader actually renders it. */
sealed class ReaderText {
    data class Chapter(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val nested: Boolean = false,
    ) : ReaderText()

    data class Text(val line: String) : ReaderText()

    data object Separator : ReaderText()

    data class Image(val data: ByteArray, val caption: String? = null) : ReaderText() {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Image && data.contentEquals(other.data) && caption == other.caption)

        override fun hashCode(): Int = 31 * data.contentHashCode() + (caption?.hashCode() ?: 0)
    }
}
