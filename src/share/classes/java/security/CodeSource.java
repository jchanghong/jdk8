package java.security;
import java.net.URL;
import java.net.SocketPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.*;
public class CodeSource implements java.io.Serializable {
    private static final long serialVersionUID = 4977541819976013951L;
    private URL location;
    private transient CodeSigner[] signers = null;
    private transient java.security.cert.Certificate certs[] = null;
    // cached SocketPermission used for matchLocation
    private transient SocketPermission sp;
    // for generating cert paths
    private transient CertificateFactory factory = null;
    public CodeSource(URL url, java.security.cert.Certificate certs[]) {
        this.location = url;
        // Copy the supplied certs
        if (certs != null) {
            this.certs = certs.clone();
        }
    }
    public CodeSource(URL url, CodeSigner[] signers) {
        this.location = url;
        // Copy the supplied signers
        if (signers != null) {
            this.signers = signers.clone();
        }
    }
    @Override
    public int hashCode() {
        if (location != null)
            return location.hashCode();
        else
            return 0;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        // objects types must be equal
        if (!(obj instanceof CodeSource))
            return false;
        CodeSource cs = (CodeSource) obj;
        // URLs must match
        if (location == null) {
            // if location is null, then cs.location must be null as well
            if (cs.location != null) return false;
        } else {
            // if location is not null, then it must equal cs.location
            if (!location.equals(cs.location)) return false;
        }
        // certs must match
        return matchCerts(cs, true);
    }
    public final URL getLocation() {
        return this.location;
    }
    public final java.security.cert.Certificate[] getCertificates() {
        if (certs != null) {
            return certs.clone();
        } else if (signers != null) {
            // Convert the code signers to certs
            ArrayList<java.security.cert.Certificate> certChains =
                        new ArrayList<>();
            for (int i = 0; i < signers.length; i++) {
                certChains.addAll(
                    signers[i].getSignerCertPath().getCertificates());
            }
            certs = certChains.toArray(
                        new java.security.cert.Certificate[certChains.size()]);
            return certs.clone();
        } else {
            return null;
        }
    }
    public final CodeSigner[] getCodeSigners() {
        if (signers != null) {
            return signers.clone();
        } else if (certs != null) {
            // Convert the certs to code signers
            signers = convertCertArrayToSignerArray(certs);
            return signers.clone();
        } else {
            return null;
        }
    }
    public boolean implies(CodeSource codesource)
    {
        if (codesource == null)
            return false;
        return matchCerts(codesource, false) && matchLocation(codesource);
    }
    private boolean matchCerts(CodeSource that, boolean strict)
    {
        boolean match;
        // match any key
        if (certs == null && signers == null) {
            if (strict) {
                return (that.certs == null && that.signers == null);
            } else {
                return true;
            }
        // both have signers
        } else if (signers != null && that.signers != null) {
            if (strict && signers.length != that.signers.length) {
                return false;
            }
            for (int i = 0; i < signers.length; i++) {
                match = false;
                for (int j = 0; j < that.signers.length; j++) {
                    if (signers[i].equals(that.signers[j])) {
                        match = true;
                        break;
                    }
                }
                if (!match) return false;
            }
            return true;
        // both have certs
        } else if (certs != null && that.certs != null) {
            if (strict && certs.length != that.certs.length) {
                return false;
            }
            for (int i = 0; i < certs.length; i++) {
                match = false;
                for (int j = 0; j < that.certs.length; j++) {
                    if (certs[i].equals(that.certs[j])) {
                        match = true;
                        break;
                    }
                }
                if (!match) return false;
            }
            return true;
        }
        return false;
    }
    private boolean matchLocation(CodeSource that) {
        if (location == null)
            return true;
        if ((that == null) || (that.location == null))
            return false;
        if (location.equals(that.location))
            return true;
        if (!location.getProtocol().equalsIgnoreCase(that.location.getProtocol()))
            return false;
        int thisPort = location.getPort();
        if (thisPort != -1) {
            int thatPort = that.location.getPort();
            int port = thatPort != -1 ? thatPort
                                      : that.location.getDefaultPort();
            if (thisPort != port)
                return false;
        }
        if (location.getFile().endsWith("/-")) {
            // Matches the directory and (recursively) all files
            // and subdirectories contained in that directory.
            // For example, "/a/b/-" implies anything that starts with
            // "/a/b/"
            String thisPath = location.getFile().substring(0,
                                            location.getFile().length()-1);
            if (!that.location.getFile().startsWith(thisPath))
                return false;
        } else if (location.getFile().endsWith("
    private CodeSigner[] convertCertArrayToSignerArray(
        java.security.cert.Certificate[] certs) {
        if (certs == null) {
            return null;
        }
        try {
            // Initialize certificate factory
            if (factory == null) {
                factory = CertificateFactory.getInstance("X.509");
            }
            // Iterate through all the certificates
            int i = 0;
            List<CodeSigner> signers = new ArrayList<>();
            while (i < certs.length) {
                List<java.security.cert.Certificate> certChain =
                        new ArrayList<>();
                certChain.add(certs[i++]); // first cert is an end-entity cert
                int j = i;
                // Extract chain of certificates
                // (loop while certs are not end-entity certs)
                while (j < certs.length &&
                    certs[j] instanceof X509Certificate &&
                    ((X509Certificate)certs[j]).getBasicConstraints() != -1) {
                    certChain.add(certs[j]);
                    j++;
                }
                i = j;
                CertPath certPath = factory.generateCertPath(certChain);
                signers.add(new CodeSigner(certPath, null));
            }
            if (signers.isEmpty()) {
                return null;
            } else {
                return signers.toArray(new CodeSigner[signers.size()]);
            }
        } catch (CertificateException e) {
            return null; //TODO - may be better to throw an ex. here
        }
    }
}
