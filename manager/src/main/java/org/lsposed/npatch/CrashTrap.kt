package org.lsposed.npatch

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

object CrashTrap {
    @JvmStatic
    fun start(context: Context) {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                
                val report = """
                    --- NPATCH ANDROID 7 CRASH REPORT ---
                    Device: ${Build.MODEL}
                    Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                    Reason: ${throwable.message}
                    
                    --- STACK TRACE ---
                    ${sw}
                """.trimIndent()

                val logFile = File(context.filesDir, "NPATCH_CRASH.txt")
                logFile.writeText(report)
                Log.e("NPatch", "FATAL CRASH CAPTURED AT: ${logFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("NPatch", "CRASH HANDLER FAILED", e)
            }
            originalHandler?.uncaughtException(thread, throwable)
        }
    }
            }
