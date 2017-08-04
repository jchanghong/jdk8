package java.security;
import java.util.*;
import java.io.ObjectStreamField;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
final class UnresolvedPermissionCollection
extends PermissionCollection
implements java.io.Serializable
{
    private transient Map<String, List<UnresolvedPermission>> perms;
    public UnresolvedPermissionCollection() {
        perms = new HashMap<String, List<UnresolvedPermission>>(11);
    }
    public void add(Permission permission)
    {
        if (! (permission instanceof UnresolvedPermission))
            throw new IllegalArgumentException("invalid permission: "+
                                               permission);
        UnresolvedPermission up = (UnresolvedPermission) permission;
        List<UnresolvedPermission> v;
        synchronized (this) {
            v = perms.get(up.getName());
            if (v == null) {
                v = new ArrayList<UnresolvedPermission>();
                perms.put(up.getName(), v);
            }
        }
        synchronized (v) {
            v.add(up);
        }
    }
    List<UnresolvedPermission> getUnresolvedPermissions(Permission p) {
        synchronized (this) {
            return perms.get(p.getClass().getName());
        }
    }
    public boolean implies(Permission permission)
    {
        return false;
    }
    public Enumeration<Permission> elements() {
        List<Permission> results =
            new ArrayList<>(); // where results are stored
        // Get iterator of Map values (which are lists of permissions)
        synchronized (this) {
            for (List<UnresolvedPermission> l : perms.values()) {
                synchronized (l) {
                    results.addAll(l);
                }
            }
        }
        return Collections.enumeration(results);
    }
    private static final long serialVersionUID = -7176153071733132400L;
    // Need to maintain serialization interoperability with earlier releases,
    // which had the serializable field:
    // private Hashtable permissions; // keyed on type
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("permissions", Hashtable.class),
    };
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Don't call out.defaultWriteObject()
        // Copy perms into a Hashtable
        Hashtable<String, Vector<UnresolvedPermission>> permissions =
            new Hashtable<>(perms.size()*2);
        // Convert each entry (List) into a Vector
        synchronized (this) {
            Set<Map.Entry<String, List<UnresolvedPermission>>> set = perms.entrySet();
            for (Map.Entry<String, List<UnresolvedPermission>> e : set) {
                // Convert list into Vector
                List<UnresolvedPermission> list = e.getValue();
                Vector<UnresolvedPermission> vec = new Vector<>(list.size());
                synchronized (list) {
                    vec.addAll(list);
                }
                // Add to Hashtable being serialized
                permissions.put(e.getKey(), vec);
            }
        }
        // Write out serializable fields
        ObjectOutputStream.PutField pfields = out.putFields();
        pfields.put("permissions", permissions);
        out.writeFields();
    }
    private void readObject(ObjectInputStream in) throws IOException,
    ClassNotFoundException {
        // Don't call defaultReadObject()
        // Read in serialized fields
        ObjectInputStream.GetField gfields = in.readFields();
        // Get permissions
        @SuppressWarnings("unchecked")
        // writeObject writes a Hashtable<String, Vector<UnresolvedPermission>>
        // for the permissions key, so this cast is safe, unless the data is corrupt.
        Hashtable<String, Vector<UnresolvedPermission>> permissions =
                (Hashtable<String, Vector<UnresolvedPermission>>)
                gfields.get("permissions", null);
        perms = new HashMap<String, List<UnresolvedPermission>>(permissions.size()*2);
        // Convert each entry (Vector) into a List
        Set<Map.Entry<String, Vector<UnresolvedPermission>>> set = permissions.entrySet();
        for (Map.Entry<String, Vector<UnresolvedPermission>> e : set) {
            // Convert Vector into ArrayList
            Vector<UnresolvedPermission> vec = e.getValue();
            List<UnresolvedPermission> list = new ArrayList<>(vec.size());
            list.addAll(vec);
            // Add to Hashtable being serialized
            perms.put(e.getKey(), list);
        }
    }
}
