package java.security.cert;
import java.util.Collection;
import java.util.Set;
public abstract class PKIXCertPathChecker
    implements CertPathChecker, Cloneable {
    protected PKIXCertPathChecker() {}
    @Override
    public abstract void init(boolean forward)
        throws CertPathValidatorException;
    @Override
    public abstract boolean isForwardCheckingSupported();
    public abstract Set<String> getSupportedExtensions();
    public abstract void check(Certificate cert,
            Collection<String> unresolvedCritExts)
            throws CertPathValidatorException;
    @Override
    public void check(Certificate cert) throws CertPathValidatorException {
        check(cert, java.util.Collections.<String>emptySet());
    }
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }
}
