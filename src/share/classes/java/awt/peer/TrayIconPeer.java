package java.awt.peer;
import java.awt.SystemTray;
import java.awt.TrayIcon;
public interface TrayIconPeer {
    void dispose();
    void setToolTip(String tooltip);
    void updateImage();
    void displayMessage(String caption, String text, String messageType);
    void showPopupMenu(int x, int y);
}
