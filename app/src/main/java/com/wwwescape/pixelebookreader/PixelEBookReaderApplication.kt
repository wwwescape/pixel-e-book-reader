package com.wwwescape.pixelebookreader

import android.app.Application
import com.wwwescape.pixelebookreader.crash.CrashHandler
import com.wwwescape.pixelebookreader.data.parser.deleteStaleCacheFiles

class PixelEBookReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        val startedAt = System.currentTimeMillis()
        Thread({ runCatching { deleteStaleCacheFiles(this, olderThan = startedAt) } }, "cache-cleanup").apply {
            priority = Thread.MIN_PRIORITY
            start()
        }
    }
}
