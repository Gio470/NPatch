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
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ParcelFileDescriptor;
import android.os.MemoryFile;

public final class SharedMemory implements Parcelable, Closeable {

    private final FileDescriptor mFileDescriptor;
    private final int mSize;
    private FileInputStream mAnchorStream; 

    private SharedMemory(FileDescriptor fd) {
        this.mFileDescriptor = fd;
        int size = -1;
        try {
            size = (int) Os.fstat(mFileDescriptor).st_size;
        } catch (ErrnoException e) {
        }
        this.mSize = size;
        this.mAnchorStream = new FileInputStream(mFileDescriptor);
    }

    public static SharedMemory create(String name, int size) throws Exception {
        if (size <= 0) throw new IllegalArgumentException("Size must be > 0");
        
        MemoryFile memoryFile = new MemoryFile(name, size);
        
        Method getFdMethod = MemoryFile.class.getDeclaredMethod("getFileDescriptor");
        getFdMethod.setAccessible(true);
        FileDescriptor fd = (FileDescriptor) getFdMethod.invoke(memoryFile);
        
        return new SharedMemory(fd);
    }

    public ByteBuffer mapReadWrite() throws IOException {
        return map(FileChannel.MapMode.READ_WRITE, 0, mSize);
    }

    public ByteBuffer map(FileChannel.MapMode mode, int offset, int length) throws IOException {
        if (getNativeFd(mFileDescriptor) == -1) {
            throw new IllegalStateException("SharedMemory is closed");
        }
        return mAnchorStream.getChannel().map(mode, offset, length);
    }

    private static int getNativeFd(FileDescriptor fd) {
        try {
            Field field = FileDescriptor.class.getDeclaredField("descriptor");
            field.setAccessible(true);
            return field.getInt(fd);
        } catch (Exception e) {
            return -1;
        }
    }

    private static void setNativeFd(FileDescriptor fd, int nativeFd) {
        try {
            Field field = FileDescriptor.class.getDeclaredField("descriptor");
            field.setAccessible(true);
            field.setInt(fd, nativeFd);
        } catch (Exception e) {}
    }

    @Override
    public void close() {
        try {
            if (mAnchorStream != null) {
                mAnchorStream.close();
            }
            Os.close(mFileDescriptor);
        } catch (Exception e) {
        } finally {
            setNativeFd(mFileDescriptor, -1);
        }
    }

    @Override
    public int describeContents() {
        return 0x0001;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFileDescriptor(mFileDescriptor);
    }

    public static final Parcelable.Creator<SharedMemory> CREATOR = new Parcelable.Creator<SharedMemory>() {
        @Override
        public SharedMemory createFromParcel(Parcel source) {
            ParcelFileDescriptor pfd = source.readFileDescriptor();
            if (pfd == null) return null;
            return new SharedMemory(pfd.getFileDescriptor());
        }

        @Override
        public SharedMemory[] newArray(int size) {
            return new SharedMemory[size];
        }
    };

    public int getSize() { return mSize; }
    public FileDescriptor getFileDescriptor() { return mFileDescriptor; }
  }
