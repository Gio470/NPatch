package org.lsposed.npatch;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashTrap {

    public static void start(final Context context) {
        final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    String stackTrace = sw.toString();

                    StringBuilder sb = new StringBuilder();
                    sb.append("--- NPATCH ANDROID 7 CRASH REPORT ---\n");
                    sb.append("Device: ").append(Build.MODEL).append("\n");
                    sb.append("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
                    sb.append("Reason: ").append(throwable.getMessage()).append("\n\n");
                    sb.append("--- STACK TRACE ---\n");
                    sb.append(stackTrace);

                    File logFile = new File(context.getFilesDir(), "NPATCH_CRASH.txt");
                    FileOutputStream fos = new FileOutputStream(logFile);
                    fos.write(sb.toString().getBytes());
                    fos.close();

                    Log.e("NPatch", "FATAL CRASH CAPTURED AT: " + logFile.getAbsolutePath());
                } catch (Exception e) {
                    Log.e("NPatch", "CRASH HANDLER FAILED", e);
                }
                if (originalHandler != null) {
                    originalHandler.uncaughtException(thread, throwable);
                }
            }
        });
    }
          }
