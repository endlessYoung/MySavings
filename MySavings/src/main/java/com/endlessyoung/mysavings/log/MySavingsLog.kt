package com.endlessyoung.mysavings.log

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MySavingsLog {

    private const val PREFIX = "MySavings"
    @Volatile var enable = true
    @Volatile var enableFile = false

    private var appContext: Context? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context, enable: Boolean = true, enableFile: Boolean = false) {
        this.appContext = context.applicationContext
        this.enable = enable
        this.enableFile = enableFile
    }

    fun v(tag: String, msg: String) = log(Log.VERBOSE, tag, msg, null)
    fun d(tag: String, msg: String) = log(Log.DEBUG, tag, msg, null)
    fun i(tag: String, msg: String) = log(Log.INFO, tag, msg, null)
    fun w(tag: String, msg: String, tr: Throwable? = null) = log(Log.WARN, tag, msg, tr)
    fun e(tag: String, msg: String, tr: Throwable? = null) = log(Log.ERROR, tag, msg, tr)

    private fun log(level: Int, tag: String, msg: String, tr: Throwable?) {
        if (!enable) return

        val finalTag = "$PREFIX-$tag"
        val finalMsg = buildString {
            append(msg)
            tr?.let { append("\n").append(Log.getStackTraceString(it)) }
        }

        when (level) {
            Log.VERBOSE -> Log.v(finalTag, finalMsg)
            Log.DEBUG -> Log.d(finalTag, finalMsg)
            Log.INFO -> Log.i(finalTag, finalMsg)
            Log.WARN -> Log.w(finalTag, finalMsg)
            Log.ERROR -> Log.e(finalTag, finalMsg)
        }

        if (enableFile) writeToFile(level, finalTag, finalMsg)
    }

    private fun writeToFile(level: Int, tag: String, msg: String) {
        val ctx = appContext ?: return
        val levelStr = when (level) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            else -> "?"
        }

        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            .format(Date())

        val line = "$time [$levelStr] $tag: $msg\n"

        ioScope.launch {
            try {
                val file = File(ctx.filesDir, "mysavings.log")
                file.appendText(line)
            } catch (_: Exception) {
            }
        }
    }
}
