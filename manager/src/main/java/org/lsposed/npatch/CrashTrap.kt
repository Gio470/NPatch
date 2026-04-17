package org.lsposed.npatch

import android.content.Context
import android.os.Build
import android.util.Log
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
                
                val report = "--- NPATCH CRASH ---\n" +
                             "Device: ${Build.MODEL}\n" +
                             "Android: ${Build.VERSION.RELEASE}\n" +
                             "Reason: ${throwable.message}\n\n" +
                             "--- STACK TRACE ---\n" + sw.toString()

                val logFile = File(context.filesDir, "NPATCH_CRASH.txt")
                logFile.writeBytes(report.toByteArray())
            } catch (e: Exception) {            
            }
            originalHandler?.uncaughtException(thread, throwable)
        }
    }
}
