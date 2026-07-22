package com.wwwescape.pixelebookreader.data.parser

import android.content.Context
import android.net.Uri
import com.wwwescape.pixelebookreader.data.filesystem.FileSystemRepository
import java.io.File

/** SAF content Uris only support sequential reads; formats needing random access into the file
 * (EPUB's ZIP, PDF's PDFBox loader) copy to a scratch file in [Context.getCacheDir] first —
 * caller is responsible for deleting the returned file once done. */
internal fun copyToCacheFile(context: Context, uri: Uri, prefix: String, suffix: String): File? {
    val temp = File.createTempFile(prefix, suffix, context.cacheDir)
    val opened = FileSystemRepository.openInputStream(context, uri)?.use { input ->
        temp.outputStream().use { output -> input.copyTo(output) }
        true
    } ?: false
    if (!opened) {
        temp.delete()
        return null
    }
    return temp
}
