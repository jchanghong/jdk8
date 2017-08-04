package java.rmi.server;
@Deprecated
public class Operation {
    private String operation;
    @Deprecated
    public Operation(String op) {
        operation = op;
    }
    @Deprecated
    public String getOperation() {
        return operation;
    }
    @Deprecated
    public String toString() {
        return operation;
    }
}
