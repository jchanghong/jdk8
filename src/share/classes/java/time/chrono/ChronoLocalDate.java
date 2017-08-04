package java.time.chrono;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoUnit.DAYS;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Comparator;
import java.util.Objects;
public interface ChronoLocalDate
        extends Temporal, TemporalAdjuster, Comparable<ChronoLocalDate> {
    static Comparator<ChronoLocalDate> timeLineOrder() {
        return AbstractChronology.DATE_ORDER;
    }
    //-----------------------------------------------------------------------
    static ChronoLocalDate from(TemporalAccessor temporal) {
        if (temporal instanceof ChronoLocalDate) {
            return (ChronoLocalDate) temporal;
        }
        Objects.requireNonNull(temporal, "temporal");
        Chronology chrono = temporal.query(TemporalQueries.chronology());
        if (chrono == null) {
            throw new DateTimeException("Unable to obtain ChronoLocalDate from TemporalAccessor: " + temporal.getClass());
        }
        return chrono.date(temporal);
    }
    //-----------------------------------------------------------------------
    Chronology getChronology();
    default Era getEra() {
        return getChronology().eraOf(get(ERA));
    }
    default boolean isLeapYear() {
        return getChronology().isLeapYear(getLong(YEAR));
    }
    int lengthOfMonth();
    default int lengthOfYear() {
        return (isLeapYear() ? 366 : 365);
    }
    @Override
    default boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return field.isDateBased();
        }
        return field != null && field.isSupportedBy(this);
    }
    @Override
    default boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isDateBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }
    //-----------------------------------------------------------------------
    // override for covariant return type
    @Override
    default ChronoLocalDate with(TemporalAdjuster adjuster) {
        return ChronoLocalDateImpl.ensureValid(getChronology(), Temporal.super.with(adjuster));
    }
    @Override
    default ChronoLocalDate with(TemporalField field, long newValue) {
        if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return ChronoLocalDateImpl.ensureValid(getChronology(), field.adjustInto(this, newValue));
    }
    @Override
    default ChronoLocalDate plus(TemporalAmount amount) {
        return ChronoLocalDateImpl.ensureValid(getChronology(), Temporal.super.plus(amount));
    }
    @Override
    default ChronoLocalDate plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        return ChronoLocalDateImpl.ensureValid(getChronology(), unit.addTo(this, amountToAdd));
    }
    @Override
    default ChronoLocalDate minus(TemporalAmount amount) {
        return ChronoLocalDateImpl.ensureValid(getChronology(), Temporal.super.minus(amount));
    }
    @Override
    default ChronoLocalDate minus(long amountToSubtract, TemporalUnit unit) {
        return ChronoLocalDateImpl.ensureValid(getChronology(), Temporal.super.minus(amountToSubtract, unit));
    }
    //-----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    @Override
    default <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.zoneId() || query == TemporalQueries.zone() || query == TemporalQueries.offset()) {
            return null;
        } else if (query == TemporalQueries.localTime()) {
            return null;
        } else if (query == TemporalQueries.chronology()) {
            return (R) getChronology();
        } else if (query == TemporalQueries.precision()) {
            return (R) DAYS;
        }
        // inline TemporalAccessor.super.query(query) as an optimization
        // non-JDK classes are not permitted to make this optimization
        return query.queryFrom(this);
    }
    @Override
    default Temporal adjustInto(Temporal temporal) {
        return temporal.with(EPOCH_DAY, toEpochDay());
    }
    @Override  // override for Javadoc
    long until(Temporal endExclusive, TemporalUnit unit);
    ChronoPeriod until(ChronoLocalDate endDateExclusive);
    default String format(DateTimeFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter");
        return formatter.format(this);
    }
    //-----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    default ChronoLocalDateTime<?> atTime(LocalTime localTime) {
        return ChronoLocalDateTimeImpl.of(this, localTime);
    }
    //-----------------------------------------------------------------------
    default long toEpochDay() {
        return getLong(EPOCH_DAY);
    }
    //-----------------------------------------------------------------------
    @Override
    default int compareTo(ChronoLocalDate other) {
        int cmp = Long.compare(toEpochDay(), other.toEpochDay());
        if (cmp == 0) {
            cmp = getChronology().compareTo(other.getChronology());
        }
        return cmp;
    }
    default boolean isAfter(ChronoLocalDate other) {
        return this.toEpochDay() > other.toEpochDay();
    }
    default boolean isBefore(ChronoLocalDate other) {
        return this.toEpochDay() < other.toEpochDay();
    }
    default boolean isEqual(ChronoLocalDate other) {
        return this.toEpochDay() == other.toEpochDay();
    }
    //-----------------------------------------------------------------------
    @Override
    boolean equals(Object obj);
    @Override
    int hashCode();
    //-----------------------------------------------------------------------
    @Override
    String toString();
}
