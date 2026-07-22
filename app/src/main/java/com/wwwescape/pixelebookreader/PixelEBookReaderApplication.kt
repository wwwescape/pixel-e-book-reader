package com.wwwescape.pixelebookreader

import android.app.Application
import com.wwwescape.pixelebookreader.crash.CrashHandler

class PixelEBookReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}
