package java.util.logging;
import java.io.UnsupportedEncodingException;
public abstract class Handler {
    private static final int offValue = Level.OFF.intValue();
    private final LogManager manager = LogManager.getLogManager();
    // We're using volatile here to avoid synchronizing getters, which
    // would prevent other threads from calling isLoggable()
    // while publish() is executing.
    // On the other hand, setters will be synchronized to exclude concurrent
    // execution with more complex methods, such as StreamHandler.publish().
    // We wouldn't want 'level' to be changed by another thread in the middle
    // of the execution of a 'publish' call.
    private volatile Filter filter;
    private volatile Formatter formatter;
    private volatile Level logLevel = Level.ALL;
    private volatile ErrorManager errorManager = new ErrorManager();
    private volatile String encoding;
    // Package private support for security checking.  When sealed
    // is true, we access check updates to the class.
    boolean sealed = true;
    protected Handler() {
    }
    public abstract void publish(LogRecord record);
    public abstract void flush();
    public abstract void close() throws SecurityException;
    public synchronized void setFormatter(Formatter newFormatter) throws SecurityException {
        checkPermission();
        // Check for a null pointer:
        newFormatter.getClass();
        formatter = newFormatter;
    }
    public Formatter getFormatter() {
        return formatter;
    }
    public synchronized void setEncoding(String encoding)
                        throws SecurityException, java.io.UnsupportedEncodingException {
        checkPermission();
        if (encoding != null) {
            try {
                if(!java.nio.charset.Charset.isSupported(encoding)) {
                    throw new UnsupportedEncodingException(encoding);
                }
            } catch (java.nio.charset.IllegalCharsetNameException e) {
                throw new UnsupportedEncodingException(encoding);
            }
        }
        this.encoding = encoding;
    }
    public String getEncoding() {
        return encoding;
    }
    public synchronized void setFilter(Filter newFilter) throws SecurityException {
        checkPermission();
        filter = newFilter;
    }
    public Filter getFilter() {
        return filter;
    }
    public synchronized void setErrorManager(ErrorManager em) {
        checkPermission();
        if (em == null) {
           throw new NullPointerException();
        }
        errorManager = em;
    }
    public ErrorManager getErrorManager() {
        checkPermission();
        return errorManager;
    }
    protected void reportError(String msg, Exception ex, int code) {
        try {
            errorManager.error(msg, ex, code);
        } catch (Exception ex2) {
            System.err.println("Handler.reportError caught:");
            ex2.printStackTrace();
        }
    }
    public synchronized void setLevel(Level newLevel) throws SecurityException {
        if (newLevel == null) {
            throw new NullPointerException();
        }
        checkPermission();
        logLevel = newLevel;
    }
    public Level getLevel() {
        return logLevel;
    }
    public boolean isLoggable(LogRecord record) {
        final int levelValue = getLevel().intValue();
        if (record.getLevel().intValue() < levelValue || levelValue == offValue) {
            return false;
        }
        final Filter filter = getFilter();
        if (filter == null) {
            return true;
        }
        return filter.isLoggable(record);
    }
    // Package-private support method for security checks.
    // If "sealed" is true, we check that the caller has
    // appropriate security privileges to update Handler
    // state and if not throw a SecurityException.
    void checkPermission() throws SecurityException {
        if (sealed) {
            manager.checkPermission();
        }
    }
}
