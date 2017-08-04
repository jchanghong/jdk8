
package java.awt;

import java.io.IOException;
import java.awt.image.*;
import java.net.URL;
import java.net.URLConnection;
import java.io.File;
import sun.util.logging.PlatformLogger;
import sun.awt.image.SunWritableRaster;


public final class SplashScreen {

    SplashScreen(long ptr) { // non-public constructor
        splashPtr = ptr;
    }


    public static  SplashScreen getSplashScreen() {
        synchronized (SplashScreen.class) {
            if (GraphicsEnvironment.isHeadless()) {
                throw new HeadlessException();
            }
            // SplashScreen class is now a singleton
            if (!wasClosed && theInstance == null) {
                java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Void>() {
                        public Void run() {
                            System.loadLibrary("splashscreen");
                            return null;
                        }
                    });
                long ptr = _getInstance();
                if (ptr != 0 && _isVisible(ptr)) {
                    theInstance = new SplashScreen(ptr);
                }
            }
            return theInstance;
        }
    }


    public void setImageURL(URL imageURL) throws NullPointerException, IOException, IllegalStateException {
        checkVisible();
        URLConnection connection = imageURL.openConnection();
        connection.connect();
        int length = connection.getContentLength();
        java.io.InputStream stream = connection.getInputStream();
        byte[] buf = new byte[length];
        int off = 0;
        while(true) {
            // check for available data
            int available = stream.available();
            if (available <= 0) {
                // no data available... well, let's try reading one byte
                // we'll see what happens then
                available = 1;
            }
            // check for enough room in buffer, realloc if needed
            // the buffer always grows in size 2x minimum
            if (off + available > length) {
                length = off*2;
                if (off + available > length) {
                    length = available+off;
                }
                byte[] oldBuf = buf;
                buf = new byte[length];
                System.arraycopy(oldBuf, 0, buf, 0, off);
            }
            // now read the data
            int result = stream.read(buf, off, available);
            if (result < 0) {
                break;
            }
            off += result;
        }
        synchronized(SplashScreen.class) {
            checkVisible();
            if (!_setImageData(splashPtr, buf)) {
                throw new IOException("Bad image format or i/o error when loading image");
            }
            this.imageURL = imageURL;
        }
    }

    private void checkVisible() {
        if (!isVisible()) {
            throw new IllegalStateException("no splash screen available");
        }
    }

    public URL getImageURL() throws IllegalStateException {
        synchronized (SplashScreen.class) {
            checkVisible();
            if (imageURL == null) {
                try {
                    String fileName = _getImageFileName(splashPtr);
                    String jarName = _getImageJarName(splashPtr);
                    if (fileName != null) {
                        if (jarName != null) {
                            imageURL = new URL("jar:"+(new File(jarName).toURL().toString())+"!/"+fileName);
                        } else {
                            imageURL = new File(fileName).toURL();
                        }
                    }
                }
                catch(java.net.MalformedURLException e) {
                    if (log.isLoggable(PlatformLogger.Level.FINE)) {
                        log.fine("MalformedURLException caught in the getImageURL() method", e);
                    }
                }
            }
            return imageURL;
        }
    }


    public Rectangle getBounds() throws IllegalStateException {
        synchronized (SplashScreen.class) {
            checkVisible();
            float scale = _getScaleFactor(splashPtr);
            Rectangle bounds = _getBounds(splashPtr);
            assert scale > 0;
            if (scale > 0 && scale != 1) {
                bounds.setSize((int) (bounds.getWidth() / scale),
                        (int) (bounds.getHeight() / scale));
            }
            return bounds;
        }
    }


    public Dimension getSize() throws IllegalStateException {
        return getBounds().getSize();
    }


    public Graphics2D createGraphics() throws IllegalStateException {
        synchronized (SplashScreen.class) {
            checkVisible();
            if (image==null) {
                // get unscaled splash image size
                Dimension dim = _getBounds(splashPtr).getSize();
                image = new BufferedImage(dim.width, dim.height,
                        BufferedImage.TYPE_INT_ARGB);
            }
            float scale = _getScaleFactor(splashPtr);
            Graphics2D g = image.createGraphics();
            assert (scale > 0);
            if (scale <= 0) {
                scale = 1;
            }
            g.scale(scale, scale);
            return g;
        }
    }


    public void update() throws IllegalStateException {
        BufferedImage image;
        synchronized (SplashScreen.class) {
            checkVisible();
            image = this.image;
        }
        if (image == null) {
            throw new IllegalStateException("no overlay image available");
        }
        DataBuffer buf = image.getRaster().getDataBuffer();
        if (!(buf instanceof DataBufferInt)) {
            throw new AssertionError("Overlay image DataBuffer is of invalid type == "+buf.getClass().getName());
        }
        int numBanks = buf.getNumBanks();
        if (numBanks!=1) {
            throw new AssertionError("Invalid number of banks =="+numBanks+" in overlay image DataBuffer");
        }
        if (!(image.getSampleModel() instanceof SinglePixelPackedSampleModel)) {
            throw new AssertionError("Overlay image has invalid sample model == "+image.getSampleModel().getClass().getName());
        }
        SinglePixelPackedSampleModel sm = (SinglePixelPackedSampleModel)image.getSampleModel();
        int scanlineStride = sm.getScanlineStride();
        Rectangle rect = image.getRaster().getBounds();
        // Note that we steal the data array here, but just for reading
        // so we do not need to mark the DataBuffer dirty...
        int[] data = SunWritableRaster.stealData((DataBufferInt)buf, 0);
        synchronized(SplashScreen.class) {
            checkVisible();
            _update(splashPtr, data, rect.x, rect.y, rect.width, rect.height, scanlineStride);
        }
    }


    public void close() throws IllegalStateException {
        synchronized (SplashScreen.class) {
            checkVisible();
            _close(splashPtr);
            image = null;
            SplashScreen.markClosed();
        }
    }

    static void markClosed() {
        synchronized (SplashScreen.class) {
            wasClosed = true;
            theInstance = null;
        }
    }



    public boolean isVisible() {
        synchronized (SplashScreen.class) {
            return !wasClosed && _isVisible(splashPtr);
        }
    }

    private BufferedImage image; // overlay image

    private final long splashPtr; // pointer to native Splash structure
    private static boolean wasClosed = false;

    private URL imageURL;


    private static SplashScreen theInstance = null;

    private static final PlatformLogger log = PlatformLogger.getLogger("java.awt.SplashScreen");

    private native static void _update(long splashPtr, int[] data, int x, int y, int width, int height, int scanlineStride);
    private native static boolean _isVisible(long splashPtr);
    private native static Rectangle _getBounds(long splashPtr);
    private native static long _getInstance();
    private native static void _close(long splashPtr);
    private native static String _getImageFileName(long splashPtr);
    private native static String _getImageJarName(long SplashPtr);
    private native static boolean _setImageData(long SplashPtr, byte[] data);
    private native static float _getScaleFactor(long SplashPtr);

}
