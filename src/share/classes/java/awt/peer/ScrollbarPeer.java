
package java.awt.peer;

import java.awt.Scrollbar;


public interface ScrollbarPeer extends ComponentPeer {


    void setValues(int value, int visible, int minimum, int maximum);


    void setLineIncrement(int l);


    void setPageIncrement(int l);
}
