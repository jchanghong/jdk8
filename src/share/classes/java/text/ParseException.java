package java.text;
public
class ParseException extends Exception {
    private static final long serialVersionUID = 2703218443322787634L;
    public ParseException(String s, int errorOffset) {
        super(s);
        this.errorOffset = errorOffset;
    }
    public int getErrorOffset () {
        return errorOffset;
    }
    //============ privates ============
    private int errorOffset;
}
