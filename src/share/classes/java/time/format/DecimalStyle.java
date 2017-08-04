package java.time.format;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
public final class DecimalStyle {
    public static final DecimalStyle STANDARD = new DecimalStyle('0', '+', '-', '.');
    private static final ConcurrentMap<Locale, DecimalStyle> CACHE = new ConcurrentHashMap<>(16, 0.75f, 2);
    private final char zeroDigit;
    private final char positiveSign;
    private final char negativeSign;
    private final char decimalSeparator;
    //-----------------------------------------------------------------------
    public static Set<Locale> getAvailableLocales() {
        Locale[] l = DecimalFormatSymbols.getAvailableLocales();
        Set<Locale> locales = new HashSet<>(l.length);
        Collections.addAll(locales, l);
        return locales;
    }
    public static DecimalStyle ofDefaultLocale() {
        return of(Locale.getDefault(Locale.Category.FORMAT));
    }
    public static DecimalStyle of(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        DecimalStyle info = CACHE.get(locale);
        if (info == null) {
            info = create(locale);
            CACHE.putIfAbsent(locale, info);
            info = CACHE.get(locale);
        }
        return info;
    }
    private static DecimalStyle create(Locale locale) {
        DecimalFormatSymbols oldSymbols = DecimalFormatSymbols.getInstance(locale);
        char zeroDigit = oldSymbols.getZeroDigit();
        char positiveSign = '+';
        char negativeSign = oldSymbols.getMinusSign();
        char decimalSeparator = oldSymbols.getDecimalSeparator();
        if (zeroDigit == '0' && negativeSign == '-' && decimalSeparator == '.') {
            return STANDARD;
        }
        return new DecimalStyle(zeroDigit, positiveSign, negativeSign, decimalSeparator);
    }
    //-----------------------------------------------------------------------
    private DecimalStyle(char zeroChar, char positiveSignChar, char negativeSignChar, char decimalPointChar) {
        this.zeroDigit = zeroChar;
        this.positiveSign = positiveSignChar;
        this.negativeSign = negativeSignChar;
        this.decimalSeparator = decimalPointChar;
    }
    //-----------------------------------------------------------------------
    public char getZeroDigit() {
        return zeroDigit;
    }
    public DecimalStyle withZeroDigit(char zeroDigit) {
        if (zeroDigit == this.zeroDigit) {
            return this;
        }
        return new DecimalStyle(zeroDigit, positiveSign, negativeSign, decimalSeparator);
    }
    //-----------------------------------------------------------------------
    public char getPositiveSign() {
        return positiveSign;
    }
    public DecimalStyle withPositiveSign(char positiveSign) {
        if (positiveSign == this.positiveSign) {
            return this;
        }
        return new DecimalStyle(zeroDigit, positiveSign, negativeSign, decimalSeparator);
    }
    //-----------------------------------------------------------------------
    public char getNegativeSign() {
        return negativeSign;
    }
    public DecimalStyle withNegativeSign(char negativeSign) {
        if (negativeSign == this.negativeSign) {
            return this;
        }
        return new DecimalStyle(zeroDigit, positiveSign, negativeSign, decimalSeparator);
    }
    //-----------------------------------------------------------------------
    public char getDecimalSeparator() {
        return decimalSeparator;
    }
    public DecimalStyle withDecimalSeparator(char decimalSeparator) {
        if (decimalSeparator == this.decimalSeparator) {
            return this;
        }
        return new DecimalStyle(zeroDigit, positiveSign, negativeSign, decimalSeparator);
    }
    //-----------------------------------------------------------------------
    int convertToDigit(char ch) {
        int val = ch - zeroDigit;
        return (val >= 0 && val <= 9) ? val : -1;
    }
    String convertNumberToI18N(String numericText) {
        if (zeroDigit == '0') {
            return numericText;
        }
        int diff = zeroDigit - '0';
        char[] array = numericText.toCharArray();
        for (int i = 0; i < array.length; i++) {
            array[i] = (char) (array[i] + diff);
        }
        return new String(array);
    }
    //-----------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DecimalStyle) {
            DecimalStyle other = (DecimalStyle) obj;
            return (zeroDigit == other.zeroDigit && positiveSign == other.positiveSign &&
                    negativeSign == other.negativeSign && decimalSeparator == other.decimalSeparator);
        }
        return false;
    }
    @Override
    public int hashCode() {
        return zeroDigit + positiveSign + negativeSign + decimalSeparator;
    }
    //-----------------------------------------------------------------------
    @Override
    public String toString() {
        return "DecimalStyle[" + zeroDigit + positiveSign + negativeSign + decimalSeparator + "]";
    }
}
