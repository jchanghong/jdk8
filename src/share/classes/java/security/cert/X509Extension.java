package java.security.cert;
import java.util.Set;
public interface X509Extension {
    public boolean hasUnsupportedCriticalExtension();
    public Set<String> getCriticalExtensionOIDs();
    public Set<String> getNonCriticalExtensionOIDs();
    public byte[] getExtensionValue(String oid);
}
