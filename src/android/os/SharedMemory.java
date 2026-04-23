package android.os;

import android.system.ErrnoException;
import android.system.Os;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public final class SharedMemory implements Parcelable, Closeable {

    private final FileDescriptor mFileDescriptor;
    private final int mSize;
    private final FileInputStream mAnchorStream;

    private SharedMemory(FileDescriptor fd, int size) {
        this.mFileDescriptor = fd;
        int s = size;
        if (s <= 0) {
            try {
                s = (int) Os.fstat(fd).st_size;
            } catch (ErrnoException ignored) {}
        }
        this.mSize = s;
        this.mAnchorStream = new FileInputStream(fd);
    }

    public static SharedMemory create(String name, int size) throws Exception {
        if (size <= 0) throw new IllegalArgumentException("Size must be > 0");
        MemoryFile memoryFile = new MemoryFile(name, size);
        Method getFdMethod = MemoryFile.class.getDeclaredMethod("getFileDescriptor");
        getFdMethod.setAccessible(true);
        FileDescriptor fd = (FileDescriptor) getFdMethod.invoke(memoryFile);
        return new SharedMemory(fd, size);
    }

    public static SharedMemory fromFileDescriptor(ParcelFileDescriptor fd) {
        if (fd == null) return null;
        return new SharedMemory(fd.getFileDescriptor(), -1);
    }

    public ByteBuffer mapReadWrite() throws IOException {
        return map(FileChannel.MapMode.READ_WRITE, 0, mSize);
    }

    public ByteBuffer mapReadOnly() throws IOException {
        return map(FileChannel.MapMode.READ_ONLY, 0, mSize);
    }

    public ByteBuffer map(FileChannel.MapMode mode, int offset, int length) throws IOException {
        if (getNativeFd(mFileDescriptor) == -1) {
            throw new IllegalStateException("SharedMemory is closed");
        }
        return mAnchorStream.getChannel().map(mode, offset, length);
    }

    public static ClassLoader buildDexClassLoader(ByteBuffer[] buffers, ClassLoader parent) {
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            try {
                return new dalvik.system.InMemoryDexClassLoader(buffers, parent);
            } catch (Throwable t) {
                android.util.Log.w("SharedMemory", "Fallback to legacy loader", t);
            }
        }

        try {
            int totalSize = 0;
            for (ByteBuffer buf : buffers) {
                buf.rewind();
                totalSize += buf.remaining();
            }

            ByteBuffer merged = ByteBuffer.allocate(totalSize);
            for (ByteBuffer buf : buffers) {
                buf.rewind();
                merged.put(buf);
            }
            merged.rewind();

            MemoryFile memoryFile = new MemoryFile("dex_bridge", totalSize);
            memoryFile.writeBytes(merged.array(), 0, 0, totalSize);

            Method getFdMethod = MemoryFile.class.getDeclaredMethod("getFileDescriptor");
            getFdMethod.setAccessible(true);
            FileDescriptor fd = (FileDescriptor) getFdMethod.invoke(memoryFile);

            FileInputStream fis = new FileInputStream(fd);
            FileChannel channel = fis.getChannel();
            ByteBuffer dexBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, totalSize);

            byte[] dexBytes = new byte[totalSize];
            dexBuffer.get(dexBytes);

            return new dalvik.system.DexClassLoader(
                    "data:application/dex;base64," + android.util.Base64.encodeToString(dexBytes, android.util.Base64.NO_WRAP),
                    null,
                    null,
                    parent
            );
        } catch (Throwable e) {
            android.util.Log.e("SharedMemory", "buildDexClassLoader failed", e);
            return parent;
        }
    }

    public boolean setProtect(int prot) {
        return true;
    }

    public static void unmap(ByteBuffer buffer) {
        if (buffer == null) return;
        try {
            Method cleanerMethod = buffer.getClass().getMethod("cleaner");
            cleanerMethod.setAccessible(true);
            Object cleaner = cleanerMethod.invoke(buffer);
            if (cleaner != null) {
                Method cleanMethod = cleaner.getClass().getMethod("clean");
                cleanMethod.invoke(cleaner);
            }
        } catch (Exception ignored) {}
    }

    private static int getNativeFd(FileDescriptor fd) {
        try {
            Field field = FileDescriptor.class.getDeclaredField("descriptor");
            field.setAccessible(true);
            return field.getInt(fd);
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static void setNativeFd(FileDescriptor fd, int value) {
        try {
            Field field = FileDescriptor.class.getDeclaredField("descriptor");
            field.setAccessible(true);
            field.setInt(fd, value);
        } catch (Exception ignored) {}
    }

    @Override
    public void close() {
        try {
            if (mAnchorStream != null) mAnchorStream.close();
            if (mFileDescriptor != null && mFileDescriptor.valid()) {
                Os.close(mFileDescriptor);
            }
        } catch (Exception ignored) {
        } finally {
            setNativeFd(mFileDescriptor, -1);
        }
    }

    public int getSize() { return mSize; }
    public FileDescriptor getFileDescriptor() { return mFileDescriptor; }
    public int getFd() { return getNativeFd(mFileDescriptor); }

    @Override
    public int describeContents() { return 0x0001; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFileDescriptor(mFileDescriptor);
    }

    public static final Parcelable.Creator<SharedMemory> CREATOR = new Parcelable.Creator<SharedMemory>() {
        @Override
        public SharedMemory createFromParcel(Parcel source) {
            ParcelFileDescriptor pfd = source.readFileDescriptor();
            return pfd != null ? new SharedMemory(pfd.getFileDescriptor(), -1) : null;
        }

        @Override
        public SharedMemory[] newArray(int size) {
            return new SharedMemory[size];
        }
    };
}