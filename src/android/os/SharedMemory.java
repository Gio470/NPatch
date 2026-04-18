package android.os;

import android.os.Parcel;
import android.os.Parcelable;
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

    private SharedMemory(FileDescriptor fd) {
        this.mFileDescriptor = fd;
        int size = -1;
        try {
            size = (int) Os.fstat(fd).st_size;
        } catch (ErrnoException ignored) {
        }
        this.mSize = size;
        this.mAnchorStream = new FileInputStream(fd);
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
        return mAnchorStream.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, mSize);
    }

    public ByteBuffer mapReadOnly() throws IOException {
        return mAnchorStream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, mSize);
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

    private static void setNativeFd(FileDescriptor fd, int value) {
        try {
            Field field = FileDescriptor.class.getDeclaredField("descriptor");
            field.setAccessible(true);
            field.setInt(fd, value);
        } catch (Exception ignored) {
        }
    }

    public FileDescriptor getFileDescriptor() {
        return mFileDescriptor;
    }

    public int getSize() {
        return mSize;
    }

    @Override
    public int describeContents() {
        return 1;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFileDescriptor(mFileDescriptor);
    }

    public static final Parcelable.Creator<SharedMemory> CREATOR = new Parcelable.Creator<SharedMemory>() {
        @Override
        public SharedMemory createFromParcel(Parcel source) {
            return new SharedMemory(source.readFileDescriptor());
        }

        @Override
        public SharedMemory[] newArray(int size) {
            return new SharedMemory[size];
        }
    };
    }
