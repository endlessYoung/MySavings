package com.endlessyoung.mysavings

import android.app.Application
import android.util.Log
import com.endlessyoung.mysavings.log.MySavingsLog

class App : Application() {
    companion object {
        private const val TAG = "MySavingsApp"
    }

    override fun onCreate() {
        super.onCreate()

        MySavingsLog.init(
            context = this,
            enable = BuildConfig.DEBUG,
            enableFile = false
        )

        MySavingsLog.d(TAG, "onCreate")
    }
}