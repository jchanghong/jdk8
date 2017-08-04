package javax.sql.rowset.spi;
import java.util.logging.*;
import java.util.*;
import java.sql.*;
import javax.sql.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.naming.*;
import sun.reflect.misc.ReflectUtil;
public class SyncFactory {
    private SyncFactory() {
    }
    public static final String ROWSET_SYNC_PROVIDER =
            "rowset.provider.classname";
    public static final String ROWSET_SYNC_VENDOR =
            "rowset.provider.vendor";
    public static final String ROWSET_SYNC_PROVIDER_VERSION =
            "rowset.provider.version";
    private static String ROWSET_PROPERTIES = "rowset.properties";
    private static final SQLPermission SET_SYNCFACTORY_PERMISSION =
            new SQLPermission("setSyncFactory");
    private static Context ic;
    private static volatile Logger rsLogger;
    private static Hashtable<String, SyncProvider> implementations;
    public static synchronized void registerProvider(String providerID)
            throws SyncFactoryException {
        ProviderImpl impl = new ProviderImpl();
        impl.setClassname(providerID);
        initMapIfNecessary();
        implementations.put(providerID, impl);
    }
    public static SyncFactory getSyncFactory() {
        return SyncFactoryHolder.factory;
    }
    public static synchronized void unregisterProvider(String providerID)
            throws SyncFactoryException {
        initMapIfNecessary();
        if (implementations.containsKey(providerID)) {
            implementations.remove(providerID);
        }
    }
    private static String colon = ":";
    private static String strFileSep = "/";
    private static synchronized void initMapIfNecessary() throws SyncFactoryException {
        // Local implementation class names and keys from Properties
        // file, translate names into Class objects using Class.forName
        // and store mappings
        final Properties properties = new Properties();
        if (implementations == null) {
            implementations = new Hashtable<>();
            try {
                // check if user is supplying his Synchronisation Provider
                // Implementation if not using Oracle's implementation.
                // properties.load(new FileInputStream(ROWSET_PROPERTIES));
                // The rowset.properties needs to be in jdk/jre/lib when
                // integrated with jdk.
                // else it should be picked from -D option from command line.
                // -Drowset.properties will add to standard properties. Similar
                // keys will over-write
                String strRowsetProperties;
                try {
                    strRowsetProperties = AccessController.doPrivileged(new PrivilegedAction<String>() {
                        public String run() {
                            return System.getProperty("rowset.properties");
                        }
                    }, null, new PropertyPermission("rowset.properties", "read"));
                } catch (Exception ex) {
                    System.out.println("errorget rowset.properties: " + ex);
                    strRowsetProperties = null;
                };
                if (strRowsetProperties != null) {
                    // Load user's implementation of SyncProvider
                    // here. -Drowset.properties=/abc/def/pqr.txt
                    ROWSET_PROPERTIES = strRowsetProperties;
                    try (FileInputStream fis = new FileInputStream(ROWSET_PROPERTIES)) {
                        properties.load(fis);
                    }
                    parseProperties(properties);
                }
                ROWSET_PROPERTIES = "javax" + strFileSep + "sql" +
                        strFileSep + "rowset" + strFileSep +
                        "rowset.properties";
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                try {
                    AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                        try (InputStream stream = (cl == null) ?
                                ClassLoader.getSystemResourceAsStream(ROWSET_PROPERTIES)
                                : cl.getResourceAsStream(ROWSET_PROPERTIES)) {
                            if (stream == null) {
                                throw new SyncFactoryException("Resource " + ROWSET_PROPERTIES + " not found");
                            }
                            properties.load(stream);
                        }
                        return null;
                    });
                } catch (PrivilegedActionException ex) {
                    Throwable e = ex.getException();
                    if (e instanceof SyncFactoryException) {
                      throw (SyncFactoryException) e;
                    } else {
                        SyncFactoryException sfe = new SyncFactoryException();
                        sfe.initCause(ex.getException());
                        throw sfe;
                    }
                }
                parseProperties(properties);
            // removed else, has properties should sum together
            } catch (FileNotFoundException e) {
                throw new SyncFactoryException("Cannot locate properties file: " + e);
            } catch (IOException e) {
                throw new SyncFactoryException("IOException: " + e);
            }
            properties.clear();
            String providerImpls;
            try {
                providerImpls = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    public String run() {
                        return System.getProperty(ROWSET_SYNC_PROVIDER);
                    }
                }, null, new PropertyPermission(ROWSET_SYNC_PROVIDER, "read"));
            } catch (Exception ex) {
                providerImpls = null;
            }
            if (providerImpls != null) {
                int i = 0;
                if (providerImpls.indexOf(colon) > 0) {
                    StringTokenizer tokenizer = new StringTokenizer(providerImpls, colon);
                    while (tokenizer.hasMoreElements()) {
                        properties.put(ROWSET_SYNC_PROVIDER + "." + i, tokenizer.nextToken());
                        i++;
                    }
                } else {
                    properties.put(ROWSET_SYNC_PROVIDER, providerImpls);
                }
                parseProperties(properties);
            }
        }
    }
    private static boolean debug = false;
    private static int providerImplIndex = 0;
    private static void parseProperties(Properties p) {
        ProviderImpl impl = null;
        String key = null;
        String[] propertyNames = null;
        for (Enumeration<?> e = p.propertyNames(); e.hasMoreElements();) {
            String str = (String) e.nextElement();
            int w = str.length();
            if (str.startsWith(SyncFactory.ROWSET_SYNC_PROVIDER)) {
                impl = new ProviderImpl();
                impl.setIndex(providerImplIndex++);
                if (w == (SyncFactory.ROWSET_SYNC_PROVIDER).length()) {
                    // no property index has been set.
                    propertyNames = getPropertyNames(false);
                } else {
                    // property index has been set.
                    propertyNames = getPropertyNames(true, str.substring(w - 1));
                }
                key = p.getProperty(propertyNames[0]);
                impl.setClassname(key);
                impl.setVendor(p.getProperty(propertyNames[1]));
                impl.setVersion(p.getProperty(propertyNames[2]));
                implementations.put(key, impl);
            }
        }
    }
    private static String[] getPropertyNames(boolean append) {
        return getPropertyNames(append, null);
    }
    private static String[] getPropertyNames(boolean append,
            String propertyIndex) {
        String dot = ".";
        String[] propertyNames =
                new String[]{SyncFactory.ROWSET_SYNC_PROVIDER,
            SyncFactory.ROWSET_SYNC_VENDOR,
            SyncFactory.ROWSET_SYNC_PROVIDER_VERSION};
        if (append) {
            for (int i = 0; i < propertyNames.length; i++) {
                propertyNames[i] = propertyNames[i] +
                        dot +
                        propertyIndex;
            }
            return propertyNames;
        } else {
            return propertyNames;
        }
    }
    private static void showImpl(ProviderImpl impl) {
        System.out.println("Provider implementation:");
        System.out.println("Classname: " + impl.getClassname());
        System.out.println("Vendor: " + impl.getVendor());
        System.out.println("Version: " + impl.getVersion());
        System.out.println("Impl index: " + impl.getIndex());
    }
    public static SyncProvider getInstance(String providerID)
            throws SyncFactoryException {
        if(providerID == null) {
            throw new SyncFactoryException("The providerID cannot be null");
        }
        initMapIfNecessary(); // populate HashTable
        initJNDIContext();    // check JNDI context for any additional bindings
        ProviderImpl impl = (ProviderImpl) implementations.get(providerID);
        if (impl == null) {
            // Requested SyncProvider is unavailable. Return default provider.
            return new com.sun.rowset.providers.RIOptimisticProvider();
        }
        try {
            ReflectUtil.checkPackageAccess(providerID);
        } catch (java.security.AccessControlException e) {
            SyncFactoryException sfe = new SyncFactoryException();
            sfe.initCause(e);
            throw sfe;
        }
        // Attempt to invoke classname from registered SyncProvider list
        Class<?> c = null;
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            c = Class.forName(providerID, true, cl);
            if (c != null) {
                return (SyncProvider) c.newInstance();
            } else {
                return new com.sun.rowset.providers.RIOptimisticProvider();
            }
        } catch (IllegalAccessException e) {
            throw new SyncFactoryException("IllegalAccessException: " + e.getMessage());
        } catch (InstantiationException e) {
            throw new SyncFactoryException("InstantiationException: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new SyncFactoryException("ClassNotFoundException: " + e.getMessage());
        }
    }
    public static Enumeration<SyncProvider> getRegisteredProviders()
            throws SyncFactoryException {
        initMapIfNecessary();
        // return a collection of classnames
        // of type SyncProvider
        return implementations.elements();
    }
    public static void setLogger(Logger logger) {
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            sec.checkPermission(SET_SYNCFACTORY_PERMISSION);
        }
        if(logger == null){
            throw new NullPointerException("You must provide a Logger");
        }
        rsLogger = logger;
    }
    public static void setLogger(Logger logger, Level level) {
        // singleton
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            sec.checkPermission(SET_SYNCFACTORY_PERMISSION);
        }
        if(logger == null){
            throw new NullPointerException("You must provide a Logger");
        }
        logger.setLevel(level);
        rsLogger = logger;
    }
    public static Logger getLogger() throws SyncFactoryException {
        Logger result = rsLogger;
        // only one logger per session
        if (result == null) {
            throw new SyncFactoryException("(SyncFactory) : No logger has been set");
        }
        return result;
    }
    public static synchronized void setJNDIContext(javax.naming.Context ctx)
            throws SyncFactoryException {
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            sec.checkPermission(SET_SYNCFACTORY_PERMISSION);
        }
        if (ctx == null) {
            throw new SyncFactoryException("Invalid JNDI context supplied");
        }
        ic = ctx;
    }
    private static synchronized void initJNDIContext() throws SyncFactoryException {
        if ((ic != null) && (lazyJNDICtxRefresh == false)) {
            try {
                parseProperties(parseJNDIContext());
                lazyJNDICtxRefresh = true; // touch JNDI namespace once.
            } catch (NamingException e) {
                e.printStackTrace();
                throw new SyncFactoryException("SPI: NamingException: " + e.getExplanation());
            } catch (Exception e) {
                e.printStackTrace();
                throw new SyncFactoryException("SPI: Exception: " + e.getMessage());
            }
        }
    }
    private static boolean lazyJNDICtxRefresh = false;
    private static Properties parseJNDIContext() throws NamingException {
        NamingEnumeration<?> bindings = ic.listBindings("");
        Properties properties = new Properties();
        // Hunt one level below context for available SyncProvider objects
        enumerateBindings(bindings, properties);
        return properties;
    }
    private static void enumerateBindings(NamingEnumeration<?> bindings,
            Properties properties) throws NamingException {
        boolean syncProviderObj = false; // move to parameters ?
        try {
            Binding bd = null;
            Object elementObj = null;
            String element = null;
            while (bindings.hasMore()) {
                bd = (Binding) bindings.next();
                element = bd.getName();
                elementObj = bd.getObject();
                if (!(ic.lookup(element) instanceof Context)) {
                    // skip directories/sub-contexts
                    if (ic.lookup(element) instanceof SyncProvider) {
                        syncProviderObj = true;
                    }
                }
                if (syncProviderObj) {
                    SyncProvider sync = (SyncProvider) elementObj;
                    properties.put(SyncFactory.ROWSET_SYNC_PROVIDER,
                            sync.getProviderID());
                    syncProviderObj = false; // reset
                }
            }
        } catch (javax.naming.NotContextException e) {
            bindings.next();
            // Re-entrant call into method
            enumerateBindings(bindings, properties);
        }
    }
    private static class SyncFactoryHolder {
        static final SyncFactory factory = new SyncFactory();
    }
}
class ProviderImpl extends SyncProvider {
    private String className = null;
    private String vendorName = null;
    private String ver = null;
    private int index;
    public void setClassname(String classname) {
        className = classname;
    }
    public String getClassname() {
        return className;
    }
    public void setVendor(String vendor) {
        vendorName = vendor;
    }
    public String getVendor() {
        return vendorName;
    }
    public void setVersion(String providerVer) {
        ver = providerVer;
    }
    public String getVersion() {
        return ver;
    }
    public void setIndex(int i) {
        index = i;
    }
    public int getIndex() {
        return index;
    }
    public int getDataSourceLock() throws SyncProviderException {
        int dsLock = 0;
        try {
            dsLock = SyncFactory.getInstance(className).getDataSourceLock();
        } catch (SyncFactoryException sfEx) {
            throw new SyncProviderException(sfEx.getMessage());
        }
        return dsLock;
    }
    public int getProviderGrade() {
        int grade = 0;
        try {
            grade = SyncFactory.getInstance(className).getProviderGrade();
        } catch (SyncFactoryException sfEx) {
            //
        }
        return grade;
    }
    public String getProviderID() {
        return className;
    }
    public javax.sql.RowSetReader getRowSetReader() {
        RowSetReader rsReader = null;
        try {
            rsReader = SyncFactory.getInstance(className).getRowSetReader();
        } catch (SyncFactoryException sfEx) {
            //
        }
        return rsReader;
    }
    public javax.sql.RowSetWriter getRowSetWriter() {
        RowSetWriter rsWriter = null;
        try {
            rsWriter = SyncFactory.getInstance(className).getRowSetWriter();
        } catch (SyncFactoryException sfEx) {
            //
        }
        return rsWriter;
    }
    public void setDataSourceLock(int param)
            throws SyncProviderException {
        try {
            SyncFactory.getInstance(className).setDataSourceLock(param);
        } catch (SyncFactoryException sfEx) {
            throw new SyncProviderException(sfEx.getMessage());
        }
    }
    public int supportsUpdatableView() {
        int view = 0;
        try {
            view = SyncFactory.getInstance(className).supportsUpdatableView();
        } catch (SyncFactoryException sfEx) {
            //
        }
        return view;
    }
}
