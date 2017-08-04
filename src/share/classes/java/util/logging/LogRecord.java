

package java.util.logging;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.io.*;

import sun.misc.JavaLangAccess;
import sun.misc.SharedSecrets;



public class LogRecord implements java.io.Serializable {
    private static final AtomicLong globalSequenceNumber
        = new AtomicLong(0);


    private static final int MIN_SEQUENTIAL_THREAD_ID = Integer.MAX_VALUE / 2;

    private static final AtomicInteger nextThreadId
        = new AtomicInteger(MIN_SEQUENTIAL_THREAD_ID);

    private static final ThreadLocal<Integer> threadIds = new ThreadLocal<>();


    private Level level;


    private long sequenceNumber;


    private String sourceClassName;


    private String sourceMethodName;


    private String message;


    private int threadID;


    private long millis;


    private Throwable thrown;


    private String loggerName;


    private String resourceBundleName;

    private transient boolean needToInferCaller;
    private transient Object parameters[];
    private transient ResourceBundle resourceBundle;


    private int defaultThreadID() {
        long tid = Thread.currentThread().getId();
        if (tid < MIN_SEQUENTIAL_THREAD_ID) {
            return (int) tid;
        } else {
            Integer id = threadIds.get();
            if (id == null) {
                id = nextThreadId.getAndIncrement();
                threadIds.set(id);
            }
            return id;
        }
    }


    public LogRecord(Level level, String msg) {
        // Make sure level isn't null, by calling random method.
        level.getClass();
        this.level = level;
        message = msg;
        // Assign a thread ID and a unique sequence number.
        sequenceNumber = globalSequenceNumber.getAndIncrement();
        threadID = defaultThreadID();
        millis = System.currentTimeMillis();
        needToInferCaller = true;
   }


    public String getLoggerName() {
        return loggerName;
    }


    public void setLoggerName(String name) {
        loggerName = name;
    }


    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }


    public void setResourceBundle(ResourceBundle bundle) {
        resourceBundle = bundle;
    }


    public String getResourceBundleName() {
        return resourceBundleName;
    }


    public void setResourceBundleName(String name) {
        resourceBundleName = name;
    }


    public Level getLevel() {
        return level;
    }


    public void setLevel(Level level) {
        if (level == null) {
            throw new NullPointerException();
        }
        this.level = level;
    }


    public long getSequenceNumber() {
        return sequenceNumber;
    }


    public void setSequenceNumber(long seq) {
        sequenceNumber = seq;
    }


    public String getSourceClassName() {
        if (needToInferCaller) {
            inferCaller();
        }
        return sourceClassName;
    }


    public void setSourceClassName(String sourceClassName) {
        this.sourceClassName = sourceClassName;
        needToInferCaller = false;
    }


    public String getSourceMethodName() {
        if (needToInferCaller) {
            inferCaller();
        }
        return sourceMethodName;
    }


    public void setSourceMethodName(String sourceMethodName) {
        this.sourceMethodName = sourceMethodName;
        needToInferCaller = false;
    }


    public String getMessage() {
        return message;
    }


    public void setMessage(String message) {
        this.message = message;
    }


    public Object[] getParameters() {
        return parameters;
    }


    public void setParameters(Object parameters[]) {
        this.parameters = parameters;
    }


    public int getThreadID() {
        return threadID;
    }


    public void setThreadID(int threadID) {
        this.threadID = threadID;
    }


    public long getMillis() {
        return millis;
    }


    public void setMillis(long millis) {
        this.millis = millis;
    }


    public Throwable getThrown() {
        return thrown;
    }


    public void setThrown(Throwable thrown) {
        this.thrown = thrown;
    }

    private static final long serialVersionUID = 5372048053134512534L;


    private void writeObject(ObjectOutputStream out) throws IOException {
        // We have to call defaultWriteObject first.
        out.defaultWriteObject();

        // Write our version number.
        out.writeByte(1);
        out.writeByte(0);
        if (parameters == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(parameters.length);
        // Write string values for the parameters.
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == null) {
                out.writeObject(null);
            } else {
                out.writeObject(parameters[i].toString());
            }
        }
    }

    private void readObject(ObjectInputStream in)
                        throws IOException, ClassNotFoundException {
        // We have to call defaultReadObject first.
        in.defaultReadObject();

        // Read version number.
        byte major = in.readByte();
        byte minor = in.readByte();
        if (major != 1) {
            throw new IOException("LogRecord: bad version: " + major + "." + minor);
        }
        int len = in.readInt();
        if (len < -1) {
            throw new NegativeArraySizeException();
        } else if (len == -1) {
            parameters = null;
        } else if (len < 255) {
            parameters = new Object[len];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = in.readObject();
            }
        } else {
            List<Object> params = new ArrayList<>(Math.min(len, 1024));
            for (int i = 0; i < len; i++) {
                params.add(in.readObject());
            }
            parameters = params.toArray(new Object[params.size()]);
        }
        // If necessary, try to regenerate the resource bundle.
        if (resourceBundleName != null) {
            try {
                // use system class loader to ensure the ResourceBundle
                // instance is a different instance than null loader uses
                final ResourceBundle bundle =
                        ResourceBundle.getBundle(resourceBundleName,
                                Locale.getDefault(),
                                ClassLoader.getSystemClassLoader());
                resourceBundle = bundle;
            } catch (MissingResourceException ex) {
                // This is not a good place to throw an exception,
                // so we simply leave the resourceBundle null.
                resourceBundle = null;
            }
        }

        needToInferCaller = false;
    }

    // Private method to infer the caller's class and method names
    private void inferCaller() {
        needToInferCaller = false;
        JavaLangAccess access = SharedSecrets.getJavaLangAccess();
        Throwable throwable = new Throwable();
        int depth = access.getStackTraceDepth(throwable);

        boolean lookingForLogger = true;
        for (int ix = 0; ix < depth; ix++) {
            // Calling getStackTraceElement directly prevents the VM
            // from paying the cost of building the entire stack frame.
            StackTraceElement frame =
                access.getStackTraceElement(throwable, ix);
            String cname = frame.getClassName();
            boolean isLoggerImpl = isLoggerImplFrame(cname);
            if (lookingForLogger) {
                // Skip all frames until we have found the first logger frame.
                if (isLoggerImpl) {
                    lookingForLogger = false;
                }
            } else {
                if (!isLoggerImpl) {
                    // skip reflection call
                    if (!cname.startsWith("java.lang.reflect.") && !cname.startsWith("sun.reflect.")) {
                       // We've found the relevant frame.
                       setSourceClassName(cname);
                       setSourceMethodName(frame.getMethodName());
                       return;
                    }
                }
            }
        }
        // We haven't found a suitable frame, so just punt.  This is
        // OK as we are only committed to making a "best effort" here.
    }

    private boolean isLoggerImplFrame(String cname) {
        // the log record could be created for a platform logger
        return (cname.equals("java.util.logging.Logger") ||
                cname.startsWith("java.util.logging.LoggingProxyImpl") ||
                cname.startsWith("sun.util.logging."));
    }
}
