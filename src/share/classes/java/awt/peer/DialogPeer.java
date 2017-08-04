

package java.awt.peer;

import java.awt.*;


public interface DialogPeer extends WindowPeer {


    void setTitle(String title);


    void setResizable(boolean resizeable);


    void blockWindows(java.util.List<Window> windows);
}
