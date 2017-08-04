


package java.security;

import java.util.Enumeration;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import sun.security.jca.GetInstance;
import sun.security.util.Debug;
import sun.security.util.SecurityConstants;




public abstract class Policy {


    public static final PermissionCollection UNSUPPORTED_EMPTY_COLLECTION =
                        new UnsupportedEmptyCollection();

    // Information about the system-wide policy.
    private static class PolicyInfo {
        // the system-wide policy
        final Policy policy;
        // a flag indicating if the system-wide policy has been initialized
        final boolean initialized;

        PolicyInfo(Policy policy, boolean initialized) {
            this.policy = policy;
            this.initialized = initialized;
        }
    }

    // PolicyInfo is stored in an AtomicReference
    private static AtomicReference<PolicyInfo> policy =
        new AtomicReference<>(new PolicyInfo(null, false));

    private static final Debug debug = Debug.getInstance("policy");

    // Cache mapping ProtectionDomain.Key to PermissionCollection
    private WeakHashMap<ProtectionDomain.Key, PermissionCollection> pdMapping;


    static boolean isSet()
    {
        PolicyInfo pi = policy.get();
        return pi.policy != null && pi.initialized == true;
    }

    private static void checkPermission(String type) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("createPolicy." + type));
        }
    }


    public static Policy getPolicy()
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(SecurityConstants.GET_POLICY_PERMISSION);
        return getPolicyNoCheck();
    }


    static Policy getPolicyNoCheck()
    {
        PolicyInfo pi = policy.get();
        // Use double-check idiom to avoid locking if system-wide policy is
        // already initialized
        if (pi.initialized == false || pi.policy == null) {
            synchronized (Policy.class) {
                PolicyInfo pinfo = policy.get();
                if (pinfo.policy == null) {
                    String policy_class = AccessController.doPrivileged(
                        new PrivilegedAction<String>() {
                        public String run() {
                            return Security.getProperty("policy.provider");
                        }
                    });
                    if (policy_class == null) {
                        policy_class = "sun.security.provider.PolicyFile";
                    }

                    try {
                        pinfo = new PolicyInfo(
                            (Policy) Class.forName(policy_class).newInstance(),
                            true);
                    } catch (Exception e) {


                        // install the bootstrap provider to avoid recursion
                        Policy polFile = new sun.security.provider.PolicyFile();
                        pinfo = new PolicyInfo(polFile, false);
                        policy.set(pinfo);

                        final String pc = policy_class;
                        Policy pol = AccessController.doPrivileged(
                            new PrivilegedAction<Policy>() {
                            public Policy run() {
                                try {
                                    ClassLoader cl =
                                            ClassLoader.getSystemClassLoader();
                                    // we want the extension loader
                                    ClassLoader extcl = null;
                                    while (cl != null) {
                                        extcl = cl;
                                        cl = cl.getParent();
                                    }
                                    return (extcl != null ? (Policy)Class.forName(
                                            pc, true, extcl).newInstance() : null);
                                } catch (Exception e) {
                                    if (debug != null) {
                                        debug.println("policy provider " +
                                                    pc +
                                                    " not available");
                                        e.printStackTrace();
                                    }
                                    return null;
                                }
                            }
                        });

                        if (pol != null) {
                            pinfo = new PolicyInfo(pol, true);
                        } else {
                            if (debug != null) {
                                debug.println("using sun.security.provider.PolicyFile");
                            }
                            pinfo = new PolicyInfo(polFile, true);
                        }
                    }
                    policy.set(pinfo);
                }
                return pinfo.policy;
            }
        }
        return pi.policy;
    }


    public static void setPolicy(Policy p)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(
                                 new SecurityPermission("setPolicy"));
        if (p != null) {
            initPolicy(p);
        }
        synchronized (Policy.class) {
            policy.set(new PolicyInfo(p, p != null));
        }
    }


    private static void initPolicy (final Policy p) {


        ProtectionDomain policyDomain =
        AccessController.doPrivileged(new PrivilegedAction<ProtectionDomain>() {
            public ProtectionDomain run() {
                return p.getClass().getProtectionDomain();
            }
        });


        PermissionCollection policyPerms = null;
        synchronized (p) {
            if (p.pdMapping == null) {
                p.pdMapping = new WeakHashMap<>();
           }
        }

        if (policyDomain.getCodeSource() != null) {
            Policy pol = policy.get().policy;
            if (pol != null) {
                policyPerms = pol.getPermissions(policyDomain);
            }

            if (policyPerms == null) { // assume it has all
                policyPerms = new Permissions();
                policyPerms.add(SecurityConstants.ALL_PERMISSION);
            }

            synchronized (p.pdMapping) {
                // cache of pd to permissions
                p.pdMapping.put(policyDomain.key, policyPerms);
            }
        }
        return;
    }



    public static Policy getInstance(String type, Policy.Parameters params)
                throws NoSuchAlgorithmException {

        checkPermission(type);
        try {
            GetInstance.Instance instance = GetInstance.getInstance("Policy",
                                                        PolicySpi.class,
                                                        type,
                                                        params);
            return new PolicyDelegate((PolicySpi)instance.impl,
                                                        instance.provider,
                                                        type,
                                                        params);
        } catch (NoSuchAlgorithmException nsae) {
            return handleException(nsae);
        }
    }


    public static Policy getInstance(String type,
                                Policy.Parameters params,
                                String provider)
                throws NoSuchProviderException, NoSuchAlgorithmException {

        if (provider == null || provider.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }

        checkPermission(type);
        try {
            GetInstance.Instance instance = GetInstance.getInstance("Policy",
                                                        PolicySpi.class,
                                                        type,
                                                        params,
                                                        provider);
            return new PolicyDelegate((PolicySpi)instance.impl,
                                                        instance.provider,
                                                        type,
                                                        params);
        } catch (NoSuchAlgorithmException nsae) {
            return handleException(nsae);
        }
    }


    public static Policy getInstance(String type,
                                Policy.Parameters params,
                                Provider provider)
                throws NoSuchAlgorithmException {

        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }

        checkPermission(type);
        try {
            GetInstance.Instance instance = GetInstance.getInstance("Policy",
                                                        PolicySpi.class,
                                                        type,
                                                        params,
                                                        provider);
            return new PolicyDelegate((PolicySpi)instance.impl,
                                                        instance.provider,
                                                        type,
                                                        params);
        } catch (NoSuchAlgorithmException nsae) {
            return handleException(nsae);
        }
    }

    private static Policy handleException(NoSuchAlgorithmException nsae)
                throws NoSuchAlgorithmException {
        Throwable cause = nsae.getCause();
        if (cause instanceof IllegalArgumentException) {
            throw (IllegalArgumentException)cause;
        }
        throw nsae;
    }


    public Provider getProvider() {
        return null;
    }


    public String getType() {
        return null;
    }


    public Policy.Parameters getParameters() {
        return null;
    }


    public PermissionCollection getPermissions(CodeSource codesource) {
        return Policy.UNSUPPORTED_EMPTY_COLLECTION;
    }


    public PermissionCollection getPermissions(ProtectionDomain domain) {
        PermissionCollection pc = null;

        if (domain == null)
            return new Permissions();

        if (pdMapping == null) {
            initPolicy(this);
        }

        synchronized (pdMapping) {
            pc = pdMapping.get(domain.key);
        }

        if (pc != null) {
            Permissions perms = new Permissions();
            synchronized (pc) {
                for (Enumeration<Permission> e = pc.elements() ; e.hasMoreElements() ;) {
                    perms.add(e.nextElement());
                }
            }
            return perms;
        }

        pc = getPermissions(domain.getCodeSource());
        if (pc == null || pc == UNSUPPORTED_EMPTY_COLLECTION) {
            pc = new Permissions();
        }

        addStaticPerms(pc, domain.getPermissions());
        return pc;
    }


    private void addStaticPerms(PermissionCollection perms,
                                PermissionCollection statics) {
        if (statics != null) {
            synchronized (statics) {
                Enumeration<Permission> e = statics.elements();
                while (e.hasMoreElements()) {
                    perms.add(e.nextElement());
                }
            }
        }
    }


    public boolean implies(ProtectionDomain domain, Permission permission) {
        PermissionCollection pc;

        if (pdMapping == null) {
            initPolicy(this);
        }

        synchronized (pdMapping) {
            pc = pdMapping.get(domain.key);
        }

        if (pc != null) {
            return pc.implies(permission);
        }

        pc = getPermissions(domain);
        if (pc == null) {
            return false;
        }

        synchronized (pdMapping) {
            // cache it
            pdMapping.put(domain.key, pc);
        }

        return pc.implies(permission);
    }


    public void refresh() { }


    private static class PolicyDelegate extends Policy {

        private PolicySpi spi;
        private Provider p;
        private String type;
        private Policy.Parameters params;

        private PolicyDelegate(PolicySpi spi, Provider p,
                        String type, Policy.Parameters params) {
            this.spi = spi;
            this.p = p;
            this.type = type;
            this.params = params;
        }

        @Override public String getType() { return type; }

        @Override public Policy.Parameters getParameters() { return params; }

        @Override public Provider getProvider() { return p; }

        @Override
        public PermissionCollection getPermissions(CodeSource codesource) {
            return spi.engineGetPermissions(codesource);
        }
        @Override
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return spi.engineGetPermissions(domain);
        }
        @Override
        public boolean implies(ProtectionDomain domain, Permission perm) {
            return spi.engineImplies(domain, perm);
        }
        @Override
        public void refresh() {
            spi.engineRefresh();
        }
    }


    public static interface Parameters { }


    private static class UnsupportedEmptyCollection
        extends PermissionCollection {

        private static final long serialVersionUID = -8492269157353014774L;

        private Permissions perms;


        public UnsupportedEmptyCollection() {
            this.perms = new Permissions();
            perms.setReadOnly();
        }


        @Override public void add(Permission permission) {
            perms.add(permission);
        }


        @Override public boolean implies(Permission permission) {
            return perms.implies(permission);
        }


        @Override public Enumeration<Permission> elements() {
            return perms.elements();
        }
    }
}
