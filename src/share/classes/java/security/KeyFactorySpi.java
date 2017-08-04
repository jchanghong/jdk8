package java.security;
import java.security.spec.KeySpec;
import java.security.spec.InvalidKeySpecException;
public abstract class KeyFactorySpi {
    protected abstract PublicKey engineGeneratePublic(KeySpec keySpec)
        throws InvalidKeySpecException;
    protected abstract PrivateKey engineGeneratePrivate(KeySpec keySpec)
        throws InvalidKeySpecException;
    protected abstract <T extends KeySpec>
        T engineGetKeySpec(Key key, Class<T> keySpec)
        throws InvalidKeySpecException;
    protected abstract Key engineTranslateKey(Key key)
        throws InvalidKeyException;
}
