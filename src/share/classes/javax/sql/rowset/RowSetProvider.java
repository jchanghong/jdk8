package javax.sql.rowset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.SQLException;
import java.util.PropertyPermission;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import javax.sql.rowset.spi.SyncFactoryException;
import sun.reflect.misc.ReflectUtil;
public class RowSetProvider {
    private static final String ROWSET_DEBUG_PROPERTY = "javax.sql.rowset.RowSetProvider.debug";
    private static final String ROWSET_FACTORY_IMPL = "com.sun.rowset.RowSetFactoryImpl";
    private static final String ROWSET_FACTORY_NAME = "javax.sql.rowset.RowSetFactory";
    private static boolean debug = true;
    static {
        // Check to see if the debug property is set
        String val = getSystemProperty(ROWSET_DEBUG_PROPERTY);
        // Allow simply setting the prop to turn on debug
        debug = val != null && !"false".equals(val);
    }
    protected RowSetProvider () {
    }
    public static RowSetFactory newFactory()
            throws SQLException {
        // Use the system property first
        RowSetFactory factory = null;
        String factoryClassName = null;
        try {
            trace("Checking for Rowset System Property...");
            factoryClassName = getSystemProperty(ROWSET_FACTORY_NAME);
            if (factoryClassName != null) {
                trace("Found system property, value=" + factoryClassName);
                factory = (RowSetFactory) ReflectUtil.newInstance(getFactoryClass(factoryClassName, null, true));
            }
        }  catch (Exception e) {
            throw new SQLException( "RowSetFactory: " + factoryClassName +
                    " could not be instantiated: ", e);
        }
        // Check to see if we found the RowSetFactory via a System property
        if (factory == null) {
            // If the RowSetFactory is not found via a System Property, now
            // look it up via the ServiceLoader API and if not found, use the
            // Java SE default.
            factory = loadViaServiceLoader();
            factory =
                    factory == null ? newFactory(ROWSET_FACTORY_IMPL, null) : factory;
        }
        return (factory);
    }
    public static RowSetFactory newFactory(String factoryClassName, ClassLoader cl)
            throws SQLException {
        trace("***In newInstance()");
        if(factoryClassName == null) {
            throw new SQLException("Error: factoryClassName cannot be null");
        }
        try {
            ReflectUtil.checkPackageAccess(factoryClassName);
        } catch (java.security.AccessControlException e) {
            throw new SQLException("Access Exception",e);
        }
        try {
            Class<?> providerClass = getFactoryClass(factoryClassName, cl, false);
            RowSetFactory instance = (RowSetFactory) providerClass.newInstance();
            if (debug) {
                trace("Created new instance of " + providerClass +
                        " using ClassLoader: " + cl);
            }
            return instance;
        } catch (ClassNotFoundException x) {
            throw new SQLException(
                    "Provider " + factoryClassName + " not found", x);
        } catch (Exception x) {
            throw new SQLException(
                    "Provider " + factoryClassName + " could not be instantiated: " + x,
                    x);
        }
    }
    static private ClassLoader getContextClassLoader() throws SecurityException {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                ClassLoader cl = null;
                cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    cl = ClassLoader.getSystemClassLoader();
                }
                return cl;
            }
        });
    }
    static private Class<?> getFactoryClass(String factoryClassName, ClassLoader cl,
            boolean doFallback) throws ClassNotFoundException {
        try {
            if (cl == null) {
                cl = getContextClassLoader();
                if (cl == null) {
                    throw new ClassNotFoundException();
                } else {
                    return cl.loadClass(factoryClassName);
                }
            } else {
                return cl.loadClass(factoryClassName);
            }
        } catch (ClassNotFoundException e) {
            if (doFallback) {
                // Use current class loader
                return Class.forName(factoryClassName, true, RowSetFactory.class.getClassLoader());
            } else {
                throw e;
            }
        }
    }
    static private RowSetFactory loadViaServiceLoader() throws SQLException {
        RowSetFactory theFactory = null;
        try {
            trace("***in loadViaServiceLoader():");
            for (RowSetFactory factory : ServiceLoader.load(javax.sql.rowset.RowSetFactory.class)) {
                trace(" Loading done by the java.util.ServiceLoader :" + factory.getClass().getName());
                theFactory = factory;
                break;
            }
        } catch (ServiceConfigurationError e) {
            throw new SQLException(
                    "RowSetFactory: Error locating RowSetFactory using Service "
                    + "Loader API: " + e, e);
        }
        return theFactory;
    }
    static private String getSystemProperty(final String propName) {
        String property = null;
        try {
            property = AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty(propName);
                }
            }, null, new PropertyPermission(propName, "read"));
        } catch (SecurityException se) {
            trace("error getting " + propName + ":  "+ se);
            if (debug) {
                se.printStackTrace();
            }
        }
        return property;
    }
    private static void trace(String msg) {
        if (debug) {
            System.err.println("###RowSets: " + msg);
        }
    }
}
