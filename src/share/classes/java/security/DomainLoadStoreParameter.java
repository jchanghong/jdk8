package java.security;
import java.net.URI;
import java.util.*;
import static java.security.KeyStore.*;
public final class DomainLoadStoreParameter implements LoadStoreParameter {
    private final URI configuration;
    private final Map<String,ProtectionParameter> protectionParams;
    public DomainLoadStoreParameter(URI configuration,
        Map<String,ProtectionParameter> protectionParams) {
        if (configuration == null || protectionParams == null) {
            throw new NullPointerException("invalid null input");
        }
        this.configuration = configuration;
        this.protectionParams =
            Collections.unmodifiableMap(new HashMap<>(protectionParams));
    }
    public URI getConfiguration() {
        return configuration;
    }
    public Map<String,ProtectionParameter> getProtectionParams() {
        return protectionParams;
    }
    @Override
    public KeyStore.ProtectionParameter getProtectionParameter() {
        return null;
    }
}
