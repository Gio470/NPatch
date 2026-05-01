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
    private static AppComponentFactory sCurrentFactory;
    private static final AppComponentFactory DEFAULT_FACTORY = new AppComponentFactory();

    public static AppComponentFactory getAppFactory() {
        return sCurrentFactory != null ? sCurrentFactory : DEFAULT_FACTORY;
    }

    public static class AutoInit extends ContentProvider {
        @Override
        public boolean onCreate() {
            if (android.os.Build.VERSION.SDK_INT < 28) {
                try {
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
                                break;
                            }
                        }
                    }

                    sCurrentFactory = (fName != null) ? (AppComponentFactory) getContext().getClassLoader().loadClass(fName).newInstance() : DEFAULT_FACTORY;

                    Class<?> atc = Class.forName("android.app.ActivityThread");
                    Object at = atc.getDeclaredMethod("currentActivityThread").invoke(null);
                    Field f = atc.getDeclaredField("mInstrumentation");
                    f.setAccessible(true);
                    Instrumentation base = (Instrumentation) f.get(at);

                    if (!(base instanceof ProxyInst)) {
                        f.set(at, new ProxyInst(base, sCurrentFactory));
                    }
                } catch (Throwable ignored) {
                    sCurrentFactory = DEFAULT_FACTORY;
                }
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
        private final Instrumentation mBase;
        private final AppComponentFactory mFactory;

        ProxyInst(Instrumentation base, AppComponentFactory factory) {
            this.mBase = base;
            this.mFactory = factory;
        }

        @Override
        public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            try {
                Activity a = mFactory.instantiateActivity(cl, className, intent);
                if (a != null) return a;
            } catch (Throwable ignored) {}
            return mBase.newActivity(cl, className, intent);
        }

        @Override
        public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            try {
                if (mFactory != DEFAULT_FACTORY) {
                    Application app = mFactory.instantiateApplication(cl, className);
                    if (app != null) {
                        Method m = Application.class.getDeclaredMethod("attach", Context.class);
                        m.setAccessible(true);
                        m.invoke(app, context);
                        return app;
                    }
                }
            } catch (Throwable ignored) {}
            return mBase.newApplication(cl, className, context);
        }

        @Override public void callApplicationOnCreate(Application a) { mBase.callApplicationOnCreate(a); }
        @Override public void callActivityOnCreate(Activity a, android.os.Bundle i) { mBase.callActivityOnCreate(a, i); }
        @Override public void callActivityOnDestroy(Activity a) { mBase.callActivityOnDestroy(a); }
        @Override public void callActivityOnPause(Activity a) { mBase.callActivityOnPause(a); }
        @Override public void callActivityOnResume(Activity a) { mBase.callActivityOnResume(a); }
        @Override public void callActivityOnStart(Activity a) { mBase.callActivityOnStart(a); }
        @Override public void callActivityOnStop(Activity a) { mBase.callActivityOnStop(a); }
        }
     }
