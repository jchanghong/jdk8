


package java.time.chrono;

import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoUnit.ERAS;

import java.time.DateTimeException;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.ValueRange;
import java.util.Locale;


public interface Era extends TemporalAccessor, TemporalAdjuster {


    int getValue();

    //-----------------------------------------------------------------------

    @Override
    default boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return field == ERA;
        }
        return field != null && field.isSupportedBy(this);
    }


    @Override  // override for Javadoc
    default ValueRange range(TemporalField field) {
        return TemporalAccessor.super.range(field);
    }


    @Override  // override for Javadoc and performance
    default int get(TemporalField field) {
        if (field == ERA) {
            return getValue();
        }
        return TemporalAccessor.super.get(field);
    }


    @Override
    default long getLong(TemporalField field) {
        if (field == ERA) {
            return getValue();
        } else if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    //-----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    default <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.precision()) {
            return (R) ERAS;
        }
        return TemporalAccessor.super.query(query);
    }


    @Override
    default Temporal adjustInto(Temporal temporal) {
        return temporal.with(ERA, getValue());
    }

    //-----------------------------------------------------------------------

    default String getDisplayName(TextStyle style, Locale locale) {
        return new DateTimeFormatterBuilder().appendText(ERA, style).toFormatter(locale).format(this);
    }

    // NOTE: methods to convert year-of-era/proleptic-year cannot be here as they may depend on month/day (Japanese)
}
