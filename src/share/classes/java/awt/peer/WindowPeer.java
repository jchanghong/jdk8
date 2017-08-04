package java.awt.peer;
import java.awt.*;
public interface WindowPeer extends ContainerPeer {
    void toFront();
    void toBack();
    void updateAlwaysOnTopState();
    void updateFocusableWindowState();
    void setModalBlocked(Dialog blocker, boolean blocked);
    void updateMinimumSize();
    void updateIconImages();
    void setOpacity(float opacity);
    void setOpaque(boolean isOpaque);
    void updateWindow();
    void repositionSecurityWarning();
}
