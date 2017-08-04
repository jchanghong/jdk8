package java.awt;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;
import sun.awt.image.SunVolatileImage;
public abstract class GraphicsConfiguration {
    private static BufferCapabilities defaultBufferCaps;
    private static ImageCapabilities defaultImageCaps;
    protected GraphicsConfiguration() {
    }
    public abstract GraphicsDevice getDevice();
    public BufferedImage createCompatibleImage(int width, int height) {
        ColorModel model = getColorModel();
        WritableRaster raster =
            model.createCompatibleWritableRaster(width, height);
        return new BufferedImage(model, raster,
                                 model.isAlphaPremultiplied(), null);
    }
    public BufferedImage createCompatibleImage(int width, int height,
                                               int transparency)
    {
        if (getColorModel().getTransparency() == transparency) {
            return createCompatibleImage(width, height);
        }
        ColorModel cm = getColorModel(transparency);
        if (cm == null) {
            throw new IllegalArgumentException("Unknown transparency: " +
                                               transparency);
        }
        WritableRaster wr = cm.createCompatibleWritableRaster(width, height);
        return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
    }
    public VolatileImage createCompatibleVolatileImage(int width, int height) {
        VolatileImage vi = null;
        try {
            vi = createCompatibleVolatileImage(width, height,
                                               null, Transparency.OPAQUE);
        } catch (AWTException e) {
            // shouldn't happen: we're passing in null caps
            assert false;
        }
        return vi;
    }
    public VolatileImage createCompatibleVolatileImage(int width, int height,
                                                       int transparency)
    {
        VolatileImage vi = null;
        try {
            vi = createCompatibleVolatileImage(width, height, null, transparency);
        } catch (AWTException e) {
            // shouldn't happen: we're passing in null caps
            assert false;
        }
        return vi;
    }
    public VolatileImage createCompatibleVolatileImage(int width, int height,
        ImageCapabilities caps) throws AWTException
    {
        return createCompatibleVolatileImage(width, height, caps,
                                             Transparency.OPAQUE);
    }
    public VolatileImage createCompatibleVolatileImage(int width, int height,
        ImageCapabilities caps, int transparency) throws AWTException
    {
        VolatileImage vi =
            new SunVolatileImage(this, width, height, transparency, caps);
        if (caps != null && caps.isAccelerated() &&
            !vi.getCapabilities().isAccelerated())
        {
            throw new AWTException("Supplied image capabilities could not " +
                                   "be met by this graphics configuration.");
        }
        return vi;
    }
    public abstract ColorModel getColorModel();
    public abstract ColorModel getColorModel(int transparency);
    public abstract AffineTransform getDefaultTransform();
    public abstract AffineTransform getNormalizingTransform();
    public abstract Rectangle getBounds();
    private static class DefaultBufferCapabilities extends BufferCapabilities {
        public DefaultBufferCapabilities(ImageCapabilities imageCaps) {
            super(imageCaps, imageCaps, null);
        }
    }
    public BufferCapabilities getBufferCapabilities() {
        if (defaultBufferCaps == null) {
            defaultBufferCaps = new DefaultBufferCapabilities(
                getImageCapabilities());
        }
        return defaultBufferCaps;
    }
    public ImageCapabilities getImageCapabilities() {
        if (defaultImageCaps == null) {
            defaultImageCaps = new ImageCapabilities(false);
        }
        return defaultImageCaps;
    }
    public boolean isTranslucencyCapable() {
        // Overridden in subclasses
        return false;
    }
}
