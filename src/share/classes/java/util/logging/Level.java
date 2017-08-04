package java.util.logging;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
public class Level implements java.io.Serializable {
    private static final String defaultBundle = "sun.util.logging.resources.logging";
    private final String name;
    private final int value;
    private final String resourceBundleName;
    // localized level name
    private transient String localizedLevelName;
    private transient Locale cachedLocale;
    public static final Level OFF = new Level("OFF",Integer.MAX_VALUE, defaultBundle);
    public static final Level SEVERE = new Level("SEVERE",1000, defaultBundle);
    public static final Level WARNING = new Level("WARNING", 900, defaultBundle);
    public static final Level INFO = new Level("INFO", 800, defaultBundle);
    public static final Level CONFIG = new Level("CONFIG", 700, defaultBundle);
    public static final Level FINE = new Level("FINE", 500, defaultBundle);
    public static final Level FINER = new Level("FINER", 400, defaultBundle);
    public static final Level FINEST = new Level("FINEST", 300, defaultBundle);
    public static final Level ALL = new Level("ALL", Integer.MIN_VALUE, defaultBundle);
    protected Level(String name, int value) {
        this(name, value, null);
    }
    protected Level(String name, int value, String resourceBundleName) {
        this(name, value, resourceBundleName, true);
    }
    // private constructor to specify whether this instance should be added
    // to the KnownLevel list from which Level.parse method does its look up
    private Level(String name, int value, String resourceBundleName, boolean visible) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.value = value;
        this.resourceBundleName = resourceBundleName;
        this.localizedLevelName = resourceBundleName == null ? name : null;
        this.cachedLocale = null;
        if (visible) {
            KnownLevel.add(this);
        }
    }
    public String getResourceBundleName() {
        return resourceBundleName;
    }
    public String getName() {
        return name;
    }
    public String getLocalizedName() {
        return getLocalizedLevelName();
    }
    // package-private getLevelName() is used by the implementation
    // instead of getName() to avoid calling the subclass's version
    final String getLevelName() {
        return this.name;
    }
    private String computeLocalizedLevelName(Locale newLocale) {
        ResourceBundle rb = ResourceBundle.getBundle(resourceBundleName, newLocale);
        final String localizedName = rb.getString(name);
        final boolean isDefaultBundle = defaultBundle.equals(resourceBundleName);
        if (!isDefaultBundle) return localizedName;
        // This is a trick to determine whether the name has been translated
        // or not. If it has not been translated, we need to use Locale.ROOT
        // when calling toUpperCase().
        final Locale rbLocale = rb.getLocale();
        final Locale locale =
                Locale.ROOT.equals(rbLocale)
                || name.equals(localizedName.toUpperCase(Locale.ROOT))
                ? Locale.ROOT : rbLocale;
        // ALL CAPS in a resource bundle's message indicates no translation
        // needed per Oracle translation guideline.  To workaround this
        // in Oracle JDK implementation, convert the localized level name
        // to uppercase for compatibility reason.
        return Locale.ROOT.equals(locale) ? name : localizedName.toUpperCase(locale);
    }
    // Avoid looking up the localizedLevelName twice if we already
    // have it.
    final String getCachedLocalizedLevelName() {
        if (localizedLevelName != null) {
            if (cachedLocale != null) {
                if (cachedLocale.equals(Locale.getDefault())) {
                    // OK: our cached value was looked up with the same
                    //     locale. We can use it.
                    return localizedLevelName;
                }
            }
        }
        if (resourceBundleName == null) {
            // No resource bundle: just use the name.
            return name;
        }
        // We need to compute the localized name.
        // Either because it's the first time, or because our cached
        // value is for a different locale. Just return null.
        return null;
    }
    final synchronized String getLocalizedLevelName() {
        // See if we have a cached localized name
        final String cachedLocalizedName = getCachedLocalizedLevelName();
        if (cachedLocalizedName != null) {
            return cachedLocalizedName;
        }
        // No cached localized name or cache invalid.
        // Need to compute the localized name.
        final Locale newLocale = Locale.getDefault();
        try {
            localizedLevelName = computeLocalizedLevelName(newLocale);
        } catch (Exception ex) {
            localizedLevelName = name;
        }
        cachedLocale = newLocale;
        return localizedLevelName;
    }
    // Returns a mirrored Level object that matches the given name as
    // specified in the Level.parse method.  Returns null if not found.
    //
    // It returns the same Level object as the one returned by Level.parse
    // method if the given name is a non-localized name or integer.
    //
    // If the name is a localized name, findLevel and parse method may
    // return a different level value if there is a custom Level subclass
    // that overrides Level.getLocalizedName() to return a different string
    // than what's returned by the default implementation.
    //
    static Level findLevel(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        KnownLevel level;
        // Look for a known Level with the given non-localized name.
        level = KnownLevel.findByName(name);
        if (level != null) {
            return level.mirroredLevel;
        }
        // Now, check if the given name is an integer.  If so,
        // first look for a Level with the given value and then
        // if necessary create one.
        try {
            int x = Integer.parseInt(name);
            level = KnownLevel.findByValue(x);
            if (level == null) {
                // add new Level
                Level levelObject = new Level(name, x);
                level = KnownLevel.findByValue(x);
            }
            return level.mirroredLevel;
        } catch (NumberFormatException ex) {
            // Not an integer.
            // Drop through.
        }
        level = KnownLevel.findByLocalizedLevelName(name);
        if (level != null) {
            return level.mirroredLevel;
        }
        return null;
    }
    @Override
    public final String toString() {
        return name;
    }
    public final int intValue() {
        return value;
    }
    private static final long serialVersionUID = -8176160795706313070L;
    // Serialization magic to prevent "doppelgangers".
    // This is a performance optimization.
    private Object readResolve() {
        KnownLevel o = KnownLevel.matches(this);
        if (o != null) {
            return o.levelObject;
        }
        // Woops.  Whoever sent us this object knows
        // about a new log level.  Add it to our list.
        Level level = new Level(this.name, this.value, this.resourceBundleName);
        return level;
    }
    public static synchronized Level parse(String name) throws IllegalArgumentException {
        // Check that name is not null.
        name.length();
        KnownLevel level;
        // Look for a known Level with the given non-localized name.
        level = KnownLevel.findByName(name);
        if (level != null) {
            return level.levelObject;
        }
        // Now, check if the given name is an integer.  If so,
        // first look for a Level with the given value and then
        // if necessary create one.
        try {
            int x = Integer.parseInt(name);
            level = KnownLevel.findByValue(x);
            if (level == null) {
                // add new Level
                Level levelObject = new Level(name, x);
                level = KnownLevel.findByValue(x);
            }
            return level.levelObject;
        } catch (NumberFormatException ex) {
            // Not an integer.
            // Drop through.
        }
        // Finally, look for a known level with the given localized name,
        // in the current default locale.
        // This is relatively expensive, but not excessively so.
        level = KnownLevel.findByLocalizedLevelName(name);
        if (level != null) {
            return level.levelObject;
        }
        // OK, we've tried everything and failed
        throw new IllegalArgumentException("Bad level \"" + name + "\"");
    }
    @Override
    public boolean equals(Object ox) {
        try {
            Level lx = (Level)ox;
            return (lx.value == this.value);
        } catch (Exception ex) {
            return false;
        }
    }
    @Override
    public int hashCode() {
        return this.value;
    }
    // KnownLevel class maintains the global list of all known levels.
    // The API allows multiple custom Level instances of the same name/value
    // be created. This class provides convenient methods to find a level
    // by a given name, by a given value, or by a given localized name.
    //
    // KnownLevel wraps the following Level objects:
    // 1. levelObject:   standard Level object or custom Level object
    // 2. mirroredLevel: Level object representing the level specified in the
    //                   logging configuration.
    //
    // Level.getName, Level.getLocalizedName, Level.getResourceBundleName methods
    // are non-final but the name and resource bundle name are parameters to
    // the Level constructor.  Use the mirroredLevel object instead of the
    // levelObject to prevent the logging framework to execute foreign code
    // implemented by untrusted Level subclass.
    //
    // Implementation Notes:
    // If Level.getName, Level.getLocalizedName, Level.getResourceBundleName methods
    // were final, the following KnownLevel implementation can be removed.
    // Future API change should take this into consideration.
    static final class KnownLevel {
        private static Map<String, List<KnownLevel>> nameToLevels = new HashMap<>();
        private static Map<Integer, List<KnownLevel>> intToLevels = new HashMap<>();
        final Level levelObject;     // instance of Level class or Level subclass
        final Level mirroredLevel;   // mirror of the custom Level
        KnownLevel(Level l) {
            this.levelObject = l;
            if (l.getClass() == Level.class) {
                this.mirroredLevel = l;
            } else {
                // this mirrored level object is hidden
                this.mirroredLevel = new Level(l.name, l.value, l.resourceBundleName, false);
            }
        }
        static synchronized void add(Level l) {
            // the mirroredLevel object is always added to the list
            // before the custom Level instance
            KnownLevel o = new KnownLevel(l);
            List<KnownLevel> list = nameToLevels.get(l.name);
            if (list == null) {
                list = new ArrayList<>();
                nameToLevels.put(l.name, list);
            }
            list.add(o);
            list = intToLevels.get(l.value);
            if (list == null) {
                list = new ArrayList<>();
                intToLevels.put(l.value, list);
            }
            list.add(o);
        }
        // Returns a KnownLevel with the given non-localized name.
        static synchronized KnownLevel findByName(String name) {
            List<KnownLevel> list = nameToLevels.get(name);
            if (list != null) {
                return list.get(0);
            }
            return null;
        }
        // Returns a KnownLevel with the given value.
        static synchronized KnownLevel findByValue(int value) {
            List<KnownLevel> list = intToLevels.get(value);
            if (list != null) {
                return list.get(0);
            }
            return null;
        }
        // Returns a KnownLevel with the given localized name matching
        // by calling the Level.getLocalizedLevelName() method (i.e. found
        // from the resourceBundle associated with the Level object).
        // This method does not call Level.getLocalizedName() that may
        // be overridden in a subclass implementation
        static synchronized KnownLevel findByLocalizedLevelName(String name) {
            for (List<KnownLevel> levels : nameToLevels.values()) {
                for (KnownLevel l : levels) {
                    String lname = l.levelObject.getLocalizedLevelName();
                    if (name.equals(lname)) {
                        return l;
                    }
                }
            }
            return null;
        }
        static synchronized KnownLevel matches(Level l) {
            List<KnownLevel> list = nameToLevels.get(l.name);
            if (list != null) {
                for (KnownLevel level : list) {
                    Level other = level.mirroredLevel;
                    Class<? extends Level> type = level.levelObject.getClass();
                    if (l.value == other.value &&
                           (l.resourceBundleName == other.resourceBundleName ||
                               (l.resourceBundleName != null &&
                                l.resourceBundleName.equals(other.resourceBundleName)))) {
                        if (type == l.getClass()) {
                            return level;
                        }
                    }
                }
            }
            return null;
        }
    }
}
