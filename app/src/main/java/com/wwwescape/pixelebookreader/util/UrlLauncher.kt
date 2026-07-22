package com.wwwescape.pixelebookreader.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Opens [url] in the user's browser. Only `https` links are launched — every caller passes a
 * hard-coded link (privacy policy, license texts), so anything else is a bug, not a feature —
 * and a device with no browser at all is a silent no-op rather than a crash. */
fun openUrl(context: Context, url: String) {
    val uri = url.toUri()
    if (uri.scheme != "https") return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
    } catch (_: ActivityNotFoundException) {
    }
}
