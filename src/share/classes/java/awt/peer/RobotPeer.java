

package java.awt.peer;

import java.awt.*;


public interface RobotPeer
{

    void mouseMove(int x, int y);


    void mousePress(int buttons);


    void mouseRelease(int buttons);


    void mouseWheel(int wheelAmt);


    void keyPress(int keycode);


    void keyRelease(int keycode);


    int getRGBPixel(int x, int y);


    int[] getRGBPixels(Rectangle bounds);


    void dispose();
}
