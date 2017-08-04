package java.awt;
import java.awt.image.ColorModel;
import sun.awt.AWTAccessor;
import sun.awt.AppContext;
import sun.awt.SunToolkit;
public abstract class GraphicsDevice {
    private Window fullScreenWindow;
    private AppContext fullScreenAppContext; // tracks which AppContext
                                             // created the FS window
    // this lock is used for making synchronous changes to the AppContext's
    // current full screen window
    private final Object fsAppContextLock = new Object();
    private Rectangle windowedModeBounds;
    protected GraphicsDevice() {
    }
    public final static int TYPE_RASTER_SCREEN          = 0;
    public final static int TYPE_PRINTER                = 1;
    public final static int TYPE_IMAGE_BUFFER           = 2;
    public static enum WindowTranslucency {
        PERPIXEL_TRANSPARENT,
        TRANSLUCENT,
        PERPIXEL_TRANSLUCENT;
    }
    public abstract int getType();
    public abstract String getIDstring();
    public abstract GraphicsConfiguration[] getConfigurations();
    public abstract GraphicsConfiguration getDefaultConfiguration();
    public GraphicsConfiguration
           getBestConfiguration(GraphicsConfigTemplate gct) {
        GraphicsConfiguration[] configs = getConfigurations();
        return gct.getBestConfiguration(configs);
    }
    public boolean isFullScreenSupported() {
        return false;
    }
    public void setFullScreenWindow(Window w) {
        if (w != null) {
            if (w.getShape() != null) {
                w.setShape(null);
            }
            if (w.getOpacity() < 1.0f) {
                w.setOpacity(1.0f);
            }
            if (!w.isOpaque()) {
                Color bgColor = w.getBackground();
                bgColor = new Color(bgColor.getRed(), bgColor.getGreen(),
                                    bgColor.getBlue(), 255);
                w.setBackground(bgColor);
            }
            // Check if this window is in fullscreen mode on another device.
            final GraphicsConfiguration gc = w.getGraphicsConfiguration();
            if (gc != null && gc.getDevice() != this
                    && gc.getDevice().getFullScreenWindow() == w) {
                gc.getDevice().setFullScreenWindow(null);
            }
        }
        if (fullScreenWindow != null && windowedModeBounds != null) {
            // if the window went into fs mode before it was realized it may
            // have (0,0) dimensions
            if (windowedModeBounds.width  == 0) windowedModeBounds.width  = 1;
            if (windowedModeBounds.height == 0) windowedModeBounds.height = 1;
            fullScreenWindow.setBounds(windowedModeBounds);
        }
        // Set the full screen window
        synchronized (fsAppContextLock) {
            // Associate fullscreen window with current AppContext
            if (w == null) {
                fullScreenAppContext = null;
            } else {
                fullScreenAppContext = AppContext.getAppContext();
            }
            fullScreenWindow = w;
        }
        if (fullScreenWindow != null) {
            windowedModeBounds = fullScreenWindow.getBounds();
            // Note that we use the graphics configuration of the device,
            // not the window's, because we're setting the fs window for
            // this device.
            final GraphicsConfiguration gc = getDefaultConfiguration();
            final Rectangle screenBounds = gc.getBounds();
            if (SunToolkit.isDispatchThreadForAppContext(fullScreenWindow)) {
                // Update graphics configuration here directly and do not wait
                // asynchronous notification from the peer. Note that
                // setBounds() will reset a GC, if it was set incorrectly.
                fullScreenWindow.setGraphicsConfiguration(gc);
            }
            fullScreenWindow.setBounds(screenBounds.x, screenBounds.y,
                                       screenBounds.width, screenBounds.height);
            fullScreenWindow.setVisible(true);
            fullScreenWindow.toFront();
        }
    }
    public Window getFullScreenWindow() {
        Window returnWindow = null;
        synchronized (fsAppContextLock) {
            // Only return a handle to the current fs window if we are in the
            // same AppContext that set the fs window
            if (fullScreenAppContext == AppContext.getAppContext()) {
                returnWindow = fullScreenWindow;
            }
        }
        return returnWindow;
    }
    public boolean isDisplayChangeSupported() {
        return false;
    }
    public void setDisplayMode(DisplayMode dm) {
        throw new UnsupportedOperationException("Cannot change display mode");
    }
    public DisplayMode getDisplayMode() {
        GraphicsConfiguration gc = getDefaultConfiguration();
        Rectangle r = gc.getBounds();
        ColorModel cm = gc.getColorModel();
        return new DisplayMode(r.width, r.height, cm.getPixelSize(), 0);
    }
    public DisplayMode[] getDisplayModes() {
        return new DisplayMode[] { getDisplayMode() };
    }
    public int getAvailableAcceleratedMemory() {
        return -1;
    }
    public boolean isWindowTranslucencySupported(WindowTranslucency translucencyKind) {
        switch (translucencyKind) {
            case PERPIXEL_TRANSPARENT:
                return isWindowShapingSupported();
            case TRANSLUCENT:
                return isWindowOpacitySupported();
            case PERPIXEL_TRANSLUCENT:
                return isWindowPerpixelTranslucencySupported();
        }
        return false;
    }
    static boolean isWindowShapingSupported() {
        Toolkit curToolkit = Toolkit.getDefaultToolkit();
        if (!(curToolkit instanceof SunToolkit)) {
            return false;
        }
        return ((SunToolkit)curToolkit).isWindowShapingSupported();
    }
    static boolean isWindowOpacitySupported() {
        Toolkit curToolkit = Toolkit.getDefaultToolkit();
        if (!(curToolkit instanceof SunToolkit)) {
            return false;
        }
        return ((SunToolkit)curToolkit).isWindowOpacitySupported();
    }
    boolean isWindowPerpixelTranslucencySupported() {
        Toolkit curToolkit = Toolkit.getDefaultToolkit();
        if (!(curToolkit instanceof SunToolkit)) {
            return false;
        }
        if (!((SunToolkit)curToolkit).isWindowTranslucencySupported()) {
            return false;
        }
        // TODO: cache translucency capable GC
        return getTranslucencyCapableGC() != null;
    }
    GraphicsConfiguration getTranslucencyCapableGC() {
        // If the default GC supports translucency return true.
        // It is important to optimize the verification this way,
        // see CR 6661196 for more details.
        GraphicsConfiguration defaultGC = getDefaultConfiguration();
        if (defaultGC.isTranslucencyCapable()) {
            return defaultGC;
        }
        // ... otherwise iterate through all the GCs.
        GraphicsConfiguration[] configs = getConfigurations();
        for (int j = 0; j < configs.length; j++) {
            if (configs[j].isTranslucencyCapable()) {
                return configs[j];
            }
        }
        return null;
    }
}
