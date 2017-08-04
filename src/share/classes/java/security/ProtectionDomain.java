

package java.security;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import sun.misc.JavaSecurityProtectionDomainAccess;
import static sun.misc.JavaSecurityProtectionDomainAccess.ProtectionDomainCache;
import sun.security.util.Debug;
import sun.security.util.SecurityConstants;
import sun.misc.JavaSecurityAccess;
import sun.misc.SharedSecrets;



public class ProtectionDomain {
    private static class JavaSecurityAccessImpl implements JavaSecurityAccess {

        private JavaSecurityAccessImpl() {
        }

        @Override
        public <T> T doIntersectionPrivilege(
                PrivilegedAction<T> action,
                final AccessControlContext stack,
                final AccessControlContext context) {
            if (action == null) {
                throw new NullPointerException();
            }

            return AccessController.doPrivileged(
                action,
                getCombinedACC(context, stack)
            );
        }

        @Override
        public <T> T doIntersectionPrivilege(
                PrivilegedAction<T> action,
                AccessControlContext context) {
            return doIntersectionPrivilege(action,
                AccessController.getContext(), context);
        }

        private static AccessControlContext getCombinedACC(AccessControlContext context, AccessControlContext stack) {
            AccessControlContext acc = new AccessControlContext(context, stack.getCombiner(), true);

            return new AccessControlContext(stack.getContext(), acc).optimize();
        }
    }

    static {
        // Set up JavaSecurityAccess in SharedSecrets
        SharedSecrets.setJavaSecurityAccess(new JavaSecurityAccessImpl());
    }


    private CodeSource codesource ;


    private ClassLoader classloader;


    private Principal[] principals;


    private PermissionCollection permissions;


    private boolean hasAllPerm = false;


    private boolean staticPermissions;


    final Key key = new Key();

    private static final Debug debug = Debug.getInstance("domain");


    public ProtectionDomain(CodeSource codesource,
                            PermissionCollection permissions) {
        this.codesource = codesource;
        if (permissions != null) {
            this.permissions = permissions;
            this.permissions.setReadOnly();
            if (permissions instanceof Permissions &&
                ((Permissions)permissions).allPermission != null) {
                hasAllPerm = true;
            }
        }
        this.classloader = null;
        this.principals = new Principal[0];
        staticPermissions = true;
    }


    public ProtectionDomain(CodeSource codesource,
                            PermissionCollection permissions,
                            ClassLoader classloader,
                            Principal[] principals) {
        this.codesource = codesource;
        if (permissions != null) {
            this.permissions = permissions;
            this.permissions.setReadOnly();
            if (permissions instanceof Permissions &&
                ((Permissions)permissions).allPermission != null) {
                hasAllPerm = true;
            }
        }
        this.classloader = classloader;
        this.principals = (principals != null ? principals.clone():
                           new Principal[0]);
        staticPermissions = false;
    }


    public final CodeSource getCodeSource() {
        return this.codesource;
    }



    public final ClassLoader getClassLoader() {
        return this.classloader;
    }



    public final Principal[] getPrincipals() {
        return this.principals.clone();
    }


    public final PermissionCollection getPermissions() {
        return permissions;
    }


    public boolean implies(Permission permission) {

        if (hasAllPerm) {
            // internal permission collection already has AllPermission -
            // no need to go to policy
            return true;
        }

        if (!staticPermissions &&
            Policy.getPolicyNoCheck().implies(this, permission))
            return true;
        if (permissions != null)
            return permissions.implies(permission);

        return false;
    }

    // called by the VM -- do not remove
    boolean impliesCreateAccessControlContext() {
        return implies(SecurityConstants.CREATE_ACC_PERMISSION);
    }


    @Override public String toString() {
        String pals = "<no principals>";
        if (principals != null && principals.length > 0) {
            StringBuilder palBuf = new StringBuilder("(principals ");

            for (int i = 0; i < principals.length; i++) {
                palBuf.append(principals[i].getClass().getName() +
                            " \"" + principals[i].getName() +
                            "\"");
                if (i < principals.length-1)
                    palBuf.append(",\n");
                else
                    palBuf.append(")\n");
            }
            pals = palBuf.toString();
        }

        // Check if policy is set; we don't want to load
        // the policy prematurely here
        PermissionCollection pc = Policy.isSet() && seeAllp() ?
                                      mergePermissions():
                                      getPermissions();

        return "ProtectionDomain "+
            " "+codesource+"\n"+
            " "+classloader+"\n"+
            " "+pals+"\n"+
            " "+pc+"\n";
    }


    private static boolean seeAllp() {
        SecurityManager sm = System.getSecurityManager();

        if (sm == null) {
            return true;
        } else {
            if (debug != null) {
                if (sm.getClass().getClassLoader() == null &&
                    Policy.getPolicyNoCheck().getClass().getClassLoader()
                                                                == null) {
                    return true;
                }
            } else {
                try {
                    sm.checkPermission(SecurityConstants.GET_POLICY_PERMISSION);
                    return true;
                } catch (SecurityException se) {
                    // fall thru and return false
                }
            }
        }

        return false;
    }

