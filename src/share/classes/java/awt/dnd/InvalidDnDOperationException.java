package java.awt.dnd;
public class InvalidDnDOperationException extends IllegalStateException {
    private static final long serialVersionUID = -6062568741193956678L;
    static private String dft_msg = "The operation requested cannot be performed by the DnD system since it is not in the appropriate state";
    public InvalidDnDOperationException() { super(dft_msg); }
    public InvalidDnDOperationException(String msg) { super(msg); }
}
