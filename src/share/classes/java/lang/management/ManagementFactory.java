package java.lang.management;
import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerPermission;
import javax.management.NotificationEmitter;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardEmitterMBean;
import javax.management.StandardMBean;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.management.JMX;
import sun.management.ManagementFactoryHelper;
import sun.management.ExtendedPlatformComponent;
public class ManagementFactory {
    // A class with only static fields and methods.
    private ManagementFactory() {};
    public final static String CLASS_LOADING_MXBEAN_NAME =
        "java.lang:type=ClassLoading";
    public final static String COMPILATION_MXBEAN_NAME =
        "java.lang:type=Compilation";
    public final static String MEMORY_MXBEAN_NAME =
        "java.lang:type=Memory";
    public final static String OPERATING_SYSTEM_MXBEAN_NAME =
        "java.lang:type=OperatingSystem";
    public final static String RUNTIME_MXBEAN_NAME =
        "java.lang:type=Runtime";
    public final static String THREAD_MXBEAN_NAME =
        "java.lang:type=Threading";
    public final static String GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE =
        "java.lang:type=GarbageCollector";
    public final static String MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE=
        "java.lang:type=MemoryManager";
    public final static String MEMORY_POOL_MXBEAN_DOMAIN_TYPE=
        "java.lang:type=MemoryPool";
    public static ClassLoadingMXBean getClassLoadingMXBean() {
        return ManagementFactoryHelper.getClassLoadingMXBean();
    }
    public static MemoryMXBean getMemoryMXBean() {
        return ManagementFactoryHelper.getMemoryMXBean();
    }
    public static ThreadMXBean getThreadMXBean() {
        return ManagementFactoryHelper.getThreadMXBean();
    }
    public static RuntimeMXBean getRuntimeMXBean() {
        return ManagementFactoryHelper.getRuntimeMXBean();
    }
    public static CompilationMXBean getCompilationMXBean() {
        return ManagementFactoryHelper.getCompilationMXBean();
    }
    public static OperatingSystemMXBean getOperatingSystemMXBean() {
        return ManagementFactoryHelper.getOperatingSystemMXBean();
    }
    public static List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        return ManagementFactoryHelper.getMemoryPoolMXBeans();
    }
    public static List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        return ManagementFactoryHelper.getMemoryManagerMXBeans();
    }
    public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        return ManagementFactoryHelper.getGarbageCollectorMXBeans();
    }
    private static MBeanServer platformMBeanServer;
    public static synchronized MBeanServer getPlatformMBeanServer() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            Permission perm = new MBeanServerPermission("createMBeanServer");
            sm.checkPermission(perm);
        }
        if (platformMBeanServer == null) {
            platformMBeanServer = MBeanServerFactory.createMBeanServer();
            for (PlatformComponent pc : PlatformComponent.values()) {
                List<? extends PlatformManagedObject> list =
                    pc.getMXBeans(pc.getMXBeanInterface());
                for (PlatformManagedObject o : list) {
                    // Each PlatformComponent represents one management
                    // interface. Some MXBean may extend another one.
                    // The MXBean instances for one platform component
                    // (returned by pc.getMXBeans()) might be also
                    // the MXBean instances for another platform component.
                    // e.g. com.sun.management.GarbageCollectorMXBean
                    //
                    // So need to check if an MXBean instance is registered
                    // before registering into the platform MBeanServer
                    if (!platformMBeanServer.isRegistered(o.getObjectName())) {
                        addMXBean(platformMBeanServer, o);
                    }
                }
            }
            HashMap<ObjectName, DynamicMBean> dynmbeans =
                    ManagementFactoryHelper.getPlatformDynamicMBeans();
            for (Map.Entry<ObjectName, DynamicMBean> e : dynmbeans.entrySet()) {
                addDynamicMBean(platformMBeanServer, e.getValue(), e.getKey());
            }
            for (final PlatformManagedObject o :
                                       ExtendedPlatformComponent.getMXBeans()) {
                if (!platformMBeanServer.isRegistered(o.getObjectName())) {
                    addMXBean(platformMBeanServer, o);
                }
            }
        }
        return platformMBeanServer;
    }
    public static <T> T
        newPlatformMXBeanProxy(MBeanServerConnection connection,
                               String mxbeanName,
                               Class<T> mxbeanInterface)
            throws java.io.IOException {
        // Only allow MXBean interfaces from rt.jar loaded by the
        // bootstrap class loader
        final Class<?> cls = mxbeanInterface;
        ClassLoader loader =
            AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return cls.getClassLoader();
                }
            });
        if (!sun.misc.VM.isSystemDomainLoader(loader)) {
            throw new IllegalArgumentException(mxbeanName +
                " is not a platform MXBean");
        }
        try {
            final ObjectName objName = new ObjectName(mxbeanName);
            // skip the isInstanceOf check for LoggingMXBean
            String intfName = mxbeanInterface.getName();
            if (!connection.isInstanceOf(objName, intfName)) {
                throw new IllegalArgumentException(mxbeanName +
                    " is not an instance of " + mxbeanInterface);
            }
            final Class[] interfaces;
            // check if the registered MBean is a notification emitter
            boolean emitter = connection.isInstanceOf(objName, NOTIF_EMITTER);
            // create an MXBean proxy
            return JMX.newMXBeanProxy(connection, objName, mxbeanInterface,
                                      emitter);
        } catch (InstanceNotFoundException|MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
        }
    }
    public static <T extends PlatformManagedObject>
            T getPlatformMXBean(Class<T> mxbeanInterface) {
        PlatformComponent pc = PlatformComponent.getPlatformComponent(mxbeanInterface);
        if (pc == null) {
            T mbean = ExtendedPlatformComponent.getMXBean(mxbeanInterface);
            if (mbean != null) {
                return mbean;
            }
            throw new IllegalArgumentException(mxbeanInterface.getName() +
                " is not a platform management interface");
        }
        if (!pc.isSingleton())
            throw new IllegalArgumentException(mxbeanInterface.getName() +
                " can have zero or more than one instances");
        return pc.getSingletonMXBean(mxbeanInterface);
    }
    public static <T extends PlatformManagedObject> List<T>
            getPlatformMXBeans(Class<T> mxbeanInterface) {
        PlatformComponent pc = PlatformComponent.getPlatformComponent(mxbeanInterface);
        if (pc == null) {
            T mbean = ExtendedPlatformComponent.getMXBean(mxbeanInterface);
            if (mbean != null) {
                return Collections.singletonList(mbean);
            }
            throw new IllegalArgumentException(mxbeanInterface.getName() +
                " is not a platform management interface");
        }
        return Collections.unmodifiableList(pc.getMXBeans(mxbeanInterface));
    }
    public static <T extends PlatformManagedObject>
            T getPlatformMXBean(MBeanServerConnection connection,
                                Class<T> mxbeanInterface)
        throws java.io.IOException
    {
        PlatformComponent pc = PlatformComponent.getPlatformComponent(mxbeanInterface);
        if (pc == null) {
            T mbean = ExtendedPlatformComponent.getMXBean(mxbeanInterface);
            if (mbean != null) {
                ObjectName on = mbean.getObjectName();
                return ManagementFactory.newPlatformMXBeanProxy(connection,
                                                                on.getCanonicalName(),
                                                                mxbeanInterface);
            }
            throw new IllegalArgumentException(mxbeanInterface.getName() +
                " is not a platform management interface");
        }
        if (!pc.isSingleton())
            throw new IllegalArgumentException(mxbeanInterface.getName() +
                " can have zero or more than one instances");
        return pc.getSingletonMXBean(connection, mxbeanInterface);
    }
    public static <T extends PlatformManagedObject>
            List<T> getPlatformMXBeans(MBeanServerConnection connection,
                                       Class<T> mxbeanInterface)
        throws java.io.IOException
    {
        PlatformComponent pc = PlatformComponent.getPlatformComponent(mxbeanInterface);
        if (pc == null) {
            T mbean = ExtendedPlatformComponent.getMXBean(mxbeanInterface);
            if (mbean != null) {
                ObjectName on = mbean.getObjectName();
                T proxy = ManagementFactory.newPlatformMXBeanProxy(connection,
                            on.getCanonicalName(), mxbeanInterface);
                return Collections.singletonList(proxy);
            }
            throw new IllegalArgumentException(mxbeanInterface.getName() +
                " is not a platform management interface");
        }
        return Collections.unmodifiableList(pc.getMXBeans(connection, mxbeanInterface));
    }
    public static Set<Class<? extends PlatformManagedObject>>
           getPlatformManagementInterfaces()
    {
        Set<Class<? extends PlatformManagedObject>> result =
            new HashSet<>();
        for (PlatformComponent component: PlatformComponent.values()) {
            result.add(component.getMXBeanInterface());
        }
        return Collections.unmodifiableSet(result);
    }
    private static final String NOTIF_EMITTER =
        "javax.management.NotificationEmitter";
    private static void addMXBean(final MBeanServer mbs, final PlatformManagedObject pmo) {
        // Make DynamicMBean out of MXBean by wrapping it with a StandardMBean
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws InstanceAlreadyExistsException,
                                         MBeanRegistrationException,
                                         NotCompliantMBeanException {
                    final DynamicMBean dmbean;
                    if (pmo instanceof DynamicMBean) {
                        dmbean = DynamicMBean.class.cast(pmo);
                    } else if (pmo instanceof NotificationEmitter) {
                        dmbean = new StandardEmitterMBean(pmo, null, true, (NotificationEmitter) pmo);
                    } else {
                        dmbean = new StandardMBean(pmo, null, true);
                    }
                    mbs.registerMBean(dmbean, pmo.getObjectName());
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw new RuntimeException(e.getException());
        }
    }
    private static void addDynamicMBean(final MBeanServer mbs,
                                        final DynamicMBean dmbean,
                                        final ObjectName on) {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws InstanceAlreadyExistsException,
                                         MBeanRegistrationException,
                                         NotCompliantMBeanException {
                    mbs.registerMBean(dmbean, on);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw new RuntimeException(e.getException());
        }
    }
}
