

package java.awt.peer;

import java.awt.FileDialog;
import java.io.FilenameFilter;


public interface FileDialogPeer extends DialogPeer {


    void setFile(String file);


    void setDirectory(String dir);


    void setFilenameFilter(FilenameFilter filter);
}
