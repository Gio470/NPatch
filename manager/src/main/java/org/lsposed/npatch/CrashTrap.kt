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
                
                val report = "--- NPATCH CRASH REPORT ---\n" +
                             "Device: " + Build.MODEL + "\n" +
                             "Android: " + Build.VERSION.RELEASE + "\n" +
                             "Reason: " + throwable.message + "\n\n" +
                             "--- STACK TRACE ---\n" + sw.toString()

                val logDir = context.getExternalFilesDir(null) ?: context.filesDir
                val logFile = File(logDir, "NPATCH_CRASH.txt")
                
                logFile.writeBytes(report.toByteArray())
            } catch (e: Exception) {
            }
            originalHandler?.uncaughtException(thread, throwable)
        }
    }
}
