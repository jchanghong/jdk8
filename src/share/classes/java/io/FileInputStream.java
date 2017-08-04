

package java.io;

import java.nio.channels.FileChannel;
import sun.nio.ch.FileChannelImpl;



public
class FileInputStream extends InputStream
{

    private final FileDescriptor fd;


    private final String path;

    private FileChannel channel = null;

    private final Object closeLock = new Object();
    private volatile boolean closed = false;


    public FileInputStream(String name) throws FileNotFoundException {
        this(name != null ? new File(name) : null);
    }


    public FileInputStream(File file) throws FileNotFoundException {
        String name = (file != null ? file.getPath() : null);
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkRead(name);
        }
        if (name == null) {
            throw new NullPointerException();
        }
        if (file.isInvalid()) {
            throw new FileNotFoundException("Invalid file path");
        }
        fd = new FileDescriptor();
        fd.attach(this);
        path = name;
        open(name);
    }


    public FileInputStream(FileDescriptor fdObj) {
        SecurityManager security = System.getSecurityManager();
        if (fdObj == null) {
            throw new NullPointerException();
        }
        if (security != null) {
            security.checkRead(fdObj);
        }
        fd = fdObj;
        path = null;


        fd.attach(this);
    }


    private native void open0(String name) throws FileNotFoundException;

    // wrap native call to allow instrumentation

    private void open(String name) throws FileNotFoundException {
        open0(name);
    }


    public int read() throws IOException {
        return read0();
    }

    private native int read0() throws IOException;


    private native int readBytes(byte b[], int off, int len) throws IOException;


    public int read(byte b[]) throws IOException {
        return readBytes(b, 0, b.length);
    }


    public int read(byte b[], int off, int len) throws IOException {
        return readBytes(b, off, len);
    }


    public long skip(long n) throws IOException {
        return skip0(n);
    }

    private native long skip0(long n) throws IOException;


    public int available() throws IOException {
        return available0();
    }

    private native int available0() throws IOException;


    public void close() throws IOException {
        synchronized (closeLock) {
            if (closed) {
                return;
            }
            closed = true;
        }
        if (channel != null) {
           channel.close();
        }

        fd.closeAll(new Closeable() {
            public void close() throws IOException {
               close0();
           }
        });
    }


    public final FileDescriptor getFD() throws IOException {
        if (fd != null) {
            return fd;
        }
        throw new IOException();
    }


    public FileChannel getChannel() {
        synchronized (this) {
            if (channel == null) {
                channel = FileChannelImpl.open(fd, path, true, false, this);
            }
            return channel;
        }
    }

    private static native void initIDs();

    private native void close0() throws IOException;

    static {
        initIDs();
    }


    protected void finalize() throws IOException {
        if ((fd != null) &&  (fd != FileDescriptor.in)) {

            close();
        }
    }
}
