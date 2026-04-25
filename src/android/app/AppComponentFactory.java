package android.app;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Intent;
import android.content.pm.ApplicationInfo;

public class AppComponentFactory {

    public @NonNull ClassLoader instantiateClassLoader(@NonNull ClassLoader cl,
            @NonNull ApplicationInfo aInfo) {
        return cl;
    }

    public @NonNull Application instantiateApplication(@NonNull ClassLoader cl,
            @NonNull String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Application) cl.loadClass(className).newInstance();
    }

    public @NonNull Activity instantiateActivity(@NonNull ClassLoader cl, @NonNull String className,
            @Nullable Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Activity) cl.loadClass(className).newInstance();
    }

    public @NonNull BroadcastReceiver instantiateReceiver(@NonNull ClassLoader cl,
            @NonNull String className, @Nullable Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (BroadcastReceiver) cl.loadClass(className).newInstance();
    }

    public @NonNull Service instantiateService(@NonNull ClassLoader cl,
            @NonNull String className, @Nullable Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Service) cl.loadClass(className).newInstance();
    }

    public @NonNull ContentProvider instantiateProvider(@NonNull ClassLoader cl,
            @NonNull String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (ContentProvider) cl.loadClass(className).newInstance();
    }

    public static final AppComponentFactory DEFAULT = new AppComponentFactory();
    }
