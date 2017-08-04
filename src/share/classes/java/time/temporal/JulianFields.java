


package java.time.temporal;

import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.FOREVER;

import java.time.DateTimeException;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.format.ResolverStyle;
import java.util.Map;


public final class JulianFields {


    private static final long JULIAN_DAY_OFFSET = 2440588L;


    public static final TemporalField JULIAN_DAY = Field.JULIAN_DAY;


    public static final TemporalField MODIFIED_JULIAN_DAY = Field.MODIFIED_JULIAN_DAY;


    public static final TemporalField RATA_DIE = Field.RATA_DIE;


    private JulianFields() {
        throw new AssertionError("Not instantiable");
    }


    private static enum Field implements TemporalField {
        JULIAN_DAY("JulianDay", DAYS, FOREVER, JULIAN_DAY_OFFSET),
        MODIFIED_JULIAN_DAY("ModifiedJulianDay", DAYS, FOREVER, 40587L),
        RATA_DIE("RataDie", DAYS, FOREVER, 719163L);

        private static final long serialVersionUID = -7501623920830201812L;

        private final transient String name;
        private final transient TemporalUnit baseUnit;
        private final transient TemporalUnit rangeUnit;
        private final transient ValueRange range;
        private final transient long offset;

        private Field(String name, TemporalUnit baseUnit, TemporalUnit rangeUnit, long offset) {
            this.name = name;
            this.baseUnit = baseUnit;
            this.rangeUnit = rangeUnit;
            this.range = ValueRange.of(-365243219162L + offset, 365241780471L + offset);
            this.offset = offset;
        }

        //-----------------------------------------------------------------------
        @Override
        public TemporalUnit getBaseUnit() {
            return baseUnit;
        }

        @Override
        public TemporalUnit getRangeUnit() {
            return rangeUnit;
        }

        @Override
        public boolean isDateBased() {
            return true;
        }

        @Override
        public boolean isTimeBased() {
            return false;
        }

        @Override
        public ValueRange range() {
            return range;
        }

        //-----------------------------------------------------------------------
        @Override
        public boolean isSupportedBy(TemporalAccessor temporal) {
            return temporal.isSupported(EPOCH_DAY);
        }

        @Override
        public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
            if (isSupportedBy(temporal) == false) {
                throw new DateTimeException("Unsupported field: " + this);
            }
            return range();
        }

        @Override
        public long getFrom(TemporalAccessor temporal) {
            return temporal.getLong(EPOCH_DAY) + offset;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R adjustInto(R temporal, long newValue) {
            if (range().isValidValue(newValue) == false) {
                throw new DateTimeException("Invalid value: " + name + " " + newValue);
            }
            return (R) temporal.with(EPOCH_DAY, Math.subtractExact(newValue, offset));
        }

        //-----------------------------------------------------------------------
        @Override
        public ChronoLocalDate resolve(
                Map<TemporalField, Long> fieldValues, TemporalAccessor partialTemporal, ResolverStyle resolverStyle) {
            long value = fieldValues.remove(this);
            Chronology chrono = Chronology.from(partialTemporal);
            if (resolverStyle == ResolverStyle.LENIENT) {
                return chrono.dateEpochDay(Math.subtractExact(value, offset));
            }
            range().checkValidValue(value, this);
            return chrono.dateEpochDay(value - offset);
        }

        //-----------------------------------------------------------------------
        @Override
        public String toString() {
            return name;
        }
    }
}
