package com.wwwescape.pixelebookreader.data.parser

import android.content.Context
import android.net.Uri
import com.wwwescape.pixelebookreader.data.filesystem.FileSystemRepository
import com.wwwescape.pixelebookreader.data.filesystem.copyToCapped
import java.io.File

/** Upper bound for a book copied to [Context.getCacheDir] — generous for any real EPUB/PDF, but
 * stops a single huge file from filling the device's storage. */
private const val MAX_CACHE_COPY_BYTES: Long = 512L * 1024 * 1024

/** Every scratch file [copyToCacheFile] creates is named with one of these prefixes, so
 * [deleteStaleCacheFiles] can find leftovers without touching anything else in the cache. */
private val SCRATCH_PREFIXES = listOf("epub_", "pdf_")

/** SAF content Uris only support sequential reads; formats needing random access into the file
 * (EPUB's ZIP, PDF's PDFBox loader) copy to a scratch file in [Context.getCacheDir] first —
 * caller is responsible for deleting the returned file once done. Returns null (leaving nothing
 * behind) if the source can't be read or is larger than [MAX_CACHE_COPY_BYTES]. */
internal fun copyToCacheFile(context: Context, uri: Uri, prefix: String, suffix: String): File? {
    val temp = File.createTempFile(prefix, suffix, context.cacheDir)
    val copied = runCatching {
        FileSystemRepository.openInputStream(context, uri)?.use { input ->
            temp.outputStream().use { output -> input.copyToCapped(output, MAX_CACHE_COPY_BYTES) }
            true
        } ?: false
    }.getOrDefault(false)
    if (!copied) {
        temp.delete()
        return null
    }
    return temp
}

/** Scratch copies are normally deleted as soon as a book is closed, but a process killed mid-read
 * (low memory, force-stop) never gets the chance — call once at startup, off the main thread, to
 * reclaim that space rather than leave potentially hundreds of MB of orphaned copies behind.
 * Only files last modified before [olderThan] (the process' start time) are removed, so a book the
 * user opens while this is still running keeps its fresh copy. */
fun deleteStaleCacheFiles(context: Context, olderThan: Long) {
    context.cacheDir.listFiles()?.forEach { file ->
        if (file.isFile && SCRATCH_PREFIXES.any { file.name.startsWith(it) } && file.lastModified() < olderThan) file.delete()
    }
}
