package com.wwwescape.pixelebookreader.data.book

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/** Longest edge a stored cover is scaled down to — comfortably sharp for the largest place a
 * cover is drawn (Book Info, on a high-density phone), without keeping a multi-megapixel scan
 * that every Library grid cell would otherwise have to decode. */
private const val MAX_COVER_EDGE_PX = 900
private const val COVER_JPEG_QUALITY = 85

/** Persists a parsed cover's bytes to app-private internal storage — independent of the SAF
 * grant that produced them, so covers keep working even if a source folder is later removed.
 * [Book.coverImagePath] stores the resulting absolute file path.
 *
 * Covers are always re-encoded rather than written as received: it downscales oversized
 * images (smaller files, faster/cheaper decoding in the Library, smaller backups), and decoding
 * first means only bytes that really are an image ever get stored — a corrupt EPUB entry or a
 * crafted backup can't plant arbitrary data — while also dropping any EXIF metadata (e.g. GPS
 * location) from a photo the user picks as a custom cover. */
object CoverStorage {

    /** Returns the stored file's path, or null if [bytes] isn't a decodable image. Does disk I/O
     * and image decoding — call off the main thread. */
    fun save(context: Context, bytes: ByteArray): String? {
        val bitmap = decodeDownscaled(bytes) ?: return null
        val dir = File(context.filesDir, "covers").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        val written = runCatching {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, COVER_JPEG_QUALITY, it) }
        }.getOrDefault(false)
        bitmap.recycle()
        if (!written) {
            file.delete()
            return null
        }
        return file.absolutePath
    }

    fun delete(path: String?) {
        if (path.isNullOrEmpty()) return
        runCatching { File(path).delete() }
    }

    private fun decodeDownscaled(bytes: ByteArray): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        // Power-of-two subsampling during decode keeps peak memory low even for huge images...
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_COVER_EDGE_PX) sampleSize *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            ?: return@runCatching null

        // ...then an exact scale to the target size.
        val longest = max(decoded.width, decoded.height)
        val scaled = if (longest > MAX_COVER_EDGE_PX) {
            val factor = MAX_COVER_EDGE_PX.toFloat() / longest
            decoded.scale((decoded.width * factor).roundToInt().coerceAtLeast(1), (decoded.height * factor).roundToInt().coerceAtLeast(1))
                .also { if (it !== decoded) decoded.recycle() }
        } else {
            decoded
        }

        // JPEG has no alpha channel — flatten transparent covers (common in PNGs) onto white
        // instead of letting transparent areas come out black.
        if (!scaled.hasAlpha()) return@runCatching scaled
        val opaque = createBitmap(scaled.width, scaled.height)
        Canvas(opaque).apply {
            drawColor(Color.WHITE)
            drawBitmap(scaled, 0f, 0f, null)
        }
        scaled.recycle()
        opaque
    }.getOrNull()
}
