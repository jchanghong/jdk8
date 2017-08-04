

package java.lang.reflect;


public class MalformedParametersException extends RuntimeException {


    private static final long serialVersionUID = 20130919L;


    public MalformedParametersException() {}


    public MalformedParametersException(String reason) {
        super(reason);
    }
}
