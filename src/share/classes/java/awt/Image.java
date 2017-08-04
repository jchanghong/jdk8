
package java.awt;

import java.awt.image.ImageProducer;
import java.awt.image.ImageObserver;
import java.awt.image.ImageFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.ReplicateScaleFilter;

import sun.awt.image.SurfaceManager;



public abstract class Image {


    private static ImageCapabilities defaultImageCaps =
        new ImageCapabilities(false);


    protected float accelerationPriority = .5f;


    public abstract int getWidth(ImageObserver observer);


    public abstract int getHeight(ImageObserver observer);


    public abstract ImageProducer getSource();


    public abstract Graphics getGraphics();


    public abstract Object getProperty(String name, ImageObserver observer);


    public static final Object UndefinedProperty = new Object();


    public Image getScaledInstance(int width, int height, int hints) {
        ImageFilter filter;
        if ((hints & (SCALE_SMOOTH | SCALE_AREA_AVERAGING)) != 0) {
            filter = new AreaAveragingScaleFilter(width, height);
        } else {
            filter = new ReplicateScaleFilter(width, height);
        }
        ImageProducer prod;
        prod = new FilteredImageSource(getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(prod);
    }


    public static final int SCALE_DEFAULT = 1;


    public static final int SCALE_FAST = 2;


    public static final int SCALE_SMOOTH = 4;


    public static final int SCALE_REPLICATE = 8;


    public static final int SCALE_AREA_AVERAGING = 16;


    public void flush() {
        if (surfaceManager != null) {
            surfaceManager.flush();
        }
    }


    public ImageCapabilities getCapabilities(GraphicsConfiguration gc) {
        if (surfaceManager != null) {
            return surfaceManager.getCapabilities(gc);
        }
        // Note: this is just a default object that gets returned in the
        // absence of any more specific information from a surfaceManager.
        // Subclasses of Image should either override this method or
        // make sure that they always have a non-null SurfaceManager
        // to return an ImageCapabilities object that is appropriate
        // for their given subclass type.
        return defaultImageCaps;
    }


    public void setAccelerationPriority(float priority) {
        if (priority < 0 || priority > 1) {
            throw new IllegalArgumentException("Priority must be a value " +
                                               "between 0 and 1, inclusive");
        }
        accelerationPriority = priority;
        if (surfaceManager != null) {
            surfaceManager.setAccelerationPriority(accelerationPriority);
        }
    }


    public float getAccelerationPriority() {
        return accelerationPriority;
    }

    SurfaceManager surfaceManager;

    static {
        SurfaceManager.setImageAccessor(new SurfaceManager.ImageAccessor() {
            public SurfaceManager getSurfaceManager(Image img) {
                return img.surfaceManager;
            }
            public void setSurfaceManager(Image img, SurfaceManager mgr) {
                img.surfaceManager = mgr;
            }
        });
    }
}
