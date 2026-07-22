package com.wwwescape.pixelebookreader.data.parser

import android.content.Context
import android.net.Uri

/** Just enough metadata to show a book in the Browse/import list quickly, without parsing the
 * full text — see [TextParser] for that. The fields below [description] are this app's
 * richer metadata, best-effort auto-populated where a format's own metadata block carries them
 * (EPUB OPF `<metadata>`, FB2 `<title-info>`/`<publish-info>`) — `null`/empty otherwise, left for
 * the user to fill in on the Book Info screen. */
data class ParsedFileMetadata(
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val series: String? = null,
    val seriesNumber: Float? = null,
    val tags: List<String> = emptyList(),
    val isbn: String? = null,
    val publishDate: String? = null,
    val language: String? = null,
)

interface FileParser {
    fun parse(context: Context, uri: Uri): ParsedFileMetadata
}

/** Implemented by formats whose metadata and cover come from the same expensive-to-open source —
 * EPUB's ZIP and PDF each need a full cache copy of the file, FB2 a full XML parse — so
 * importing a book opens it once instead of once for [FileParser] and again for [CoverParser]. */
interface CombinedImportParser {
    fun parseWithCover(context: Context, uri: Uri): Pair<ParsedFileMetadata, ByteArray?>
}
