package java.time.chrono;
import static java.time.temporal.ChronoField.ERA;
import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
public enum HijrahEra implements Era {
    AH;
    //-----------------------------------------------------------------------
    public static HijrahEra of(int hijrahEra) {
        if (hijrahEra == 1 ) {
            return AH;
        } else {
            throw new DateTimeException("Invalid era: " + hijrahEra);
        }
    }
    //-----------------------------------------------------------------------
    @Override
    public int getValue() {
        return 1;
    }
    //-----------------------------------------------------------------------
    @Override  // override as super would return range from 0 to 1
    public ValueRange range(TemporalField field) {
        if (field == ERA) {
            return ValueRange.of(1, 1);
        }
        return Era.super.range(field);
    }
}
