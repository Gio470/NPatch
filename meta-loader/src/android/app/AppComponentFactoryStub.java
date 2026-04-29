package android.app;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Intent;

public class AppComponentFactoryStub {
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
}
