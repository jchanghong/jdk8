

package java.rmi.activation;


public class ActivationException extends Exception {


    public Throwable detail;


    private static final long serialVersionUID = -4320118837291406071L;


    public ActivationException() {
        initCause(null);  // Disallow subsequent initCause
    }


    public ActivationException(String s) {
        super(s);
        initCause(null);  // Disallow subsequent initCause
    }


    public ActivationException(String s, Throwable cause) {
        super(s);
        initCause(null);  // Disallow subsequent initCause
        detail = cause;
    }


    public String getMessage() {
        if (detail == null)
            return super.getMessage();
        else
            return super.getMessage() +
                "; nested exception is: \n\t" +
                detail.toString();
    }


    public Throwable getCause() {
        return detail;
    }
}
