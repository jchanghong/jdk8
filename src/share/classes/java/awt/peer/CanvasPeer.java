
package java.awt.peer;

import java.awt.Canvas;
import java.awt.GraphicsConfiguration;


public interface CanvasPeer extends ComponentPeer {

    GraphicsConfiguration getAppropriateGraphicsConfiguration(
            GraphicsConfiguration gc);
}
