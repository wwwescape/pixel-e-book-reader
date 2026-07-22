package com.wwwescape.pixelebookreader.crash

import android.content.Context
import android.content.Intent
import android.os.Process
import kotlin.system.exitProcess

/** Installed once from [com.wwwescape.pixelebookreader.PixelEBookReaderApplication.onCreate] —
 * replaces the platform's default "Unfortunately, X has stopped" dialog with [CrashActivity],
 * which can at least tell the user what happened and let them copy/share the stack trace for
 * a bug report, rather than a dead end.
 *
 * The stack trace travels via an `Intent` extra, not a static field — after
 * [Process.killProcess], nothing in this process' memory survives, so anything the crash screen
 * needs has to be handed to Android before that point. [Intent.FLAG_ACTIVITY_NEW_TASK] lets
 * `startActivity` succeed even though this call happens outside of any Activity context (the
 * crashing thread could be anywhere), and the request itself is a fire-and-forget IPC to
 * `system_server` — it doesn't need this process to stay alive to complete, so killing the
 * process immediately after is safe. */
object CrashHandler {
    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val intent = Intent(appContext, CrashActivity::class.java).apply {
                    putExtra(CrashActivity.EXTRA_STACK_TRACE, throwable.stackTraceToString())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                appContext.startActivity(intent)
                Process.killProcess(Process.myPid())
                exitProcess(10)
            } catch (fallback: Throwable) {
                // Our own handling code crashed — fall through to the platform default rather
                // than risk silently hanging with no crash UI at all.
                defaultHandler?.uncaughtException(thread, throwable) ?: exitProcess(10)
            }
        }
    }
}
