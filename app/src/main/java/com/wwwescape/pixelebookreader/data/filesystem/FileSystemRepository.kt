package com.wwwescape.pixelebookreader.data.filesystem

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

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

    /** The root [DocumentFile] for a granted folder tree (a Browse source's uri) — the starting
     * point for navigating into it. */
    fun rootDocument(context: Context, treeUri: Uri): DocumentFile? = DocumentFile.fromTreeUri(context, treeUri)

    /** A [DocumentFile] for any document uri returned by [listChildren] — usable to navigate
     * further into a subfolder, since a directory's children are themselves listable the same
     * way regardless of whether they were reached via a tree uri or a plain document uri. */
    fun documentFor(context: Context, uri: Uri): DocumentFile? = DocumentFile.fromSingleUri(context, uri)

    /** Immediate children of [directory] — not recursive; callers navigate deeper by resolving
     * a returned [BrowsableFile.uri] via [documentFor] and calling this again. */
    fun listChildren(directory: DocumentFile): List<BrowsableFile> =
        directory.listFiles().mapNotNull { file ->
            val name = file.name ?: return@mapNotNull null
            BrowsableFile(
                name = name,
                uri = file.uri,
                isDirectory = file.isDirectory,
                size = file.length(),
                lastModified = file.lastModified(),
            )
        }

    /** Opens a document's bytes for a format parser to read — caller is responsible for
     * closing the returned stream. */
    fun openInputStream(context: Context, documentUri: Uri) =
        context.contentResolver.openInputStream(documentUri)

    /** Reads a whole document into memory — convenience for format parsers working with small-
     * to-medium files (TXT/HTML/MD/FB2). Large formats (EPUB/PDF) use [openInputStream]
     * directly instead of buffering the whole file. */
    fun readBytes(context: Context, documentUri: Uri): ByteArray? =
        openInputStream(context, documentUri)?.use { it.readBytes() }
}
