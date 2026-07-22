package com.wwwescape.pixelebookreader.ui.screens.reader

import android.content.Context
import android.content.Intent

private fun launchProcessText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_PROCESS_TEXT, text)
        putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
    }
    startExternal(context, Intent.createChooser(intent, null))
}

private fun startExternal(context: Context, intent: Intent) {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/** Fires the double-tap-to-translate gesture directly for a single word,
 * bypassing manual selection.
 *
 * This used to sit alongside a custom Copy/Translate/Share/Search/Highlight selection toolbar
 * (a [androidx.compose.ui.platform.TextToolbar] override + [androidx.compose.ui.window.Popup]),
 * removed after it turned out to never render on-device: Compose Foundation 1.11 defaults
 * `SelectionContainer` to a newer context-menu system (`ComposeFoundationFlags.
 * isNewContextMenuEnabled = true`) that bypasses `LocalTextToolbar` in favor of Android's native
 * ActionMode + TextClassifier "smart" chips, and even forcing that flag back off (confirmed via
 * decompiling `SelectionManager.updateSelectionToolbar()`) left the legacy `Popup`-based toolbar
 * itself never attaching to the window on a real device — a library regression, not something
 * fixable from app code. The app now accepts Android's native selection toolbar as-is (which,
 * with Smart Selection enabled, already offers roughly equivalent Translate/Share/Search
 * behavior for free) instead of fighting it. Highlighting was moved to a separate flow: copy
 * text via that native toolbar, then tap the Highlight action in [ReaderTopBar].
 */
fun translateWord(context: Context, word: String) = launchProcessText(context, word)