    private PermissionCollection mergePermissions() {
        if (staticPermissions)
            return permissions;

        PermissionCollection perms =
            java.security.AccessController.doPrivileged
            (new java.security.PrivilegedAction<PermissionCollection>() {
                    public PermissionCollection run() {
                        Policy p = Policy.getPolicyNoCheck();
                        return p.getPermissions(ProtectionDomain.this);
                    }
                });

        Permissions mergedPerms = new Permissions();
        int swag = 32;
        int vcap = 8;
        Enumeration<Permission> e;
        List<Permission> pdVector = new ArrayList<>(vcap);
        List<Permission> plVector = new ArrayList<>(swag);

        //
        // Build a vector of domain permissions for subsequent merge
        if (permissions != null) {
            synchronized (permissions) {
                e = permissions.elements();
                while (e.hasMoreElements()) {
                    pdVector.add(e.nextElement());
                }
            }
        }

        //
        // Build a vector of Policy permissions for subsequent merge
        if (perms != null) {
            synchronized (perms) {
                e = perms.elements();
                while (e.hasMoreElements()) {
                    plVector.add(e.nextElement());
                    vcap++;
                }
            }
        }

        if (perms != null && permissions != null) {
            //
            // Weed out the duplicates from the policy. Unless a refresh
            // has occurred since the pd was consed this should result in
            // an empty vector.
            synchronized (permissions) {
                e = permissions.elements();   // domain vs policy
                while (e.hasMoreElements()) {
                    Permission pdp = e.nextElement();
                    Class<?> pdpClass = pdp.getClass();
                    String pdpActions = pdp.getActions();
                    String pdpName = pdp.getName();
                    for (int i = 0; i < plVector.size(); i++) {
                        Permission pp = plVector.get(i);
                        if (pdpClass.isInstance(pp)) {
                            // The equals() method on some permissions
                            // have some side effects so this manual
                            // comparison is sufficient.
                            if (pdpName.equals(pp.getName()) &&
                                pdpActions.equals(pp.getActions())) {
                                plVector.remove(i);
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (perms !=null) {
            // the order of adding to merged perms and permissions
            // needs to preserve the bugfix 4301064

            for (int i = plVector.size()-1; i >= 0; i--) {
                mergedPerms.add(plVector.get(i));
            }
        }
        if (permissions != null) {
            for (int i = pdVector.size()-1; i >= 0; i--) {
                mergedPerms.add(pdVector.get(i));
            }
        }

        return mergedPerms;
    }


    static final class Key {}

    static {
        SharedSecrets.setJavaSecurityProtectionDomainAccess(
            new JavaSecurityProtectionDomainAccess() {
                @Override
                public ProtectionDomainCache getProtectionDomainCache() {
                    return new PDCache();
                }

                @Override
                public boolean getStaticPermissionsField(ProtectionDomain pd) {
                    return pd.staticPermissions;
                }
            });
    }


    private static class PDCache implements ProtectionDomainCache {
        private final ConcurrentHashMap<WeakProtectionDomainKey,
                                        SoftReference<PermissionCollection>>
                                        pdMap = new ConcurrentHashMap<>();
        private final ReferenceQueue<Key> queue = new ReferenceQueue<>();

        @Override
        public void put(ProtectionDomain pd, PermissionCollection pc) {
            processQueue(queue, pdMap);
            WeakProtectionDomainKey weakPd =
                new WeakProtectionDomainKey(pd, queue);
            pdMap.put(weakPd, new SoftReference<>(pc));
        }

        @Override
        public PermissionCollection get(ProtectionDomain pd) {
            processQueue(queue, pdMap);
            WeakProtectionDomainKey weakPd = new WeakProtectionDomainKey(pd);
            SoftReference<PermissionCollection> sr = pdMap.get(weakPd);
            return (sr == null) ? null : sr.get();
        }


        private static void processQueue(ReferenceQueue<Key> queue,
                                         ConcurrentHashMap<? extends
                                         WeakReference<Key>, ?> pdMap) {
            Reference<? extends Key> ref;
            while ((ref = queue.poll()) != null) {
                pdMap.remove(ref);
            }
        }
    }


    private static class WeakProtectionDomainKey extends WeakReference<Key> {

        private final int hash;


        private static final Key NULL_KEY = new Key();


        WeakProtectionDomainKey(ProtectionDomain pd, ReferenceQueue<Key> rq) {
            this((pd == null ? NULL_KEY : pd.key), rq);
        }

        WeakProtectionDomainKey(ProtectionDomain pd) {
            this(pd == null ? NULL_KEY : pd.key);
        }

        private WeakProtectionDomainKey(Key key, ReferenceQueue<Key> rq) {
            super(key, rq);
            hash = key.hashCode();
        }

        private WeakProtectionDomainKey(Key key) {
            super(key);
            hash = key.hashCode();
        }


        @Override
        public int hashCode() {
            return hash;
        }


        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof WeakProtectionDomainKey) {
                Object referent = get();
                return (referent != null) &&
                       (referent == ((WeakProtectionDomainKey)obj).get());
            } else {
                return false;
            }
        }
    }
}
