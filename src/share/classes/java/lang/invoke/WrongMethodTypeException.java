

package java.lang.invoke;


public class WrongMethodTypeException extends RuntimeException {
    private static final long serialVersionUID = 292L;


    public WrongMethodTypeException() {
        super();
    }


    public WrongMethodTypeException(String s) {
        super(s);
    }


    //FIXME: make this public in MR1
     WrongMethodTypeException(String s, Throwable cause) {
        super(s, cause);
    }


    //FIXME: make this public in MR1
     WrongMethodTypeException(Throwable cause) {
        super(cause);
    }
}
