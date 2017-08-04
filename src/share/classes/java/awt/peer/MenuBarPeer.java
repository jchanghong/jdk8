
package java.awt.peer;

import java.awt.Menu;
import java.awt.MenuBar;


public interface MenuBarPeer extends MenuComponentPeer {


    void addMenu(Menu m);


    void delMenu(int index);


    void addHelpMenu(Menu m);
}
