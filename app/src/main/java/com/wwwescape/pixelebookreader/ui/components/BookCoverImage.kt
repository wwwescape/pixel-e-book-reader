package com.wwwescape.pixelebookreader.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp

/** Book covers are always local files (see `CoverStorage`) — never remote URLs — so plain
 * [BitmapFactory] decoding is enough and keeps this app dependency-light rather than pulling in
 * an image-loading library (Coil, used by Pixel Photo Slideshow) for a job this small. Decoding
 * happens off the main thread, subsampled to the size the cover is actually laid out at, and is
 * cached (see [rememberDecodedImage]) — so scrolling a big Library grid neither janks nor keeps
 * re-decoding full-size images. Falls back to a generic book glyph when [coverPath] is null, still
 * decoding, or fails to decode. */
@Composable
fun BookCoverImage(coverPath: String?, modifier: Modifier = Modifier) {
    var widthPx by remember { mutableIntStateOf(0) }
    val bitmap = rememberDecodedImage(coverPath, widthPx) { options -> BitmapFactory.decodeFile(coverPath, options) }
    val sizedModifier = modifier.onSizeChanged { widthPx = it.width }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = sizedModifier,
        )
    } else {
        Box(
            modifier = sizedModifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}
