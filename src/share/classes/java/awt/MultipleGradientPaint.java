package java.awt;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.lang.ref.SoftReference;
import java.util.Arrays;
public abstract class MultipleGradientPaint implements Paint {
    public static enum CycleMethod {
        NO_CYCLE,
        REFLECT,
        REPEAT
    }
    public static enum ColorSpaceType {
        SRGB,
        LINEAR_RGB
    }
    final int transparency;
    final float[] fractions;
    final Color[] colors;
    final AffineTransform gradientTransform;
    final CycleMethod cycleMethod;
    final ColorSpaceType colorSpace;
    ColorModel model;
    float[] normalizedIntervals;
    boolean isSimpleLookup;
    SoftReference<int[][]> gradients;
    SoftReference<int[]> gradient;
    int fastGradientArraySize;
    MultipleGradientPaint(float[] fractions,
                          Color[] colors,
                          CycleMethod cycleMethod,
                          ColorSpaceType colorSpace,
                          AffineTransform gradientTransform)
    {
        if (fractions == null) {
            throw new NullPointerException("Fractions array cannot be null");
        }
        if (colors == null) {
            throw new NullPointerException("Colors array cannot be null");
        }
        if (cycleMethod == null) {
            throw new NullPointerException("Cycle method cannot be null");
        }
        if (colorSpace == null) {
            throw new NullPointerException("Color space cannot be null");
        }
        if (gradientTransform == null) {
            throw new NullPointerException("Gradient transform cannot be "+
                                           "null");
        }
        if (fractions.length != colors.length) {
            throw new IllegalArgumentException("Colors and fractions must " +
                                               "have equal size");
        }
        if (colors.length < 2) {
            throw new IllegalArgumentException("User must specify at least " +
                                               "2 colors");
        }
        // check that values are in the proper range and progress
        // in increasing order from 0 to 1
        float previousFraction = -1.0f;
        for (float currentFraction : fractions) {
            if (currentFraction < 0f || currentFraction > 1f) {
                throw new IllegalArgumentException("Fraction values must " +
                                                   "be in the range 0 to 1: " +
                                                   currentFraction);
            }
            if (currentFraction <= previousFraction) {
                throw new IllegalArgumentException("Keyframe fractions " +
                                                   "must be increasing: " +
                                                   currentFraction);
            }
            previousFraction = currentFraction;
        }
        // We have to deal with the cases where the first gradient stop is not
        // equal to 0 and/or the last gradient stop is not equal to 1.
        // In both cases, create a new point and replicate the previous
        // extreme point's color.
        boolean fixFirst = false;
        boolean fixLast = false;
        int len = fractions.length;
        int off = 0;
        if (fractions[0] != 0f) {
            // first stop is not equal to zero, fix this condition
            fixFirst = true;
            len++;
            off++;
        }
        if (fractions[fractions.length-1] != 1f) {
            // last stop is not equal to one, fix this condition
            fixLast = true;
            len++;
        }
        this.fractions = new float[len];
        System.arraycopy(fractions, 0, this.fractions, off, fractions.length);
        this.colors = new Color[len];
        System.arraycopy(colors, 0, this.colors, off, colors.length);
        if (fixFirst) {
            this.fractions[0] = 0f;
            this.colors[0] = colors[0];
        }
        if (fixLast) {
            this.fractions[len-1] = 1f;
            this.colors[len-1] = colors[colors.length - 1];
        }
        // copy some flags
        this.colorSpace = colorSpace;
        this.cycleMethod = cycleMethod;
        // copy the gradient transform
        this.gradientTransform = new AffineTransform(gradientTransform);
        // determine transparency
        boolean opaque = true;
        for (int i = 0; i < colors.length; i++){
            opaque = opaque && (colors[i].getAlpha() == 0xff);
        }
        this.transparency = opaque ? OPAQUE : TRANSLUCENT;
    }
    public final float[] getFractions() {
        return Arrays.copyOf(fractions, fractions.length);
    }
    public final Color[] getColors() {
        return Arrays.copyOf(colors, colors.length);
    }
    public final CycleMethod getCycleMethod() {
        return cycleMethod;
    }
    public final ColorSpaceType getColorSpace() {
        return colorSpace;
    }
    public final AffineTransform getTransform() {
        return new AffineTransform(gradientTransform);
    }
    public final int getTransparency() {
        return transparency;
    }
}
