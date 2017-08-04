

package java.security;

import java.util.ArrayList;
import java.util.List;
import sun.security.util.Debug;
import sun.security.util.SecurityConstants;




public final class AccessControlContext {

    private ProtectionDomain context[];
    // isPrivileged and isAuthorized are referenced by the VM - do not remove
    // or change their names
    private boolean isPrivileged;
    private boolean isAuthorized = false;

    // Note: This field is directly used by the virtual machine
    // native codes. Don't touch it.
    private AccessControlContext privilegedContext;

    private DomainCombiner combiner = null;

    // limited privilege scope
    private Permission permissions[];
    private AccessControlContext parent;
    private boolean isWrapped;

    // is constrained by limited privilege scope?
    private boolean isLimited;
    private ProtectionDomain limitedContext[];

    private static boolean debugInit = false;
    private static Debug debug = null;

    static Debug getDebug()
    {
        if (debugInit)
            return debug;
        else {
            if (Policy.isSet()) {
                debug = Debug.getInstance("access");
                debugInit = true;
            }
            return debug;
        }
    }


    public AccessControlContext(ProtectionDomain context[])
    {
        if (context.length == 0) {
            this.context = null;
        } else if (context.length == 1) {
            if (context[0] != null) {
                this.context = context.clone();
            } else {
                this.context = null;
            }
        } else {
            List<ProtectionDomain> v = new ArrayList<>(context.length);
            for (int i =0; i< context.length; i++) {
                if ((context[i] != null) &&  (!v.contains(context[i])))
                    v.add(context[i]);
            }
            if (!v.isEmpty()) {
                this.context = new ProtectionDomain[v.size()];
                this.context = v.toArray(this.context);
            }
        }
    }


    public AccessControlContext(AccessControlContext acc,
                                DomainCombiner combiner) {

        this(acc, combiner, false);
    }


