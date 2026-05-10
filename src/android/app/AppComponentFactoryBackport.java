package android.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.net.Uri;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AppComponentFactoryBackport {

    public static class AutoInit extends ContentProvider {
        @Override
        public void attachInfo(Context context, ProviderInfo info) {
            super.attachInfo(context, info);
            init(context);
        }

        private void init(Context context) {
            try {
                String fName = null;
                try (XmlResourceParser p = context.getPackageManager().getXml(context.getPackageName(), 0, null)) {
                    int t;
                    while ((t = p.next()) != 1) {
                        if (t == 2 && "application".equals(p.getName())) {
                            for (int i = 0; i < p.getAttributeCount(); i++) {
                                if ("appComponentFactory".equals(p.getAttributeName(i))) {
                                    fName = p.getAttributeValue(i);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }

                if (fName == null) return;

                ClassLoader cl = context.getClassLoader();
                AppComponentFactory factory = (AppComponentFactory) cl.loadClass(fName).newInstance();

                Class<?> atc = Class.forName("android.app.ActivityThread");
                Object at = atc.getDeclaredMethod("currentActivityThread").invoke(null);

                Field mBoundAppField = atc.getDeclaredField("mBoundApplication");
                mBoundAppField.setAccessible(true);
                Object boundApp = mBoundAppField.get(at);

                Field infoField = boundApp.getClass().getDeclaredField("info");
                infoField.setAccessible(true);
                Object loadedApk = infoField.get(boundApp);

                try {
                    Field fFactory = loadedApk.getClass().getDeclaredField("mAppComponentFactory");
                    fFactory.setAccessible(true);
                    fFactory.set(loadedApk, factory);
                } catch (Throwable ignored) {}

                Field fInst = atc.getDeclaredField("mInstrumentation");
                fInst.setAccessible(true);
                Instrumentation base = (Instrumentation) fInst.get(at);

                if (!(base instanceof ProxyInst)) {
                    fInst.set(at, new ProxyInst(base, factory));
                }
            } catch (Throwable ignored) {}
        }

        @Override public boolean onCreate() { return true; }
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
        public Application newApplication(ClassLoader cl, String className, Context ctx) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            try {
                Application app = f.instantiateApplication(cl, className);
                Method m = Application.class.getDeclaredMethod("attach", Context.class);
                m.setAccessible(true);
                m.invoke(app, ctx);
                return app;
            } catch (Throwable e) {
                return b.newApplication(cl, className, ctx);
            }
        }

        @Override
        public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            try {
                return f.instantiateActivity(cl, className, intent);
            } catch (Throwable e) {
                return b.newActivity(cl, className, intent);
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
