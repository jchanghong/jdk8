
package java.awt.peer;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;


public interface CheckboxPeer extends ComponentPeer {


    void setState(boolean state);


    void setCheckboxGroup(CheckboxGroup g);


    void setLabel(String label);

}
