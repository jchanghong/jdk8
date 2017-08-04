package java.time.format;
public enum SignStyle {
    NORMAL,
    ALWAYS,
    NEVER,
    NOT_NEGATIVE,
    EXCEEDS_PAD;
    boolean parse(boolean positive, boolean strict, boolean fixedWidth) {
        switch (ordinal()) {
            case 0: // NORMAL
                // valid if negative or (positive and lenient)
                return !positive || !strict;
            case 1: // ALWAYS
            case 4: // EXCEEDS_PAD
                return true;
            default:
                // valid if lenient and not fixed width
                return !strict && !fixedWidth;
        }
    }
}
