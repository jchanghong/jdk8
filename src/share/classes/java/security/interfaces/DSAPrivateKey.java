package java.security.interfaces;
import java.math.BigInteger;
public interface DSAPrivateKey extends DSAKey, java.security.PrivateKey {
    // Declare serialVersionUID to be compatible with JDK1.1
    static final long serialVersionUID = 7776497482533790279L;
    public BigInteger getX();
}
