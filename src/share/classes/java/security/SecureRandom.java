package java.security;
import java.util.*;
import java.util.regex.*;
import java.security.Provider.Service;
import sun.security.jca.*;
import sun.security.jca.GetInstance.Instance;
import sun.security.util.Debug;
public class SecureRandom extends java.util.Random {
    private static final Debug pdebug =
                        Debug.getInstance("provider", "Provider");
    private static final boolean skipDebug =
        Debug.isOn("engine=") && !Debug.isOn("securerandom");
    private Provider provider = null;
    private SecureRandomSpi secureRandomSpi = null;
    private String algorithm;
    // Seed Generator
    private static volatile SecureRandom seedGenerator = null;
    public SecureRandom() {
        super(0);
        getDefaultPRNG(false, null);
    }
    public SecureRandom(byte seed[]) {
        super(0);
        getDefaultPRNG(true, seed);
    }
    private void getDefaultPRNG(boolean setSeed, byte[] seed) {
        String prng = getPrngAlgorithm();
        if (prng == null) {
            // bummer, get the SUN implementation
            prng = "SHA1PRNG";
            this.secureRandomSpi = new sun.security.provider.SecureRandom();
            this.provider = Providers.getSunProvider();
            if (setSeed) {
                this.secureRandomSpi.engineSetSeed(seed);
            }
        } else {
            try {
                SecureRandom random = SecureRandom.getInstance(prng);
                this.secureRandomSpi = random.getSecureRandomSpi();
                this.provider = random.getProvider();
                if (setSeed) {
                    this.secureRandomSpi.engineSetSeed(seed);
                }
            } catch (NoSuchAlgorithmException nsae) {
                // never happens, because we made sure the algorithm exists
                throw new RuntimeException(nsae);
            }
        }
        // JDK 1.1 based implementations subclass SecureRandom instead of
        // SecureRandomSpi. They will also go through this code path because
        // they must call a SecureRandom constructor as it is their superclass.
        // If we are dealing with such an implementation, do not set the
        // algorithm value as it would be inaccurate.
        if (getClass() == SecureRandom.class) {
            this.algorithm = prng;
        }
    }
    protected SecureRandom(SecureRandomSpi secureRandomSpi,
                           Provider provider) {
        this(secureRandomSpi, provider, null);
    }
    private SecureRandom(SecureRandomSpi secureRandomSpi, Provider provider,
            String algorithm) {
        super(0);
        this.secureRandomSpi = secureRandomSpi;
        this.provider = provider;
        this.algorithm = algorithm;
        if (!skipDebug && pdebug != null) {
            pdebug.println("SecureRandom." + algorithm +
                " algorithm from: " + this.provider.getName());
        }
    }
    public static SecureRandom getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        Instance instance = GetInstance.getInstance("SecureRandom",
            SecureRandomSpi.class, algorithm);
        return new SecureRandom((SecureRandomSpi)instance.impl,
            instance.provider, algorithm);
    }
    public static SecureRandom getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        Instance instance = GetInstance.getInstance("SecureRandom",
            SecureRandomSpi.class, algorithm, provider);
        return new SecureRandom((SecureRandomSpi)instance.impl,
            instance.provider, algorithm);
    }
    public static SecureRandom getInstance(String algorithm,
            Provider provider) throws NoSuchAlgorithmException {
        Instance instance = GetInstance.getInstance("SecureRandom",
            SecureRandomSpi.class, algorithm, provider);
        return new SecureRandom((SecureRandomSpi)instance.impl,
            instance.provider, algorithm);
    }
    SecureRandomSpi getSecureRandomSpi() {
        return secureRandomSpi;
    }
    public final Provider getProvider() {
        return provider;
    }
    public String getAlgorithm() {
        return (algorithm != null) ? algorithm : "unknown";
    }
    synchronized public void setSeed(byte[] seed) {
        secureRandomSpi.engineSetSeed(seed);
    }
    @Override
    public void setSeed(long seed) {
        if (seed != 0) {
            secureRandomSpi.engineSetSeed(longToByteArray(seed));
        }
    }
    @Override
    public void nextBytes(byte[] bytes) {
        secureRandomSpi.engineNextBytes(bytes);
    }
    @Override
    final protected int next(int numBits) {
        int numBytes = (numBits+7)/8;
        byte b[] = new byte[numBytes];
        int next = 0;
        nextBytes(b);
        for (int i = 0; i < numBytes; i++) {
            next = (next << 8) + (b[i] & 0xFF);
        }
        return next >>> (numBytes*8 - numBits);
    }
    public static byte[] getSeed(int numBytes) {
        if (seedGenerator == null) {
            seedGenerator = new SecureRandom();
        }
        return seedGenerator.generateSeed(numBytes);
    }
    public byte[] generateSeed(int numBytes) {
        return secureRandomSpi.engineGenerateSeed(numBytes);
    }
    private static byte[] longToByteArray(long l) {
        byte[] retVal = new byte[8];
        for (int i = 0; i < 8; i++) {
            retVal[i] = (byte) l;
            l >>= 8;
        }
        return retVal;
    }
    private static String getPrngAlgorithm() {
        for (Provider p : Providers.getProviderList().providers()) {
            for (Service s : p.getServices()) {
                if (s.getType().equals("SecureRandom")) {
                    return s.getAlgorithm();
                }
            }
        }
        return null;
    }
    private static final class StrongPatternHolder {
        private static Pattern pattern =
            Pattern.compile(
                "\\s*([\\S&&[^:,]]*)(\\:([\\S&&[^,]]*))?\\s*(\\,(.*))?");
    }
    public static SecureRandom getInstanceStrong()
            throws NoSuchAlgorithmException {
        String property = AccessController.doPrivileged(
            new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return Security.getProperty(
                        "securerandom.strongAlgorithms");
                }
            });
        if ((property == null) || (property.length() == 0)) {
            throw new NoSuchAlgorithmException(
                "Null/empty securerandom.strongAlgorithms Security Property");
        }
        String remainder = property;
        while (remainder != null) {
            Matcher m;
            if ((m = StrongPatternHolder.pattern.matcher(
                    remainder)).matches()) {
                String alg = m.group(1);
                String prov = m.group(3);
                try {
                    if (prov == null) {
                        return SecureRandom.getInstance(alg);
                    } else {
                        return SecureRandom.getInstance(alg, prov);
                    }
                } catch (NoSuchAlgorithmException |
                        NoSuchProviderException e) {
                }
                remainder = m.group(5);
            } else {
                remainder = null;
            }
        }
        throw new NoSuchAlgorithmException(
            "No strong SecureRandom impls available: " + property);
    }
    // Declare serialVersionUID to be compatible with JDK1.1
    static final long serialVersionUID = 4940670005562187L;
    // Retain unused values serialized from JDK1.1
    private byte[] state;
    private MessageDigest digest = null;
    private byte[] randomBytes;
    private int randomBytesUsed;
    private long counter;
}
