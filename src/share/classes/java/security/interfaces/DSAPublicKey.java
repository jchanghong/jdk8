

package java.security.interfaces;

import java.math.BigInteger;


public interface DSAPublicKey extends DSAKey, java.security.PublicKey {

    // Declare serialVersionUID to be compatible with JDK1.1


    static final long serialVersionUID = 1234526332779022332L;


    public BigInteger getY();
}
