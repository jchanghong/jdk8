


package java.util.logging;

import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;


public class Logger {
    private static final Handler emptyHandlers[] = new Handler[0];
    private static final int offValue = Level.OFF.intValue();

    static final String SYSTEM_LOGGER_RB_NAME = "sun.util.logging.resources.logging";

    // This class is immutable and it is important that it remains so.
    private static final class LoggerBundle {
        final String resourceBundleName; // Base name of the bundle.
        final ResourceBundle userBundle; // Bundle set through setResourceBundle.
        private LoggerBundle(String resourceBundleName, ResourceBundle bundle) {
            this.resourceBundleName = resourceBundleName;
            this.userBundle = bundle;
        }
        boolean isSystemBundle() {
            return SYSTEM_LOGGER_RB_NAME.equals(resourceBundleName);
        }
        static LoggerBundle get(String name, ResourceBundle bundle) {
            if (name == null && bundle == null) {
                return NO_RESOURCE_BUNDLE;
            } else if (SYSTEM_LOGGER_RB_NAME.equals(name) && bundle == null) {
                return SYSTEM_BUNDLE;
            } else {
                return new LoggerBundle(name, bundle);
            }
        }
    }

    // This instance will be shared by all loggers created by the system
    // code
    private static final LoggerBundle SYSTEM_BUNDLE =
            new LoggerBundle(SYSTEM_LOGGER_RB_NAME, null);

    // This instance indicates that no resource bundle has been specified yet,
    // and it will be shared by all loggers which have no resource bundle.
    private static final LoggerBundle NO_RESOURCE_BUNDLE =
            new LoggerBundle(null, null);

    private volatile LogManager manager;
    private String name;
    private final CopyOnWriteArrayList<Handler> handlers =
        new CopyOnWriteArrayList<>();
    private volatile LoggerBundle loggerBundle = NO_RESOURCE_BUNDLE;
    private volatile boolean useParentHandlers = true;
    private volatile Filter filter;
    private boolean anonymous;

    // Cache to speed up behavior of findResourceBundle:
    private ResourceBundle catalog;     // Cached resource bundle
    private String catalogName;         // name associated with catalog
    private Locale catalogLocale;       // locale associated with catalog

    // The fields relating to parent-child relationships and levels
    // are managed under a separate lock, the treeLock.
    private static final Object treeLock = new Object();
    // We keep weak references from parents to children, but strong
    // references from children to parents.
    private volatile Logger parent;    // our nearest parent.
    private ArrayList<LogManager.LoggerWeakRef> kids;   // WeakReferences to loggers that have us as parent
    private volatile Level levelObject;
    private volatile int levelValue;  // current effective level value
    private WeakReference<ClassLoader> callersClassLoaderRef;
    private final boolean isSystemLogger;


    public static final String GLOBAL_LOGGER_NAME = "global";


    public static final Logger getGlobal() {
        // In order to break a cyclic dependence between the LogManager
        // and Logger static initializers causing deadlocks, the global
        // logger is created with a special constructor that does not
        // initialize its log manager.
        //
        // If an application calls Logger.getGlobal() before any logger
        // has been initialized, it is therefore possible that the
        // LogManager class has not been initialized yet, and therefore
        // Logger.global.manager will be null.
        //
        // In order to finish the initialization of the global logger, we
        // will therefore call LogManager.getLogManager() here.
        //
        // To prevent race conditions we also need to call
        // LogManager.getLogManager() unconditionally here.
        // Indeed we cannot rely on the observed value of global.manager,
        // because global.manager will become not null somewhere during
        // the initialization of LogManager.
        // If two threads are calling getGlobal() concurrently, one thread
        // will see global.manager null and call LogManager.getLogManager(),
        // but the other thread could come in at a time when global.manager
        // is already set although ensureLogManagerInitialized is not finished
        // yet...
        // Calling LogManager.getLogManager() unconditionally will fix that.

        LogManager.getLogManager();

        // Now the global LogManager should be initialized,
        // and the global logger should have been added to
        // it, unless we were called within the constructor of a LogManager
        // subclass installed as LogManager, in which case global.manager
        // would still be null, and global will be lazily initialized later on.

        return global;
    }


