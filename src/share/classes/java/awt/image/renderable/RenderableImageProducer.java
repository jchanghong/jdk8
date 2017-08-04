package java.awt.image.renderable;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Enumeration;
import java.util.Vector;
public class RenderableImageProducer implements ImageProducer, Runnable {
    RenderableImage rdblImage;
    RenderContext rc;
    Vector ics = new Vector();
    public RenderableImageProducer(RenderableImage rdblImage,
                                   RenderContext rc) {
        this.rdblImage = rdblImage;
        this.rc = rc;
    }
    public synchronized void setRenderContext(RenderContext rc) {
        this.rc = rc;
    }
    public synchronized void addConsumer(ImageConsumer ic) {
        if (!ics.contains(ic)) {
            ics.addElement(ic);
        }
    }
    public synchronized boolean isConsumer(ImageConsumer ic) {
        return ics.contains(ic);
    }
    public synchronized void removeConsumer(ImageConsumer ic) {
        ics.removeElement(ic);
    }
    public synchronized void startProduction(ImageConsumer ic) {
        addConsumer(ic);
        // Need to build a runnable object for the Thread.
        Thread thread = new Thread(this, "RenderableImageProducer Thread");
        thread.start();
    }
    public void requestTopDownLeftRightResend(ImageConsumer ic) {
        // So far, all pixels are already sent in TDLR order
    }
    public void run() {
        // First get the rendered image
        RenderedImage rdrdImage;
        if (rc != null) {
            rdrdImage = rdblImage.createRendering(rc);
        } else {
            rdrdImage = rdblImage.createDefaultRendering();
        }
        // And its ColorModel
        ColorModel colorModel = rdrdImage.getColorModel();
        Raster raster = rdrdImage.getData();
        SampleModel sampleModel = raster.getSampleModel();
        DataBuffer dataBuffer = raster.getDataBuffer();
        if (colorModel == null) {
            colorModel = ColorModel.getRGBdefault();
        }
        int minX = raster.getMinX();
        int minY = raster.getMinY();
        int width = raster.getWidth();
        int height = raster.getHeight();
        Enumeration icList;
        ImageConsumer ic;
        // Set up the ImageConsumers
        icList = ics.elements();
        while (icList.hasMoreElements()) {
            ic = (ImageConsumer)icList.nextElement();
            ic.setDimensions(width,height);
            ic.setHints(ImageConsumer.TOPDOWNLEFTRIGHT |
                        ImageConsumer.COMPLETESCANLINES |
                        ImageConsumer.SINGLEPASS |
                        ImageConsumer.SINGLEFRAME);
        }
        // Get RGB pixels from the raster scanline by scanline and
        // send to consumers.
        int pix[] = new int[width];
        int i,j;
        int numBands = sampleModel.getNumBands();
        int tmpPixel[] = new int[numBands];
        for (j = 0; j < height; j++) {
            for(i = 0; i < width; i++) {
                sampleModel.getPixel(i, j, tmpPixel, dataBuffer);
                pix[i] = colorModel.getDataElement(tmpPixel, 0);
            }
            // Now send the scanline to the Consumers
            icList = ics.elements();
            while (icList.hasMoreElements()) {
                ic = (ImageConsumer)icList.nextElement();
                ic.setPixels(0, j, width, 1, colorModel, pix, 0, width);
            }
        }
        // Now tell the consumers we're done.
        icList = ics.elements();
        while (icList.hasMoreElements()) {
            ic = (ImageConsumer)icList.nextElement();
            ic.imageComplete(ImageConsumer.STATICIMAGEDONE);
        }
    }
}
