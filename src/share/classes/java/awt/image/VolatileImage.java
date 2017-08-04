package java.awt.image;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.Toolkit;
import java.awt.Transparency;
public abstract class VolatileImage extends Image implements Transparency
{
    // Return codes for validate() method
    public static final int IMAGE_OK = 0;
    public static final int IMAGE_RESTORED = 1;
    public static final int IMAGE_INCOMPATIBLE = 2;
    public abstract BufferedImage getSnapshot();
    public abstract int getWidth();
    public abstract int getHeight();
    // Image overrides
    public ImageProducer getSource() {
        // REMIND: Make sure this functionality is in line with the
        // spec.  In particular, we are returning the Source for a
        // static image (the snapshot), not a changing image (the
        // VolatileImage).  So if the user expects the Source to be
        // up-to-date with the current contents of the VolatileImage,
        // they will be disappointed...
        // REMIND: This assumes that getSnapshot() returns something
        // valid and not the default null object returned by this class
        // (so it assumes that the actual VolatileImage object is
        // subclassed off something that does the right thing
        // (e.g., SunVolatileImage).
        return getSnapshot().getSource();
    }
    // REMIND: if we want any decent performance for getScaledInstance(),
    // we should override the Image implementation of it...
    public Graphics getGraphics() {
        return createGraphics();
    }
    public abstract Graphics2D createGraphics();
    // Volatile management methods
    public abstract int validate(GraphicsConfiguration gc);
    public abstract boolean contentsLost();
    public abstract ImageCapabilities getCapabilities();
    protected int transparency = TRANSLUCENT;
    public int getTransparency() {
        return transparency;
    }
}
