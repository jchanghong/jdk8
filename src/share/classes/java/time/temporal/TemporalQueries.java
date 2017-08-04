package java.time.temporal;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.NANO_OF_DAY;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.Chronology;
public final class TemporalQueries {
    // note that it is vital that each method supplies a constant, not a
    // calculated value, as they will be checked for using ==
    // it is also vital that each constant is different (due to the == checking)
    // as such, alterations to this code must be done with care
    private TemporalQueries() {
    }
    //-----------------------------------------------------------------------
    // special constants should be used to extract information from a TemporalAccessor
    // that cannot be derived in other ways
    // Javadoc added here, so as to pretend they are more normal than they really are
    public static TemporalQuery<ZoneId> zoneId() {
        return TemporalQueries.ZONE_ID;
    }
    public static TemporalQuery<Chronology> chronology() {
        return TemporalQueries.CHRONO;
    }
    public static TemporalQuery<TemporalUnit> precision() {
        return TemporalQueries.PRECISION;
    }
    //-----------------------------------------------------------------------
    // non-special constants are standard queries that derive information from other information
    public static TemporalQuery<ZoneId> zone() {
        return TemporalQueries.ZONE;
    }
    public static TemporalQuery<ZoneOffset> offset() {
        return TemporalQueries.OFFSET;
    }
    public static TemporalQuery<LocalDate> localDate() {
        return TemporalQueries.LOCAL_DATE;
    }
    public static TemporalQuery<LocalTime> localTime() {
        return TemporalQueries.LOCAL_TIME;
    }
    //-----------------------------------------------------------------------
    static final TemporalQuery<ZoneId> ZONE_ID = (temporal) ->
        temporal.query(TemporalQueries.ZONE_ID);
    static final TemporalQuery<Chronology> CHRONO = (temporal) ->
        temporal.query(TemporalQueries.CHRONO);
    static final TemporalQuery<TemporalUnit> PRECISION = (temporal) ->
        temporal.query(TemporalQueries.PRECISION);
    //-----------------------------------------------------------------------
    static final TemporalQuery<ZoneOffset> OFFSET = (temporal) -> {
        if (temporal.isSupported(OFFSET_SECONDS)) {
            return ZoneOffset.ofTotalSeconds(temporal.get(OFFSET_SECONDS));
        }
        return null;
    };
    static final TemporalQuery<ZoneId> ZONE = (temporal) -> {
        ZoneId zone = temporal.query(ZONE_ID);
        return (zone != null ? zone : temporal.query(OFFSET));
    };
    static final TemporalQuery<LocalDate> LOCAL_DATE = (temporal) -> {
        if (temporal.isSupported(EPOCH_DAY)) {
            return LocalDate.ofEpochDay(temporal.getLong(EPOCH_DAY));
        }
        return null;
    };
    static final TemporalQuery<LocalTime> LOCAL_TIME = (temporal) -> {
        if (temporal.isSupported(NANO_OF_DAY)) {
            return LocalTime.ofNanoOfDay(temporal.getLong(NANO_OF_DAY));
        }
        return null;
    };
}
