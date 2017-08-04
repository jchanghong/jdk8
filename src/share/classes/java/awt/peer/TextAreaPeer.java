
package java.awt.peer;

import java.awt.Dimension;
import java.awt.TextArea;


public interface TextAreaPeer extends TextComponentPeer {


    void insert(String text, int pos);


    void replaceRange(String text, int start, int end);


    Dimension getPreferredSize(int rows, int columns);


    Dimension getMinimumSize(int rows, int columns);

}
