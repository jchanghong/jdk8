


package java.time.chrono;

import static java.time.chrono.ThaiBuddhistChronology.YEARS_DIFFERENCE;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Objects;


public final class ThaiBuddhistDate
        extends ChronoLocalDateImpl<ThaiBuddhistDate>
        implements ChronoLocalDate, Serializable {


    private static final long serialVersionUID = -8722293800195731463L;


    private final transient LocalDate isoDate;

    //-----------------------------------------------------------------------

    public static ThaiBuddhistDate now() {
        return now(Clock.systemDefaultZone());
    }


    public static ThaiBuddhistDate now(ZoneId zone) {
        return now(Clock.system(zone));
    }


    public static ThaiBuddhistDate now(Clock clock) {
        return new ThaiBuddhistDate(LocalDate.now(clock));
    }


    public static ThaiBuddhistDate of(int prolepticYear, int month, int dayOfMonth) {
        return new ThaiBuddhistDate(LocalDate.of(prolepticYear - YEARS_DIFFERENCE, month, dayOfMonth));
    }


    public static ThaiBuddhistDate from(TemporalAccessor temporal) {
        return ThaiBuddhistChronology.INSTANCE.date(temporal);
    }

    //-----------------------------------------------------------------------

    ThaiBuddhistDate(LocalDate isoDate) {
        Objects.requireNonNull(isoDate, "isoDate");
        this.isoDate = isoDate;
    }

    //-----------------------------------------------------------------------

    @Override
    public ThaiBuddhistChronology getChronology() {
        return ThaiBuddhistChronology.INSTANCE;
    }


    @Override
    public ThaiBuddhistEra getEra() {
        return (getProlepticYear() >= 1 ? ThaiBuddhistEra.BE : ThaiBuddhistEra.BEFORE_BE);
    }


    @Override
    public int lengthOfMonth() {
        return isoDate.lengthOfMonth();
    }

    //-----------------------------------------------------------------------
    @Override
    public ValueRange range(TemporalField field) {
        if (field instanceof ChronoField) {
            if (isSupported(field)) {
                ChronoField f = (ChronoField) field;
                switch (f) {
                    case DAY_OF_MONTH:
                    case DAY_OF_YEAR:
                    case ALIGNED_WEEK_OF_MONTH:
                        return isoDate.range(field);
                    case YEAR_OF_ERA: {
                        ValueRange range = YEAR.range();
                        long max = (getProlepticYear() <= 0 ? -(range.getMinimum() + YEARS_DIFFERENCE) + 1 : range.getMaximum() + YEARS_DIFFERENCE);
                        return ValueRange.of(1, max);
                    }
                }
                return getChronology().range(f);
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field instanceof ChronoField) {
            switch ((ChronoField) field) {
                case PROLEPTIC_MONTH:
                    return getProlepticMonth();
                case YEAR_OF_ERA: {
                    int prolepticYear = getProlepticYear();
                    return (prolepticYear >= 1 ? prolepticYear : 1 - prolepticYear);
                }
                case YEAR:
                    return getProlepticYear();
                case ERA:
                    return (getProlepticYear() >= 1 ? 1 : 0);
            }
            return isoDate.getLong(field);
        }
        return field.getFrom(this);
    }

    private long getProlepticMonth() {
        return getProlepticYear() * 12L + isoDate.getMonthValue() - 1;
    }

    private int getProlepticYear() {
        return isoDate.getYear() + YEARS_DIFFERENCE;
    }

    //-----------------------------------------------------------------------
    @Override
    public ThaiBuddhistDate with(TemporalField field, long newValue) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            if (getLong(f) == newValue) {
                return this;
            }
            switch (f) {
                case PROLEPTIC_MONTH:
                    getChronology().range(f).checkValidValue(newValue, f);
                    return plusMonths(newValue - getProlepticMonth());
                case YEAR_OF_ERA:
                case YEAR:
                case ERA: {
                    int nvalue = getChronology().range(f).checkValidIntValue(newValue, f);
                    switch (f) {
                        case YEAR_OF_ERA:
                            return with(isoDate.withYear((getProlepticYear() >= 1 ? nvalue : 1 - nvalue)  - YEARS_DIFFERENCE));
                        case YEAR:
                            return with(isoDate.withYear(nvalue - YEARS_DIFFERENCE));
                        case ERA:
                            return with(isoDate.withYear((1 - getProlepticYear()) - YEARS_DIFFERENCE));
                    }
                }
            }
            return with(isoDate.with(field, newValue));
        }
        return super.with(field, newValue);
    }


    @Override
    public  ThaiBuddhistDate with(TemporalAdjuster adjuster) {
        return super.with(adjuster);
    }


    @Override
    public ThaiBuddhistDate plus(TemporalAmount amount) {
        return super.plus(amount);
    }


    @Override
    public ThaiBuddhistDate minus(TemporalAmount amount) {
        return super.minus(amount);
    }

    //-----------------------------------------------------------------------
    @Override
    ThaiBuddhistDate plusYears(long years) {
        return with(isoDate.plusYears(years));
    }

    @Override
    ThaiBuddhistDate plusMonths(long months) {
        return with(isoDate.plusMonths(months));
    }

    @Override
    ThaiBuddhistDate plusWeeks(long weeksToAdd) {
        return super.plusWeeks(weeksToAdd);
    }

    @Override
    ThaiBuddhistDate plusDays(long days) {
        return with(isoDate.plusDays(days));
    }

    @Override
    public ThaiBuddhistDate plus(long amountToAdd, TemporalUnit unit) {
        return super.plus(amountToAdd, unit);
    }

    @Override
    public ThaiBuddhistDate minus(long amountToAdd, TemporalUnit unit) {
        return super.minus(amountToAdd, unit);
    }

    @Override
    ThaiBuddhistDate minusYears(long yearsToSubtract) {
        return super.minusYears(yearsToSubtract);
    }

    @Override
    ThaiBuddhistDate minusMonths(long monthsToSubtract) {
        return super.minusMonths(monthsToSubtract);
    }

    @Override
    ThaiBuddhistDate minusWeeks(long weeksToSubtract) {
        return super.minusWeeks(weeksToSubtract);
    }

    @Override
    ThaiBuddhistDate minusDays(long daysToSubtract) {
        return super.minusDays(daysToSubtract);
    }

    private ThaiBuddhistDate with(LocalDate newDate) {
        return (newDate.equals(isoDate) ? this : new ThaiBuddhistDate(newDate));
    }

    @Override        // for javadoc and covariant return type
    @SuppressWarnings("unchecked")
    public final ChronoLocalDateTime<ThaiBuddhistDate> atTime(LocalTime localTime) {
        return (ChronoLocalDateTime<ThaiBuddhistDate>) super.atTime(localTime);
    }

    @Override
    public ChronoPeriod until(ChronoLocalDate endDate) {
        Period period = isoDate.until(endDate);
        return getChronology().period(period.getYears(), period.getMonths(), period.getDays());
    }

    @Override  // override for performance
    public long toEpochDay() {
        return isoDate.toEpochDay();
    }

    //-------------------------------------------------------------------------

    @Override  // override for performance
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ThaiBuddhistDate) {
            ThaiBuddhistDate otherDate = (ThaiBuddhistDate) obj;
            return this.isoDate.equals(otherDate.isoDate);
        }
        return false;
    }


    @Override  // override for performance
    public int hashCode() {
        return getChronology().getId().hashCode() ^ isoDate.hashCode();
    }

    //-----------------------------------------------------------------------

    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }


    private Object writeReplace() {
        return new Ser(Ser.THAIBUDDHIST_DATE_TYPE, this);
    }

    void writeExternal(DataOutput out) throws IOException {
        // ThaiBuddhistChronology is implicit in the THAIBUDDHIST_DATE_TYPE
        out.writeInt(this.get(YEAR));
        out.writeByte(this.get(MONTH_OF_YEAR));
        out.writeByte(this.get(DAY_OF_MONTH));
    }

    static ThaiBuddhistDate readExternal(DataInput in) throws IOException {
        int year = in.readInt();
        int month = in.readByte();
        int dayOfMonth = in.readByte();
        return ThaiBuddhistChronology.INSTANCE.date(year, month, dayOfMonth);
    }

}
