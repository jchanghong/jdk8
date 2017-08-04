

package java.io;

import java.security.*;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Collections;
import sun.security.util.SecurityConstants;



public final class FilePermission extends Permission implements Serializable {


    private final static int EXECUTE = 0x1;

    private final static int WRITE   = 0x2;

    private final static int READ    = 0x4;

    private final static int DELETE  = 0x8;

    private final static int READLINK    = 0x10;


    private final static int ALL     = READ|WRITE|EXECUTE|DELETE|READLINK;

    private final static int NONE    = 0x0;

    // the actions mask
    private transient int mask;

    // does path indicate a directory? (wildcard or recursive)
    private transient boolean directory;

    // is it a recursive directory specification?
    private transient boolean recursive;


    private String actions; // Left null as long as possible, then
                            // created and re-used in the getAction function.

    // canonicalized dir path. In the case of
    // directories, it is the name "/blah

    private static final long serialVersionUID = 7930732926638008763L;


    private void init(int mask) {
        if ((mask & ALL) != mask)
                throw new IllegalArgumentException("invalid actions mask");

        if (mask == NONE)
                throw new IllegalArgumentException("invalid actions mask");

        if ((cpath = getName()) == null)
                throw new NullPointerException("name can't be null");

        this.mask = mask;

        if (cpath.equals("<<ALL FILES>>")) {
            directory = true;
            recursive = true;
            cpath = "";
            return;
        }

        // store only the canonical cpath if possible
        cpath = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                try {
                    String path = cpath;
                    if (cpath.endsWith("*")) {
                        // call getCanonicalPath with a path with wildcard character
                        // replaced to avoid calling it with paths that are
                        // intended to match all entries in a directory
                        path = path.substring(0, path.length()-1) + "-";
                        path = new File(path).getCanonicalPath();
                        return path.substring(0, path.length()-1) + "*";
                    } else {
                        return new File(path).getCanonicalPath();
                    }
                } catch (IOException ioe) {
                    return cpath;
                }
            }
        });

        int len = cpath.length();
        char last = ((len > 0) ? cpath.charAt(len - 1) : 0);

        if (last == RECURSIVE_CHAR &&
            cpath.charAt(len - 2) == File.separatorChar) {
            directory = true;
            recursive = true;
            cpath = cpath.substring(0, --len);
        } else if (last == WILD_CHAR &&
            cpath.charAt(len - 2) == File.separatorChar) {
            directory = true;
            //recursive = false;
            cpath = cpath.substring(0, --len);
        } else {
            // overkill since they are initialized to false, but
            // commented out here to remind us...
            //directory = false;
            //recursive = false;
        }

        // XXX: at this point the path should be absolute. die if it isn't?
    }


    public FilePermission(String path, String actions) {
        super(path);
        init(getMask(actions));
    }



    // package private for use by the FilePermissionCollection add method
    FilePermission(String path, int mask) {
        super(path);
        init(mask);
    }


    public boolean implies(Permission p) {
        if (!(p instanceof FilePermission))
            return false;

        FilePermission that = (FilePermission) p;

        // we get the effective mask. i.e., the "and" of this and that.
        // They must be equal to that.mask for implies to return true.

        return ((this.mask & that.mask) == that.mask) && impliesIgnoreMask(that);
    }


    boolean impliesIgnoreMask(FilePermission that) {
        if (this.directory) {
            if (this.recursive) {
                // make sure that.path is longer then path so
                // something like /foo/- does not imply /foo
                if (that.directory) {
                    return (that.cpath.length() >= this.cpath.length()) &&
                            that.cpath.startsWith(this.cpath);
                }  else {
                    return ((that.cpath.length() > this.cpath.length()) &&
                        that.cpath.startsWith(this.cpath));
                }
            } else {
                if (that.directory) {
                    // if the permission passed in is a directory
                    // specification, make sure that a non-recursive
                    // permission (i.e., this object) can't imply a recursive
                    // permission.
                    if (that.recursive)
                        return false;
                    else
                        return (this.cpath.equals(that.cpath));
                } else {
                    int last = that.cpath.lastIndexOf(File.separatorChar);
                    if (last == -1)
                        return false;
                    else {
                        // this.cpath.equals(that.cpath.substring(0, last+1));
                        // Use regionMatches to avoid creating new string
                        return (this.cpath.length() == (last + 1)) &&
                            this.cpath.regionMatches(0, that.cpath, 0, last+1);
                    }
                }
            }
        } else if (that.directory) {
            // if this is NOT recursive/wildcarded,
            // do not let it imply a recursive/wildcarded permission
            return false;
        } else {
            return (this.cpath.equals(that.cpath));
        }
    }


    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (! (obj instanceof FilePermission))
            return false;

        FilePermission that = (FilePermission) obj;

        return (this.mask == that.mask) &&
            this.cpath.equals(that.cpath) &&
            (this.directory == that.directory) &&
            (this.recursive == that.recursive);
    }


    public int hashCode() {
        return 0;
    }


    private static int getMask(String actions) {
        int mask = NONE;

        // Null action valid?
        if (actions == null) {
            return mask;
        }

        // Use object identity comparison against known-interned strings for
        // performance benefit (these values are used heavily within the JDK).
        if (actions == SecurityConstants.FILE_READ_ACTION) {
            return READ;
        } else if (actions == SecurityConstants.FILE_WRITE_ACTION) {
            return WRITE;
        } else if (actions == SecurityConstants.FILE_EXECUTE_ACTION) {
            return EXECUTE;
        } else if (actions == SecurityConstants.FILE_DELETE_ACTION) {
            return DELETE;
        } else if (actions == SecurityConstants.FILE_READLINK_ACTION) {
            return READLINK;
        }

        char[] a = actions.toCharArray();

        int i = a.length - 1;
        if (i < 0)
            return mask;

        while (i != -1) {
            char c;

            // skip whitespace
            while ((i!=-1) && ((c = a[i]) == ' ' ||
                               c == '\r' ||
                               c == '\n' ||
                               c == '\f' ||
                               c == '\t'))
                i--;

            // check for the known strings
            int matchlen;

            if (i >= 3 && (a[i-3] == 'r' || a[i-3] == 'R') &&
                          (a[i-2] == 'e' || a[i-2] == 'E') &&
                          (a[i-1] == 'a' || a[i-1] == 'A') &&
                          (a[i] == 'd' || a[i] == 'D'))
            {
                matchlen = 4;
                mask |= READ;

            } else if (i >= 4 && (a[i-4] == 'w' || a[i-4] == 'W') &&
                                 (a[i-3] == 'r' || a[i-3] == 'R') &&
                                 (a[i-2] == 'i' || a[i-2] == 'I') &&
                                 (a[i-1] == 't' || a[i-1] == 'T') &&
                                 (a[i] == 'e' || a[i] == 'E'))
            {
                matchlen = 5;
                mask |= WRITE;

            } else if (i >= 6 && (a[i-6] == 'e' || a[i-6] == 'E') &&
                                 (a[i-5] == 'x' || a[i-5] == 'X') &&
                                 (a[i-4] == 'e' || a[i-4] == 'E') &&
                                 (a[i-3] == 'c' || a[i-3] == 'C') &&
                                 (a[i-2] == 'u' || a[i-2] == 'U') &&
                                 (a[i-1] == 't' || a[i-1] == 'T') &&
                                 (a[i] == 'e' || a[i] == 'E'))
            {
                matchlen = 7;
                mask |= EXECUTE;

            } else if (i >= 5 && (a[i-5] == 'd' || a[i-5] == 'D') &&
                                 (a[i-4] == 'e' || a[i-4] == 'E') &&
                                 (a[i-3] == 'l' || a[i-3] == 'L') &&
                                 (a[i-2] == 'e' || a[i-2] == 'E') &&
                                 (a[i-1] == 't' || a[i-1] == 'T') &&
                                 (a[i] == 'e' || a[i] == 'E'))
            {
                matchlen = 6;
                mask |= DELETE;

            } else if (i >= 7 && (a[i-7] == 'r' || a[i-7] == 'R') &&
                                 (a[i-6] == 'e' || a[i-6] == 'E') &&
                                 (a[i-5] == 'a' || a[i-5] == 'A') &&
                                 (a[i-4] == 'd' || a[i-4] == 'D') &&
                                 (a[i-3] == 'l' || a[i-3] == 'L') &&
                                 (a[i-2] == 'i' || a[i-2] == 'I') &&
                                 (a[i-1] == 'n' || a[i-1] == 'N') &&
                                 (a[i] == 'k' || a[i] == 'K'))
            {
                matchlen = 8;
                mask |= READLINK;

            } else {
                // parse error
                throw new IllegalArgumentException(
                        "invalid permission: " + actions);
            }

            // make sure we didn't just match the tail of a word
            // like "ackbarfaccept".  Also, skip to the comma.
            boolean seencomma = false;
            while (i >= matchlen && !seencomma) {
                switch(a[i-matchlen]) {
                case ',':
                    seencomma = true;
                    break;
                case ' ': case '\r': case '\n':
                case '\f': case '\t':
                    break;
                default:
                    throw new IllegalArgumentException(
                            "invalid permission: " + actions);
                }
                i--;
            }

            // point i at the location of the comma minus one (or -1).
            i -= matchlen;
        }

        return mask;
    }


    int getMask() {
        return mask;
    }


    private static String getActions(int mask) {
        StringBuilder sb = new StringBuilder();
        boolean comma = false;

        if ((mask & READ) == READ) {
            comma = true;
            sb.append("read");
        }

        if ((mask & WRITE) == WRITE) {
            if (comma) sb.append(',');
            else comma = true;
            sb.append("write");
        }

        if ((mask & EXECUTE) == EXECUTE) {
            if (comma) sb.append(',');
            else comma = true;
            sb.append("execute");
        }

        if ((mask & DELETE) == DELETE) {
            if (comma) sb.append(',');
            else comma = true;
            sb.append("delete");
        }

        if ((mask & READLINK) == READLINK) {
            if (comma) sb.append(',');
            else comma = true;
            sb.append("readlink");
        }

        return sb.toString();
    }


    public String getActions() {
        if (actions == null)
            actions = getActions(this.mask);

        return actions;
    }


    public PermissionCollection newPermissionCollection() {
        return new FilePermissionCollection();
    }


    private void writeObject(ObjectOutputStream s)
        throws IOException
    {
        // Write out the actions. The superclass takes care of the name
        // call getActions to make sure actions field is initialized
        if (actions == null)
            getActions();
        s.defaultWriteObject();
    }


    private void readObject(ObjectInputStream s)
         throws IOException, ClassNotFoundException
    {
        // Read in the actions, then restore everything else by calling init.
        s.defaultReadObject();
        init(getMask(actions));
    }
}



