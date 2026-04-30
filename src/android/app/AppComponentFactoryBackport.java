package android.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.net.Uri;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AppComponentFactoryBackport {

    public static class AutoInit extends ContentProvider {
        @Override
        public boolean onCreate() {
            if (android.os.Build.VERSION.SDK_INT < 28) {
                try {
                    Class<?> atc = Class.forName("android.app.ActivityThread");
                    Object at = atc.getDeclaredMethod("currentActivityThread").invoke(null);
                    
                    String fName = null;
                    try (XmlResourceParser p = getContext().getAssets().openXmlResourceParser("AndroidManifest.xml")) {
                        int t;
                        while ((t = p.next()) != 1) {
                            if (t == 2 && "application".equals(p.getName())) {
                                for (int i = 0; i < p.getAttributeCount(); i++) {
                                    if ("appComponentFactory".equals(p.getAttributeName(i))) {
                                        fName = p.getAttributeValue(i);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (fName != null) {
                        ClassLoader cl = getContext().getClassLoader();
                        AppComponentFactory factory = (AppComponentFactory) cl.loadClass(fName).newInstance();
                        
                        Field f = atc.getDeclaredField("mInstrumentation");
                        f.setAccessible(true);
                        Instrumentation base = (Instrumentation) f.get(at);
                        
                        if (!(base instanceof ProxyInst)) {
                            f.set(at, new ProxyInst(base, factory));
                        }
                    }
                } catch (Throwable ignored) {}
            }
            return true;
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

        ProxyInst(Instrumentation base, AppComponentFactory factory) {
            this.b = base;
            this.f = factory;
        }

        @Override
        public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            try {
                return f.instantiateActivity(cl, className, intent);
            } catch (Exception e) {
                return b.newActivity(cl, className, intent);
            }
        }

        @Override
        public Application newApplication(ClassLoader cl, String className, Context ctx) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            try {
                Application app = f.instantiateApplication(cl, className);
                try {
                    Method m = Application.class.getDeclaredMethod("attach", Context.class);
                    m.setAccessible(true);
                    m.invoke(app, ctx);
                } catch (Throwable ignored) {}
                return app;
            } catch (Exception e) {
                return b.newApplication(cl, className, ctx);
            }
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
