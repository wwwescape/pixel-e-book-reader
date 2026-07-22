package com.wwwescape.pixelebookreader.data.parser

import android.content.Context
import android.net.Uri

interface CoverParser {
    /** Raw cover image bytes, or null if this format/file doesn't carry one. */
    fun parseCover(context: Context, uri: Uri): ByteArray?
}
