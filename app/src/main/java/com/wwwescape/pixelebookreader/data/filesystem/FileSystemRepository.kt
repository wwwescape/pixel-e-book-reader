package com.wwwescape.pixelebookreader.data.filesystem

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Upper bound for reading a whole document into memory ([readBytes]) — far beyond any real
 * TXT/HTML/MD/FB2 book or backup file, but low enough that a huge or hostile file fails cleanly
 * instead of taking the process down with an `OutOfMemoryError`. */
const val MAX_IN_MEMORY_BYTES: Long = 64L * 1024 * 1024

/** Thin wrapper over the Storage Access Framework — the only way this app touches book files,
 * matching the sibling apps' SAF-only, no-storage-permission approach. */
object FileSystemRepository {

    /** Persists read access to a uri returned directly by
     * [androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree] or
     * [androidx.activity.result.contract.ActivityResultContracts.OpenDocument], so it survives
     * past this process/session without re-prompting. Only call this on a uri that actually
     * came from one of those pickers — document uris obtained by listing an already-granted
     * tree (see [listChildren]) inherit that tree's grant automatically and aren't themselves
     * grantable (silently no-ops via `runCatching` if attempted). */
    fun persistPermission(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun releasePermission(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** A document's user-facing name (e.g. "brave-new-world.epub") — a single, small provider
     * query; null if the provider doesn't report one. */
    fun displayName(context: Context, documentUri: Uri): String? = runCatching {
        context.contentResolver.query(documentUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
        }
    }.getOrNull()

    /** The document uri for the root of a granted folder tree (a Browse source's uri) — the
     * starting point for navigating into it with [listChildren]. */
    fun rootDocumentUri(treeUri: Uri): Uri? = runCatching {
        DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
    }.getOrNull()

    /** Immediate children of the directory at [directoryUri] (a tree-based document uri, from
     * [rootDocumentUri] or a previous [listChildren] call) — not recursive.
     *
     * Deliberately one `ContentResolver` query for the whole folder rather than
     * `DocumentFile.listFiles()`, which costs a separate provider round-trip *per field per
     * file* (name, type, size, date) — dozens of IPC calls for an ordinary folder, every time it
     * was shown. Must be called off the main thread. */
    fun listChildren(context: Context, directoryUri: Uri): List<BrowsableFile> {
        val childrenUri = runCatching {
            DocumentsContract.buildChildDocumentsUriUsingTree(directoryUri, DocumentsContract.getDocumentId(directoryUri))
        }.getOrNull() ?: return emptyList()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val result = mutableListOf<BrowsableFile>()
        runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(0) ?: continue
                    val name = cursor.getString(1) ?: continue
                    result.add(
                        BrowsableFile(
                            name = name,
                            uri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId),
                            isDirectory = cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR,
                            size = if (cursor.isNull(3)) 0L else cursor.getLong(3),
                            lastModified = if (cursor.isNull(4)) 0L else cursor.getLong(4),
                        ),
                    )
                }
            }
        }
        return result
    }

    /** Opens a document's bytes for a format parser to read — caller is responsible for
     * closing the returned stream. Only `content://` uris are opened: every book/backup uri this
     * app stores came from SAF, and refusing anything else means a crafted backup file can't
     * point a "book" at a `file://` path inside this app's own private storage. */
    fun openInputStream(context: Context, documentUri: Uri): InputStream? {
        if (documentUri.scheme != ContentResolver.SCHEME_CONTENT) return null
        return context.contentResolver.openInputStream(documentUri)
    }

    /** Reads a whole document into memory — convenience for format parsers working with small-
     * to-medium files (TXT/HTML/MD/FB2). Large formats (EPUB/PDF) use [openInputStream]
     * directly instead of buffering the whole file. Returns null for anything over
     * [MAX_IN_MEMORY_BYTES]. */
    fun readBytes(context: Context, documentUri: Uri): ByteArray? =
        openInputStream(context, documentUri)?.use { it.readBytesCapped(MAX_IN_MEMORY_BYTES) }
}

/** [InputStream.readBytes] with an upper bound — returns null instead of buffering past
 * [maxBytes], so a decompression bomb or an unexpectedly huge file can't exhaust the heap. */
fun InputStream.readBytesCapped(maxBytes: Long): ByteArray? {
    val buffer = java.io.ByteArrayOutputStream()
    val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(chunk)
        if (read < 0) break
        total += read
        if (total > maxBytes) return null
        buffer.write(chunk, 0, read)
    }
    return buffer.toByteArray()
}

/** [InputStream.copyTo] with an upper bound — throws once more than [maxBytes] have been
 * copied, so a single oversized file can't fill the device's storage. */
fun InputStream.copyToCapped(out: OutputStream, maxBytes: Long): Long {
    val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(chunk)
        if (read < 0) break
        total += read
        if (total > maxBytes) throw IOException("File exceeds $maxBytes bytes")
        out.write(chunk, 0, read)
    }
    return total
}
