
package java.awt.peer;

import java.awt.TextComponent;
import java.awt.im.InputMethodRequests;


public interface TextComponentPeer extends ComponentPeer {


    void setEditable(boolean editable);


    String getText();


    void setText(String text);


    int getSelectionStart();


    int getSelectionEnd();


    void select(int selStart, int selEnd);


    void setCaretPosition(int pos);


    int getCaretPosition();


    InputMethodRequests getInputMethodRequests();
}
