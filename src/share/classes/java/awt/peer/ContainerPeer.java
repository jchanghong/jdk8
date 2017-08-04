package java.awt.peer;
import java.awt.*;
public interface ContainerPeer extends ComponentPeer {
    Insets getInsets();
    void beginValidate();
    void endValidate();
    void beginLayout();
    void endLayout();
}
