package org.lsposed.npatch

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

object CrashTrap {
    @JvmStatic
    fun start(ctx: Context?) {
        val context = ctx ?: return
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))

                val report = buildString {
                    appendLine("--- NPATCH CRASH REPORT ---")
                    appendLine("Device: ${Build.MODEL}")
                    appendLine("Android: ${Build.VERSION.RELEASE}")
                    appendLine("Thread: ${thread.name}")
                    appendLine("Reason: ${throwable.message}")
                    appendLine()
                    appendLine("--- STACK TRACE ---")
                    appendLine(sw.toString())
                    appendLine()
                    appendLine("--- CAUSED BY ---")
                    var cause = throwable.cause
                    while (cause != null) {
                        appendLine("Caused by: ${cause.javaClass.name}: ${cause.message}")
                        cause.stackTrace.take(5).forEach { appendLine("\tat $it") }
                        cause = cause.cause
                    }
                }

                val logDir = context.getExternalFilesDir(null) ?: context.filesDir
                val logFile = File(logDir, "NPATCH_CRASH.txt")
                logFile.writeText(report)
            } catch (e: Exception) {}
            originalHandler?.uncaughtException(thread, throwable)
        }
    }
}
