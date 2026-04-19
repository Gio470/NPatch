package android.os;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public final class SharedMemory implements Parcelable, Closeable {

    private final FileDescriptor mFileDescriptor;
    private final int mSize;
    private final FileInputStream mAnchorStream;

    private SharedMemory(FileDescriptor fd, int size) {
        this.mFileDescriptor = fd;
        this.mSize = size;
        this.mAnchorStream = new FileInputStream(fd);
    }

    public static SharedMemory create(String name, int size) throws Exception {
        if (size <= 0) throw new IllegalArgumentException("Size must be > 0");
        File tempFile = File.createTempFile("npatch_mem", ".tmp");
        RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
        raf.setLength(size);
        Field field = FileDescriptor.class.getDeclaredField("descriptor");
        field.setAccessible(true);
        FileDescriptor fd = raf.getFD();
        return new SharedMemory(fd, size);
    }

    public ByteBuffer mapReadWrite() throws IOException {
        return mAnchorStream.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, mSize);
    }

    public ByteBuffer mapReadOnly() throws IOException {
        return mAnchorStream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, mSize);
    }

    public void close() {
        try {
            mAnchorStream.close();
        } catch (IOException ignored) {}
    }

    public FileDescriptor getFileDescriptor() { return mFileDescriptor; }
    public int getSize() { return mSize; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFileDescriptor(mFileDescriptor);
    }

        public static final Parcelable.Creator<SharedMemory> CREATOR = new Parcelable.Creator<SharedMemory>() {
        @Override
        public SharedMemory createFromParcel(Parcel source) {
            ParcelFileDescriptor pfd = source.readFileDescriptor();
            return pfd != null ? new SharedMemory(pfd.getFileDescriptor()) : null;
        }

        @Override
        public SharedMemory[] newArray(int size) {
            return new SharedMemory[size];
        }
    };
