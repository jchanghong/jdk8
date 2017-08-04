

package java.lang;


public class BootstrapMethodError extends LinkageError {
    private static final long serialVersionUID = 292L;


    public BootstrapMethodError() {
        super();
    }


    public BootstrapMethodError(String s) {
        super(s);
    }


    public BootstrapMethodError(String s, Throwable cause) {
        super(s, cause);
    }


    public BootstrapMethodError(Throwable cause) {
        // cf. Throwable(Throwable cause) constructor.
        super(cause == null ? null : cause.toString());
        initCause(cause);
    }
}
