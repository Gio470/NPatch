package android.app;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import org.lsposed.npatch.metaloader.LSPAppComponentFactoryStub;

@SuppressLint("UnsafeDynamicallyLoadedCode")
public class AppComponentFactory extends Application {
    private static final String TAG = "AppComponentFactory";
    private static boolean initialized = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (!initialized) {
            initialized = true;
            LSPAppComponentFactoryStub.bootstrap();
        }
    }

    public ClassLoader instantiateClassLoader(ClassLoader cl, ApplicationInfo appInfo) {
        return cl;
    }

    public Application instantiateApplication(ClassLoader cl, String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Application) cl.loadClass(className).newInstance();
    }

    public Activity instantiateActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Activity) cl.loadClass(className).newInstance();
    }

    public BroadcastReceiver instantiateReceiver(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (BroadcastReceiver) cl.loadClass(className).newInstance();
    }

    public Service instantiateService(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Service) cl.loadClass(className).newInstance();
    }

    public ContentProvider instantiateProvider(ClassLoader cl, String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (ContentProvider) cl.loadClass(className).newInstance();
    }

    public static final AppComponentFactory DEFAULT = new AppComponentFactory();
          }
