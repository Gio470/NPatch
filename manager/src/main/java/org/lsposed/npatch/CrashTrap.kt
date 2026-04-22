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
        
        val prefixes = arrayOf("org.lsposed")

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stackTrace = throwable.stackTrace
                
                var culprit = "None"
                
                outer@ for (element in stackTrace) {
                    val className = element.className
                    for (prefix in prefixes) {
                        if (className.startsWith(prefix)) {
                            culprit = className
                            break@outer
                        }
                    }
                }
                
                val report = "--- NPATCH CRASH REPORT ---\n" +
                             "Device: " + Build.MODEL + "\n" +
                             "Android: " + Build.VERSION.RELEASE + "\n" +
                             "Culprit Class: " + culprit + "\n" +
                             "Reason: " + throwable.message + "\n\n" +
                             "--- STACK TRACE ---\n" + sw.toString()

                val logDir = context.getExternalFilesDir(null) ?: context.filesDir
                val logFile = File(logDir, "NPATCH_CRASH.txt")
                
                logFile.writeBytes(report.toByteArray())
            } catch (e: Exception) {}
            originalHandler?.uncaughtException(thread, throwable)
        }
    }
}
