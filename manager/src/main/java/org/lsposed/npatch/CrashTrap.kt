package org.lsposed.npatch;

import android.content.Context;
import android.os.Build;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.FileOutputStream;

public class CrashTrap {
    public static void start(Context ctx) {
        if (ctx == null) return;
        final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));

                    String report = "--- NPATCH CRASH REPORT ---\n" +
                                 "Device: " + Build.MODEL + "\n" +
                                 "Android: " + Build.VERSION.RELEASE + "\n" +
                                 "Reason: " + throwable.getMessage() + "\n\n" +
                                 "--- STACK TRACE ---\n" + sw.toString();

                    File logDir = ctx.getExternalFilesDir(null) != null ? ctx.getExternalFilesDir(null) : ctx.getFilesDir();
                    File logFile = new File(logDir, "NPATCH_CRASH.txt");
                    
                    try (FileOutputStream fos = new FileOutputStream(logFile)) {
                        fos.write(report.getBytes());
                    }
                } catch (Exception e) {}
                
                if (originalHandler != null) {
                    originalHandler.uncaughtException(thread, throwable);
                }
            }
        });
    }
}
