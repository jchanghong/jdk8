


package java.time.zone;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;


public abstract class ZoneRulesProvider {


    private static final CopyOnWriteArrayList<ZoneRulesProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private static final ConcurrentMap<String, ZoneRulesProvider> ZONES = new ConcurrentHashMap<>(512, 0.75f, 2);

    static {
        // if the property java.time.zone.DefaultZoneRulesProvider is
        // set then its value is the class name of the default provider
        final List<ZoneRulesProvider> loaded = new ArrayList<>();
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                String prop = System.getProperty("java.time.zone.DefaultZoneRulesProvider");
                if (prop != null) {
                    try {
                        Class<?> c = Class.forName(prop, true, ClassLoader.getSystemClassLoader());
                        ZoneRulesProvider provider = ZoneRulesProvider.class.cast(c.newInstance());
                        registerProvider(provider);
                        loaded.add(provider);
                    } catch (Exception x) {
                        throw new Error(x);
                    }
                } else {
                    registerProvider(new TzdbZoneRulesProvider());
                }
                return null;
            }
        });

        ServiceLoader<ZoneRulesProvider> sl = ServiceLoader.load(ZoneRulesProvider.class, ClassLoader.getSystemClassLoader());
        Iterator<ZoneRulesProvider> it = sl.iterator();
        while (it.hasNext()) {
            ZoneRulesProvider provider;
            try {
                provider = it.next();
            } catch (ServiceConfigurationError ex) {
                if (ex.getCause() instanceof SecurityException) {
                    continue;  // ignore the security exception, try the next provider
                }
                throw ex;
            }
            boolean found = false;
            for (ZoneRulesProvider p : loaded) {
                if (p.getClass() == provider.getClass()) {
                    found = true;
                }
            }
            if (!found) {
                registerProvider0(provider);
                loaded.add(provider);
            }
        }
        // CopyOnWriteList could be slow if lots of providers and each added individually
        PROVIDERS.addAll(loaded);
    }

    //-------------------------------------------------------------------------

    public static Set<String> getAvailableZoneIds() {
        return new HashSet<>(ZONES.keySet());
    }


    public static ZoneRules getRules(String zoneId, boolean forCaching) {
        Objects.requireNonNull(zoneId, "zoneId");
        return getProvider(zoneId).provideRules(zoneId, forCaching);
    }


    public static NavigableMap<String, ZoneRules> getVersions(String zoneId) {
        Objects.requireNonNull(zoneId, "zoneId");
        return getProvider(zoneId).provideVersions(zoneId);
    }


    private static ZoneRulesProvider getProvider(String zoneId) {
        ZoneRulesProvider provider = ZONES.get(zoneId);
        if (provider == null) {
            if (ZONES.isEmpty()) {
                throw new ZoneRulesException("No time-zone data files registered");
            }
            throw new ZoneRulesException("Unknown time-zone ID: " + zoneId);
        }
        return provider;
    }

    //-------------------------------------------------------------------------

    public static void registerProvider(ZoneRulesProvider provider) {
        Objects.requireNonNull(provider, "provider");
        registerProvider0(provider);
        PROVIDERS.add(provider);
    }


    private static void registerProvider0(ZoneRulesProvider provider) {
        for (String zoneId : provider.provideZoneIds()) {
            Objects.requireNonNull(zoneId, "zoneId");
            ZoneRulesProvider old = ZONES.putIfAbsent(zoneId, provider);
            if (old != null) {
                throw new ZoneRulesException(
                    "Unable to register zone as one already registered with that ID: " + zoneId +
                    ", currently loading from provider: " + provider);
            }
        }
    }


    public static boolean refresh() {
        boolean changed = false;
        for (ZoneRulesProvider provider : PROVIDERS) {
            changed |= provider.provideRefresh();
        }
        return changed;
    }


    protected ZoneRulesProvider() {
    }

    //-----------------------------------------------------------------------

    protected abstract Set<String> provideZoneIds();


    protected abstract ZoneRules provideRules(String zoneId, boolean forCaching);


    protected abstract NavigableMap<String, ZoneRules> provideVersions(String zoneId);


    protected boolean provideRefresh() {
        return false;
    }

}
