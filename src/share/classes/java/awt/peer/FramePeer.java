

package java.awt.peer;

import java.awt.*;

import sun.awt.EmbeddedFrame;


public interface FramePeer extends WindowPeer {


    void setTitle(String title);


    void setMenuBar(MenuBar mb);


    void setResizable(boolean resizeable);


    void setState(int state);


    int getState();


    void setMaximizedBounds(Rectangle bounds);


    // TODO: This is only used in EmbeddedFrame, and should probably be moved
    // into an EmbeddedFramePeer which would extend FramePeer
    void setBoundsPrivate(int x, int y, int width, int height);


    // TODO: This is only used in EmbeddedFrame, and should probably be moved
    // into an EmbeddedFramePeer which would extend FramePeer
    Rectangle getBoundsPrivate();


    void emulateActivation(boolean activate);
}
