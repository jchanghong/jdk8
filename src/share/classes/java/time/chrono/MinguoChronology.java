package java.time.chrono;
import java.io.InvalidObjectException;
import static java.time.temporal.ChronoField.PROLEPTIC_MONTH;
import static java.time.temporal.ChronoField.YEAR;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public final class MinguoChronology extends AbstractChronology implements Serializable {
    public static final MinguoChronology INSTANCE = new MinguoChronology();
    private static final long serialVersionUID = 1039765215346859963L;
    static final int YEARS_DIFFERENCE = 1911;
    private MinguoChronology() {
    }
    //-----------------------------------------------------------------------
    @Override
    public String getId() {
        return "Minguo";
    }
    @Override
    public String getCalendarType() {
        return "roc";
    }
    //-----------------------------------------------------------------------
    @Override
    public MinguoDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        return date(prolepticYear(era, yearOfEra), month, dayOfMonth);
    }
    @Override
    public MinguoDate date(int prolepticYear, int month, int dayOfMonth) {
        return new MinguoDate(LocalDate.of(prolepticYear + YEARS_DIFFERENCE, month, dayOfMonth));
    }
    @Override
    public MinguoDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        return dateYearDay(prolepticYear(era, yearOfEra), dayOfYear);
    }
    @Override
    public MinguoDate dateYearDay(int prolepticYear, int dayOfYear) {
        return new MinguoDate(LocalDate.ofYearDay(prolepticYear + YEARS_DIFFERENCE, dayOfYear));
    }
    @Override  // override with covariant return type
    public MinguoDate dateEpochDay(long epochDay) {
        return new MinguoDate(LocalDate.ofEpochDay(epochDay));
    }
    @Override
    public MinguoDate dateNow() {
        return dateNow(Clock.systemDefaultZone());
    }
    @Override
    public MinguoDate dateNow(ZoneId zone) {
        return dateNow(Clock.system(zone));
    }
    @Override
    public MinguoDate dateNow(Clock clock) {
        return date(LocalDate.now(clock));
    }
    @Override
    public MinguoDate date(TemporalAccessor temporal) {
        if (temporal instanceof MinguoDate) {
            return (MinguoDate) temporal;
        }
        return new MinguoDate(LocalDate.from(temporal));
    }
    @Override
    @SuppressWarnings("unchecked")
    public ChronoLocalDateTime<MinguoDate> localDateTime(TemporalAccessor temporal) {
        return (ChronoLocalDateTime<MinguoDate>)super.localDateTime(temporal);
    }
    @Override
    @SuppressWarnings("unchecked")
    public ChronoZonedDateTime<MinguoDate> zonedDateTime(TemporalAccessor temporal) {
        return (ChronoZonedDateTime<MinguoDate>)super.zonedDateTime(temporal);
    }
    @Override
    @SuppressWarnings("unchecked")
    public ChronoZonedDateTime<MinguoDate> zonedDateTime(Instant instant, ZoneId zone) {
        return (ChronoZonedDateTime<MinguoDate>)super.zonedDateTime(instant, zone);
    }
    //-----------------------------------------------------------------------
    @Override
    public boolean isLeapYear(long prolepticYear) {
        return IsoChronology.INSTANCE.isLeapYear(prolepticYear + YEARS_DIFFERENCE);
    }
    @Override
    public int prolepticYear(Era era, int yearOfEra) {
        if (era instanceof MinguoEra == false) {
            throw new ClassCastException("Era must be MinguoEra");
        }
        return (era == MinguoEra.ROC ? yearOfEra : 1 - yearOfEra);
    }
    @Override
    public MinguoEra eraOf(int eraValue) {
        return MinguoEra.of(eraValue);
    }
    @Override
    public List<Era> eras() {
        return Arrays.<Era>asList(MinguoEra.values());
    }
    //-----------------------------------------------------------------------
    @Override
    public ValueRange range(ChronoField field) {
        switch (field) {
            case PROLEPTIC_MONTH: {
                ValueRange range = PROLEPTIC_MONTH.range();
                return ValueRange.of(range.getMinimum() - YEARS_DIFFERENCE * 12L, range.getMaximum() - YEARS_DIFFERENCE * 12L);
            }
            case YEAR_OF_ERA: {
                ValueRange range = YEAR.range();
                return ValueRange.of(1, range.getMaximum() - YEARS_DIFFERENCE, -range.getMinimum() + 1 + YEARS_DIFFERENCE);
            }
            case YEAR: {
                ValueRange range = YEAR.range();
                return ValueRange.of(range.getMinimum() - YEARS_DIFFERENCE, range.getMaximum() - YEARS_DIFFERENCE);
            }
        }
        return field.range();
    }
    //-----------------------------------------------------------------------
    @Override  // override for return type
    public MinguoDate resolveDate(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        return (MinguoDate) super.resolveDate(fieldValues, resolverStyle);
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
