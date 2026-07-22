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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/** Book covers are always local files (see `CoverStorage`) — never remote URLs — so plain
 * [BitmapFactory] decoding is enough and keeps this app dependency-light rather than pulling in
 * an image-loading library (Coil, used by Pixel Photo Slideshow) for a job this small. Falls
 * back to a generic book glyph when [coverPath] is null or fails to decode. */
@Composable
fun BookCoverImage(coverPath: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(coverPath) {
        coverPath?.let { path -> runCatching { BitmapFactory.decodeFile(path) }.getOrNull() }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
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
