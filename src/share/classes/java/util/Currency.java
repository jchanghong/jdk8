

package java.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.spi.CurrencyNameProvider;
import sun.util.locale.provider.LocaleServiceProviderPool;
import sun.util.logging.PlatformLogger;



public final class Currency implements Serializable {

    private static final long serialVersionUID = -158308464356906721L;


    private final String currencyCode;


    transient private final int defaultFractionDigits;


    transient private final int numericCode;


    // class data: instance map

    private static ConcurrentMap<String, Currency> instances = new ConcurrentHashMap<>(7);
    private static HashSet<Currency> available;

    // Class data: currency data obtained from currency.data file.
    // Purpose:
    // - determine valid country codes
    // - determine valid currency codes
    // - map country codes to currency codes
    // - obtain default fraction digits for currency codes
    //
    // sc = special case; dfd = default fraction digits
    // Simple countries are those where the country code is a prefix of the
    // currency code, and there are no known plans to change the currency.
    //
    // table formats:
    // - mainTable:
    //   - maps country code to 32-bit int
    //   - 26*26 entries, corresponding to [A-Z]*[A-Z]
    //   - \u007F -> not valid country
    //   - bits 20-31: unused
    //   - bits 10-19: numeric code (0 to 1023)
    //   - bit 9: 1 - special case, bits 0-4 indicate which one
    //            0 - simple country, bits 0-4 indicate final char of currency code
    //   - bits 5-8: fraction digits for simple countries, 0 for special cases
    //   - bits 0-4: final char for currency code for simple country, or ID of special case
    // - special case IDs:
    //   - 0: country has no currency
    //   - other: index into sc* arrays + 1
    // - scCutOverTimes: cut-over time in millis as returned by
    //   System.currentTimeMillis for special case countries that are changing
    //   currencies; Long.MAX_VALUE for countries that are not changing currencies
    // - scOldCurrencies: old currencies for special case countries
    // - scNewCurrencies: new currencies for special case countries that are
    //   changing currencies; null for others
    // - scOldCurrenciesDFD: default fraction digits for old currencies
    // - scNewCurrenciesDFD: default fraction digits for new currencies, 0 for
    //   countries that are not changing currencies
    // - otherCurrencies: concatenation of all currency codes that are not the
    //   main currency of a simple country, separated by "-"
    // - otherCurrenciesDFD: decimal format digits for currencies in otherCurrencies, same order

    static int formatVersion;
    static int dataVersion;
    static int[] mainTable;
    static long[] scCutOverTimes;
    static String[] scOldCurrencies;
    static String[] scNewCurrencies;
    static int[] scOldCurrenciesDFD;
    static int[] scNewCurrenciesDFD;
    static int[] scOldCurrenciesNumericCode;
    static int[] scNewCurrenciesNumericCode;
    static String otherCurrencies;
    static int[] otherCurrenciesDFD;
    static int[] otherCurrenciesNumericCode;

    // handy constants - must match definitions in GenerateCurrencyData
    // magic number
    private static final int MAGIC_NUMBER = 0x43757244;
    // number of characters from A to Z
    private static final int A_TO_Z = ('Z' - 'A') + 1;
    // entry for invalid country codes
    private static final int INVALID_COUNTRY_ENTRY = 0x0000007F;
    // entry for countries without currency
    private static final int COUNTRY_WITHOUT_CURRENCY_ENTRY = 0x00000200;
    // mask for simple case country entries
    private static final int SIMPLE_CASE_COUNTRY_MASK = 0x00000000;
    // mask for simple case country entry final character
    private static final int SIMPLE_CASE_COUNTRY_FINAL_CHAR_MASK = 0x0000001F;
    // mask for simple case country entry default currency digits
    private static final int SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_MASK = 0x000001E0;
    // shift count for simple case country entry default currency digits
    private static final int SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT = 5;
    // maximum number for simple case country entry default currency digits
    private static final int SIMPLE_CASE_COUNTRY_MAX_DEFAULT_DIGITS = 9;
    // mask for special case country entries
    private static final int SPECIAL_CASE_COUNTRY_MASK = 0x00000200;
    // mask for special case country index
    private static final int SPECIAL_CASE_COUNTRY_INDEX_MASK = 0x0000001F;
    // delta from entry index component in main table to index into special case tables
    private static final int SPECIAL_CASE_COUNTRY_INDEX_DELTA = 1;
    // mask for distinguishing simple and special case countries
    private static final int COUNTRY_TYPE_MASK = SIMPLE_CASE_COUNTRY_MASK | SPECIAL_CASE_COUNTRY_MASK;
    // mask for the numeric code of the currency
    private static final int NUMERIC_CODE_MASK = 0x000FFC00;
    // shift count for the numeric code of the currency
    private static final int NUMERIC_CODE_SHIFT = 10;

