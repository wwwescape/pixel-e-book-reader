package com.wwwescape.pixelebookreader.data.parser

import android.content.Context
import android.net.Uri

interface TextParser {
    /** Full reader content in reading order — see [ReaderText]. */
    suspend fun parseText(context: Context, uri: Uri): List<ReaderText>
}
