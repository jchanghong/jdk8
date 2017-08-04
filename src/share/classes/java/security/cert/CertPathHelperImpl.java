package java.security.cert;
import java.util.*;
import sun.security.provider.certpath.CertPathHelper;
import sun.security.x509.GeneralNameInterface;
class CertPathHelperImpl extends CertPathHelper {
    private CertPathHelperImpl() {
        // empty
    }
    synchronized static void initialize() {
        if (CertPathHelper.instance == null) {
            CertPathHelper.instance = new CertPathHelperImpl();
        }
    }
    protected void implSetPathToNames(X509CertSelector sel,
            Set<GeneralNameInterface> names) {
        sel.setPathToNamesInternal(names);
    }
    protected void implSetDateAndTime(X509CRLSelector sel, Date date, long skew) {
        sel.setDateAndTime(date, skew);
    }
}