    // Currency data format version
    private static final int VALID_FORMAT_VERSION = 2;

    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                String homeDir = System.getProperty("java.home");
                try {
                    String dataFile = homeDir + File.separator +
                            "lib" + File.separator + "currency.data";
                    try (DataInputStream dis = new DataInputStream(
                             new BufferedInputStream(
                             new FileInputStream(dataFile)))) {
                        if (dis.readInt() != MAGIC_NUMBER) {
                            throw new InternalError("Currency data is possibly corrupted");
                        }
                        formatVersion = dis.readInt();
                        if (formatVersion != VALID_FORMAT_VERSION) {
                            throw new InternalError("Currency data format is incorrect");
                        }
                        dataVersion = dis.readInt();
                        mainTable = readIntArray(dis, A_TO_Z * A_TO_Z);
                        int scCount = dis.readInt();
                        scCutOverTimes = readLongArray(dis, scCount);
                        scOldCurrencies = readStringArray(dis, scCount);
                        scNewCurrencies = readStringArray(dis, scCount);
                        scOldCurrenciesDFD = readIntArray(dis, scCount);
                        scNewCurrenciesDFD = readIntArray(dis, scCount);
                        scOldCurrenciesNumericCode = readIntArray(dis, scCount);
                        scNewCurrenciesNumericCode = readIntArray(dis, scCount);
                        int ocCount = dis.readInt();
                        otherCurrencies = dis.readUTF();
                        otherCurrenciesDFD = readIntArray(dis, ocCount);
                        otherCurrenciesNumericCode = readIntArray(dis, ocCount);
                    }
                } catch (IOException e) {
                    throw new InternalError(e);
                }