final class FilePermissionCollection extends PermissionCollection
    implements Serializable
{
    // Not serialized; see serialization section at end of class
    private transient List<Permission> perms;


    public FilePermissionCollection() {
        perms = new ArrayList<>();
    }


    public void add(Permission permission) {
        if (! (permission instanceof FilePermission))
            throw new IllegalArgumentException("invalid permission: "+
                                               permission);
        if (isReadOnly())
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");

        synchronized (this) {
            perms.add(permission);
        }
    }


    public boolean implies(Permission permission) {
        if (! (permission instanceof FilePermission))
            return false;

        FilePermission fp = (FilePermission) permission;

        int desired = fp.getMask();
        int effective = 0;
        int needed = desired;

        synchronized (this) {
            int len = perms.size();
            for (int i = 0; i < len; i++) {
                FilePermission x = (FilePermission) perms.get(i);
                if (((needed & x.getMask()) != 0) && x.impliesIgnoreMask(fp)) {
                    effective |=  x.getMask();
                    if ((effective & desired) == desired)
                        return true;
                    needed = (desired ^ effective);
                }
            }
        }
        return false;
    }


    public Enumeration<Permission> elements() {
        // Convert Iterator into Enumeration
        synchronized (this) {
            return Collections.enumeration(perms);
        }
    }

    private static final long serialVersionUID = 2202956749081564585L;

    // Need to maintain serialization interoperability with earlier releases,
    // which had the serializable field:
    //    private Vector permissions;


    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("permissions", Vector.class),
    };



    private void writeObject(ObjectOutputStream out) throws IOException {
        // Don't call out.defaultWriteObject()

        // Write out Vector
        Vector<Permission> permissions = new Vector<>(perms.size());
        synchronized (this) {
            permissions.addAll(perms);
        }

        ObjectOutputStream.PutField pfields = out.putFields();
        pfields.put("permissions", permissions);
        out.writeFields();
    }


    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        // Don't call defaultReadObject()

        // Read in serialized fields
        ObjectInputStream.GetField gfields = in.readFields();

        // Get the one we want
        @SuppressWarnings("unchecked")
        Vector<Permission> permissions = (Vector<Permission>)gfields.get("permissions", null);
        perms = new ArrayList<>(permissions.size());
        perms.addAll(permissions);
    }
}
