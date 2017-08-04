package java.time.chrono;
import static java.time.chrono.JapaneseDate.MEIJI_6_ISODATE;
import static java.time.temporal.ChronoField.ERA;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import sun.util.calendar.CalendarDate;
public final class JapaneseEra
        implements Era, Serializable {
    // The offset value to 0-based index from the era value.
    // i.e., getValue() + ERA_OFFSET == 0-based index
    static final int ERA_OFFSET = 2;
    static final sun.util.calendar.Era[] ERA_CONFIG;
    public static final JapaneseEra MEIJI = new JapaneseEra(-1, LocalDate.of(1868, 1, 1));
    public static final JapaneseEra TAISHO = new JapaneseEra(0, LocalDate.of(1912, 7, 30));
    public static final JapaneseEra SHOWA = new JapaneseEra(1, LocalDate.of(1926, 12, 25));
    public static final JapaneseEra HEISEI = new JapaneseEra(2, LocalDate.of(1989, 1, 8));
    // The number of predefined JapaneseEra constants.
    // There may be a supplemental era defined by the property.
    private static final int N_ERA_CONSTANTS = HEISEI.getValue() + ERA_OFFSET;
    private static final long serialVersionUID = 1466499369062886794L;
    // array for the singleton JapaneseEra instances
    private static final JapaneseEra[] KNOWN_ERAS;
    static {
        ERA_CONFIG = JapaneseChronology.JCAL.getEras();
        KNOWN_ERAS = new JapaneseEra[ERA_CONFIG.length];
        KNOWN_ERAS[0] = MEIJI;
        KNOWN_ERAS[1] = TAISHO;
        KNOWN_ERAS[2] = SHOWA;
        KNOWN_ERAS[3] = HEISEI;
        for (int i = N_ERA_CONSTANTS; i < ERA_CONFIG.length; i++) {
            CalendarDate date = ERA_CONFIG[i].getSinceDate();
            LocalDate isoDate = LocalDate.of(date.getYear(), date.getMonth(), date.getDayOfMonth());
            KNOWN_ERAS[i] = new JapaneseEra(i - ERA_OFFSET + 1, isoDate);
        }
    };
    private final transient int eraValue;
    // the first day of the era
    private final transient LocalDate since;
    private JapaneseEra(int eraValue, LocalDate since) {
        this.eraValue = eraValue;
        this.since = since;
    }
    //-----------------------------------------------------------------------
    sun.util.calendar.Era getPrivateEra() {
        return ERA_CONFIG[ordinal(eraValue)];
    }
    //-----------------------------------------------------------------------
    public static JapaneseEra of(int japaneseEra) {
        if (japaneseEra < MEIJI.eraValue || japaneseEra + ERA_OFFSET > KNOWN_ERAS.length) {
            throw new DateTimeException("Invalid era: " + japaneseEra);
        }
        return KNOWN_ERAS[ordinal(japaneseEra)];
    }
    public static JapaneseEra valueOf(String japaneseEra) {
        Objects.requireNonNull(japaneseEra, "japaneseEra");
        for (JapaneseEra era : KNOWN_ERAS) {
            if (era.getName().equals(japaneseEra)) {
                return era;
            }
        }
        throw new IllegalArgumentException("japaneseEra is invalid");
    }
    public static JapaneseEra[] values() {
        return Arrays.copyOf(KNOWN_ERAS, KNOWN_ERAS.length);
    }
    @Override
    public String getDisplayName(TextStyle style, Locale locale) {
        // If this JapaneseEra is a supplemental one, obtain the name from
        // the era definition.
        if (getValue() > N_ERA_CONSTANTS - ERA_OFFSET) {
            Objects.requireNonNull(locale, "locale");
            return style.asNormal() == TextStyle.NARROW ? getAbbreviation() : getName();
        }
        return Era.super.getDisplayName(style, locale);
    }
    //-----------------------------------------------------------------------
    static JapaneseEra from(LocalDate date) {
        if (date.isBefore(MEIJI_6_ISODATE)) {
            throw new DateTimeException("JapaneseDate before Meiji 6 are not supported");
        }
        for (int i = KNOWN_ERAS.length - 1; i > 0; i--) {
            JapaneseEra era = KNOWN_ERAS[i];
            if (date.compareTo(era.since) >= 0) {
                return era;
            }
        }
        return null;
    }
    static JapaneseEra toJapaneseEra(sun.util.calendar.Era privateEra) {
        for (int i = ERA_CONFIG.length - 1; i >= 0; i--) {
            if (ERA_CONFIG[i].equals(privateEra)) {
                return KNOWN_ERAS[i];
            }
        }
        return null;
    }
    static sun.util.calendar.Era privateEraFrom(LocalDate isoDate) {
        for (int i = KNOWN_ERAS.length - 1; i > 0; i--) {
            JapaneseEra era = KNOWN_ERAS[i];
            if (isoDate.compareTo(era.since) >= 0) {
                return ERA_CONFIG[i];
            }
        }
        return null;
    }
    private static int ordinal(int eraValue) {
        return eraValue + ERA_OFFSET - 1;
    }
    //-----------------------------------------------------------------------
    @Override
    public int getValue() {
        return eraValue;
    }
    //-----------------------------------------------------------------------
    @Override  // override as super would return range from 0 to 1
    public ValueRange range(TemporalField field) {
        if (field == ERA) {
            return JapaneseChronology.INSTANCE.range(ERA);
        }
        return Era.super.range(field);
    }
    //-----------------------------------------------------------------------
    String getAbbreviation() {
        return ERA_CONFIG[ordinal(getValue())].getAbbreviation();
    }
    String getName() {
        return ERA_CONFIG[ordinal(getValue())].getName();
    }
    @Override
    public String toString() {
        return getName();
    }
    //-----------------------------------------------------------------------
    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }
    //-----------------------------------------------------------------------
    private Object writeReplace() {
        return new Ser(Ser.JAPANESE_ERA_TYPE, this);
    }
    void writeExternal(DataOutput out) throws IOException {
        out.writeByte(this.getValue());
    }
    static JapaneseEra readExternal(DataInput in) throws IOException {
        byte eraValue = in.readByte();
        return JapaneseEra.of(eraValue);
    }
}
