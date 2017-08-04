



package java.text;

import java.lang.ref.SoftReference;
import java.text.spi.CollatorProvider;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleServiceProviderPool;




public abstract class Collator
    implements java.util.Comparator<Object>, Cloneable
{

    public final static int PRIMARY = 0;

    public final static int SECONDARY = 1;

    public final static int TERTIARY = 2;


    public final static int IDENTICAL = 3;


    public final static int NO_DECOMPOSITION = 0;


    public final static int CANONICAL_DECOMPOSITION = 1;


    public final static int FULL_DECOMPOSITION = 2;


    public static synchronized Collator getInstance() {
        return getInstance(Locale.getDefault());
    }


    public static Collator getInstance(Locale desiredLocale) {
        SoftReference<Collator> ref = cache.get(desiredLocale);
        Collator result = (ref != null) ? ref.get() : null;
        if (result == null) {
            LocaleProviderAdapter adapter;
            adapter = LocaleProviderAdapter.getAdapter(CollatorProvider.class,
                                                       desiredLocale);
            CollatorProvider provider = adapter.getCollatorProvider();
            result = provider.getInstance(desiredLocale);
            if (result == null) {
                result = LocaleProviderAdapter.forJRE()
                             .getCollatorProvider().getInstance(desiredLocale);
            }
            while (true) {
                if (ref != null) {
                    // Remove the empty SoftReference if any
                    cache.remove(desiredLocale, ref);
                }
                ref = cache.putIfAbsent(desiredLocale, new SoftReference<>(result));
                if (ref == null) {
                    break;
                }
                Collator cachedColl = ref.get();
                if (cachedColl != null) {
                    result = cachedColl;
                    break;
                }
            }
        }
        return (Collator) result.clone(); // make the world safe
    }


    public abstract int compare(String source, String target);


    @Override
    public int compare(Object o1, Object o2) {
    return compare((String)o1, (String)o2);
    }


    public abstract CollationKey getCollationKey(String source);


    public boolean equals(String source, String target)
    {
        return (compare(source, target) == Collator.EQUAL);
    }


    public synchronized int getStrength()
    {
        return strength;
    }


    public synchronized void setStrength(int newStrength) {
        if ((newStrength != PRIMARY) &&
            (newStrength != SECONDARY) &&
            (newStrength != TERTIARY) &&
            (newStrength != IDENTICAL)) {
            throw new IllegalArgumentException("Incorrect comparison level.");
        }
        strength = newStrength;
    }


    public synchronized int getDecomposition()
    {
        return decmp;
    }

    public synchronized void setDecomposition(int decompositionMode) {
        if ((decompositionMode != NO_DECOMPOSITION) &&
            (decompositionMode != CANONICAL_DECOMPOSITION) &&
            (decompositionMode != FULL_DECOMPOSITION)) {
            throw new IllegalArgumentException("Wrong decomposition mode.");
        }
        decmp = decompositionMode;
    }


    public static synchronized Locale[] getAvailableLocales() {
        LocaleServiceProviderPool pool =
            LocaleServiceProviderPool.getPool(CollatorProvider.class);
        return pool.getAvailableLocales();
    }


    @Override
    public Object clone()
    {
        try {
            return (Collator)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }


    @Override
    public boolean equals(Object that)
    {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Collator other = (Collator) that;
        return ((strength == other.strength) &&
                (decmp == other.decmp));
    }


    @Override
    abstract public int hashCode();


    protected Collator()
    {
        strength = TERTIARY;
        decmp = CANONICAL_DECOMPOSITION;
    }

    private int strength = 0;
    private int decmp = 0;
    private static final ConcurrentMap<Locale, SoftReference<Collator>> cache
            = new ConcurrentHashMap<>();

    //
    // FIXME: These three constants should be removed.
    //

    final static int LESS = -1;

    final static int EQUAL = 0;

    final static int GREATER = 1;
 }
