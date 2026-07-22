package com.wwwescape.pixelebookreader.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A decoded image plus whether it was decoded at full resolution — if so, no larger request can
 * ever be served better by decoding again. */
private class DecodedImage(val bitmap: ImageBitmap, val isFullResolution: Boolean)

/** Recently decoded images, bounded to an eighth of the heap (sized in KB) — scrolling a cover
 * back into view in the Library, or a picture back into view in the Reader, reuses the bitmap
 * instead of decoding the same file again (each decode is real CPU work, and so real battery).
 * Keyed by source only (a cover's file path, an in-book image's byte array — arrays compare by
 * identity, which is exactly right here), so a cached image can be shown on the very first frame,
 * before the composable has even been measured. */
private val decodedImageCache = object : LruCache<Any, DecodedImage>((Runtime.getRuntime().maxMemory() / 8 / 1024).toInt()) {
    override fun sizeOf(key: Any, value: DecodedImage): Int = (value.bitmap.width * value.bitmap.height * 4 / 1024).coerceAtLeast(1)
}

/** Decodes an image off the main thread, subsampled so it's no larger than needed for
 * [targetWidthPx] (0 = not measured yet: only a cached copy is shown), and caches it under
 * [cacheKey]. [decode] is handed the [BitmapFactory.Options] to use — first a bounds-only pass,
 * then the real one. Returns null while decoding, or if the data isn't a decodable image. */
@Composable
fun rememberDecodedImage(
    cacheKey: Any?,
    targetWidthPx: Int,
    decode: (BitmapFactory.Options) -> Bitmap?,
): ImageBitmap? {
    val state = remember(cacheKey) { mutableStateOf(cacheKey?.let { decodedImageCache.get(it) }) }
    LaunchedEffect(cacheKey, targetWidthPx) {
        if (cacheKey == null || targetWidthPx <= 0) return@LaunchedEffect
        val current = state.value
        if (current != null && (current.isFullResolution || current.bitmap.width >= targetWidthPx)) return@LaunchedEffect
        val decoded = withContext(Dispatchers.Default) { decodeSampled(targetWidthPx, decode) } ?: return@LaunchedEffect
        decodedImageCache.put(cacheKey, decoded)
        state.value = decoded
    }
    return state.value?.bitmap
}

private fun decodeSampled(targetWidthPx: Int, decode: (BitmapFactory.Options) -> Bitmap?): DecodedImage? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    decode(bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= targetWidthPx) sampleSize *= 2
    val bitmap = decode(BitmapFactory.Options().apply { inSampleSize = sampleSize }) ?: return@runCatching null
    DecodedImage(bitmap.asImageBitmap(), isFullResolution = sampleSize == 1)
}.getOrNull()
