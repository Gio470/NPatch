package android.app;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AppComponentFactory {
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
                    String fName = parse();
                    if (fName != null) {
                        Class<?> atc = Class.forName("android.app.ActivityThread");
                        Object at = atc.getDeclaredMethod("currentActivityThread").invoke(null);
                        Field ifld = atc.getDeclaredField("mInstrumentation");
                        ifld.setAccessible(true);
                        Instrumentation base = (Instrumentation) ifld.get(at);
                        AppComponentFactory factory = (AppComponentFactory) getContext().getClassLoader().loadClass(fName).newInstance();
                        ifld.set(at, new ProxyInst(base, factory));
                    }
                } catch (Throwable ignored) {}
            }
            return true;
        }

        private String parse() {
            try {
                android.content.res.XmlResourceParser p = getContext().getAssets().openXmlResourceParser("AndroidManifest.xml");
                int t;
                while ((t = p.next()) != 1) {
                    if (t == 2 && "application".equals(p.getName())) {
                        for (int i = 0; i < p.getAttributeCount(); i++) {
                            if ("appComponentFactory".equals(p.getAttributeName(i))) return p.getAttributeValue(i);
                        }
                    }
                }
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
        private final AppComponentFactory f;
        ProxyInst(Instrumentation base, AppComponentFactory factory) { b = base; f = factory; }

        @Override
        public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            try { return f.instantiateActivity(cl, className, intent); } catch (Exception e) { throw new InstantiationException(e.getMessage()); }
        }

        @Override
        public Application newApplication(ClassLoader cl, String className, Context ctx) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            try {
                Application app = f.instantiateApplication(cl, className);
                Method m = Application.class.getDeclaredMethod("attach", Context.class);
                m.setAccessible(true);
                m.invoke(app, ctx);
                return app;
            } catch (Exception e) { throw new InstantiationException(e.getMessage()); }
        }

        @Override public void callApplicationOnCreate(Application a) { b.callApplicationOnCreate(a); }
        @Override public void callActivityOnCreate(Activity a, android.os.Bundle i) { b.callActivityOnCreate(a, i); }
        @Override public void callActivityOnDestroy(Activity a) { b.callActivityOnDestroy(a); }
        @Override public void callActivityOnPause(Activity a) { b.callActivityOnPause(a); }
        @Override public void callActivityOnResume(Activity a) { b.callActivityOnResume(a); }
        @Override public void callActivityOnStart(Activity a) { b.callActivityOnStart(a); }
        @Override public void callActivityOnStop(Activity a) { b.callActivityOnStop(a); }
    }
            }