                // look for the properties file for overrides
                String propsFile = System.getProperty("java.util.currency.data");
                if (propsFile == null) {
                    propsFile = homeDir + File.separator + "lib" +
                        File.separator + "currency.properties";
                }
                try {
                    File propFile = new File(propsFile);
                    if (propFile.exists()) {
                        Properties props = new Properties();
                        try (FileReader fr = new FileReader(propFile)) {
                            props.load(fr);
                        }
                        Set<String> keys = props.stringPropertyNames();
                        Pattern propertiesPattern =
                            Pattern.compile("([A-Z]{3})\\s*,\\s*(\\d{3})\\s*,\\s*" +
                                "(\\d+)\\s*,?\\s*(\\d{4}-\\d{2}-\\d{2}T\\d{2}:" +
                                "\\d{2}:\\d{2})?");
                        for (String key : keys) {
                           replaceCurrencyData(propertiesPattern,
                               key.toUpperCase(Locale.ROOT),
                               props.getProperty(key).toUpperCase(Locale.ROOT));
                        }
                    }
                } catch (IOException e) {
                    info("currency.properties is ignored because of an IOException", e);
                }
                return null;
            }
        });
    }


    private static final int SYMBOL = 0;
    private static final int DISPLAYNAME = 1;



    private Currency(String currencyCode, int defaultFractionDigits, int numericCode) {
        this.currencyCode = currencyCode;
        this.defaultFractionDigits = defaultFractionDigits;
        this.numericCode = numericCode;
    }


    public static Currency getInstance(String currencyCode) {
        return getInstance(currencyCode, Integer.MIN_VALUE, 0);
    }

    private static Currency getInstance(String currencyCode, int defaultFractionDigits,
        int numericCode) {
        // Try to look up the currency code in the instances table.
        // This does the null pointer check as a side effect.
        // Also, if there already is an entry, the currencyCode must be valid.
        Currency instance = instances.get(currencyCode);
        if (instance != null) {
            return instance;
        }

        if (defaultFractionDigits == Integer.MIN_VALUE) {
            // Currency code not internally generated, need to verify first
            // A currency code must have 3 characters and exist in the main table
            // or in the list of other currencies.
            if (currencyCode.length() != 3) {
                throw new IllegalArgumentException();
            }
            char char1 = currencyCode.charAt(0);
            char char2 = currencyCode.charAt(1);
            int tableEntry = getMainTableEntry(char1, char2);
            if ((tableEntry & COUNTRY_TYPE_MASK) == SIMPLE_CASE_COUNTRY_MASK
                    && tableEntry != INVALID_COUNTRY_ENTRY
                    && currencyCode.charAt(2) - 'A' == (tableEntry & SIMPLE_CASE_COUNTRY_FINAL_CHAR_MASK)) {
                defaultFractionDigits = (tableEntry & SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_MASK) >> SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT;
                numericCode = (tableEntry & NUMERIC_CODE_MASK) >> NUMERIC_CODE_SHIFT;
            } else {
                // Check for '-' separately so we don't get false hits in the table.
                if (currencyCode.charAt(2) == '-') {
                    throw new IllegalArgumentException();
                }
                int index = otherCurrencies.indexOf(currencyCode);
                if (index == -1) {
                    throw new IllegalArgumentException();
                }
                defaultFractionDigits = otherCurrenciesDFD[index / 4];
                numericCode = otherCurrenciesNumericCode[index / 4];
            }
        }

        Currency currencyVal =
            new Currency(currencyCode, defaultFractionDigits, numericCode);
        instance = instances.putIfAbsent(currencyCode, currencyVal);
        return (instance != null ? instance : currencyVal);
    }


    public static Currency getInstance(Locale locale) {
        String country = locale.getCountry();
        if (country == null) {
            throw new NullPointerException();
        }

        if (country.length() != 2) {
            throw new IllegalArgumentException();
        }

        char char1 = country.charAt(0);
        char char2 = country.charAt(1);
        int tableEntry = getMainTableEntry(char1, char2);
        if ((tableEntry & COUNTRY_TYPE_MASK) == SIMPLE_CASE_COUNTRY_MASK
                    && tableEntry != INVALID_COUNTRY_ENTRY) {
            char finalChar = (char) ((tableEntry & SIMPLE_CASE_COUNTRY_FINAL_CHAR_MASK) + 'A');
            int defaultFractionDigits = (tableEntry & SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_MASK) >> SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT;
            int numericCode = (tableEntry & NUMERIC_CODE_MASK) >> NUMERIC_CODE_SHIFT;
            StringBuilder sb = new StringBuilder(country);
            sb.append(finalChar);
            return getInstance(sb.toString(), defaultFractionDigits, numericCode);
        } else {
            // special cases
            if (tableEntry == INVALID_COUNTRY_ENTRY) {
                throw new IllegalArgumentException();
            }
            if (tableEntry == COUNTRY_WITHOUT_CURRENCY_ENTRY) {
                return null;
            } else {
                int index = (tableEntry & SPECIAL_CASE_COUNTRY_INDEX_MASK) - SPECIAL_CASE_COUNTRY_INDEX_DELTA;
                if (scCutOverTimes[index] == Long.MAX_VALUE || System.currentTimeMillis() < scCutOverTimes[index]) {
                    return getInstance(scOldCurrencies[index], scOldCurrenciesDFD[index],
                        scOldCurrenciesNumericCode[index]);
                } else {
                    return getInstance(scNewCurrencies[index], scNewCurrenciesDFD[index],
                        scNewCurrenciesNumericCode[index]);
                }
            }
        }
    }


    public static Set<Currency> getAvailableCurrencies() {
        synchronized(Currency.class) {
            if (available == null) {
                available = new HashSet<>(256);

                // Add simple currencies first
                for (char c1 = 'A'; c1 <= 'Z'; c1 ++) {
                    for (char c2 = 'A'; c2 <= 'Z'; c2 ++) {
                        int tableEntry = getMainTableEntry(c1, c2);
                        if ((tableEntry & COUNTRY_TYPE_MASK) == SIMPLE_CASE_COUNTRY_MASK
                             && tableEntry != INVALID_COUNTRY_ENTRY) {
                            char finalChar = (char) ((tableEntry & SIMPLE_CASE_COUNTRY_FINAL_CHAR_MASK) + 'A');
                            int defaultFractionDigits = (tableEntry & SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_MASK) >> SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT;
                            int numericCode = (tableEntry & NUMERIC_CODE_MASK) >> NUMERIC_CODE_SHIFT;
                            StringBuilder sb = new StringBuilder();
                            sb.append(c1);
                            sb.append(c2);
                            sb.append(finalChar);
                            available.add(getInstance(sb.toString(), defaultFractionDigits, numericCode));
                        }
                    }
                }

                // Now add other currencies
                StringTokenizer st = new StringTokenizer(otherCurrencies, "-");
                while (st.hasMoreElements()) {
                    available.add(getInstance((String)st.nextElement()));
                }
            }
        }

        @SuppressWarnings("unchecked")
        Set<Currency> result = (Set<Currency>) available.clone();
        return result;
    }


    public String getCurrencyCode() {
        return currencyCode;
    }


    public String getSymbol() {
        return getSymbol(Locale.getDefault(Locale.Category.DISPLAY));
    }


    public String getSymbol(Locale locale) {
        LocaleServiceProviderPool pool =
            LocaleServiceProviderPool.getPool(CurrencyNameProvider.class);
        String symbol = pool.getLocalizedObject(
                                CurrencyNameGetter.INSTANCE,
                                locale, currencyCode, SYMBOL);
        if (symbol != null) {
            return symbol;
        }

        // use currency code as symbol of last resort
        return currencyCode;
    }


    public int getDefaultFractionDigits() {
        return defaultFractionDigits;
    }


    public int getNumericCode() {
        return numericCode;
    }


    public String getDisplayName() {
        return getDisplayName(Locale.getDefault(Locale.Category.DISPLAY));
    }


    public String getDisplayName(Locale locale) {
        LocaleServiceProviderPool pool =
            LocaleServiceProviderPool.getPool(CurrencyNameProvider.class);
        String result = pool.getLocalizedObject(
                                CurrencyNameGetter.INSTANCE,
                                locale, currencyCode, DISPLAYNAME);
        if (result != null) {
            return result;
        }

        // use currency code as symbol of last resort
        return currencyCode;
    }


    @Override
    public String toString() {
        return currencyCode;
    }


    private Object readResolve() {
        return getInstance(currencyCode);
    }


    private static int getMainTableEntry(char char1, char char2) {
        if (char1 < 'A' || char1 > 'Z' || char2 < 'A' || char2 > 'Z') {
            throw new IllegalArgumentException();
        }
        return mainTable[(char1 - 'A') * A_TO_Z + (char2 - 'A')];
    }


    private static void setMainTableEntry(char char1, char char2, int entry) {
        if (char1 < 'A' || char1 > 'Z' || char2 < 'A' || char2 > 'Z') {
            throw new IllegalArgumentException();
        }
        mainTable[(char1 - 'A') * A_TO_Z + (char2 - 'A')] = entry;
    }


    private static class CurrencyNameGetter
        implements LocaleServiceProviderPool.LocalizedObjectGetter<CurrencyNameProvider,
                                                                   String> {
        private static final CurrencyNameGetter INSTANCE = new CurrencyNameGetter();

        @Override
        public String getObject(CurrencyNameProvider currencyNameProvider,
                                Locale locale,
                                String key,
                                Object... params) {
            assert params.length == 1;
            int type = (Integer)params[0];

            switch(type) {
            case SYMBOL:
                return currencyNameProvider.getSymbol(key, locale);
            case DISPLAYNAME:
                return currencyNameProvider.getDisplayName(key, locale);
            default:
                assert false; // shouldn't happen
            }

            return null;
        }
    }

    private static int[] readIntArray(DataInputStream dis, int count) throws IOException {
        int[] ret = new int[count];
        for (int i = 0; i < count; i++) {
            ret[i] = dis.readInt();
        }

        return ret;
    }

    private static long[] readLongArray(DataInputStream dis, int count) throws IOException {
        long[] ret = new long[count];
        for (int i = 0; i < count; i++) {
            ret[i] = dis.readLong();
        }

        return ret;
    }

    private static String[] readStringArray(DataInputStream dis, int count) throws IOException {
        String[] ret = new String[count];
        for (int i = 0; i < count; i++) {
            ret[i] = dis.readUTF();
        }

        return ret;
    }


    private static void replaceCurrencyData(Pattern pattern, String ctry, String curdata) {

        if (ctry.length() != 2) {
            // ignore invalid country code
            info("currency.properties entry for " + ctry +
                    " is ignored because of the invalid country code.", null);
            return;
        }

        Matcher m = pattern.matcher(curdata);
        if (!m.find() || (m.group(4) == null && countOccurrences(curdata, ',') >= 3)) {
            // format is not recognized.  ignore the data
            // if group(4) date string is null and we've 4 values, bad date value
            info("currency.properties entry for " + ctry +
                    " ignored because the value format is not recognized.", null);
            return;
        }

        try {
            if (m.group(4) != null && !isPastCutoverDate(m.group(4))) {
                info("currency.properties entry for " + ctry +
                        " ignored since cutover date has not passed :" + curdata, null);
                return;
            }
        } catch (ParseException ex) {
            info("currency.properties entry for " + ctry +
                        " ignored since exception encountered :" + ex.getMessage(), null);
            return;
        }

        String code = m.group(1);
        int numeric = Integer.parseInt(m.group(2));
        int entry = numeric << NUMERIC_CODE_SHIFT;
        int fraction = Integer.parseInt(m.group(3));
        if (fraction > SIMPLE_CASE_COUNTRY_MAX_DEFAULT_DIGITS) {
            info("currency.properties entry for " + ctry +
                " ignored since the fraction is more than " +
                SIMPLE_CASE_COUNTRY_MAX_DEFAULT_DIGITS + ":" + curdata, null);
            return;
        }

        int index;
        for (index = 0; index < scOldCurrencies.length; index++) {
            if (scOldCurrencies[index].equals(code)) {
                break;
            }
        }

        if (index == scOldCurrencies.length) {
            // simple case
            entry |= (fraction << SIMPLE_CASE_COUNTRY_DEFAULT_DIGITS_SHIFT) |
                     (code.charAt(2) - 'A');
        } else {
            // special case
            entry |= SPECIAL_CASE_COUNTRY_MASK |
                     (index + SPECIAL_CASE_COUNTRY_INDEX_DELTA);
        }
        setMainTableEntry(ctry.charAt(0), ctry.charAt(1), entry);
    }

    private static boolean isPastCutoverDate(String s) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        format.setLenient(false);
        long time = format.parse(s.trim()).getTime();
        return System.currentTimeMillis() > time;

    }

    private static int countOccurrences(String value, char match) {
        int count = 0;
        for (char c : value.toCharArray()) {
            if (c == match) {
               ++count;
            }
        }
        return count;
    }

    private static void info(String message, Throwable t) {
        PlatformLogger logger = PlatformLogger.getLogger("java.util.Currency");
        if (logger.isLoggable(PlatformLogger.Level.INFO)) {
            if (t != null) {
                logger.info(message, t);
            } else {
                logger.info(message);
            }
        }
    }
}
