

package java.security.cert;

import java.security.InvalidAlgorithmParameterException;


public abstract class CertPathBuilderSpi {


    public CertPathBuilderSpi() { }


    public abstract CertPathBuilderResult engineBuild(CertPathParameters params)
        throws CertPathBuilderException, InvalidAlgorithmParameterException;


    public CertPathChecker engineGetRevocationChecker() {
        throw new UnsupportedOperationException();
    }
}
