package java.rmi.server;
import java.io.*;
import java.util.*;
@Deprecated
public class LogStream extends PrintStream {
    private static Map<String,LogStream> known = new HashMap<>(5);
    private static PrintStream  defaultStream = System.err;
    private String name;
    private OutputStream logOut;
    private OutputStreamWriter logWriter;
    private StringBuffer buffer = new StringBuffer();
    private ByteArrayOutputStream bufOut;
    @Deprecated
    private LogStream(String name, OutputStream out)
    {
        super(new ByteArrayOutputStream());
        bufOut = (ByteArrayOutputStream) super.out;
        this.name = name;
        setOutputStream(out);
    }
    @Deprecated
    public static LogStream log(String name) {
        LogStream stream;
        synchronized (known) {
            stream = known.get(name);
            if (stream == null) {
                stream = new LogStream(name, defaultStream);
            }
            known.put(name, stream);
        }
        return stream;
    }
    @Deprecated
    public static synchronized PrintStream getDefaultStream() {
        return defaultStream;
    }
    @Deprecated
    public static synchronized void setDefaultStream(PrintStream newDefault) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(
                new java.util.logging.LoggingPermission("control", null));
        }
        defaultStream = newDefault;
    }
    @Deprecated
    public synchronized OutputStream getOutputStream()
    {
        return logOut;
    }
    @Deprecated
    public synchronized void setOutputStream(OutputStream out)
    {
        logOut = out;
        // Maintain an OutputStreamWriter with default CharToByteConvertor
        // (just like new PrintStream) for writing log message prefixes.
        logWriter = new OutputStreamWriter(logOut);
    }
    @Deprecated
    public void write(int b)
    {
        if (b == '\n') {
            // synchronize on "this" first to avoid potential deadlock
            synchronized (this) {
                synchronized (logOut) {
                    // construct prefix for log messages:
                    buffer.setLength(0);;
                    buffer.append(              // date/time stamp...
                        (new Date()).toString());
                    buffer.append(':');
                    buffer.append(name);        // ...log name...
                    buffer.append(':');
                    buffer.append(Thread.currentThread().getName());
                    buffer.append(':'); // ...and thread name
                    try {
                        // write prefix through to underlying byte stream
                        logWriter.write(buffer.toString());
                        logWriter.flush();
                        // finally, write the already converted bytes of
                        // the log message
                        bufOut.writeTo(logOut);
                        logOut.write(b);
                        logOut.flush();
                    } catch (IOException e) {
                        setError();
                    } finally {
                        bufOut.reset();
                    }
                }
            }
        }
        else
            super.write(b);
    }
    @Deprecated
    public void write(byte b[], int off, int len)
    {
        if (len < 0)
            throw new ArrayIndexOutOfBoundsException(len);
        for (int i = 0; i < len; ++ i)
            write(b[off + i]);
    }
    @Deprecated
    public String toString()
    {
        return name;
    }
    public static final int SILENT  = 0;
    public static final int BRIEF   = 10;
    public static final int VERBOSE = 20;
    @Deprecated
    public static int parseLevel(String s)
    {
        if ((s == null) || (s.length() < 1))
            return -1;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
        }
        if (s.length() < 1)
            return -1;
        if ("SILENT".startsWith(s.toUpperCase()))
            return SILENT;
        else if ("BRIEF".startsWith(s.toUpperCase()))
            return BRIEF;
        else if ("VERBOSE".startsWith(s.toUpperCase()))
            return VERBOSE;
        return -1;
    }
}
