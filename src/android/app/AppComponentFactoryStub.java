package android.app;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

public class AppComponentFactory {

    private static final WeakHashMap<android.content.pm.ApplicationInfo, String> sStorage = new WeakHashMap<>();

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

    public static class BackportProvider extends ContentProvider {

        @Override
        public boolean onCreate() {
            if (Build.VERSION.SDK_INT < 28) {
                try {
                    setup();
                } catch (Throwable ignored) {}
            }
            return true;
        }

        private void setup() throws Exception {
            android.content.Context ctx = getContext();
            String factory = parseFactory(ctx);
            if (factory == null) return;

            sStorage.put(ctx.getApplicationInfo(), factory);

            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Method m = atClass.getDeclaredMethod("currentActivityThread");
            m.setAccessible(true);
            Object at = m.invoke(null);

            Field f = atClass.getDeclaredField("mInstrumentation");
            f.setAccessible(true);
            Instrumentation base = (Instrumentation) f.get(at);

            if (!(base instanceof ProxyInst)) {
                AppComponentFactoryStub stub = (AppComponentFactoryStub) ctx.getClassLoader()
                    .loadClass(factory).newInstance();
                f.set(at, new ProxyInst(base, stub));
            }
        }

        private String parseFactory(android.content.Context ctx) {
            try {
                android.content.res.XmlResourceParser p = ctx.getAssets().openXmlResourceParser("AndroidManifest.xml");
                int type;
                while ((type = p.next()) != android.content.res.XmlResourceParser.END_DOCUMENT) {
                    if (type == android.content.res.XmlResourceParser.START_TAG && "application".equals(p.getName())) {
                        for (int i = 0; i < p.getAttributeCount(); i++) {
                            if ("appComponentFactory".equals(p.getAttributeName(i)))
                                return p.getAttributeValue(i);
                        }
                    }
                }
                p.close();
            } catch (Throwable ignored) {}
            return null;
        }

        @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
        @Override public String getType(Uri u) { return null; }
        @Override public Uri insert(Uri u, ContentValues v) { return null; }
        @Override public int delete(Uri u, String s, String[] a) { return 0; }
        @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
    }

    private static class ProxyInst extends Instrumentation {
        private final Instrumentation b;
        private final AppComponentFactoryStub f;

        ProxyInst(Instrumentation base, AppComponentFactoryStub stub) { b = base; f = stub; }

        @Override
        public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            try { return f.instantiateActivity(cl, className, intent); }
            catch (Exception e) { throw new InstantiationException(e.getMessage()); }
        }

        @Override
        public Application newApplication(ClassLoader cl, String className, Context ctx) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            try {
                Application app = f.instantiateApplication(cl, className);                
                return app;
            } catch (Exception e) { throw new InstantiationException(e.getMessage()); }
        }

        @Override public void callApplicationOnCreate(Application app) { b.callApplicationOnCreate(app); }
        @Override public void callActivityOnCreate(Activity a, android.os.Bundle ic) { b.callActivityOnCreate(a, ic); }
        @Override public void callActivityOnDestroy(Activity a) { b.callActivityOnDestroy(a); }
        @Override public void callActivityOnPause(Activity a) { b.callActivityOnPause(a); }
        @Override public void callActivityOnResume(Activity a) { b.callActivityOnResume(a); }
        @Override public void callActivityOnStart(Activity a) { b.callActivityOnStart(a); }
        @Override public void callActivityOnStop(Activity a) { b.callActivityOnStop(a); }
    }
}


