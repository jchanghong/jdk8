package java.io;
public class OptionalDataException extends ObjectStreamException {
    private static final long serialVersionUID = -8011121865681257820L;
    OptionalDataException(int len) {
        eof = false;
        length = len;
    }
    OptionalDataException(boolean end) {
        length = 0;
        eof = end;
    }
    public int length;
    public boolean eof;
}
