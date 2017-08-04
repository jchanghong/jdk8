

package java.security.cert;


public interface CertPathChecker {


    void init(boolean forward) throws CertPathValidatorException;


    boolean isForwardCheckingSupported();


    void check(Certificate cert) throws CertPathValidatorException;
}
