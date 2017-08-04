package java.util;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.spi.ResourceBundleControlProvider;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;
import sun.util.locale.BaseLocale;
import sun.util.locale.LocaleObjectCache;
public abstract class ResourceBundle {
    private static final int INITIAL_CACHE_SIZE = 32;
    private static final ResourceBundle NONEXISTENT_BUNDLE = new ResourceBundle() {
            public Enumeration<String> getKeys() { return null; }
            protected Object handleGetObject(String key) { return null; }
            public String toString() { return "NONEXISTENT_BUNDLE"; }
        };
    private static final ConcurrentMap<CacheKey, BundleReference> cacheList
        = new ConcurrentHashMap<>(INITIAL_CACHE_SIZE);
    private static final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
    public String getBaseBundleName() {
        return name;
    }
    protected ResourceBundle parent = null;
    private Locale locale = null;
    private String name;
    private volatile boolean expired;
    private volatile CacheKey cacheKey;
    private volatile Set<String> keySet;
    private static final List<ResourceBundleControlProvider> providers;
    static {
        List<ResourceBundleControlProvider> list = null;
        ServiceLoader<ResourceBundleControlProvider> serviceLoaders
                = ServiceLoader.loadInstalled(ResourceBundleControlProvider.class);
        for (ResourceBundleControlProvider provider : serviceLoaders) {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(provider);
        }
        providers = list;
    }
    public ResourceBundle() {
    }
    public final String getString(String key) {
        return (String) getObject(key);
    }
    public final String[] getStringArray(String key) {
        return (String[]) getObject(key);
    }
    public final Object getObject(String key) {
        Object obj = handleGetObject(key);
        if (obj == null) {
            if (parent != null) {
                obj = parent.getObject(key);
            }
            if (obj == null) {
                throw new MissingResourceException("Can't find resource for bundle "
                                                   +this.getClass().getName()
                                                   +", key "+key,
                                                   this.getClass().getName(),
                                                   key);
            }
        }
        return obj;
    }
    public Locale getLocale() {
        return locale;
    }
    private static ClassLoader getLoader(Class<?> caller) {
        ClassLoader cl = caller == null ? null : caller.getClassLoader();
        if (cl == null) {
            // When the caller's loader is the boot class loader, cl is null
            // here. In that case, ClassLoader.getSystemClassLoader() may
            // return the same class loader that the application is
            // using. We therefore use a wrapper ClassLoader to create a
            // separate scope for bundles loaded on behalf of the Java
            // runtime so that these bundles cannot be returned from the
            // cache to the application (5048280).
            cl = RBClassLoader.INSTANCE;
        }
        return cl;
    }
    private static class RBClassLoader extends ClassLoader {
        private static final RBClassLoader INSTANCE = AccessController.doPrivileged(
                    new PrivilegedAction<RBClassLoader>() {
                        public RBClassLoader run() {
                            return new RBClassLoader();
                        }
                    });
        private static final ClassLoader loader = ClassLoader.getSystemClassLoader();
        private RBClassLoader() {
        }
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (loader != null) {
                return loader.loadClass(name);
            }
            return Class.forName(name);
        }
        public URL getResource(String name) {
            if (loader != null) {
                return loader.getResource(name);
            }
            return ClassLoader.getSystemResource(name);
        }
        public InputStream getResourceAsStream(String name) {
            if (loader != null) {
                return loader.getResourceAsStream(name);
            }
            return ClassLoader.getSystemResourceAsStream(name);
        }
    }
    protected void setParent(ResourceBundle parent) {
        assert parent != NONEXISTENT_BUNDLE;
        this.parent = parent;
    }
    private static class CacheKey implements Cloneable {
        // These three are the actual keys for lookup in Map.
        private String name;
        private Locale locale;
        private LoaderReference loaderRef;
        // bundle format which is necessary for calling
        // Control.needsReload().
        private String format;
        // These time values are in CacheKey so that NONEXISTENT_BUNDLE
        // doesn't need to be cloned for caching.
        // The time when the bundle has been loaded
        private volatile long loadTime;
        // The time when the bundle expires in the cache, or either
        // Control.TTL_DONT_CACHE or Control.TTL_NO_EXPIRATION_CONTROL.
        private volatile long expirationTime;
        // Placeholder for an error report by a Throwable
        private Throwable cause;
        // Hash code value cache to avoid recalculating the hash code
        // of this instance.
        private int hashCodeCache;
        CacheKey(String baseName, Locale locale, ClassLoader loader) {
            this.name = baseName;
            this.locale = locale;
            if (loader == null) {
                this.loaderRef = null;
            } else {
                loaderRef = new LoaderReference(loader, referenceQueue, this);
            }
            calculateHashCode();
        }
        String getName() {
            return name;
        }
        CacheKey setName(String baseName) {
            if (!this.name.equals(baseName)) {
                this.name = baseName;
                calculateHashCode();
            }
            return this;
        }
        Locale getLocale() {
            return locale;
        }
        CacheKey setLocale(Locale locale) {
            if (!this.locale.equals(locale)) {
                this.locale = locale;
                calculateHashCode();
            }
            return this;
        }
        ClassLoader getLoader() {
            return (loaderRef != null) ? loaderRef.get() : null;
        }
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            try {
                final CacheKey otherEntry = (CacheKey)other;
                //quick check to see if they are not equal
                if (hashCodeCache != otherEntry.hashCodeCache) {
                    return false;
                }
                //are the names the same?
                if (!name.equals(otherEntry.name)) {
                    return false;
                }
                // are the locales the same?
                if (!locale.equals(otherEntry.locale)) {
                    return false;
                }
                //are refs (both non-null) or (both null)?
                if (loaderRef == null) {
                    return otherEntry.loaderRef == null;
                }
                ClassLoader loader = loaderRef.get();
                return (otherEntry.loaderRef != null)
                        // with a null reference we can no longer find
                        // out which class loader was referenced; so
                        // treat it as unequal
                        && (loader != null)
                        && (loader == otherEntry.loaderRef.get());
            } catch (    NullPointerException | ClassCastException e) {
            }
            return false;
        }
        public int hashCode() {
            return hashCodeCache;
        }
        private void calculateHashCode() {
            hashCodeCache = name.hashCode() << 3;
            hashCodeCache ^= locale.hashCode();
            ClassLoader loader = getLoader();
            if (loader != null) {
                hashCodeCache ^= loader.hashCode();
            }
        }
        public Object clone() {
            try {
                CacheKey clone = (CacheKey) super.clone();
                if (loaderRef != null) {
                    clone.loaderRef = new LoaderReference(loaderRef.get(),
                                                          referenceQueue, clone);
                }
                // Clear the reference to a Throwable
                clone.cause = null;
                return clone;
            } catch (CloneNotSupportedException e) {
                //this should never happen
                throw new InternalError(e);
            }
        }
        String getFormat() {
            return format;
        }
        void setFormat(String format) {
            this.format = format;
        }
        private void setCause(Throwable cause) {
            if (this.cause == null) {
                this.cause = cause;
            } else {
                // Override the cause if the previous one is
                // ClassNotFoundException.
                if (this.cause instanceof ClassNotFoundException) {
                    this.cause = cause;
                }
            }
        }
        private Throwable getCause() {
            return cause;
        }
        public String toString() {
            String l = locale.toString();
            if (l.length() == 0) {
                if (locale.getVariant().length() != 0) {
                    l = "__" + locale.getVariant();
                } else {
                    l = "\"\"";
                }
            }
            return "CacheKey[" + name + ", lc=" + l + ", ldr=" + getLoader()
                + "(format=" + format + ")]";
        }
    }
    private static interface CacheKeyReference {
        public CacheKey getCacheKey();
    }
    private static class LoaderReference extends WeakReference<ClassLoader>
                                         implements CacheKeyReference {
        private CacheKey cacheKey;
        LoaderReference(ClassLoader referent, ReferenceQueue<Object> q, CacheKey key) {
            super(referent, q);
            cacheKey = key;
        }
        public CacheKey getCacheKey() {
            return cacheKey;
        }
    }
    private static class BundleReference extends SoftReference<ResourceBundle>
                                         implements CacheKeyReference {
        private CacheKey cacheKey;
        BundleReference(ResourceBundle referent, ReferenceQueue<Object> q, CacheKey key) {
            super(referent, q);
            cacheKey = key;
        }
        public CacheKey getCacheKey() {
            return cacheKey;
        }
    }
    @CallerSensitive
    public static final ResourceBundle getBundle(String baseName)
    {
        return getBundleImpl(baseName, Locale.getDefault(),
                             getLoader(Reflection.getCallerClass()),
                             getDefaultControl(baseName));
    }
    @CallerSensitive
    public static final ResourceBundle getBundle(String baseName,
                                                 Control control) {
        return getBundleImpl(baseName, Locale.getDefault(),
                             getLoader(Reflection.getCallerClass()),
                             control);
    }
    @CallerSensitive
    public static final ResourceBundle getBundle(String baseName,
                                                 Locale locale)
    {
        return getBundleImpl(baseName, locale,
                             getLoader(Reflection.getCallerClass()),
                             getDefaultControl(baseName));
    }
    @CallerSensitive
    public static final ResourceBundle getBundle(String baseName, Locale targetLocale,
                                                 Control control) {
        return getBundleImpl(baseName, targetLocale,
                             getLoader(Reflection.getCallerClass()),
                             control);
    }
    public static ResourceBundle getBundle(String baseName, Locale locale,
                                           ClassLoader loader)
    {
        if (loader == null) {
            throw new NullPointerException();
        }
        return getBundleImpl(baseName, locale, loader, getDefaultControl(baseName));
    }
    public static ResourceBundle getBundle(String baseName, Locale targetLocale,
                                           ClassLoader loader, Control control) {
        if (loader == null || control == null) {
            throw new NullPointerException();
        }
        return getBundleImpl(baseName, targetLocale, loader, control);
    }
    private static Control getDefaultControl(String baseName) {
        if (providers != null) {
            for (ResourceBundleControlProvider provider : providers) {
                Control control = provider.getControl(baseName);
                if (control != null) {
                    return control;
                }
            }
        }
        return Control.INSTANCE;
    }
    private static ResourceBundle getBundleImpl(String baseName, Locale locale,
                                                ClassLoader loader, Control control) {
        if (locale == null || control == null) {
            throw new NullPointerException();
        }
        // We create a CacheKey here for use by this call. The base
        // name and loader will never change during the bundle loading
        // process. We have to make sure that the locale is set before
        // using it as a cache key.
        CacheKey cacheKey = new CacheKey(baseName, locale, loader);
        ResourceBundle bundle = null;
        // Quick lookup of the cache.
        BundleReference bundleRef = cacheList.get(cacheKey);
        if (bundleRef != null) {
            bundle = bundleRef.get();
            bundleRef = null;
        }
        // If this bundle and all of its parents are valid (not expired),
        // then return this bundle. If any of the bundles is expired, we
        // don't call control.needsReload here but instead drop into the
        // complete loading process below.
        if (isValidBundle(bundle) && hasValidParentChain(bundle)) {
            return bundle;
        }
        // No valid bundle was found in the cache, so we need to load the
        // resource bundle and its parents.
        boolean isKnownControl = (control == Control.INSTANCE) ||
                                   (control instanceof SingleFormatControl);
        List<String> formats = control.getFormats(baseName);
        if (!isKnownControl && !checkList(formats)) {
            throw new IllegalArgumentException("Invalid Control: getFormats");
        }
        ResourceBundle baseBundle = null;
        for (Locale targetLocale = locale;
             targetLocale != null;
             targetLocale = control.getFallbackLocale(baseName, targetLocale)) {
            List<Locale> candidateLocales = control.getCandidateLocales(baseName, targetLocale);
            if (!isKnownControl && !checkList(candidateLocales)) {
                throw new IllegalArgumentException("Invalid Control: getCandidateLocales");
            }
            bundle = findBundle(cacheKey, candidateLocales, formats, 0, control, baseBundle);
            // If the loaded bundle is the base bundle and exactly for the
            // requested locale or the only candidate locale, then take the
            // bundle as the resulting one. If the loaded bundle is the base
            // bundle, it's put on hold until we finish processing all
            // fallback locales.
            if (isValidBundle(bundle)) {
                boolean isBaseBundle = Locale.ROOT.equals(bundle.locale);
                if (!isBaseBundle || bundle.locale.equals(locale)
                    || (candidateLocales.size() == 1
                        && bundle.locale.equals(candidateLocales.get(0)))) {
                    break;
                }
                // If the base bundle has been loaded, keep the reference in
                // baseBundle so that we can avoid any redundant loading in case
                // the control specify not to cache bundles.
                if (isBaseBundle && baseBundle == null) {
                    baseBundle = bundle;
                }
            }
        }
        if (bundle == null) {
            if (baseBundle == null) {
                throwMissingResourceException(baseName, locale, cacheKey.getCause());
            }
            bundle = baseBundle;
        }
        return bundle;
    }
    private static boolean checkList(List<?> a) {
        boolean valid = (a != null && !a.isEmpty());
        if (valid) {
            int size = a.size();
            for (int i = 0; valid && i < size; i++) {
                valid = (a.get(i) != null);
            }
        }
        return valid;
    }
    private static ResourceBundle findBundle(CacheKey cacheKey,
                                             List<Locale> candidateLocales,
                                             List<String> formats,
                                             int index,
                                             Control control,
                                             ResourceBundle baseBundle) {
        Locale targetLocale = candidateLocales.get(index);
        ResourceBundle parent = null;
        if (index != candidateLocales.size() - 1) {
            parent = findBundle(cacheKey, candidateLocales, formats, index + 1,
                                control, baseBundle);
        } else if (baseBundle != null && Locale.ROOT.equals(targetLocale)) {
            return baseBundle;
        }
        // Before we do the real loading work, see whether we need to
        // do some housekeeping: If references to class loaders or
        // resource bundles have been nulled out, remove all related
        // information from the cache.
        Object ref;
        while ((ref = referenceQueue.poll()) != null) {
            cacheList.remove(((CacheKeyReference)ref).getCacheKey());
        }
        // flag indicating the resource bundle has expired in the cache
        boolean expiredBundle = false;
        // First, look up the cache to see if it's in the cache, without
        // attempting to load bundle.
        cacheKey.setLocale(targetLocale);
        ResourceBundle bundle = findBundleInCache(cacheKey, control);
        if (isValidBundle(bundle)) {
            expiredBundle = bundle.expired;
            if (!expiredBundle) {
                // If its parent is the one asked for by the candidate
                // locales (the runtime lookup path), we can take the cached
                // one. (If it's not identical, then we'd have to check the
                // parent's parents to be consistent with what's been
                // requested.)
                if (bundle.parent == parent) {
                    return bundle;
                }
                // Otherwise, remove the cached one since we can't keep
                // the same bundles having different parents.
                BundleReference bundleRef = cacheList.get(cacheKey);
                if (bundleRef != null && bundleRef.get() == bundle) {
                    cacheList.remove(cacheKey, bundleRef);
                }
            }
        }
        if (bundle != NONEXISTENT_BUNDLE) {
            CacheKey constKey = (CacheKey) cacheKey.clone();
            try {
                bundle = loadBundle(cacheKey, formats, control, expiredBundle);
                if (bundle != null) {
                    if (bundle.parent == null) {
                        bundle.setParent(parent);
                    }
                    bundle.locale = targetLocale;
                    bundle = putBundleInCache(cacheKey, bundle, control);
                    return bundle;
                }
                // Put NONEXISTENT_BUNDLE in the cache as a mark that there's no bundle
                // instance for the locale.
                putBundleInCache(cacheKey, NONEXISTENT_BUNDLE, control);
            } finally {
                if (constKey.getCause() instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return parent;
    }
    private static ResourceBundle loadBundle(CacheKey cacheKey,
                                             List<String> formats,
                                             Control control,
                                             boolean reload) {
        // Here we actually load the bundle in the order of formats
        // specified by the getFormats() value.
        Locale targetLocale = cacheKey.getLocale();
        ResourceBundle bundle = null;
        int size = formats.size();
        for (int i = 0; i < size; i++) {
            String format = formats.get(i);
            try {
                bundle = control.newBundle(cacheKey.getName(), targetLocale, format,
                                           cacheKey.getLoader(), reload);
            } catch (LinkageError error) {
                // We need to handle the LinkageError case due to
                // inconsistent case-sensitivity in ClassLoader.
                // See 6572242 for details.
                cacheKey.setCause(error);
            } catch (Exception cause) {
                cacheKey.setCause(cause);
            }
            if (bundle != null) {
                // Set the format in the cache key so that it can be
                // used when calling needsReload later.
                cacheKey.setFormat(format);
                bundle.name = cacheKey.getName();
                bundle.locale = targetLocale;
                // Bundle provider might reuse instances. So we should make
                // sure to clear the expired flag here.
                bundle.expired = false;
                break;
            }
        }
        return bundle;
    }
    private static boolean isValidBundle(ResourceBundle bundle) {
        return bundle != null && bundle != NONEXISTENT_BUNDLE;
    }
    private static boolean hasValidParentChain(ResourceBundle bundle) {
        long now = System.currentTimeMillis();
        while (bundle != null) {
            if (bundle.expired) {
                return false;
            }
            CacheKey key = bundle.cacheKey;
            if (key != null) {
                long expirationTime = key.expirationTime;
                if (expirationTime >= 0 && expirationTime <= now) {
                    return false;
                }
            }
            bundle = bundle.parent;
        }
        return true;
    }
    private static void throwMissingResourceException(String baseName,
                                                      Locale locale,
                                                      Throwable cause) {
        // If the cause is a MissingResourceException, avoid creating
        // a long chain. (6355009)
        if (cause instanceof MissingResourceException) {
            cause = null;
        }
        throw new MissingResourceException("Can't find bundle for base name "
                                           + baseName + ", locale " + locale,
                                           baseName + "_" + locale, // className
                                           "",                      // key
                                           cause);
    }
    private static ResourceBundle findBundleInCache(CacheKey cacheKey,
                                                    Control control) {
        BundleReference bundleRef = cacheList.get(cacheKey);
        if (bundleRef == null) {
            return null;
        }
        ResourceBundle bundle = bundleRef.get();
        if (bundle == null) {
            return null;
        }
        ResourceBundle p = bundle.parent;
        assert p != NONEXISTENT_BUNDLE;
        // If the parent has expired, then this one must also expire. We
        // check only the immediate parent because the actual loading is
        // done from the root (base) to leaf (child) and the purpose of
        // checking is to propagate expiration towards the leaf. For
        // example, if the requested locale is ja_JP_JP and there are
        // bundles for all of the candidates in the cache, we have a list,
        //
        // base <- ja <- ja_JP <- ja_JP_JP
        //
        // If ja has expired, then it will reload ja and the list becomes a
        // tree.
        //
        // base <- ja (new)
        //  "   <- ja (expired) <- ja_JP <- ja_JP_JP
        //
        // When looking up ja_JP in the cache, it finds ja_JP in the cache
        // which references to the expired ja. Then, ja_JP is marked as
        // expired and removed from the cache. This will be propagated to
        // ja_JP_JP.
        //
        // Now, it's possible, for example, that while loading new ja_JP,
        // someone else has started loading the same bundle and finds the
        // base bundle has expired. Then, what we get from the first
        // getBundle call includes the expired base bundle. However, if
        // someone else didn't start its loading, we wouldn't know if the
        // base bundle has expired at the end of the loading process. The
        // expiration control doesn't guarantee that the returned bundle and
        // its parents haven't expired.
        //
        // We could check the entire parent chain to see if there's any in
        // the chain that has expired. But this process may never end. An
        // extreme case would be that getTimeToLive returns 0 and
        // needsReload always returns true.
        if (p != null && p.expired) {
            assert bundle != NONEXISTENT_BUNDLE;
            bundle.expired = true;
            bundle.cacheKey = null;
            cacheList.remove(cacheKey, bundleRef);
            bundle = null;
        } else {
            CacheKey key = bundleRef.getCacheKey();
            long expirationTime = key.expirationTime;
            if (!bundle.expired && expirationTime >= 0 &&
                expirationTime <= System.currentTimeMillis()) {
                // its TTL period has expired.
                if (bundle != NONEXISTENT_BUNDLE) {
                    // Synchronize here to call needsReload to avoid
                    // redundant concurrent calls for the same bundle.
                    synchronized (bundle) {
                        expirationTime = key.expirationTime;
                        if (!bundle.expired && expirationTime >= 0 &&
                            expirationTime <= System.currentTimeMillis()) {
                            try {
                                bundle.expired = control.needsReload(key.getName(),
                                                                     key.getLocale(),
                                                                     key.getFormat(),
                                                                     key.getLoader(),
                                                                     bundle,
                                                                     key.loadTime);
                            } catch (Exception e) {
                                cacheKey.setCause(e);
                            }
                            if (bundle.expired) {
                                // If the bundle needs to be reloaded, then
                                // remove the bundle from the cache, but
                                // return the bundle with the expired flag
                                // on.
                                bundle.cacheKey = null;
                                cacheList.remove(cacheKey, bundleRef);
                            } else {
                                // Update the expiration control info. and reuse
                                // the same bundle instance
                                setExpirationTime(key, control);
                            }
                        }
                    }
                } else {
                    // We just remove NONEXISTENT_BUNDLE from the cache.
                    cacheList.remove(cacheKey, bundleRef);
                    bundle = null;
                }
            }
        }
        return bundle;
    }
    private static ResourceBundle putBundleInCache(CacheKey cacheKey,
                                                   ResourceBundle bundle,
                                                   Control control) {
        setExpirationTime(cacheKey, control);
        if (cacheKey.expirationTime != Control.TTL_DONT_CACHE) {
            CacheKey key = (CacheKey) cacheKey.clone();
            BundleReference bundleRef = new BundleReference(bundle, referenceQueue, key);
            bundle.cacheKey = key;
            // Put the bundle in the cache if it's not been in the cache.
            BundleReference result = cacheList.putIfAbsent(key, bundleRef);
            // If someone else has put the same bundle in the cache before
            // us and it has not expired, we should use the one in the cache.
            if (result != null) {
                ResourceBundle rb = result.get();
                if (rb != null && !rb.expired) {
                    // Clear the back link to the cache key
                    bundle.cacheKey = null;
                    bundle = rb;
                    // Clear the reference in the BundleReference so that
                    // it won't be enqueued.
                    bundleRef.clear();
                } else {
                    // Replace the invalid (garbage collected or expired)
                    // instance with the valid one.
                    cacheList.put(key, bundleRef);
                }
            }
        }
        return bundle;
    }
    private static void setExpirationTime(CacheKey cacheKey, Control control) {
        long ttl = control.getTimeToLive(cacheKey.getName(),
                                         cacheKey.getLocale());
        if (ttl >= 0) {
            // If any expiration time is specified, set the time to be
            // expired in the cache.
            long now = System.currentTimeMillis();
            cacheKey.loadTime = now;
            cacheKey.expirationTime = now + ttl;
        } else if (ttl >= Control.TTL_NO_EXPIRATION_CONTROL) {
            cacheKey.expirationTime = ttl;
        } else {
            throw new IllegalArgumentException("Invalid Control: TTL=" + ttl);
        }
    }
    @CallerSensitive
    public static final void clearCache() {
        clearCache(getLoader(Reflection.getCallerClass()));
    }
    public static final void clearCache(ClassLoader loader) {
        if (loader == null) {
            throw new NullPointerException();
        }
        Set<CacheKey> set = cacheList.keySet();
        for (CacheKey key : set) {
            if (key.getLoader() == loader) {
                set.remove(key);
            }
        }
    }
    protected abstract Object handleGetObject(String key);
    public abstract Enumeration<String> getKeys();
    public boolean containsKey(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        for (ResourceBundle rb = this; rb != null; rb = rb.parent) {
            if (rb.handleKeySet().contains(key)) {
                return true;
            }
        }
        return false;
    }
    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();
        for (ResourceBundle rb = this; rb != null; rb = rb.parent) {
            keys.addAll(rb.handleKeySet());
        }
        return keys;
    }
    protected Set<String> handleKeySet() {
        if (keySet == null) {
            synchronized (this) {
                if (keySet == null) {
                    Set<String> keys = new HashSet<>();
                    Enumeration<String> enumKeys = getKeys();
                    while (enumKeys.hasMoreElements()) {
                        String key = enumKeys.nextElement();
                        if (handleGetObject(key) != null) {
                            keys.add(key);
                        }
                    }
                    keySet = keys;
                }
            }
        }
        return keySet;
    }
    public static class Control {
        public static final List<String> FORMAT_DEFAULT
            = Collections.unmodifiableList(Arrays.asList("java.class",
                                                         "java.properties"));
        public static final List<String> FORMAT_CLASS
            = Collections.unmodifiableList(Arrays.asList("java.class"));
        public static final List<String> FORMAT_PROPERTIES
            = Collections.unmodifiableList(Arrays.asList("java.properties"));
        public static final long TTL_DONT_CACHE = -1;
        public static final long TTL_NO_EXPIRATION_CONTROL = -2;
        private static final Control INSTANCE = new Control();
        protected Control() {
        }
        public static final Control getControl(List<String> formats) {
            if (formats.equals(Control.FORMAT_PROPERTIES)) {
                return SingleFormatControl.PROPERTIES_ONLY;
            }
            if (formats.equals(Control.FORMAT_CLASS)) {
                return SingleFormatControl.CLASS_ONLY;
            }
            if (formats.equals(Control.FORMAT_DEFAULT)) {
                return Control.INSTANCE;
            }
            throw new IllegalArgumentException();
        }
        public static final Control getNoFallbackControl(List<String> formats) {
            if (formats.equals(Control.FORMAT_DEFAULT)) {
                return NoFallbackControl.NO_FALLBACK;
            }
            if (formats.equals(Control.FORMAT_PROPERTIES)) {
                return NoFallbackControl.PROPERTIES_ONLY_NO_FALLBACK;
            }
            if (formats.equals(Control.FORMAT_CLASS)) {
                return NoFallbackControl.CLASS_ONLY_NO_FALLBACK;
            }
            throw new IllegalArgumentException();
        }
        public List<String> getFormats(String baseName) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            return FORMAT_DEFAULT;
        }
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            return new ArrayList<>(CANDIDATES_CACHE.get(locale.getBaseLocale()));
        }
        private static final CandidateListCache CANDIDATES_CACHE = new CandidateListCache();
        private static class CandidateListCache extends LocaleObjectCache<BaseLocale, List<Locale>> {
            protected List<Locale> createObject(BaseLocale base) {
                String language = base.getLanguage();
                String script = base.getScript();
                String region = base.getRegion();
                String variant = base.getVariant();
                // Special handling for Norwegian
                boolean isNorwegianBokmal = false;
                boolean isNorwegianNynorsk = false;
                if (language.equals("no")) {
                    if (region.equals("NO") && variant.equals("NY")) {
                        variant = "";
                        isNorwegianNynorsk = true;
                    } else {
                        isNorwegianBokmal = true;
                    }
                }
                if (language.equals("nb") || isNorwegianBokmal) {
                    List<Locale> tmpList = getDefaultList("nb", script, region, variant);
                    // Insert a locale replacing "nb" with "no" for every list entry
                    List<Locale> bokmalList = new LinkedList<>();
                    for (Locale l : tmpList) {
                        bokmalList.add(l);
                        if (l.getLanguage().length() == 0) {
                            break;
                        }
                        bokmalList.add(Locale.getInstance("no", l.getScript(), l.getCountry(),
                                l.getVariant(), null));
                    }
                    return bokmalList;
                } else if (language.equals("nn") || isNorwegianNynorsk) {
                    // Insert no_NO_NY, no_NO, no after nn
                    List<Locale> nynorskList = getDefaultList("nn", script, region, variant);
                    int idx = nynorskList.size() - 1;
                    nynorskList.add(idx++, Locale.getInstance("no", "NO", "NY"));
                    nynorskList.add(idx++, Locale.getInstance("no", "NO", ""));
                    nynorskList.add(idx++, Locale.getInstance("no", "", ""));
                    return nynorskList;
                }
                // Special handling for Chinese
                else if (language.equals("zh")) {
                    if (script.length() == 0 && region.length() > 0) {
                        // Supply script for users who want to use zh_Hans/zh_Hant
                        // as bundle names (recommended for Java7+)
                        switch (region) {
                        case "TW":
                        case "HK":
                        case "MO":
                            script = "Hant";
                            break;
                        case "CN":
                        case "SG":
                            script = "Hans";
                            break;
                        }
                    } else if (script.length() > 0 && region.length() == 0) {
                        // Supply region(country) for users who still package Chinese
                        // bundles using old convension.
                        switch (script) {
                        case "Hans":
                            region = "CN";
                            break;
                        case "Hant":
                            region = "TW";
                            break;
                        }
                    }
                }
                return getDefaultList(language, script, region, variant);
            }
            private static List<Locale> getDefaultList(String language, String script, String region, String variant) {
                List<String> variants = null;
                if (variant.length() > 0) {
                    variants = new LinkedList<>();
                    int idx = variant.length();
                    while (idx != -1) {
                        variants.add(variant.substring(0, idx));
                        idx = variant.lastIndexOf('_', --idx);
                    }
                }
                List<Locale> list = new LinkedList<>();
                if (variants != null) {
                    for (String v : variants) {
                        list.add(Locale.getInstance(language, script, region, v, null));
                    }
                }
                if (region.length() > 0) {
                    list.add(Locale.getInstance(language, script, region, "", null));
                }
                if (script.length() > 0) {
                    list.add(Locale.getInstance(language, script, "", "", null));
                    // With script, after truncating variant, region and script,
                    // start over without script.
                    if (variants != null) {
                        for (String v : variants) {
                            list.add(Locale.getInstance(language, "", region, v, null));
                        }
                    }
                    if (region.length() > 0) {
                        list.add(Locale.getInstance(language, "", region, "", null));
                    }
                }
                if (language.length() > 0) {
                    list.add(Locale.getInstance(language, "", "", "", null));
                }
                // Add root locale at the end
                list.add(Locale.ROOT);
                return list;
            }
        }
        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            Locale defaultLocale = Locale.getDefault();
            return locale.equals(defaultLocale) ? null : defaultLocale;
        }
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload)
                    throws IllegalAccessException, InstantiationException, IOException {
            String bundleName = toBundleName(baseName, locale);
            ResourceBundle bundle = null;
            if (format.equals("java.class")) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends ResourceBundle> bundleClass
                        = (Class<? extends ResourceBundle>)loader.loadClass(bundleName);
                    // If the class isn't a ResourceBundle subclass, throw a
                    // ClassCastException.
                    if (ResourceBundle.class.isAssignableFrom(bundleClass)) {
                        bundle = bundleClass.newInstance();
                    } else {
                        throw new ClassCastException(bundleClass.getName()
                                     + " cannot be cast to ResourceBundle");
                    }
                } catch (ClassNotFoundException e) {
                }
            } else if (format.equals("java.properties")) {
                final String resourceName = toResourceName0(bundleName, "properties");
                if (resourceName == null) {
                    return bundle;
                }
                final ClassLoader classLoader = loader;
                final boolean reloadFlag = reload;
                InputStream stream = null;
                try {
                    stream = AccessController.doPrivileged(
                        new PrivilegedExceptionAction<InputStream>() {
                            public InputStream run() throws IOException {
                                InputStream is = null;
                                if (reloadFlag) {
                                    URL url = classLoader.getResource(resourceName);
                                    if (url != null) {
                                        URLConnection connection = url.openConnection();
                                        if (connection != null) {
                                            // Disable caches to get fresh data for
                                            // reloading.
                                            connection.setUseCaches(false);
                                            is = connection.getInputStream();
                                        }
                                    }
                                } else {
                                    is = classLoader.getResourceAsStream(resourceName);
                                }
                                return is;
                            }
                        });
                } catch (PrivilegedActionException e) {
                    throw (IOException) e.getException();
                }
                if (stream != null) {
                    try {
                        bundle = new PropertyResourceBundle(stream);
                    } finally {
                        stream.close();
                    }
                }
            } else {
                throw new IllegalArgumentException("unknown format: " + format);
            }
            return bundle;
        }
        public long getTimeToLive(String baseName, Locale locale) {
            if (baseName == null || locale == null) {
                throw new NullPointerException();
            }
            return TTL_NO_EXPIRATION_CONTROL;
        }
        public boolean needsReload(String baseName, Locale locale,
                                   String format, ClassLoader loader,
                                   ResourceBundle bundle, long loadTime) {
            if (bundle == null) {
                throw new NullPointerException();
            }
            if (format.equals("java.class") || format.equals("java.properties")) {
                format = format.substring(5);
            }
            boolean result = false;
            try {
                String resourceName = toResourceName0(toBundleName(baseName, locale), format);
                if (resourceName == null) {
                    return result;
                }
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    long lastModified = 0;
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        // disable caches to get the correct data
                        connection.setUseCaches(false);
                        if (connection instanceof JarURLConnection) {
                            JarEntry ent = ((JarURLConnection)connection).getJarEntry();
                            if (ent != null) {
                                lastModified = ent.getTime();
                                if (lastModified == -1) {
                                    lastModified = 0;
                                }
                            }
                        } else {
                            lastModified = connection.getLastModified();
                        }
                    }
                    result = lastModified >= loadTime;
                }
            } catch (NullPointerException npe) {
                throw npe;
            } catch (Exception e) {
                // ignore other exceptions
            }
            return result;
        }
        public String toBundleName(String baseName, Locale locale) {
            if (locale == Locale.ROOT) {
                return baseName;
            }
            String language = locale.getLanguage();
            String script = locale.getScript();
            String country = locale.getCountry();
            String variant = locale.getVariant();
            if (language == "" && country == "" && variant == "") {
                return baseName;
            }
            StringBuilder sb = new StringBuilder(baseName);
            sb.append('_');
            if (script != "") {
                if (variant != "") {
                    sb.append(language).append('_').append(script).append('_').append(country).append('_').append(variant);
                } else if (country != "") {
                    sb.append(language).append('_').append(script).append('_').append(country);
                } else {
                    sb.append(language).append('_').append(script);
                }
            } else {
                if (variant != "") {
                    sb.append(language).append('_').append(country).append('_').append(variant);
                } else if (country != "") {
                    sb.append(language).append('_').append(country);
                } else {
                    sb.append(language);
                }
            }
            return sb.toString();
        }
        public final String toResourceName(String bundleName, String suffix) {
            StringBuilder sb = new StringBuilder(bundleName.length() + 1 + suffix.length());
            sb.append(bundleName.replace('.', '/')).append('.').append(suffix);
            return sb.toString();
        }
        private String toResourceName0(String bundleName, String suffix) {
            // application protocol check
            if (bundleName.contains("://")) {
                return null;
            } else {
                return toResourceName(bundleName, suffix);
            }
        }
    }
    private static class SingleFormatControl extends Control {
        private static final Control PROPERTIES_ONLY
            = new SingleFormatControl(FORMAT_PROPERTIES);
        private static final Control CLASS_ONLY
            = new SingleFormatControl(FORMAT_CLASS);
        private final List<String> formats;
        protected SingleFormatControl(List<String> formats) {
            this.formats = formats;
        }
        public List<String> getFormats(String baseName) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            return formats;
        }
    }
    private static final class NoFallbackControl extends SingleFormatControl {
        private static final Control NO_FALLBACK
            = new NoFallbackControl(FORMAT_DEFAULT);
        private static final Control PROPERTIES_ONLY_NO_FALLBACK
            = new NoFallbackControl(FORMAT_PROPERTIES);
        private static final Control CLASS_ONLY_NO_FALLBACK
            = new NoFallbackControl(FORMAT_CLASS);
        protected NoFallbackControl(List<String> formats) {
            super(formats);
        }
        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName == null || locale == null) {
                throw new NullPointerException();
            }
            return null;
        }
    }
}
