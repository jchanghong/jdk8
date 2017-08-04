package java.awt.peer;
import java.awt.Label;
public interface LabelPeer extends ComponentPeer {
    void setText(String label);
    void setAlignment(int alignment);
}
