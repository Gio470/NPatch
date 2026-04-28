package android.app;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AppComponentFactory extends ContentProvider {
    public static final AppComponentFactory DEFAULT = new AppComponentFactory();

    @Override
    public boolean onCreate() {
        if (Build.VERSION.SDK_INT < 28) {
            try {
                Class<?> atc = Class.forName("android.app.ActivityThread");
                Method catm = atc.getDeclaredMethod("currentActivityThread");
                catm.setAccessible(true);
                Object at = catm.invoke(null);
                Field instField = atc.getDeclaredField("mInstrumentation");
                instField.setAccessible(true);
                Instrumentation old = (Instrumentation) instField.get(at);
                instField.set(at, new ProxyInst(this, old));
            } catch (Throwable ignored) {}
        }
        return true;
    }

    public Application instantiateApplication(ClassLoader cl, String className) throws Exception {
        return (Application) cl.loadClass(className).newInstance();
    }

    public Activity instantiateActivity(ClassLoader cl, String className, Intent intent) throws Exception {
        return (Activity) cl.loadClass(className).newInstance();
    }

    public BroadcastReceiver instantiateReceiver(ClassLoader cl, String className, Intent intent) throws Exception {
        return (BroadcastReceiver) cl.loadClass(className).newInstance();
    }

    public Service instantiateService(ClassLoader cl, String className, Intent intent) throws Exception {
        return (Service) cl.loadClass(className).newInstance();
    }

    public ContentProvider instantiateProvider(ClassLoader cl, String className) throws Exception {
        return (ContentProvider) cl.loadClass(className).newInstance();
    }

    private static class ProxyInst extends Instrumentation {
        private final AppComponentFactory f;
        private final Instrumentation o;
        ProxyInst(AppComponentFactory factory, Instrumentation old) { f = factory; o = old; }
        @Override
        public Activity newActivity(ClassLoader cl, String className, Intent intent) throws Exception {
            return f.instantiateActivity(cl, className, intent);
        }
        @Override
        public void callApplicationOnCreate(Application app) { o.callApplicationOnCreate(app); }
    }

    @Override public Cursor query(Uri u, String[] s, String r, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}