    AccessControlContext(AccessControlContext acc,
                        DomainCombiner combiner,
                        boolean preauthorized) {
        if (!preauthorized) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(SecurityConstants.CREATE_ACC_PERMISSION);
                this.isAuthorized = true;
            }
        } else {
            this.isAuthorized = true;
        }

        this.context = acc.context;

        // we do not need to run the combine method on the
        // provided ACC.  it was already "combined" when the
        // context was originally retrieved.
        //
        // at this point in time, we simply throw away the old
        // combiner and use the newly provided one.
        this.combiner = combiner;
    }


    AccessControlContext(ProtectionDomain caller, DomainCombiner combiner,
        AccessControlContext parent, AccessControlContext context,
        Permission[] perms)
    {

        ProtectionDomain[] callerPDs = null;
        if (caller != null) {
             callerPDs = new ProtectionDomain[] { caller };
        }
        if (context != null) {
            if (combiner != null) {
                this.context = combiner.combine(callerPDs, context.context);
            } else {
                this.context = combine(callerPDs, context.context);
            }
        } else {

            if (combiner != null) {
                this.context = combiner.combine(callerPDs, null);
            } else {
                this.context = combine(callerPDs, null);
            }
        }
        this.combiner = combiner;

        Permission[] tmp = null;
        if (perms != null) {
            tmp = new Permission[perms.length];
            for (int i=0; i < perms.length; i++) {
                if (perms[i] == null) {
                    throw new NullPointerException("permission can't be null");
                }


                if (perms[i].getClass() == AllPermission.class) {
                    parent = null;
                }
                tmp[i] = perms[i];
            }
        }


        if (parent != null) {
            this.limitedContext = combine(parent.context, parent.limitedContext);
            this.isLimited = true;
            this.isWrapped = true;
            this.permissions = tmp;
            this.parent = parent;
            this.privilegedContext = context; // used in checkPermission2()
        }
        this.isAuthorized = true;
    }




    AccessControlContext(ProtectionDomain context[],
                         boolean isPrivileged)
    {
        this.context = context;
        this.isPrivileged = isPrivileged;
        this.isAuthorized = true;
    }


    AccessControlContext(ProtectionDomain[] context,
                         AccessControlContext privilegedContext)
    {
        this.context = context;
        this.privilegedContext = privilegedContext;
        this.isPrivileged = true;
    }


    ProtectionDomain[] getContext() {
        return context;
    }


    boolean isPrivileged()
    {
        return isPrivileged;
    }


    DomainCombiner getAssignedCombiner() {
        AccessControlContext acc;
        if (isPrivileged) {
            acc = privilegedContext;
        } else {
            acc = AccessController.getInheritedAccessControlContext();
        }
        if (acc != null) {
            return acc.combiner;
        }
        return null;
    }


    public DomainCombiner getDomainCombiner() {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SecurityConstants.GET_COMBINER_PERMISSION);
        }
        return getCombiner();
    }


    DomainCombiner getCombiner() {
        return combiner;
    }

    boolean isAuthorized() {
        return isAuthorized;
    }


    public void checkPermission(Permission perm)
        throws AccessControlException
    {
        boolean dumpDebug = false;

        if (perm == null) {
            throw new NullPointerException("permission can't be null");
        }
        if (getDebug() != null) {
            // If "codebase" is not specified, we dump the info by default.
            dumpDebug = !Debug.isOn("codebase=");
            if (!dumpDebug) {
                // If "codebase" is specified, only dump if the specified code
                // value is in the stack.
                for (int i = 0; context != null && i < context.length; i++) {
                    if (context[i].getCodeSource() != null &&
                        context[i].getCodeSource().getLocation() != null &&
                        Debug.isOn("codebase=" + context[i].getCodeSource().getLocation().toString())) {
                        dumpDebug = true;
                        break;
                    }
                }
            }

            dumpDebug &= !Debug.isOn("permission=") ||
                Debug.isOn("permission=" + perm.getClass().getCanonicalName());

            if (dumpDebug && Debug.isOn("stack")) {
                Thread.dumpStack();
            }

            if (dumpDebug && Debug.isOn("domain")) {
                if (context == null) {
                    debug.println("domain (context is null)");
                } else {
                    for (int i=0; i< context.length; i++) {
                        debug.println("domain "+i+" "+context[i]);
                    }
                }
            }
        }





        if (context == null) {
            checkPermission2(perm);
            return;
        }

        for (int i=0; i< context.length; i++) {
            if (context[i] != null &&  !context[i].implies(perm)) {
                if (dumpDebug) {
                    debug.println("access denied " + perm);
                }

                if (Debug.isOn("failure") && debug != null) {
                    // Want to make sure this is always displayed for failure,
                    // but do not want to display again if already displayed
                    // above.
                    if (!dumpDebug) {
                        debug.println("access denied " + perm);
                    }
                    Thread.dumpStack();
                    final ProtectionDomain pd = context[i];
                    final Debug db = debug;
                    AccessController.doPrivileged (new PrivilegedAction<Void>() {
                        public Void run() {
                            db.println("domain that failed "+pd);
                            return null;
                        }
                    });
                }
                throw new AccessControlException("access denied "+perm, perm);
            }
        }

        // allow if all of them allowed access
        if (dumpDebug) {
            debug.println("access allowed "+perm);
        }

        checkPermission2(perm);
    }


    private void checkPermission2(Permission perm) {
        if (!isLimited) {
            return;
        }


        if (privilegedContext != null) {
            privilegedContext.checkPermission2(perm);
        }


        if (isWrapped) {
            return;
        }


        if (permissions != null) {
            Class<?> permClass = perm.getClass();
            for (int i=0; i < permissions.length; i++) {
                Permission limit = permissions[i];
                if (limit.getClass().equals(permClass) && limit.implies(perm)) {
                    return;
                }
            }
        }


        if (parent != null) {

            if (permissions == null) {
                parent.checkPermission2(perm);
            } else {
                parent.checkPermission(perm);
            }
        }
    }


    AccessControlContext optimize() {
        // the assigned (privileged or inherited) context
        AccessControlContext acc;
        DomainCombiner combiner = null;
        AccessControlContext parent = null;
        Permission[] permissions = null;

        if (isPrivileged) {
            acc = privilegedContext;
            if (acc != null) {

                if (acc.isWrapped) {
                    permissions = acc.permissions;
                    parent = acc.parent;
                }
            }
        } else {
            acc = AccessController.getInheritedAccessControlContext();
            if (acc != null) {

                if (acc.isLimited) {
                    parent = acc;
                }
            }
        }

        // this.context could be null if only system code is on the stack;
        // in that case, ignore the stack context
        boolean skipStack = (context == null);

        // acc.context could be null if only system code was involved;
        // in that case, ignore the assigned context
        boolean skipAssigned = (acc == null || acc.context == null);
        ProtectionDomain[] assigned = (skipAssigned) ? null : acc.context;
        ProtectionDomain[] pd;

        // if there is no enclosing limited privilege scope on the stack or
        // inherited from a parent thread
        boolean skipLimited = ((acc == null || !acc.isWrapped) && parent == null);

        if (acc != null && acc.combiner != null) {
            // let the assigned acc's combiner do its thing
            if (getDebug() != null) {
                debug.println("AccessControlContext invoking the Combiner");
            }

            // No need to clone current and assigned.context
            // combine() will not update them
            combiner = acc.combiner;
            pd = combiner.combine(context, assigned);
        } else {
            if (skipStack) {
                if (skipAssigned) {
                    calculateFields(acc, parent, permissions);
                    return this;
                } else if (skipLimited) {
                    return acc;
                }
            } else if (assigned != null) {
                if (skipLimited) {
                    // optimization: if there is a single stack domain and
                    // that domain is already in the assigned context; no
                    // need to combine
                    if (context.length == 1 && context[0] == assigned[0]) {
                        return acc;
                    }
                }
            }

            pd = combine(context, assigned);
            if (skipLimited && !skipAssigned && pd == assigned) {
                return acc;
            } else if (skipAssigned && pd == context) {
                calculateFields(acc, parent, permissions);
                return this;
            }
        }

        // Reuse existing ACC
        this.context = pd;
        this.combiner = combiner;
        this.isPrivileged = false;

        calculateFields(acc, parent, permissions);
        return this;
    }



    private static ProtectionDomain[] combine(ProtectionDomain[]current,
        ProtectionDomain[] assigned) {

        // current could be null if only system code is on the stack;
        // in that case, ignore the stack context
        boolean skipStack = (current == null);

        // assigned could be null if only system code was involved;
        // in that case, ignore the assigned context
        boolean skipAssigned = (assigned == null);

        int slen = (skipStack) ? 0 : current.length;

        // optimization: if there is no assigned context and the stack length
        // is less then or equal to two; there is no reason to compress the
        // stack context, it already is
        if (skipAssigned && slen <= 2) {
            return current;
        }

        int n = (skipAssigned) ? 0 : assigned.length;

        // now we combine both of them, and create a new context
        ProtectionDomain pd[] = new ProtectionDomain[slen + n];

        // first copy in the assigned context domains, no need to compress
        if (!skipAssigned) {
            System.arraycopy(assigned, 0, pd, 0, n);
        }

        // now add the stack context domains, discarding nulls and duplicates
    outer:
        for (int i = 0; i < slen; i++) {
            ProtectionDomain sd = current[i];
            if (sd != null) {
                for (int j = 0; j < n; j++) {
                    if (sd == pd[j]) {
                        continue outer;
                    }
                }
                pd[n++] = sd;
            }
        }

        // if length isn't equal, we need to shorten the array
        if (n != pd.length) {
            // optimization: if we didn't really combine anything
            if (!skipAssigned && n == assigned.length) {
                return assigned;
            } else if (skipAssigned && n == slen) {
                return current;
            }
            ProtectionDomain tmp[] = new ProtectionDomain[n];
            System.arraycopy(pd, 0, tmp, 0, n);
            pd = tmp;
        }

        return pd;
    }



    private void calculateFields(AccessControlContext assigned,
        AccessControlContext parent, Permission[] permissions)
    {
        ProtectionDomain[] parentLimit = null;
        ProtectionDomain[] assignedLimit = null;
        ProtectionDomain[] newLimit;

        parentLimit = (parent != null)? parent.limitedContext: null;
        assignedLimit = (assigned != null)? assigned.limitedContext: null;
        newLimit = combine(parentLimit, assignedLimit);
        if (newLimit != null) {
            if (context == null || !containsAllPDs(newLimit, context)) {
                this.limitedContext = newLimit;
                this.permissions = permissions;
                this.parent = parent;
                this.isLimited = true;
            }
        }
    }



    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (! (obj instanceof AccessControlContext))
            return false;

        AccessControlContext that = (AccessControlContext) obj;

        if (!equalContext(that))
            return false;

        if (!equalLimitedContext(that))
            return false;

        return true;
    }


    private boolean equalContext(AccessControlContext that) {
        if (!equalPDs(this.context, that.context))
            return false;

        if (this.combiner == null && that.combiner != null)
            return false;

        if (this.combiner != null && !this.combiner.equals(that.combiner))
            return false;

        return true;
    }

    private boolean equalPDs(ProtectionDomain[] a, ProtectionDomain[] b) {
        if (a == null) {
            return (b == null);
        }

        if (b == null)
            return false;

        if (!(containsAllPDs(a, b) && containsAllPDs(b, a)))
            return false;

        return true;
    }


    private boolean equalLimitedContext(AccessControlContext that) {
        if (that == null)
            return false;


        if (!this.isLimited && !that.isLimited)
            return true;


         if (!(this.isLimited && that.isLimited))
             return false;


        if ((this.isWrapped && !that.isWrapped) ||
            (!this.isWrapped && that.isWrapped)) {
            return false;
        }

        if (this.permissions == null && that.permissions != null)
            return false;

        if (this.permissions != null && that.permissions == null)
            return false;

        if (!(this.containsAllLimits(that) && that.containsAllLimits(this)))
            return false;


        AccessControlContext thisNextPC = getNextPC(this);
        AccessControlContext thatNextPC = getNextPC(that);


        if (thisNextPC == null && thatNextPC != null && thatNextPC.isLimited)
            return false;

        if (thisNextPC != null && !thisNextPC.equalLimitedContext(thatNextPC))
            return false;

        if (this.parent == null && that.parent != null)
            return false;

        if (this.parent != null && !this.parent.equals(that.parent))
            return false;

        return true;
    }


    private static AccessControlContext getNextPC(AccessControlContext acc) {
        while (acc != null && acc.privilegedContext != null) {
            acc = acc.privilegedContext;
            if (!acc.isWrapped)
                return acc;
        }
        return null;
    }

    private static boolean containsAllPDs(ProtectionDomain[] thisContext,
        ProtectionDomain[] thatContext) {
        boolean match = false;

        //
        // ProtectionDomains within an ACC currently cannot be null
        // and this is enforced by the constructor and the various
        // optimize methods. However, historically this logic made attempts
        // to support the notion of a null PD and therefore this logic continues
        // to support that notion.
        ProtectionDomain thisPd;
        for (int i = 0; i < thisContext.length; i++) {
            match = false;
            if ((thisPd = thisContext[i]) == null) {
                for (int j = 0; (j < thatContext.length) && !match; j++) {
                    match = (thatContext[j] == null);
                }
            } else {
                Class<?> thisPdClass = thisPd.getClass();
                ProtectionDomain thatPd;
                for (int j = 0; (j < thatContext.length) && !match; j++) {
                    thatPd = thatContext[j];

                    // Class check required to avoid PD exposure (4285406)
                    match = (thatPd != null &&
                        thisPdClass == thatPd.getClass() && thisPd.equals(thatPd));
                }
            }
            if (!match) return false;
        }
        return match;
    }

    private boolean containsAllLimits(AccessControlContext that) {
        boolean match = false;
        Permission thisPerm;

        if (this.permissions == null && that.permissions == null)
            return true;

        for (int i = 0; i < this.permissions.length; i++) {
            Permission limit = this.permissions[i];
            Class <?> limitClass = limit.getClass();
            match = false;
            for (int j = 0; (j < that.permissions.length) && !match; j++) {
                Permission perm = that.permissions[j];
                match = (limitClass.equals(perm.getClass()) &&
                    limit.equals(perm));
            }
            if (!match) return false;
        }
        return match;
    }




    public int hashCode() {
        int hashCode = 0;

        if (context == null)
            return hashCode;

        for (int i =0; i < context.length; i++) {
            if (context[i] != null)
                hashCode ^= context[i].hashCode();
        }

        return hashCode;
    }
}
