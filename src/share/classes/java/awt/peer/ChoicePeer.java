
package java.awt.peer;

import java.awt.Choice;


public interface ChoicePeer extends ComponentPeer {


    void add(String item, int index);


    void remove(int index);


    void removeAll();


    void select(int index);

}
