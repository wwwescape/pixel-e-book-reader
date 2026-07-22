package com.wwwescape.pixelebookreader.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}
