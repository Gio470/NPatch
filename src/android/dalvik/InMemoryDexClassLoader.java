package android.dalvik;

import android.os.Build;
import android.os.SharedMemory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class InMemoryDexClassLoader extends ClassLoader {

    private static final String TAG = "InMemoryDexClassLoader";
    private final ClassLoader mDelegate;

    public InMemoryDexClassLoader(ByteBuffer[] buffers, ClassLoader parent) {
        super(parent);
        mDelegate = buildClassLoader(buffers, parent);
    }

    public InMemoryDexClassLoader(ByteBuffer buffer, ClassLoader parent) {
        this(new ByteBuffer[]{buffer}, parent);
    }

    private ClassLoader buildClassLoader(ByteBuffer[] buffers, ClassLoader parent) {
        if (Build.VERSION.SDK_INT >= 27) {            
            try {
                return new dalvik.system.InMemoryDexClassLoader(buffers, parent);
            } catch (Throwable t) {
                Log.w(TAG, "Native InMemoryDexClassLoader failed, falling back", t);
            }
        }
        
        return buildLegacyClassLoader(buffers, parent);
    }

    private ClassLoader buildLegacyClassLoader(ByteBuffer[] buffers, ClassLoader parent) {
        try {           
            File tmpDir = new File(System.getProperty("java.io.tmpdir"), "npatch_dex_" + System.currentTimeMillis());
            if (!tmpDir.mkdirs()) {
                Log.e(TAG, "Failed to create temp dir");
                return parent;
            }

            StringBuilder dexPathBuilder = new StringBuilder();

            for (int i = 0; i < buffers.length; i++) {
                ByteBuffer buffer = buffers[i];
                File dexFile = new File(tmpDir, "classes_" + i + ".dex");
                
                buffer.rewind();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);

                try (FileOutputStream fos = new FileOutputStream(dexFile)) {
                    fos.write(bytes);
                }

                if (i > 0) dexPathBuilder.append(File.pathSeparator);
                dexPathBuilder.append(dexFile.getAbsolutePath());
            }

            File optimizedDir = new File(tmpDir, "odex");
            optimizedDir.mkdirs();
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteRecursive(tmpDir)));

            return new dalvik.system.DexClassLoader(
                    dexPathBuilder.toString(),
                    optimizedDir.getAbsolutePath(),
                    null,
                    parent
            );
        } catch (Throwable e) {
            Log.e(TAG, "Failed to build legacy ClassLoader", e);
            return parent;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return mDelegate.loadClass(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return mDelegate.loadClass(name);
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        file.delete();
    }
}