    @Deprecated
    public static final Logger global = new Logger(GLOBAL_LOGGER_NAME);


    protected Logger(String name, String resourceBundleName) {
        this(name, resourceBundleName, null, LogManager.getLogManager(), false);
    }

    Logger(String name, String resourceBundleName, Class<?> caller, LogManager manager, boolean isSystemLogger) {
        this.manager = manager;
        this.isSystemLogger = isSystemLogger;
        setupResourceInfo(resourceBundleName, caller);
        this.name = name;
        levelValue = Level.INFO.intValue();
    }

    private void setCallersClassLoaderRef(Class<?> caller) {
        ClassLoader callersClassLoader = ((caller != null)
                                         ? caller.getClassLoader()
                                         : null);
        if (callersClassLoader != null) {
            this.callersClassLoaderRef = new WeakReference<>(callersClassLoader);
        }
    }

    private ClassLoader getCallersClassLoader() {
        return (callersClassLoaderRef != null)
                ? callersClassLoaderRef.get()
                : null;
    }

    // This constructor is used only to create the global Logger.
    // It is needed to break a cyclic dependence between the LogManager
    // and Logger static initializers causing deadlocks.
    private Logger(String name) {
        // The manager field is not initialized here.
        this.name = name;
        this.isSystemLogger = true;
        levelValue = Level.INFO.intValue();
    }

    // It is called from LoggerContext.addLocalLogger() when the logger
    // is actually added to a LogManager.
    void setLogManager(LogManager manager) {
        this.manager = manager;
    }

    private void checkPermission() throws SecurityException {
        if (!anonymous) {
            if (manager == null) {
                // Complete initialization of the global Logger.
                manager = LogManager.getLogManager();
            }
            manager.checkPermission();
        }
    }

