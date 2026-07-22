package com.wwwescape.pixelebookreader.data.book

import android.content.Context
import java.io.File
import java.util.UUID

/** Persists a parsed cover's bytes to app-private internal storage — independent of the SAF
 * grant that produced them, so covers keep working even if a source folder is later removed.
 * [Book.coverImagePath] stores the resulting absolute file path. */
object CoverStorage {
    fun save(context: Context, bytes: ByteArray): String {
        val dir = File(context.filesDir, "covers").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun delete(path: String?) {
        if (path.isNullOrEmpty()) return
        runCatching { File(path).delete() }
    }
}
