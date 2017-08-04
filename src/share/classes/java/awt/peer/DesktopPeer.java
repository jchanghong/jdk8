package java.awt.peer;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.awt.Desktop.Action;
public interface DesktopPeer {
    boolean isSupported(Action action);
    void open(File file) throws IOException;
    void edit(File file) throws IOException;
    void print(File file) throws IOException;
    void mail(URI mailtoURL) throws IOException;
    void browse(URI uri) throws IOException;
}
