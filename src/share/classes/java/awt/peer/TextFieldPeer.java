package java.awt.peer;
import java.awt.Dimension;
import java.awt.TextField;
public interface TextFieldPeer extends TextComponentPeer {
    void setEchoChar(char echoChar);
    Dimension getPreferredSize(int columns);
    Dimension getMinimumSize(int columns);
}
