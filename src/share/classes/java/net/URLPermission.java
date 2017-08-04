

package java.net;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.security.Permission;


public final class URLPermission extends Permission {

    private static final long serialVersionUID = -2702463814894478682L;

    private transient String scheme;
    private transient String ssp;                 // scheme specific part
    private transient String path;
    private transient List<String> methods;
    private transient List<String> requestHeaders;
    private transient Authority authority;

    // serialized field
    private String actions;


    public URLPermission(String url, String actions) {
        super(url);
        init(actions);
    }

    private void init(String actions) {
        parseURI(getName());
        int colon = actions.indexOf(':');
        if (actions.lastIndexOf(':') != colon) {
            throw new IllegalArgumentException(
                "Invalid actions string: \"" + actions + "\"");
        }

        String methods, headers;
        if (colon == -1) {
            methods = actions;
            headers = "";
        } else {
            methods = actions.substring(0, colon);
            headers = actions.substring(colon+1);
        }

        List<String> l = normalizeMethods(methods);
        Collections.sort(l);
        this.methods = Collections.unmodifiableList(l);

        l = normalizeHeaders(headers);
        Collections.sort(l);
        this.requestHeaders = Collections.unmodifiableList(l);

        this.actions = actions();
    }


    public URLPermission(String url) {
        this(url, "*:*");
    }


    public String getActions() {
        return actions;
    }


    public boolean implies(Permission p) {
        if (! (p instanceof URLPermission)) {
            return false;
        }

        URLPermission that = (URLPermission)p;

        if (!this.methods.get(0).equals("*") &&
                Collections.indexOfSubList(this.methods, that.methods) == -1) {
            return false;
        }

        if (this.requestHeaders.isEmpty() && !that.requestHeaders.isEmpty()) {
            return false;
        }

        if (!this.requestHeaders.isEmpty() &&
            !this.requestHeaders.get(0).equals("*") &&
             Collections.indexOfSubList(this.requestHeaders,
                                        that.requestHeaders) == -1) {
            return false;
        }

        if (!this.scheme.equals(that.scheme)) {
            return false;
        }

        if (this.ssp.equals("*")) {
            return true;
        }

        if (!this.authority.implies(that.authority)) {
            return false;
        }

        if (this.path == null) {
            return that.path == null;
        }
        if (that.path == null) {
            return false;
        }

        if (this.path.endsWith("/-")) {
            String thisprefix = this.path.substring(0, this.path.length() - 1);
            return that.path.startsWith(thisprefix);
            }

        if (this.path.endsWith("
                return true;
            }
            return thisrange[0] <= thatrange[0] &&
                        thisrange[1] >= thatrange[1];
        }

        boolean equals(Authority that) {
            return this.p.equals(that.p);
        }

        public int hashCode() {
            return p.hashCode();
        }
    }
}
