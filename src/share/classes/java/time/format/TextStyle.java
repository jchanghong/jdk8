package java.time.format;
import java.util.Calendar;
public enum TextStyle {
    // ordered from large to small
    // ordered so that bit 0 of the ordinal indicates stand-alone.
    FULL(Calendar.LONG_FORMAT, 0),
    FULL_STANDALONE(Calendar.LONG_STANDALONE, 0),
    SHORT(Calendar.SHORT_FORMAT, 1),
    SHORT_STANDALONE(Calendar.SHORT_STANDALONE, 1),
    NARROW(Calendar.NARROW_FORMAT, 1),
    NARROW_STANDALONE(Calendar.NARROW_STANDALONE, 1);
    private final int calendarStyle;
    private final int zoneNameStyleIndex;
    private TextStyle(int calendarStyle, int zoneNameStyleIndex) {
        this.calendarStyle = calendarStyle;
        this.zoneNameStyleIndex = zoneNameStyleIndex;
    }
    public boolean isStandalone() {
        return (ordinal() & 1) == 1;
    }
    public TextStyle asStandalone() {
        return TextStyle.values()[ordinal()  | 1];
    }
    public TextStyle asNormal() {
        return TextStyle.values()[ordinal() & ~1];
    }
    int toCalendarStyle() {
        return calendarStyle;
    }
    int zoneNameStyleIndex() {
        return zoneNameStyleIndex;
    }
}
