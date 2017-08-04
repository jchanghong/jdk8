


package java.time.chrono;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.PROLEPTIC_MONTH;
import static java.time.temporal.ChronoField.YEAR_OF_ERA;

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Objects;


abstract class ChronoLocalDateImpl<D extends ChronoLocalDate>
        implements ChronoLocalDate, Temporal, TemporalAdjuster, Serializable {


    private static final long serialVersionUID = 6282433883239719096L;


    static <D extends ChronoLocalDate> D ensureValid(Chronology chrono, Temporal temporal) {
        @SuppressWarnings("unchecked")
        D other = (D) temporal;
        if (chrono.equals(other.getChronology()) == false) {
            throw new ClassCastException("Chronology mismatch, expected: " + chrono.getId() + ", actual: " + other.getChronology().getId());
        }
        return other;
    }

    //-----------------------------------------------------------------------

    ChronoLocalDateImpl() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public D with(TemporalAdjuster adjuster) {
        return (D) ChronoLocalDate.super.with(adjuster);
    }

    @Override
    @SuppressWarnings("unchecked")
    public D with(TemporalField field, long value) {
        return (D) ChronoLocalDate.super.with(field, value);
    }

    //-----------------------------------------------------------------------
    @Override
    @SuppressWarnings("unchecked")
    public D plus(TemporalAmount amount) {
        return (D) ChronoLocalDate.super.plus(amount);
    }

    //-----------------------------------------------------------------------
    @Override
    @SuppressWarnings("unchecked")
    public D plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            ChronoUnit f = (ChronoUnit) unit;
            switch (f) {
                case DAYS: return plusDays(amountToAdd);
                case WEEKS: return plusDays(Math.multiplyExact(amountToAdd, 7));
                case MONTHS: return plusMonths(amountToAdd);
                case YEARS: return plusYears(amountToAdd);
                case DECADES: return plusYears(Math.multiplyExact(amountToAdd, 10));
                case CENTURIES: return plusYears(Math.multiplyExact(amountToAdd, 100));
                case MILLENNIA: return plusYears(Math.multiplyExact(amountToAdd, 1000));
                case ERAS: return with(ERA, Math.addExact(getLong(ERA), amountToAdd));
            }
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        return (D) ChronoLocalDate.super.plus(amountToAdd, unit);
    }

    @Override
    @SuppressWarnings("unchecked")
    public D minus(TemporalAmount amount) {
        return (D) ChronoLocalDate.super.minus(amount);
    }

    @Override
    @SuppressWarnings("unchecked")
    public D minus(long amountToSubtract, TemporalUnit unit) {
        return (D) ChronoLocalDate.super.minus(amountToSubtract, unit);
    }

    //-----------------------------------------------------------------------

    abstract D plusYears(long yearsToAdd);


    abstract D plusMonths(long monthsToAdd);


    D plusWeeks(long weeksToAdd) {
        return plusDays(Math.multiplyExact(weeksToAdd, 7));
    }


    abstract D plusDays(long daysToAdd);

    //-----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    D minusYears(long yearsToSubtract) {
        return (yearsToSubtract == Long.MIN_VALUE ? ((ChronoLocalDateImpl<D>)plusYears(Long.MAX_VALUE)).plusYears(1) : plusYears(-yearsToSubtract));
    }


    @SuppressWarnings("unchecked")
    D minusMonths(long monthsToSubtract) {
        return (monthsToSubtract == Long.MIN_VALUE ? ((ChronoLocalDateImpl<D>)plusMonths(Long.MAX_VALUE)).plusMonths(1) : plusMonths(-monthsToSubtract));
    }


    @SuppressWarnings("unchecked")
    D minusWeeks(long weeksToSubtract) {
        return (weeksToSubtract == Long.MIN_VALUE ? ((ChronoLocalDateImpl<D>)plusWeeks(Long.MAX_VALUE)).plusWeeks(1) : plusWeeks(-weeksToSubtract));
    }


    @SuppressWarnings("unchecked")
    D minusDays(long daysToSubtract) {
        return (daysToSubtract == Long.MIN_VALUE ? ((ChronoLocalDateImpl<D>)plusDays(Long.MAX_VALUE)).plusDays(1) : plusDays(-daysToSubtract));
    }

    //-----------------------------------------------------------------------
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        Objects.requireNonNull(endExclusive, "endExclusive");
        ChronoLocalDate end = getChronology().date(endExclusive);
        if (unit instanceof ChronoUnit) {
            switch ((ChronoUnit) unit) {
                case DAYS: return daysUntil(end);
                case WEEKS: return daysUntil(end) / 7;
                case MONTHS: return monthsUntil(end);
                case YEARS: return monthsUntil(end) / 12;
                case DECADES: return monthsUntil(end) / 120;
                case CENTURIES: return monthsUntil(end) / 1200;
                case MILLENNIA: return monthsUntil(end) / 12000;
                case ERAS: return end.getLong(ERA) - getLong(ERA);
            }
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        Objects.requireNonNull(unit, "unit");
        return unit.between(this, end);
    }

    private long daysUntil(ChronoLocalDate end) {
        return end.toEpochDay() - toEpochDay();  // no overflow
    }

    private long monthsUntil(ChronoLocalDate end) {
        ValueRange range = getChronology().range(MONTH_OF_YEAR);
        if (range.getMaximum() != 12) {
            throw new IllegalStateException("ChronoLocalDateImpl only supports Chronologies with 12 months per year");
        }
        long packed1 = getLong(PROLEPTIC_MONTH) * 32L + get(DAY_OF_MONTH);  // no overflow
        long packed2 = end.getLong(PROLEPTIC_MONTH) * 32L + end.get(DAY_OF_MONTH);  // no overflow
        return (packed2 - packed1) / 32;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ChronoLocalDate) {
            return compareTo((ChronoLocalDate) obj) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        long epDay = toEpochDay();
        return getChronology().hashCode() ^ ((int) (epDay ^ (epDay >>> 32)));
    }

    @Override
    public String toString() {
        // getLong() reduces chances of exceptions in toString()
        long yoe = getLong(YEAR_OF_ERA);
        long moy = getLong(MONTH_OF_YEAR);
        long dom = getLong(DAY_OF_MONTH);
        StringBuilder buf = new StringBuilder(30);
        buf.append(getChronology().toString())
                .append(" ")
                .append(getEra())
                .append(" ")
                .append(yoe)
                .append(moy < 10 ? "-0" : "-").append(moy)
                .append(dom < 10 ? "-0" : "-").append(dom);
        return buf.toString();
    }

}
