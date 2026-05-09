package android.os;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public final class SharedMemory implements Parcelable, Closeable {

    private static final boolean USE_SYSTEM_API = Build.VERSION.SDK_INT >= 27;

    private final FileDescriptor mFileDescriptor;
    private final int mSize;
    private final MemoryFile mMemoryFile;
    private final Object mSystemSharedMemory;

    private SharedMemory(Object systemSharedMemory) {
        this.mSystemSharedMemory = systemSharedMemory;
        this.mMemoryFile = null;
        int size = 0;
        FileDescriptor fd = null;
        try {
            size = (int) systemSharedMemory.getClass().getMethod("getSize").invoke(systemSharedMemory);
            fd = (FileDescriptor) systemSharedMemory.getClass().getMethod("getFileDescriptor").invoke(systemSharedMemory);
        } catch (Throwable ignored) {}
        this.mSize = size;
        this.mFileDescriptor = fd;
    }

    private SharedMemory(FileDescriptor fd, int size, MemoryFile memoryFile) {
        this.mFileDescriptor = fd;
        this.mSize = size;
        this.mMemoryFile = memoryFile;
        this.mSystemSharedMemory = null;
    }

    public static SharedMemory create(String name, int size) throws Exception {
        if (size <= 0) throw new IllegalArgumentException("Size must be > 0");
        
        if (USE_SYSTEM_API) {
            try {
                Class<?> sysClass = Class.forName("android.os.SharedMemory");
                Object sysInstance = sysClass.getMethod("create", String.class, int.class).invoke(null, name, size);
                return new SharedMemory(sysInstance);
            } catch (Throwable ignored) {}
        }
        
        MemoryFile memoryFile = new MemoryFile(name, size);
        Method getFdMethod = MemoryFile.class.getDeclaredMethod("getFileDescriptor");
        getFdMethod.setAccessible(true);
        FileDescriptor fd = (FileDescriptor) getFdMethod.invoke(memoryFile);
        return new SharedMemory(fd, size, memoryFile);
    }

    public ByteBuffer mapReadWrite() throws Exception {
        if (mSystemSharedMemory != null) {
            return (ByteBuffer) mSystemSharedMemory.getClass().getMethod("mapReadWrite").invoke(mSystemSharedMemory);
        }
        return map(FileChannel.MapMode.READ_WRITE, 0, mSize);
    }

    public ByteBuffer mapReadOnly() throws Exception {
        if (mSystemSharedMemory != null) {
            return (ByteBuffer) mSystemSharedMemory.getClass().getMethod("mapReadOnly").invoke(mSystemSharedMemory);
        }
        return map(FileChannel.MapMode.READ_ONLY, 0, mSize);
    }

    public ByteBuffer map(FileChannel.MapMode mode, int offset, int length) throws Exception {
        if (mSystemSharedMemory != null) {
            Class<?> sysClass = mSystemSharedMemory.getClass();
            Class<?> modeClass = Class.forName("android.system.OsConstants");
            int prot = mode == FileChannel.MapMode.READ_ONLY ? modeClass.getField("PROT_READ").getInt(null) : 
                       modeClass.getField("PROT_READ").getInt(null) | modeClass.getField("PROT_WRITE").getInt(null);
            return (ByteBuffer) sysClass.getMethod("map", int.class, int.class, int.class).invoke(mSystemSharedMemory, prot, offset, length);
        }
        
        if (mFileDescriptor == null || !mFileDescriptor.valid()) throw new IllegalStateException("Closed");
        return new FileInputStream(mFileDescriptor).getChannel().map(mode, offset, length);
    }

    public static ClassLoader buildDexClassLoader(ByteBuffer[] buffers, ClassLoader parent) {
        if (USE_SYSTEM_API) {
            try {
                return new dalvik.system.InMemoryDexClassLoader(buffers, parent);
            } catch (Throwable ignored) {}
        }
        
        try {
            int totalSize = 0;
            for (ByteBuffer buf : buffers) totalSize += buf.remaining();
            byte[] dexBytes = new byte[totalSize];
            int offset = 0;
            for (ByteBuffer buf : buffers) {
                int len = buf.remaining();
                buf.get(dexBytes, offset, len);
                offset += len;
            }
            Class<?> dexFileClass = Class.forName("dalvik.system.DexFile");
            Method openDexFileMethod = dexFileClass.getDeclaredMethod("openDexFile", byte[].class);
            openDexFileMethod.setAccessible(true);
            Object cookie = openDexFileMethod.invoke(null, dexBytes);
            dalvik.system.DexClassLoader dummyLoader = new dalvik.system.DexClassLoader("", null, null, parent);
            Field pathListField = Class.forName("dalvik.system.BaseDexClassLoader").getDeclaredField("pathList");
            pathListField.setAccessible(true);
            Object pathList = pathListField.get(dummyLoader);
            Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
            dexElementsField.setAccessible(true);
            Object[] dexElements = (Object[]) dexElementsField.get(pathList);
            Field dexFileField = dexElements[0].getClass().getDeclaredField("dexFile");
            dexFileField.setAccessible(true);
            Object dexFileObj = dexFileField.get(dexElements[0]);
            Field mCookieField = dexFileClass.getDeclaredField("mCookie");
            mCookieField.setAccessible(true);
            mCookieField.set(dexFileObj, cookie);
            return dummyLoader;
        } catch (Throwable e) {
            return parent;
        }
    }

    public void setProtect(int prot) {
        if (mSystemSharedMemory != null) {
            try {
                mSystemSharedMemory.getClass().getMethod("setProtect", int.class).invoke(mSystemSharedMemory, prot);
                return;
            } catch (Throwable ignored) {}
        }
        
        try {
            Class<?> libcore = Class.forName("libcore.io.Libcore");
            Field osField = libcore.getField("os");
            Object os = osField.get(null);
            Method mprotect = os.getClass().getMethod("mprotect", long.class, long.class, int.class);
            ByteBuffer buffer = mapReadWrite();
            Field addressField = java.nio.Buffer.class.getDeclaredField("address");
            addressField.setAccessible(true);
            long address = addressField.getLong(buffer);
            mprotect.invoke(os, address, (long) mSize, prot);
        } catch (Throwable ignored) {}
    }

    public static void unmap(ByteBuffer buffer) {
        if (buffer == null) return;
        
        if (USE_SYSTEM_API) {
            try {
                Class<?> sysClass = Class.forName("android.os.SharedMemory");
                sysClass.getMethod("unmap", ByteBuffer.class).invoke(null, buffer);
                return;
            } catch (Throwable ignored) {}
        }
        
        try {
            Method cleanerMethod = buffer.getClass().getMethod("cleaner");
            cleanerMethod.setAccessible(true);
            Object cleaner = cleanerMethod.invoke(buffer);
            if (cleaner != null) {
                cleaner.getClass().getMethod("clean").invoke(cleaner);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void close() {
        if (mSystemSharedMemory != null) {
            try {
                mSystemSharedMemory.getClass().getMethod("close").invoke(mSystemSharedMemory);
            } catch (Throwable ignored) {}
        } else if (mMemoryFile != null) {
            mMemoryFile.close();
        }
    }

    public int getSize() { return mSize; }

    public FileDescriptor getFileDescriptor() { return mFileDescriptor; }

    @Override public int describeContents() { return 1; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (mSystemSharedMemory != null) {
            try {
                mSystemSharedMemory.getClass().getMethod("writeToParcel", Parcel.class, int.class).invoke(mSystemSharedMemory, dest, flags);
                return;
            } catch (Throwable ignored) {}
        }
        
        try {
            Method writeFd = Parcel.class.getMethod("writeFileDescriptor", FileDescriptor.class);
            writeFd.invoke(dest, mFileDescriptor);
        } catch (Throwable ignored) {}
    }

    public static final Parcelable.Creator<SharedMemory> CREATOR = new Parcelable.Creator<SharedMemory>() {
        @Override
        public SharedMemory createFromParcel(Parcel source) {
            if (USE_SYSTEM_API) {
                try {
                    Class<?> sysClass = Class.forName("android.os.SharedMemory");
                    Field creatorField = sysClass.getField("CREATOR");
                    Parcelable.Creator<?> sysCreator = (Parcelable.Creator<?>) creatorField.get(null);
                    return new SharedMemory(sysCreator.createFromParcel(source));
                } catch (Throwable ignored) {}
            }
            
            try {
                Method readFd = Parcel.class.getMethod("readFileDescriptor");
                FileDescriptor fd = (FileDescriptor) readFd.invoke(source);
                return new SharedMemory(fd, -1, null);
            } catch (Throwable e) {
                return null;
            }
        }
        @Override public SharedMemory[] newArray(int size) { return new SharedMemory[size]; }
    };
}
