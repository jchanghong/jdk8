

package java.awt.image;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import sun.java2d.cmm.CMSManager;
import sun.java2d.cmm.ColorTransform;
import sun.java2d.cmm.PCMM;
import java.awt.Toolkit;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;


public abstract class ColorModel implements Transparency{
    private long pData;         // Placeholder for data for native functions


    protected int pixel_bits;
    int nBits[];
    int transparency = Transparency.TRANSLUCENT;
    boolean supportsAlpha = true;
    boolean isAlphaPremultiplied = false;
    int numComponents = -1;
    int numColorComponents = -1;
    ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    int colorSpaceType = ColorSpace.TYPE_RGB;
    int maxBits;
    boolean is_sRGB = true;


    protected int transferType;


    private static boolean loaded = false;
    static void loadLibraries() {
        if (!loaded) {
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Void>() {
                    public Void run() {
                        System.loadLibrary("awt");
                        return null;
                    }
                });
            loaded = true;
        }
    }
    private static native void initIDs();
    static {

        loadLibraries();
        initIDs();
    }
    private static ColorModel RGBdefault;


    public static ColorModel getRGBdefault() {
        if (RGBdefault == null) {
            RGBdefault = new DirectColorModel(32,
                                              0x00ff0000,       // Red
                                              0x0000ff00,       // Green
                                              0x000000ff,       // Blue
                                              0xff000000        // Alpha
                                              );
        }
        return RGBdefault;
    }


    public ColorModel(int bits) {
        pixel_bits = bits;
        if (bits < 1) {
            throw new IllegalArgumentException("Number of bits must be > 0");
        }
        numComponents = 4;
        numColorComponents = 3;
        maxBits = bits;
        // REMIND: make sure transferType is set correctly
        transferType = ColorModel.getDefaultTransferType(bits);
    }


    protected ColorModel(int pixel_bits, int[] bits, ColorSpace cspace,
                         boolean hasAlpha,
                         boolean isAlphaPremultiplied,
                         int transparency,
                         int transferType) {
        colorSpace                = cspace;
        colorSpaceType            = cspace.getType();
        numColorComponents        = cspace.getNumComponents();
        numComponents             = numColorComponents + (hasAlpha ? 1 : 0);
        supportsAlpha             = hasAlpha;
        if (bits.length < numComponents) {
            throw new IllegalArgumentException("Number of color/alpha "+
                                               "components should be "+
                                               numComponents+
                                               " but length of bits array is "+
                                               bits.length);
        }

        // 4186669
        if (transparency < Transparency.OPAQUE ||
            transparency > Transparency.TRANSLUCENT)
        {
            throw new IllegalArgumentException("Unknown transparency: "+
                                               transparency);
        }

        if (supportsAlpha == false) {
            this.isAlphaPremultiplied = false;
            this.transparency = Transparency.OPAQUE;
        }
        else {
            this.isAlphaPremultiplied = isAlphaPremultiplied;
            this.transparency         = transparency;
        }

        nBits = bits.clone();
        this.pixel_bits = pixel_bits;
        if (pixel_bits <= 0) {
            throw new IllegalArgumentException("Number of pixel bits must "+
                                               "be > 0");
        }
        // Check for bits < 0
        maxBits = 0;
        for (int i=0; i < bits.length; i++) {
            // bug 4304697
            if (bits[i] < 0) {
                throw new
                    IllegalArgumentException("Number of bits must be >= 0");
            }
            if (maxBits < bits[i]) {
                maxBits = bits[i];
            }
        }

        // Make sure that we don't have all 0-bit components
        if (maxBits == 0) {
            throw new IllegalArgumentException("There must be at least "+
                                               "one component with > 0 "+
                                              "pixel bits.");
        }

        // Save this since we always need to check if it is the default CS
        if (cspace != ColorSpace.getInstance(ColorSpace.CS_sRGB)) {
            is_sRGB = false;
        }

        // Save the transfer type
        this.transferType = transferType;
    }


    final public boolean hasAlpha() {
        return supportsAlpha;
    }


    final public boolean isAlphaPremultiplied() {
        return isAlphaPremultiplied;
    }


    final public int getTransferType() {
        return transferType;
    }


    public int getPixelSize() {
        return pixel_bits;
    }


    public int getComponentSize(int componentIdx) {
        // REMIND:
        if (nBits == null) {
            throw new NullPointerException("Number of bits array is null.");
        }

        return nBits[componentIdx];
    }


    public int[] getComponentSize() {
        if (nBits != null) {
            return nBits.clone();
        }

        return null;
    }


    public int getTransparency() {
        return transparency;
    }


    public int getNumComponents() {
        return numComponents;
    }


    public int getNumColorComponents() {
        return numColorComponents;
    }


    public abstract int getRed(int pixel);


    public abstract int getGreen(int pixel);


    public abstract int getBlue(int pixel);


    public abstract int getAlpha(int pixel);


    public int getRGB(int pixel) {
        return (getAlpha(pixel) << 24)
            | (getRed(pixel) << 16)
            | (getGreen(pixel) << 8)
            | (getBlue(pixel) << 0);
    }


    public int getRed(Object inData) {
        int pixel=0,length=0;
        switch (transferType) {
            case DataBuffer.TYPE_BYTE:
               byte bdata[] = (byte[])inData;
               pixel = bdata[0] & 0xff;
               length = bdata.length;
            break;
            case DataBuffer.TYPE_USHORT:
               short sdata[] = (short[])inData;
               pixel = sdata[0] & 0xffff;
               length = sdata.length;
            break;
            case DataBuffer.TYPE_INT:
               int idata[] = (int[])inData;
               pixel = idata[0];
               length = idata.length;
            break;
            default:
               throw new UnsupportedOperationException("This method has not been "+
                   "implemented for transferType " + transferType);
        }
        if (length == 1) {
            return getRed(pixel);
        }
        else {
            throw new UnsupportedOperationException
                ("This method is not supported by this color model");
        }
    }


    public int getGreen(Object inData) {
        int pixel=0,length=0;
        switch (transferType) {
            case DataBuffer.TYPE_BYTE:
               byte bdata[] = (byte[])inData;
               pixel = bdata[0] & 0xff;
               length = bdata.length;
            break;
            case DataBuffer.TYPE_USHORT:
               short sdata[] = (short[])inData;
               pixel = sdata[0] & 0xffff;
               length = sdata.length;
            break;
            case DataBuffer.TYPE_INT:
               int idata[] = (int[])inData;
               pixel = idata[0];
               length = idata.length;
            break;
            default:
               throw new UnsupportedOperationException("This method has not been "+
                   "implemented for transferType " + transferType);
        }
        if (length == 1) {
            return getGreen(pixel);
        }
        else {
            throw new UnsupportedOperationException
                ("This method is not supported by this color model");
        }
    }


    public int getBlue(Object inData) {
        int pixel=0,length=0;
        switch (transferType) {
            case DataBuffer.TYPE_BYTE:
               byte bdata[] = (byte[])inData;
               pixel = bdata[0] & 0xff;
               length = bdata.length;
            break;
            case DataBuffer.TYPE_USHORT:
               short sdata[] = (short[])inData;
               pixel = sdata[0] & 0xffff;
               length = sdata.length;
            break;
            case DataBuffer.TYPE_INT:
               int idata[] = (int[])inData;
               pixel = idata[0];
               length = idata.length;
            break;
            default:
               throw new UnsupportedOperationException("This method has not been "+
                   "implemented for transferType " + transferType);
        }
        if (length == 1) {
            return getBlue(pixel);
        }
        else {
            throw new UnsupportedOperationException
                ("This method is not supported by this color model");
        }
    }


    public int getAlpha(Object inData) {
        int pixel=0,length=0;
        switch (transferType) {
            case DataBuffer.TYPE_BYTE:
               byte bdata[] = (byte[])inData;
               pixel = bdata[0] & 0xff;
               length = bdata.length;
            break;
            case DataBuffer.TYPE_USHORT:
               short sdata[] = (short[])inData;
               pixel = sdata[0] & 0xffff;
               length = sdata.length;
            break;
            case DataBuffer.TYPE_INT:
               int idata[] = (int[])inData;
               pixel = idata[0];
               length = idata.length;
            break;
            default:
               throw new UnsupportedOperationException("This method has not been "+
                   "implemented for transferType " + transferType);
        }
        if (length == 1) {
            return getAlpha(pixel);
        }
        else {
            throw new UnsupportedOperationException
                ("This method is not supported by this color model");
        }
    }


    public int getRGB(Object inData) {
        return (getAlpha(inData) << 24)
            | (getRed(inData) << 16)
            | (getGreen(inData) << 8)
            | (getBlue(inData) << 0);
    }


    public Object getDataElements(int rgb, Object pixel) {
        throw new UnsupportedOperationException
            ("This method is not supported by this color model.");
    }


    public int[] getComponents(int pixel, int[] components, int offset) {
        throw new UnsupportedOperationException
            ("This method is not supported by this color model.");
    }


    public int[] getComponents(Object pixel, int[] components, int offset) {
        throw new UnsupportedOperationException
            ("This method is not supported by this color model.");
    }


    public int[] getUnnormalizedComponents(float[] normComponents,
                                           int normOffset,
                                           int[] components, int offset) {
        // Make sure that someone isn't using a custom color model
        // that called the super(bits) constructor.
        if (colorSpace == null) {
            throw new UnsupportedOperationException("This method is not supported "+
                                        "by this color model.");
        }

        if (nBits == null) {
            throw new UnsupportedOperationException ("This method is not supported.  "+
                                         "Unable to determine #bits per "+
                                         "component.");
        }
        if ((normComponents.length - normOffset) < numComponents) {
            throw new
                IllegalArgumentException(
                        "Incorrect number of components.  Expecting "+
                        numComponents);
        }

        if (components == null) {
            components = new int[offset+numComponents];
        }

        if (supportsAlpha && isAlphaPremultiplied) {
            float normAlpha = normComponents[normOffset+numColorComponents];
            for (int i=0; i < numColorComponents; i++) {
                components[offset+i] = (int) (normComponents[normOffset+i]
                                              * ((1<<nBits[i]) - 1)
                                              * normAlpha + 0.5f);
            }
            components[offset+numColorComponents] = (int)
                (normAlpha * ((1<<nBits[numColorComponents]) - 1) + 0.5f);
        }
        else {
            for (int i=0; i < numComponents; i++) {
                components[offset+i] = (int) (normComponents[normOffset+i]
                                              * ((1<<nBits[i]) - 1) + 0.5f);
            }
        }

        return components;
    }


    public float[] getNormalizedComponents(int[] components, int offset,
                                           float[] normComponents,
                                           int normOffset) {
        // Make sure that someone isn't using a custom color model
        // that called the super(bits) constructor.
        if (colorSpace == null) {
            throw new UnsupportedOperationException("This method is not supported by "+
                                        "this color model.");
        }
        if (nBits == null) {
            throw new UnsupportedOperationException ("This method is not supported.  "+
                                         "Unable to determine #bits per "+
                                         "component.");
        }

        if ((components.length - offset) < numComponents) {
            throw new
                IllegalArgumentException(
                        "Incorrect number of components.  Expecting "+
                        numComponents);
        }

        if (normComponents == null) {
            normComponents = new float[numComponents+normOffset];
        }

        if (supportsAlpha && isAlphaPremultiplied) {
            // Normalized coordinates are non premultiplied
            float normAlpha = (float)components[offset+numColorComponents];
            normAlpha /= (float) ((1<<nBits[numColorComponents]) - 1);
            if (normAlpha != 0.0f) {
                for (int i=0; i < numColorComponents; i++) {
                    normComponents[normOffset+i] =
                        ((float) components[offset+i]) /
                        (normAlpha * ((float) ((1<<nBits[i]) - 1)));
                }
            } else {
                for (int i=0; i < numColorComponents; i++) {
                    normComponents[normOffset+i] = 0.0f;
                }
            }
            normComponents[normOffset+numColorComponents] = normAlpha;
        }
        else {
            for (int i=0; i < numComponents; i++) {
                normComponents[normOffset+i] = ((float) components[offset+i]) /
                                               ((float) ((1<<nBits[i]) - 1));
            }
        }

        return normComponents;
    }


    public int getDataElement(int[] components, int offset) {
        throw new UnsupportedOperationException("This method is not supported "+
                                    "by this color model.");
    }


    public Object getDataElements(int[] components, int offset, Object obj) {
        throw new UnsupportedOperationException("This method has not been implemented "+
                                    "for this color model.");
    }


    public int getDataElement(float[] normComponents, int normOffset) {
        int components[] = getUnnormalizedComponents(normComponents,
                                                     normOffset, null, 0);
        return getDataElement(components, 0);
    }


    public Object getDataElements(float[] normComponents, int normOffset,
                                  Object obj) {
        int components[] = getUnnormalizedComponents(normComponents,
                                                     normOffset, null, 0);
        return getDataElements(components, 0, obj);
    }


    public float[] getNormalizedComponents(Object pixel,
                                           float[] normComponents,
                                           int normOffset) {
        int components[] = getComponents(pixel, null, 0);
        return getNormalizedComponents(components, 0,
                                       normComponents, normOffset);
    }


    public boolean equals(Object obj) {
        if (!(obj instanceof ColorModel)) {
            return false;
        }
        ColorModel cm = (ColorModel) obj;

        if (this == cm) {
            return true;
        }
        if (supportsAlpha != cm.hasAlpha() ||
            isAlphaPremultiplied != cm.isAlphaPremultiplied() ||
            pixel_bits != cm.getPixelSize() ||
            transparency != cm.getTransparency() ||
            numComponents != cm.getNumComponents())
        {
            return false;
        }

        int[] nb = cm.getComponentSize();

        if ((nBits != null) && (nb != null)) {
            for (int i = 0; i < numComponents; i++) {
                if (nBits[i] != nb[i]) {
                    return false;
                }
            }
        } else {
            return ((nBits == null) && (nb == null));
        }

        return true;
    }


    public int hashCode() {

        int result = 0;

        result = (supportsAlpha ? 2 : 3) +
                 (isAlphaPremultiplied ? 4 : 5) +
                 pixel_bits * 6 +
                 transparency * 7 +
                 numComponents * 8;

        if (nBits != null) {
            for (int i = 0; i < numComponents; i++) {
                result = result + nBits[i] * (i + 9);
            }
        }

        return result;
    }


    final public ColorSpace getColorSpace() {
        return colorSpace;
    }


    public ColorModel coerceData (WritableRaster raster,
                                  boolean isAlphaPremultiplied) {
        throw new UnsupportedOperationException
            ("This method is not supported by this color model");
    }


    public boolean isCompatibleRaster(Raster raster) {
        throw new UnsupportedOperationException(
            "This method has not been implemented for this ColorModel.");
    }


    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        throw new UnsupportedOperationException
            ("This method is not supported by this color model");
    }


    public SampleModel createCompatibleSampleModel(int w, int h) {
        throw new UnsupportedOperationException
            ("This method is not supported by this color model");
    }


    public boolean isCompatibleSampleModel(SampleModel sm) {
        throw new UnsupportedOperationException
            ("This method is not supported by this color model");
    }


    public void finalize() {
    }



    public WritableRaster getAlphaRaster(WritableRaster raster) {
        return null;
    }


    public String toString() {
       return new String("ColorModel: #pixelBits = "+pixel_bits
                         + " numComponents = "+numComponents
                         + " color space = "+colorSpace
                         + " transparency = "+transparency
                         + " has alpha = "+supportsAlpha
                         + " isAlphaPre = "+isAlphaPremultiplied
                         );
    }

    static int getDefaultTransferType(int pixel_bits) {
        if (pixel_bits <= 8) {
            return DataBuffer.TYPE_BYTE;
        } else if (pixel_bits <= 16) {
            return DataBuffer.TYPE_USHORT;
        } else if (pixel_bits <= 32) {
            return DataBuffer.TYPE_INT;
        } else {
            return DataBuffer.TYPE_UNDEFINED;
        }
    }

    static byte[] l8Tos8 = null;   // 8-bit linear to 8-bit non-linear sRGB LUT
    static byte[] s8Tol8 = null;   // 8-bit non-linear sRGB to 8-bit linear LUT
    static byte[] l16Tos8 = null;  // 16-bit linear to 8-bit non-linear sRGB LUT
    static short[] s8Tol16 = null; // 8-bit non-linear sRGB to 16-bit linear LUT

                                // Maps to hold LUTs for grayscale conversions
    static Map<ICC_ColorSpace, byte[]> g8Tos8Map = null;     // 8-bit gray values to 8-bit sRGB values
    static Map<ICC_ColorSpace, byte[]> lg16Toog8Map = null;  // 16-bit linear to 8-bit "other" gray
    static Map<ICC_ColorSpace, byte[]> g16Tos8Map = null;    // 16-bit gray values to 8-bit sRGB values
    static Map<ICC_ColorSpace, short[]> lg16Toog16Map = null; // 16-bit linear to 16-bit "other" gray

    static boolean isLinearRGBspace(ColorSpace cs) {
        // Note: CMM.LINEAR_RGBspace will be null if the linear
        // RGB space has not been created yet.
        return (cs == CMSManager.LINEAR_RGBspace);
    }

    static boolean isLinearGRAYspace(ColorSpace cs) {
        // Note: CMM.GRAYspace will be null if the linear
        // gray space has not been created yet.
        return (cs == CMSManager.GRAYspace);
    }

    static byte[] getLinearRGB8TosRGB8LUT() {
        if (l8Tos8 == null) {
            l8Tos8 = new byte[256];
            float input, output;
            // algorithm for linear RGB to nonlinear sRGB conversion
            // is from the IEC 61966-2-1 International Standard,
            // Colour Management - Default RGB colour space - sRGB,
            // First Edition, 1999-10,
            // avaiable for order at http://www.iec.ch
            for (int i = 0; i <= 255; i++) {
                input = ((float) i) / 255.0f;
                if (input <= 0.0031308f) {
                    output = input * 12.92f;
                } else {
                    output = 1.055f * ((float) Math.pow(input, (1.0 / 2.4)))
                             - 0.055f;
                }
                l8Tos8[i] = (byte) Math.round(output * 255.0f);
            }
        }
        return l8Tos8;
    }

    static byte[] getsRGB8ToLinearRGB8LUT() {
        if (s8Tol8 == null) {
            s8Tol8 = new byte[256];
            float input, output;
            // algorithm from IEC 61966-2-1 International Standard
            for (int i = 0; i <= 255; i++) {
                input = ((float) i) / 255.0f;
                if (input <= 0.04045f) {
                    output = input / 12.92f;
                } else {
                    output = (float) Math.pow((input + 0.055f) / 1.055f, 2.4);
                }
                s8Tol8[i] = (byte) Math.round(output * 255.0f);
            }
        }
        return s8Tol8;
    }

    static byte[] getLinearRGB16TosRGB8LUT() {
        if (l16Tos8 == null) {
            l16Tos8 = new byte[65536];
            float input, output;
            // algorithm from IEC 61966-2-1 International Standard
            for (int i = 0; i <= 65535; i++) {
                input = ((float) i) / 65535.0f;
                if (input <= 0.0031308f) {
                    output = input * 12.92f;
                } else {
                    output = 1.055f * ((float) Math.pow(input, (1.0 / 2.4)))
                             - 0.055f;
                }
                l16Tos8[i] = (byte) Math.round(output * 255.0f);
            }
        }
        return l16Tos8;
    }

    static short[] getsRGB8ToLinearRGB16LUT() {
        if (s8Tol16 == null) {
            s8Tol16 = new short[256];
            float input, output;
            // algorithm from IEC 61966-2-1 International Standard
            for (int i = 0; i <= 255; i++) {
                input = ((float) i) / 255.0f;
                if (input <= 0.04045f) {
                    output = input / 12.92f;
                } else {
                    output = (float) Math.pow((input + 0.055f) / 1.055f, 2.4);
                }
                s8Tol16[i] = (short) Math.round(output * 65535.0f);
            }
        }
        return s8Tol16;
    }


    static byte[] getGray8TosRGB8LUT(ICC_ColorSpace grayCS) {
        if (isLinearGRAYspace(grayCS)) {
            return getLinearRGB8TosRGB8LUT();
        }
        if (g8Tos8Map != null) {
            byte[] g8Tos8LUT = g8Tos8Map.get(grayCS);
            if (g8Tos8LUT != null) {
                return g8Tos8LUT;
            }
        }
        byte[] g8Tos8LUT = new byte[256];
        for (int i = 0; i <= 255; i++) {
            g8Tos8LUT[i] = (byte) i;
        }
        ColorTransform[] transformList = new ColorTransform[2];
        PCMM mdl = CMSManager.getModule();
        ICC_ColorSpace srgbCS =
            (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_sRGB);
        transformList[0] = mdl.createTransform(
            grayCS.getProfile(), ColorTransform.Any, ColorTransform.In);
        transformList[1] = mdl.createTransform(
            srgbCS.getProfile(), ColorTransform.Any, ColorTransform.Out);
        ColorTransform t = mdl.createTransform(transformList);
        byte[] tmp = t.colorConvert(g8Tos8LUT, null);
        for (int i = 0, j= 2; i <= 255; i++, j += 3) {
            // All three components of tmp should be equal, since
            // the input color space to colorConvert is a gray scale
            // space.  However, there are slight anomalies in the results.
            // Copy tmp starting at index 2, since colorConvert seems
            // to be slightly more accurate for the third component!
            g8Tos8LUT[i] = tmp[j];
        }
        if (g8Tos8Map == null) {
            g8Tos8Map = Collections.synchronizedMap(new WeakHashMap<ICC_ColorSpace, byte[]>(2));
        }
        g8Tos8Map.put(grayCS, g8Tos8LUT);
        return g8Tos8LUT;
    }


    static byte[] getLinearGray16ToOtherGray8LUT(ICC_ColorSpace grayCS) {
        if (lg16Toog8Map != null) {
            byte[] lg16Toog8LUT = lg16Toog8Map.get(grayCS);
            if (lg16Toog8LUT != null) {
                return lg16Toog8LUT;
            }
        }
        short[] tmp = new short[65536];
        for (int i = 0; i <= 65535; i++) {
            tmp[i] = (short) i;
        }
        ColorTransform[] transformList = new ColorTransform[2];
        PCMM mdl = CMSManager.getModule();
        ICC_ColorSpace lgCS =
            (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_GRAY);
        transformList[0] = mdl.createTransform (
            lgCS.getProfile(), ColorTransform.Any, ColorTransform.In);
        transformList[1] = mdl.createTransform (
            grayCS.getProfile(), ColorTransform.Any, ColorTransform.Out);
        ColorTransform t = mdl.createTransform(transformList);
        tmp = t.colorConvert(tmp, null);
        byte[] lg16Toog8LUT = new byte[65536];
        for (int i = 0; i <= 65535; i++) {
            // scale unsigned short (0 - 65535) to unsigned byte (0 - 255)
            lg16Toog8LUT[i] =
                (byte) (((float) (tmp[i] & 0xffff)) * (1.0f /257.0f) + 0.5f);
        }
        if (lg16Toog8Map == null) {
            lg16Toog8Map = Collections.synchronizedMap(new WeakHashMap<ICC_ColorSpace, byte[]>(2));
        }
        lg16Toog8Map.put(grayCS, lg16Toog8LUT);
        return lg16Toog8LUT;
    }


    static byte[] getGray16TosRGB8LUT(ICC_ColorSpace grayCS) {
        if (isLinearGRAYspace(grayCS)) {
            return getLinearRGB16TosRGB8LUT();
        }
        if (g16Tos8Map != null) {
            byte[] g16Tos8LUT = g16Tos8Map.get(grayCS);
            if (g16Tos8LUT != null) {
                return g16Tos8LUT;
            }
        }
        short[] tmp = new short[65536];
        for (int i = 0; i <= 65535; i++) {
            tmp[i] = (short) i;
        }
        ColorTransform[] transformList = new ColorTransform[2];
        PCMM mdl = CMSManager.getModule();
        ICC_ColorSpace srgbCS =
            (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_sRGB);
        transformList[0] = mdl.createTransform (
            grayCS.getProfile(), ColorTransform.Any, ColorTransform.In);
        transformList[1] = mdl.createTransform (
            srgbCS.getProfile(), ColorTransform.Any, ColorTransform.Out);
        ColorTransform t = mdl.createTransform(transformList);
        tmp = t.colorConvert(tmp, null);
        byte[] g16Tos8LUT = new byte[65536];
        for (int i = 0, j= 2; i <= 65535; i++, j += 3) {
            // All three components of tmp should be equal, since
            // the input color space to colorConvert is a gray scale
            // space.  However, there are slight anomalies in the results.
            // Copy tmp starting at index 2, since colorConvert seems
            // to be slightly more accurate for the third component!

            // scale unsigned short (0 - 65535) to unsigned byte (0 - 255)
            g16Tos8LUT[i] =
                (byte) (((float) (tmp[j] & 0xffff)) * (1.0f /257.0f) + 0.5f);
        }
        if (g16Tos8Map == null) {
            g16Tos8Map = Collections.synchronizedMap(new WeakHashMap<ICC_ColorSpace, byte[]>(2));
        }
        g16Tos8Map.put(grayCS, g16Tos8LUT);
        return g16Tos8LUT;
    }


    static short[] getLinearGray16ToOtherGray16LUT(ICC_ColorSpace grayCS) {
        if (lg16Toog16Map != null) {
            short[] lg16Toog16LUT = lg16Toog16Map.get(grayCS);
            if (lg16Toog16LUT != null) {
                return lg16Toog16LUT;
            }
        }
        short[] tmp = new short[65536];
        for (int i = 0; i <= 65535; i++) {
            tmp[i] = (short) i;
        }
        ColorTransform[] transformList = new ColorTransform[2];
        PCMM mdl = CMSManager.getModule();
        ICC_ColorSpace lgCS =
            (ICC_ColorSpace) ColorSpace.getInstance(ColorSpace.CS_GRAY);
        transformList[0] = mdl.createTransform (
            lgCS.getProfile(), ColorTransform.Any, ColorTransform.In);
        transformList[1] = mdl.createTransform(
            grayCS.getProfile(), ColorTransform.Any, ColorTransform.Out);
        ColorTransform t = mdl.createTransform(
            transformList);
        short[] lg16Toog16LUT = t.colorConvert(tmp, null);
        if (lg16Toog16Map == null) {
            lg16Toog16Map = Collections.synchronizedMap(new WeakHashMap<ICC_ColorSpace, short[]>(2));
        }
        lg16Toog16Map.put(grayCS, lg16Toog16LUT);
        return lg16Toog16LUT;
    }

}
