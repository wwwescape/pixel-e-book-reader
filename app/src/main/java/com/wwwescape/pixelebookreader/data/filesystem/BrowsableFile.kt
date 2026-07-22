package com.wwwescape.pixelebookreader.data.filesystem

import android.net.Uri

data class BrowsableFile(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
)
