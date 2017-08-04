package java.time.chrono;
import java.io.InvalidObjectException;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.PROLEPTIC_MONTH;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoField.YEAR_OF_ERA;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
public final class IsoChronology extends AbstractChronology implements Serializable {
    public static final IsoChronology INSTANCE = new IsoChronology();
    private static final long serialVersionUID = -1440403870442975015L;
    private IsoChronology() {
    }
    //-----------------------------------------------------------------------
    @Override
    public String getId() {
        return "ISO";
    }
    @Override
    public String getCalendarType() {
        return "iso8601";
    }
    //-----------------------------------------------------------------------
    @Override  // override with covariant return type
    public LocalDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        return date(prolepticYear(era, yearOfEra), month, dayOfMonth);
    }
    @Override  // override with covariant return type
    public LocalDate date(int prolepticYear, int month, int dayOfMonth) {
        return LocalDate.of(prolepticYear, month, dayOfMonth);
    }
    @Override  // override with covariant return type
    public LocalDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        return dateYearDay(prolepticYear(era, yearOfEra), dayOfYear);
    }
    @Override  // override with covariant return type
    public LocalDate dateYearDay(int prolepticYear, int dayOfYear) {
        return LocalDate.ofYearDay(prolepticYear, dayOfYear);
    }
    @Override  // override with covariant return type
    public LocalDate dateEpochDay(long epochDay) {
        return LocalDate.ofEpochDay(epochDay);
    }
    //-----------------------------------------------------------------------
    @Override  // override with covariant return type
    public LocalDate date(TemporalAccessor temporal) {
        return LocalDate.from(temporal);
    }
    @Override  // override with covariant return type
    public LocalDateTime localDateTime(TemporalAccessor temporal) {
        return LocalDateTime.from(temporal);
    }
    @Override  // override with covariant return type
    public ZonedDateTime zonedDateTime(TemporalAccessor temporal) {
        return ZonedDateTime.from(temporal);
    }
    @Override
    public ZonedDateTime zonedDateTime(Instant instant, ZoneId zone) {
        return ZonedDateTime.ofInstant(instant, zone);
    }
    //-----------------------------------------------------------------------
    @Override  // override with covariant return type
    public LocalDate dateNow() {
        return dateNow(Clock.systemDefaultZone());
    }
    @Override  // override with covariant return type
    public LocalDate dateNow(ZoneId zone) {
        return dateNow(Clock.system(zone));
    }
    @Override  // override with covariant return type
    public LocalDate dateNow(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        return date(LocalDate.now(clock));
    }
    //-----------------------------------------------------------------------
    @Override
    public boolean isLeapYear(long prolepticYear) {
        return ((prolepticYear & 3) == 0) && ((prolepticYear % 100) != 0 || (prolepticYear % 400) == 0);
    }
    @Override
    public int prolepticYear(Era era, int yearOfEra) {
        if (era instanceof IsoEra == false) {
            throw new ClassCastException("Era must be IsoEra");
        }
        return (era == IsoEra.CE ? yearOfEra : 1 - yearOfEra);
    }
    @Override
    public IsoEra eraOf(int eraValue) {
        return IsoEra.of(eraValue);
    }
    @Override
    public List<Era> eras() {
        return Arrays.<Era>asList(IsoEra.values());
    }
    //-----------------------------------------------------------------------
    @Override  // override for performance
    public LocalDate resolveDate(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        return (LocalDate) super.resolveDate(fieldValues, resolverStyle);
    }
    @Override  // override for better proleptic algorithm
    void resolveProlepticMonth(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        Long pMonth = fieldValues.remove(PROLEPTIC_MONTH);
        if (pMonth != null) {
            if (resolverStyle != ResolverStyle.LENIENT) {
                PROLEPTIC_MONTH.checkValidValue(pMonth);
            }
            addFieldValue(fieldValues, MONTH_OF_YEAR, Math.floorMod(pMonth, 12) + 1);
            addFieldValue(fieldValues, YEAR, Math.floorDiv(pMonth, 12));
        }
    }
    @Override  // override for enhanced behaviour
    LocalDate resolveYearOfEra(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        Long yoeLong = fieldValues.remove(YEAR_OF_ERA);
        if (yoeLong != null) {
            if (resolverStyle != ResolverStyle.LENIENT) {
                YEAR_OF_ERA.checkValidValue(yoeLong);
            }
            Long era = fieldValues.remove(ERA);
            if (era == null) {
                Long year = fieldValues.get(YEAR);
                if (resolverStyle == ResolverStyle.STRICT) {
                    // do not invent era if strict, but do cross-check with year
                    if (year != null) {
                        addFieldValue(fieldValues, YEAR, (year > 0 ? yoeLong: Math.subtractExact(1, yoeLong)));
                    } else {
                        // reinstate the field removed earlier, no cross-check issues
                        fieldValues.put(YEAR_OF_ERA, yoeLong);
                    }
                } else {
                    // invent era
                    addFieldValue(fieldValues, YEAR, (year == null || year > 0 ? yoeLong: Math.subtractExact(1, yoeLong)));
                }
            } else if (era.longValue() == 1L) {
                addFieldValue(fieldValues, YEAR, yoeLong);
            } else if (era.longValue() == 0L) {
                addFieldValue(fieldValues, YEAR, Math.subtractExact(1, yoeLong));
            } else {
                throw new DateTimeException("Invalid value for era: " + era);
            }
        } else if (fieldValues.containsKey(ERA)) {
            ERA.checkValidValue(fieldValues.get(ERA));  // always validated
        }
        return null;
    }
    @Override  // override for performance
    LocalDate resolveYMD(Map <TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        int y = YEAR.checkValidIntValue(fieldValues.remove(YEAR));
        if (resolverStyle == ResolverStyle.LENIENT) {
            long months = Math.subtractExact(fieldValues.remove(MONTH_OF_YEAR), 1);
            long days = Math.subtractExact(fieldValues.remove(DAY_OF_MONTH), 1);
            return LocalDate.of(y, 1, 1).plusMonths(months).plusDays(days);
        }
        int moy = MONTH_OF_YEAR.checkValidIntValue(fieldValues.remove(MONTH_OF_YEAR));
        int dom = DAY_OF_MONTH.checkValidIntValue(fieldValues.remove(DAY_OF_MONTH));
        if (resolverStyle == ResolverStyle.SMART) {  // previous valid
            if (moy == 4 || moy == 6 || moy == 9 || moy == 11) {
                dom = Math.min(dom, 30);
            } else if (moy == 2) {
                dom = Math.min(dom, Month.FEBRUARY.length(Year.isLeap(y)));
            }
        }
        return LocalDate.of(y, moy, dom);
    }
    //-----------------------------------------------------------------------
    @Override
    public ValueRange range(ChronoField field) {
        return field.range();
    }
    //-----------------------------------------------------------------------
    @Override  // override with covariant return type
    public Period period(int years, int months, int days) {
        return Period.of(years, months, days);
    }
    //-----------------------------------------------------------------------
    @Override
    Object writeReplace() {
        return super.writeReplace();
    }
    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }
}