    // Until all JDK code converted to call sun.util.logging.PlatformLogger
    // (see 7054233), we need to determine if Logger.getLogger is to add
    // a system logger or user logger.
    //
    // As an interim solution, if the immediate caller whose caller loader is
    // null, we assume it's a system logger and add it to the system context.
    // These system loggers only set the resource bundle to the given
    // resource bundle name (rather than the default system resource bundle).
    private static class SystemLoggerHelper {
        static boolean disableCallerCheck = getBooleanProperty("sun.util.logging.disableCallerCheck");
        private static boolean getBooleanProperty(final String key) {
            String s = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(key);
                }
            });
            return Boolean.valueOf(s);
        }
    }

    private static Logger demandLogger(String name, String resourceBundleName, Class<?> caller) {
        LogManager manager = LogManager.getLogManager();
        SecurityManager sm = System.getSecurityManager();
        if (sm != null && !SystemLoggerHelper.disableCallerCheck) {
            if (caller.getClassLoader() == null) {
                return manager.demandSystemLogger(name, resourceBundleName);
            }
        }
        return manager.demandLogger(name, resourceBundleName, caller);
        // ends up calling new Logger(name, resourceBundleName, caller)
        // iff the logger doesn't exist already
    }



    // Synchronization is not required here. All synchronization for
    // adding a new Logger object is handled by LogManager.addLogger().
    @CallerSensitive
    public static Logger getLogger(String name) {
        // This method is intentionally not a wrapper around a call
        // to getLogger(name, resourceBundleName). If it were then
        // this sequence:
        //
        //     getLogger("Foo", "resourceBundleForFoo");
        //     getLogger("Foo");
        //
        // would throw an IllegalArgumentException in the second call
        // because the wrapper would result in an attempt to replace
        // the existing "resourceBundleForFoo" with null.
        return demandLogger(name, null, Reflection.getCallerClass());
    }



    // Synchronization is not required here. All synchronization for
    // adding a new Logger object is handled by LogManager.addLogger().
    @CallerSensitive
    public static Logger getLogger(String name, String resourceBundleName) {
        Class<?> callerClass = Reflection.getCallerClass();
        Logger result = demandLogger(name, resourceBundleName, callerClass);

        // MissingResourceException or IllegalArgumentException can be
        // thrown by setupResourceInfo().
        // We have to set the callers ClassLoader here in case demandLogger
        // above found a previously created Logger.  This can happen, for
        // example, if Logger.getLogger(name) is called and subsequently
        // Logger.getLogger(name, resourceBundleName) is called.  In this case
        // we won't necessarily have the correct classloader saved away, so
        // we need to set it here, too.

        result.setupResourceInfo(resourceBundleName, callerClass);
        return result;
    }

    // package-private
    // Add a platform logger to the system context.
    // i.e. caller of sun.util.logging.PlatformLogger.getLogger
    static Logger getPlatformLogger(String name) {
        LogManager manager = LogManager.getLogManager();

        // all loggers in the system context will default to
        // the system logger's resource bundle
        Logger result = manager.demandSystemLogger(name, SYSTEM_LOGGER_RB_NAME);
        return result;
    }


    public static Logger getAnonymousLogger() {
        return getAnonymousLogger(null);
    }



    // Synchronization is not required here. All synchronization for
    // adding a new anonymous Logger object is handled by doSetParent().
    @CallerSensitive
    public static Logger getAnonymousLogger(String resourceBundleName) {
        LogManager manager = LogManager.getLogManager();
        // cleanup some Loggers that have been GC'ed
        manager.drainLoggerRefQueueBounded();
        Logger result = new Logger(null, resourceBundleName,
                                   Reflection.getCallerClass(), manager, false);
        result.anonymous = true;
        Logger root = manager.getLogger("");
        result.doSetParent(root);
        return result;
    }


    public ResourceBundle getResourceBundle() {
        return findResourceBundle(getResourceBundleName(), true);
    }


    public String getResourceBundleName() {
        return loggerBundle.resourceBundleName;
    }


    public void setFilter(Filter newFilter) throws SecurityException {
        checkPermission();
        filter = newFilter;
    }


    public Filter getFilter() {
        return filter;
    }


    public void log(LogRecord record) {
        if (!isLoggable(record.getLevel())) {
            return;
        }
        Filter theFilter = filter;
        if (theFilter != null && !theFilter.isLoggable(record)) {
            return;
        }

        // Post the LogRecord to all our Handlers, and then to
        // our parents' handlers, all the way up the tree.

        Logger logger = this;
        while (logger != null) {
            final Handler[] loggerHandlers = isSystemLogger
                ? logger.accessCheckedHandlers()
                : logger.getHandlers();

            for (Handler handler : loggerHandlers) {
                handler.publish(record);
            }

            final boolean useParentHdls = isSystemLogger
                ? logger.useParentHandlers
                : logger.getUseParentHandlers();

            if (!useParentHdls) {
                break;
            }

            logger = isSystemLogger ? logger.parent : logger.getParent();
        }
    }

    // private support method for logging.
    // We fill in the logger name, resource bundle name, and
    // resource bundle and then call "void log(LogRecord)".
    private void doLog(LogRecord lr) {
        lr.setLoggerName(name);
        final LoggerBundle lb = getEffectiveLoggerBundle();
        final ResourceBundle  bundle = lb.userBundle;
        final String ebname = lb.resourceBundleName;
        if (ebname != null && bundle != null) {
            lr.setResourceBundleName(ebname);
            lr.setResourceBundle(bundle);
        }
        log(lr);
    }


    //================================================================
    // Start of convenience methods WITHOUT className and methodName
    //================================================================


    public void log(Level level, String msg) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        doLog(lr);
    }


    public void log(Level level, Supplier<String> msgSupplier) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msgSupplier.get());
        doLog(lr);
    }


    public void log(Level level, String msg, Object param1) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        Object params[] = { param1 };
        lr.setParameters(params);
        doLog(lr);
    }


    public void log(Level level, String msg, Object params[]) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setParameters(params);
        doLog(lr);
    }


    public void log(Level level, String msg, Throwable thrown) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setThrown(thrown);
        doLog(lr);
    }


    public void log(Level level, Throwable thrown, Supplier<String> msgSupplier) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msgSupplier.get());
        lr.setThrown(thrown);
        doLog(lr);
    }

    //================================================================
    // Start of convenience methods WITH className and methodName
    //================================================================


    public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        doLog(lr);
    }


    public void logp(Level level, String sourceClass, String sourceMethod,
                     Supplier<String> msgSupplier) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msgSupplier.get());
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        doLog(lr);
    }


    public void logp(Level level, String sourceClass, String sourceMethod,
                                                String msg, Object param1) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        Object params[] = { param1 };
        lr.setParameters(params);
        doLog(lr);
    }


    public void logp(Level level, String sourceClass, String sourceMethod,
                                                String msg, Object params[]) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setParameters(params);
        doLog(lr);
    }


    public void logp(Level level, String sourceClass, String sourceMethod,
                     String msg, Throwable thrown) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        doLog(lr);
    }


    public void logp(Level level, String sourceClass, String sourceMethod,
                     Throwable thrown, Supplier<String> msgSupplier) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msgSupplier.get());
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        doLog(lr);
    }


    //=========================================================================
    // Start of convenience methods WITH className, methodName and bundle name.
    //=========================================================================

    // Private support method for logging for "logrb" methods.
    // We fill in the logger name, resource bundle name, and
    // resource bundle and then call "void log(LogRecord)".
    private void doLog(LogRecord lr, String rbname) {
        lr.setLoggerName(name);
        if (rbname != null) {
            lr.setResourceBundleName(rbname);
            lr.setResourceBundle(findResourceBundle(rbname, false));
        }
        log(lr);
    }

    // Private support method for logging for "logrb" methods.
    private void doLog(LogRecord lr, ResourceBundle rb) {
        lr.setLoggerName(name);
        if (rb != null) {
            lr.setResourceBundleName(rb.getBaseBundleName());
            lr.setResourceBundle(rb);
        }
        log(lr);
    }


    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod,
                                String bundleName, String msg) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        doLog(lr, bundleName);
    }


    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod,
                                String bundleName, String msg, Object param1) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        Object params[] = { param1 };
        lr.setParameters(params);
        doLog(lr, bundleName);
    }


    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod,
                                String bundleName, String msg, Object params[]) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setParameters(params);
        doLog(lr, bundleName);
    }


    public void logrb(Level level, String sourceClass, String sourceMethod,
                      ResourceBundle bundle, String msg, Object... params) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        if (params != null && params.length != 0) {
            lr.setParameters(params);
        }
        doLog(lr, bundle);
    }


    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod,
                                        String bundleName, String msg, Throwable thrown) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        doLog(lr, bundleName);
    }


    public void logrb(Level level, String sourceClass, String sourceMethod,
                      ResourceBundle bundle, String msg, Throwable thrown) {
        if (!isLoggable(level)) {
            return;
        }
        LogRecord lr = new LogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        doLog(lr, bundle);
    }

    //======================================================================
    // Start of convenience methods for logging method entries and returns.
    //======================================================================


    public void entering(String sourceClass, String sourceMethod) {
        logp(Level.FINER, sourceClass, sourceMethod, "ENTRY");
    }


    public void entering(String sourceClass, String sourceMethod, Object param1) {
        logp(Level.FINER, sourceClass, sourceMethod, "ENTRY {0}", param1);
    }


    public void entering(String sourceClass, String sourceMethod, Object params[]) {
        String msg = "ENTRY";
        if (params == null ) {
           logp(Level.FINER, sourceClass, sourceMethod, msg);
           return;
        }
        if (!isLoggable(Level.FINER)) return;
        for (int i = 0; i < params.length; i++) {
            msg = msg + " {" + i + "}";
        }
        logp(Level.FINER, sourceClass, sourceMethod, msg, params);
    }


    public void exiting(String sourceClass, String sourceMethod) {
        logp(Level.FINER, sourceClass, sourceMethod, "RETURN");
    }



    public void exiting(String sourceClass, String sourceMethod, Object result) {
        logp(Level.FINER, sourceClass, sourceMethod, "RETURN {0}", result);
    }


    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
        if (!isLoggable(Level.FINER)) {
            return;
        }
        LogRecord lr = new LogRecord(Level.FINER, "THROW");
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        doLog(lr);
    }

    //=======================================================================
    // Start of simple convenience methods using level names as method names
    //=======================================================================


    public void severe(String msg) {
        log(Level.SEVERE, msg);
    }


    public void warning(String msg) {
        log(Level.WARNING, msg);
    }


    public void info(String msg) {
        log(Level.INFO, msg);
    }


    public void config(String msg) {
        log(Level.CONFIG, msg);
    }


    public void fine(String msg) {
        log(Level.FINE, msg);
    }


    public void finer(String msg) {
        log(Level.FINER, msg);
    }


    public void finest(String msg) {
        log(Level.FINEST, msg);
    }

    //=======================================================================
    // Start of simple convenience methods using level names as method names
    // and use Supplier<String>
    //=======================================================================


    public void severe(Supplier<String> msgSupplier) {
        log(Level.SEVERE, msgSupplier);
    }


    public void warning(Supplier<String> msgSupplier) {
        log(Level.WARNING, msgSupplier);
    }


    public void info(Supplier<String> msgSupplier) {
        log(Level.INFO, msgSupplier);
    }


    public void config(Supplier<String> msgSupplier) {
        log(Level.CONFIG, msgSupplier);
    }


    public void fine(Supplier<String> msgSupplier) {
        log(Level.FINE, msgSupplier);
    }


    public void finer(Supplier<String> msgSupplier) {
        log(Level.FINER, msgSupplier);
    }


    public void finest(Supplier<String> msgSupplier) {
        log(Level.FINEST, msgSupplier);
    }

    //================================================================
    // End of convenience methods
    //================================================================


    public void setLevel(Level newLevel) throws SecurityException {
        checkPermission();
        synchronized (treeLock) {
            levelObject = newLevel;
            updateEffectiveLevel();
        }
    }

    final boolean isLevelInitialized() {
        return levelObject != null;
    }


    public Level getLevel() {
        return levelObject;
    }


    public boolean isLoggable(Level level) {
        if (level.intValue() < levelValue || levelValue == offValue) {
            return false;
        }
        return true;
    }


    public String getName() {
        return name;
    }


    public void addHandler(Handler handler) throws SecurityException {
        // Check for null handler
        handler.getClass();
        checkPermission();
        handlers.add(handler);
    }


    public void removeHandler(Handler handler) throws SecurityException {
        checkPermission();
        if (handler == null) {
            return;
        }
        handlers.remove(handler);
    }


    public Handler[] getHandlers() {
        return accessCheckedHandlers();
    }

    // This method should ideally be marked final - but unfortunately
    // it needs to be overridden by LogManager.RootLogger
    Handler[] accessCheckedHandlers() {
        return handlers.toArray(emptyHandlers);
    }


    public void setUseParentHandlers(boolean useParentHandlers) {
        checkPermission();
        this.useParentHandlers = useParentHandlers;
    }


    public boolean getUseParentHandlers() {
        return useParentHandlers;
    }

    private static ResourceBundle findSystemResourceBundle(final Locale locale) {
        // the resource bundle is in a restricted package
        return AccessController.doPrivileged(new PrivilegedAction<ResourceBundle>() {
            @Override
            public ResourceBundle run() {
                try {
                    return ResourceBundle.getBundle(SYSTEM_LOGGER_RB_NAME,
                                                    locale,
                                                    ClassLoader.getSystemClassLoader());
                } catch (MissingResourceException e) {
                    throw new InternalError(e.toString());
                }
            }
        });
    }


    private synchronized ResourceBundle findResourceBundle(String name,
                                                           boolean useCallersClassLoader) {
        // For all lookups, we first check the thread context class loader
        // if it is set.  If not, we use the system classloader.  If we
        // still haven't found it we use the callersClassLoaderRef if it
        // is set and useCallersClassLoader is true.  We set
        // callersClassLoaderRef initially upon creating the logger with a
        // non-null resource bundle name.

        // Return a null bundle for a null name.
        if (name == null) {
            return null;
        }

        Locale currentLocale = Locale.getDefault();
        final LoggerBundle lb = loggerBundle;

        // Normally we should hit on our simple one entry cache.
        if (lb.userBundle != null &&
                name.equals(lb.resourceBundleName)) {
            return lb.userBundle;
        } else if (catalog != null && currentLocale.equals(catalogLocale)
                && name.equals(catalogName)) {
            return catalog;
        }

        if (name.equals(SYSTEM_LOGGER_RB_NAME)) {
            catalog = findSystemResourceBundle(currentLocale);
            catalogName = name;
            catalogLocale = currentLocale;
            return catalog;
        }

        // Use the thread's context ClassLoader.  If there isn't one, use the
        // {@linkplain java.lang.ClassLoader#getSystemClassLoader() system ClassLoader}.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        try {
            catalog = ResourceBundle.getBundle(name, currentLocale, cl);
            catalogName = name;
            catalogLocale = currentLocale;
            return catalog;
        } catch (MissingResourceException ex) {
            // We can't find the ResourceBundle in the default
            // ClassLoader.  Drop through.
        }

        if (useCallersClassLoader) {
            // Try with the caller's ClassLoader
            ClassLoader callersClassLoader = getCallersClassLoader();

            if (callersClassLoader == null || callersClassLoader == cl) {
                return null;
            }

            try {
                catalog = ResourceBundle.getBundle(name, currentLocale,
                                                   callersClassLoader);
                catalogName = name;
                catalogLocale = currentLocale;
                return catalog;
            } catch (MissingResourceException ex) {
                return null; // no luck
            }
        } else {
            return null;
        }
    }

    // Private utility method to initialize our one entry
    // resource bundle name cache and the callers ClassLoader
    // Note: for consistency reasons, we are careful to check
    // that a suitable ResourceBundle exists before setting the
    // resourceBundleName field.
    // Synchronized to prevent races in setting the fields.
    private synchronized void setupResourceInfo(String name,
                                                Class<?> callersClass) {
        final LoggerBundle lb = loggerBundle;
        if (lb.resourceBundleName != null) {
            // this Logger already has a ResourceBundle

            if (lb.resourceBundleName.equals(name)) {
                // the names match so there is nothing more to do
                return;
            }

            // cannot change ResourceBundles once they are set
            throw new IllegalArgumentException(
                lb.resourceBundleName + " != " + name);
        }

        if (name == null) {
            return;
        }

        setCallersClassLoaderRef(callersClass);
        if (isSystemLogger && getCallersClassLoader() != null) {
            checkPermission();
        }
        if (findResourceBundle(name, true) == null) {
            // We've failed to find an expected ResourceBundle.
            // unset the caller's ClassLoader since we were unable to find the
            // the bundle using it
            this.callersClassLoaderRef = null;
            throw new MissingResourceException("Can't find " + name + " bundle",
                                                name, "");
        }

        // if lb.userBundle is not null we won't reach this line.
        assert lb.userBundle == null;
        loggerBundle = LoggerBundle.get(name, null);
    }


    public void setResourceBundle(ResourceBundle bundle) {
        checkPermission();

        // Will throw NPE if bundle is null.
        final String baseName = bundle.getBaseBundleName();

        // bundle must have a name
        if (baseName == null || baseName.isEmpty()) {
            throw new IllegalArgumentException("resource bundle must have a name");
        }

        synchronized (this) {
            LoggerBundle lb = loggerBundle;
            final boolean canReplaceResourceBundle = lb.resourceBundleName == null
                    || lb.resourceBundleName.equals(baseName);

            if (!canReplaceResourceBundle) {
                throw new IllegalArgumentException("can't replace resource bundle");
            }


            loggerBundle = LoggerBundle.get(baseName, bundle);
        }
    }


    public Logger getParent() {
        // Note: this used to be synchronized on treeLock.  However, this only
        // provided memory semantics, as there was no guarantee that the caller
        // would synchronize on treeLock (in fact, there is no way for external
        // callers to so synchronize).  Therefore, we have made parent volatile
        // instead.
        return parent;
    }


    public void setParent(Logger parent) {
        if (parent == null) {
            throw new NullPointerException();
        }

        // check permission for all loggers, including anonymous loggers
        if (manager == null) {
            manager = LogManager.getLogManager();
        }
        manager.checkPermission();

        doSetParent(parent);
    }

    // Private method to do the work for parenting a child
    // Logger onto a parent logger.
    private void doSetParent(Logger newParent) {

        // System.err.println("doSetParent \"" + getName() + "\" \""
        //                              + newParent.getName() + "\"");

        synchronized (treeLock) {

            // Remove ourself from any previous parent.
            LogManager.LoggerWeakRef ref = null;
            if (parent != null) {
                // assert parent.kids != null;
                for (Iterator<LogManager.LoggerWeakRef> iter = parent.kids.iterator(); iter.hasNext(); ) {
                    ref = iter.next();
                    Logger kid =  ref.get();
                    if (kid == this) {
                        // ref is used down below to complete the reparenting
                        iter.remove();
                        break;
                    } else {
                        ref = null;
                    }
                }
                // We have now removed ourself from our parents' kids.
            }

            // Set our new parent.
            parent = newParent;
            if (parent.kids == null) {
                parent.kids = new ArrayList<>(2);
            }
            if (ref == null) {
                // we didn't have a previous parent
                ref = manager.new LoggerWeakRef(this);
            }
            ref.setParentRef(new WeakReference<>(parent));
            parent.kids.add(ref);

            // As a result of the reparenting, the effective level
            // may have changed for us and our children.
            updateEffectiveLevel();

        }
    }

    // Package-level method.
    // Remove the weak reference for the specified child Logger from the
    // kid list. We should only be called from LoggerWeakRef.dispose().
    final void removeChildLogger(LogManager.LoggerWeakRef child) {
        synchronized (treeLock) {
            for (Iterator<LogManager.LoggerWeakRef> iter = kids.iterator(); iter.hasNext(); ) {
                LogManager.LoggerWeakRef ref = iter.next();
                if (ref == child) {
                    iter.remove();
                    return;
                }
            }
        }
    }

    // Recalculate the effective level for this node and
    // recursively for our children.

    private void updateEffectiveLevel() {
        // assert Thread.holdsLock(treeLock);

        // Figure out our current effective level.
        int newLevelValue;
        if (levelObject != null) {
            newLevelValue = levelObject.intValue();
        } else {
            if (parent != null) {
                newLevelValue = parent.levelValue;
            } else {
                // This may happen during initialization.
                newLevelValue = Level.INFO.intValue();
            }
        }

        // If our effective value hasn't changed, we're done.
        if (levelValue == newLevelValue) {
            return;
        }

        levelValue = newLevelValue;

        // System.err.println("effective level: \"" + getName() + "\" := " + level);

        // Recursively update the level on each of our kids.
        if (kids != null) {
            for (int i = 0; i < kids.size(); i++) {
                LogManager.LoggerWeakRef ref = kids.get(i);
                Logger kid =  ref.get();
                if (kid != null) {
                    kid.updateEffectiveLevel();
                }
            }
        }
    }


    // Private method to get the potentially inherited
    // resource bundle and resource bundle name for this Logger.
    // This method never returns null.
    private LoggerBundle getEffectiveLoggerBundle() {
        final LoggerBundle lb = loggerBundle;
        if (lb.isSystemBundle()) {
            return SYSTEM_BUNDLE;
        }

        // first take care of this logger
        final ResourceBundle b = getResourceBundle();
        if (b != null && b == lb.userBundle) {
            return lb;
        } else if (b != null) {
            // either lb.userBundle is null or getResourceBundle() is
            // overriden
            final String rbName = getResourceBundleName();
            return LoggerBundle.get(rbName, b);
        }

        // no resource bundle was specified on this logger, look up the
        // parent stack.
        Logger target = this.parent;
        while (target != null) {
            final LoggerBundle trb = target.loggerBundle;
            if (trb.isSystemBundle()) {
                return SYSTEM_BUNDLE;
            }
            if (trb.userBundle != null) {
                return trb;
            }
            final String rbName = isSystemLogger
                // ancestor of a system logger is expected to be a system logger.
                // ignore resource bundle name if it's not.
                ? (target.isSystemLogger ? trb.resourceBundleName : null)
                : target.getResourceBundleName();
            if (rbName != null) {
                return LoggerBundle.get(rbName,
                        findResourceBundle(rbName, true));
            }
            target = isSystemLogger ? target.parent : target.getParent();
        }
        return NO_RESOURCE_BUNDLE;
    }

}
