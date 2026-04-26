package android.app;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;

public class AppComponentFactory extends Application {

    static {
        if (Build.VERSION.SDK_INT < 28) {
            appComponentFactory();
        }
    }

    private static void appComponentFactory() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object currentThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            
            if (currentThread != null) {
                Object boundApp = activityThreadClass.getField("mBoundApplication").get(currentThread);
                if (boundApp != null) {
                    ApplicationInfo appInfo = (ApplicationInfo) boundApp.getClass()
                        .getField("appInfo").get(boundApp);
                    
                    if (appInfo != null) {
                        java.lang.reflect.Field factoryField = ApplicationInfo.class.getDeclaredField("appComponentFactory");
                        factoryField.setAccessible(true);
                        String factory = (String) factoryField.get(appInfo);
                        
                        if (factory != null && !factory.isEmpty()) {
                            appInfo.name = factory;
                        }
                    }
                }
            }
        } catch (Throwable t) {
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

    public static final AppComponentFactory DEFAULT = new AppComponentFactory()
    }
