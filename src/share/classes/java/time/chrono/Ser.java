package java.time.chrono;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
final class Ser implements Externalizable {
    private static final long serialVersionUID = -6103370247208168577L;
    static final byte CHRONO_TYPE = 1;
    static final byte CHRONO_LOCAL_DATE_TIME_TYPE = 2;
    static final byte CHRONO_ZONE_DATE_TIME_TYPE = 3;
    static final byte JAPANESE_DATE_TYPE = 4;
    static final byte JAPANESE_ERA_TYPE = 5;
    static final byte HIJRAH_DATE_TYPE = 6;
    static final byte MINGUO_DATE_TYPE = 7;
    static final byte THAIBUDDHIST_DATE_TYPE = 8;
    static final byte CHRONO_PERIOD_TYPE = 9;
    private byte type;
    private Object object;
    public Ser() {
    }
    Ser(byte type, Object object) {
        this.type = type;
        this.object = object;
    }
    //-----------------------------------------------------------------------
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        writeInternal(type, object, out);
    }
    private static void writeInternal(byte type, Object object, ObjectOutput out) throws IOException {
        out.writeByte(type);
        switch (type) {
            case CHRONO_TYPE:
                ((AbstractChronology) object).writeExternal(out);
                break;
            case CHRONO_LOCAL_DATE_TIME_TYPE:
                ((ChronoLocalDateTimeImpl<?>) object).writeExternal(out);
                break;
            case CHRONO_ZONE_DATE_TIME_TYPE:
                ((ChronoZonedDateTimeImpl<?>) object).writeExternal(out);
                break;
            case JAPANESE_DATE_TYPE:
                ((JapaneseDate) object).writeExternal(out);
                break;
            case JAPANESE_ERA_TYPE:
                ((JapaneseEra) object).writeExternal(out);
                break;
            case HIJRAH_DATE_TYPE:
                ((HijrahDate) object).writeExternal(out);
                break;
            case MINGUO_DATE_TYPE:
                ((MinguoDate) object).writeExternal(out);
                break;
            case THAIBUDDHIST_DATE_TYPE:
                ((ThaiBuddhistDate) object).writeExternal(out);
                break;
            case CHRONO_PERIOD_TYPE:
                ((ChronoPeriodImpl) object).writeExternal(out);
                break;
            default:
                throw new InvalidClassException("Unknown serialized type");
        }
    }
    //-----------------------------------------------------------------------
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        type = in.readByte();
        object = readInternal(type, in);
    }
    static Object read(ObjectInput in) throws IOException, ClassNotFoundException {
        byte type = in.readByte();
        return readInternal(type, in);
    }
    private static Object readInternal(byte type, ObjectInput in) throws IOException, ClassNotFoundException {
        switch (type) {
            case CHRONO_TYPE: return AbstractChronology.readExternal(in);
            case CHRONO_LOCAL_DATE_TIME_TYPE: return ChronoLocalDateTimeImpl.readExternal(in);
            case CHRONO_ZONE_DATE_TIME_TYPE: return ChronoZonedDateTimeImpl.readExternal(in);
            case JAPANESE_DATE_TYPE:  return JapaneseDate.readExternal(in);
            case JAPANESE_ERA_TYPE: return JapaneseEra.readExternal(in);
            case HIJRAH_DATE_TYPE: return HijrahDate.readExternal(in);
            case MINGUO_DATE_TYPE: return MinguoDate.readExternal(in);
            case THAIBUDDHIST_DATE_TYPE: return ThaiBuddhistDate.readExternal(in);
            case CHRONO_PERIOD_TYPE: return ChronoPeriodImpl.readExternal(in);
            default: throw new StreamCorruptedException("Unknown serialized type");
        }
    }
    private Object readResolve() {
         return object;
    }
}
