

package java.sql;


public class SQLWarning extends SQLException {


     public SQLWarning(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
        DriverManager.println("SQLWarning: reason(" + reason +
                              ") SQLState(" + SQLState +
                              ") vendor code(" + vendorCode + ")");
    }



    public SQLWarning(String reason, String SQLState) {
        super(reason, SQLState);
        DriverManager.println("SQLWarning: reason(" + reason +
                                  ") SQLState(" + SQLState + ")");
    }


    public SQLWarning(String reason) {
        super(reason);
        DriverManager.println("SQLWarning: reason(" + reason + ")");
    }


    public SQLWarning() {
        super();
        DriverManager.println("SQLWarning: ");
    }


    public SQLWarning(Throwable cause) {
        super(cause);
        DriverManager.println("SQLWarning");
    }


    public SQLWarning(String reason, Throwable cause) {
        super(reason,cause);
        DriverManager.println("SQLWarning : reason("+ reason + ")");
    }


    public SQLWarning(String reason, String SQLState, Throwable cause) {
        super(reason,SQLState,cause);
        DriverManager.println("SQLWarning: reason(" + reason +
                                  ") SQLState(" + SQLState + ")");
    }


    public SQLWarning(String reason, String SQLState, int vendorCode, Throwable cause) {
        super(reason,SQLState,vendorCode,cause);
        DriverManager.println("SQLWarning: reason(" + reason +
                              ") SQLState(" + SQLState +
                              ") vendor code(" + vendorCode + ")");

    }

    public SQLWarning getNextWarning() {
        try {
            return ((SQLWarning)getNextException());
        } catch (ClassCastException ex) {
            // The chained value isn't a SQLWarning.
            // This is a programming error by whoever added it to
            // the SQLWarning chain.  We throw a Java "Error".
            throw new Error("SQLWarning chain holds value that is not a SQLWarning");
        }
    }


    public void setNextWarning(SQLWarning w) {
        setNextException(w);
    }

    private static final long serialVersionUID = 3917336774604784856L;
}
