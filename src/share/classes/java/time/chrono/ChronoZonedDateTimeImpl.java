package java.time.chrono;
import static java.time.temporal.ChronoUnit.SECONDS;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.List;
import java.util.Objects;
final class ChronoZonedDateTimeImpl<D extends ChronoLocalDate>
        implements ChronoZonedDateTime<D>, Serializable {
    private static final long serialVersionUID = -5261813987200935591L;
    private final transient ChronoLocalDateTimeImpl<D> dateTime;
    private final transient ZoneOffset offset;
    private final transient ZoneId zone;
    //-----------------------------------------------------------------------
    static <R extends ChronoLocalDate> ChronoZonedDateTime<R> ofBest(
            ChronoLocalDateTimeImpl<R> localDateTime, ZoneId zone, ZoneOffset preferredOffset) {
        Objects.requireNonNull(localDateTime, "localDateTime");
        Objects.requireNonNull(zone, "zone");
        if (zone instanceof ZoneOffset) {
            return new ChronoZonedDateTimeImpl<>(localDateTime, (ZoneOffset) zone, zone);
        }
        ZoneRules rules = zone.getRules();
        LocalDateTime isoLDT = LocalDateTime.from(localDateTime);
        List<ZoneOffset> validOffsets = rules.getValidOffsets(isoLDT);
        ZoneOffset offset;
        if (validOffsets.size() == 1) {
            offset = validOffsets.get(0);
        } else if (validOffsets.size() == 0) {
            ZoneOffsetTransition trans = rules.getTransition(isoLDT);
            localDateTime = localDateTime.plusSeconds(trans.getDuration().getSeconds());
            offset = trans.getOffsetAfter();
        } else {
            if (preferredOffset != null && validOffsets.contains(preferredOffset)) {
                offset = preferredOffset;
            } else {
                offset = validOffsets.get(0);
            }
        }
        Objects.requireNonNull(offset, "offset");  // protect against bad ZoneRules
        return new ChronoZonedDateTimeImpl<>(localDateTime, offset, zone);
    }
    static ChronoZonedDateTimeImpl<?> ofInstant(Chronology chrono, Instant instant, ZoneId zone) {
        ZoneRules rules = zone.getRules();
        ZoneOffset offset = rules.getOffset(instant);
        Objects.requireNonNull(offset, "offset");  // protect against bad ZoneRules
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), offset);
        ChronoLocalDateTimeImpl<?> cldt = (ChronoLocalDateTimeImpl<?>)chrono.localDateTime(ldt);
        return new ChronoZonedDateTimeImpl<>(cldt, offset, zone);
    }
    @SuppressWarnings("unchecked")
    private ChronoZonedDateTimeImpl<D> create(Instant instant, ZoneId zone) {
        return (ChronoZonedDateTimeImpl<D>)ofInstant(getChronology(), instant, zone);
    }
    static <R extends ChronoLocalDate> ChronoZonedDateTimeImpl<R> ensureValid(Chronology chrono, Temporal temporal) {
        @SuppressWarnings("unchecked")
        ChronoZonedDateTimeImpl<R> other = (ChronoZonedDateTimeImpl<R>) temporal;
        if (chrono.equals(other.getChronology()) == false) {
            throw new ClassCastException("Chronology mismatch, required: " + chrono.getId()
                    + ", actual: " + other.getChronology().getId());
        }
        return other;
    }
    //-----------------------------------------------------------------------
    private ChronoZonedDateTimeImpl(ChronoLocalDateTimeImpl<D> dateTime, ZoneOffset offset, ZoneId zone) {
        this.dateTime = Objects.requireNonNull(dateTime, "dateTime");
        this.offset = Objects.requireNonNull(offset, "offset");
        this.zone = Objects.requireNonNull(zone, "zone");
    }
    //-----------------------------------------------------------------------
    @Override
    public ZoneOffset getOffset() {
        return offset;
    }
    @Override
    public ChronoZonedDateTime<D> withEarlierOffsetAtOverlap() {
        ZoneOffsetTransition trans = getZone().getRules().getTransition(LocalDateTime.from(this));
        if (trans != null && trans.isOverlap()) {
            ZoneOffset earlierOffset = trans.getOffsetBefore();
            if (earlierOffset.equals(offset) == false) {
                return new ChronoZonedDateTimeImpl<>(dateTime, earlierOffset, zone);
            }
        }
        return this;
    }
    @Override
    public ChronoZonedDateTime<D> withLaterOffsetAtOverlap() {
        ZoneOffsetTransition trans = getZone().getRules().getTransition(LocalDateTime.from(this));
        if (trans != null) {
            ZoneOffset offset = trans.getOffsetAfter();
            if (offset.equals(getOffset()) == false) {
                return new ChronoZonedDateTimeImpl<>(dateTime, offset, zone);
            }
        }
        return this;
    }
    //-----------------------------------------------------------------------
    @Override
    public ChronoLocalDateTime<D> toLocalDateTime() {
        return dateTime;
    }
    @Override
    public ZoneId getZone() {
        return zone;
    }
    @Override
    public ChronoZonedDateTime<D> withZoneSameLocal(ZoneId zone) {
        return ofBest(dateTime, zone, offset);
    }
    @Override
    public ChronoZonedDateTime<D> withZoneSameInstant(ZoneId zone) {
        Objects.requireNonNull(zone, "zone");
        return this.zone.equals(zone) ? this : create(dateTime.toInstant(offset), zone);
    }
    //-----------------------------------------------------------------------
    @Override
    public boolean isSupported(TemporalField field) {
        return field instanceof ChronoField || (field != null && field.isSupportedBy(this));
    }
    //-----------------------------------------------------------------------
    @Override
    public ChronoZonedDateTime<D> with(TemporalField field, long newValue) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            switch (f) {
                case INSTANT_SECONDS: return plus(newValue - toEpochSecond(), SECONDS);
                case OFFSET_SECONDS: {
                    ZoneOffset offset = ZoneOffset.ofTotalSeconds(f.checkValidIntValue(newValue));
                    return create(dateTime.toInstant(offset), zone);
                }
            }
            return ofBest(dateTime.with(field, newValue), zone, offset);
        }
        return ChronoZonedDateTimeImpl.ensureValid(getChronology(), field.adjustInto(this, newValue));
    }
    //-----------------------------------------------------------------------
    @Override
    public ChronoZonedDateTime<D> plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return with(dateTime.plus(amountToAdd, unit));
        }
        return ChronoZonedDateTimeImpl.ensureValid(getChronology(), unit.addTo(this, amountToAdd));   /// TODO: Generics replacement Risk!
    }
    //-----------------------------------------------------------------------
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        Objects.requireNonNull(endExclusive, "endExclusive");
        @SuppressWarnings("unchecked")
        ChronoZonedDateTime<D> end = (ChronoZonedDateTime<D>) getChronology().zonedDateTime(endExclusive);
        if (unit instanceof ChronoUnit) {
            end = end.withZoneSameInstant(offset);
            return dateTime.until(end.toLocalDateTime(), unit);
        }
        Objects.requireNonNull(unit, "unit");
        return unit.between(this, end);
    }
    //-----------------------------------------------------------------------
    private Object writeReplace() {
        return new Ser(Ser.CHRONO_ZONE_DATE_TIME_TYPE, this);
    }
    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }
    void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(dateTime);
        out.writeObject(offset);
        out.writeObject(zone);
    }
    static ChronoZonedDateTime<?> readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ChronoLocalDateTime<?> dateTime = (ChronoLocalDateTime<?>) in.readObject();
        ZoneOffset offset = (ZoneOffset) in.readObject();
        ZoneId zone = (ZoneId) in.readObject();
        return dateTime.atZone(offset).withZoneSameLocal(zone);
        // TODO: ZDT uses ofLenient()
    }
    //-------------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ChronoZonedDateTime) {
            return compareTo((ChronoZonedDateTime<?>) obj) == 0;
        }
        return false;
    }
    @Override
    public int hashCode() {
        return toLocalDateTime().hashCode() ^ getOffset().hashCode() ^ Integer.rotateLeft(getZone().hashCode(), 3);
    }
    @Override
    public String toString() {
        String str = toLocalDateTime().toString() + getOffset().toString();
        if (getOffset() != getZone()) {
            str += '[' + getZone().toString() + ']';
        }
        return str;
    }
}
