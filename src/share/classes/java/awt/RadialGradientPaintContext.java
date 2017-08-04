

package java.awt;

import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;


final class RadialGradientPaintContext extends MultipleGradientPaintContext {


    private boolean isSimpleFocus = false;


    private boolean isNonCyclic = false;


    private float radius;


    private float centerX, centerY, focusX, focusY;


    private float radiusSq;


    private float constA, constB;


    private float gDeltaDelta;


    private float trivial;


    private static final float SCALEBACK = .99f;


    RadialGradientPaintContext(RadialGradientPaint paint,
                               ColorModel cm,
                               Rectangle deviceBounds,
                               Rectangle2D userBounds,
                               AffineTransform t,
                               RenderingHints hints,
                               float cx, float cy,
                               float r,
                               float fx, float fy,
                               float[] fractions,
                               Color[] colors,
                               CycleMethod cycleMethod,
                               ColorSpaceType colorSpace)
    {
        super(paint, cm, deviceBounds, userBounds, t, hints,
              fractions, colors, cycleMethod, colorSpace);

        // copy some parameters
        centerX = cx;
        centerY = cy;
        focusX = fx;
        focusY = fy;
        radius = r;

        this.isSimpleFocus = (focusX == centerX) && (focusY == centerY);
        this.isNonCyclic = (cycleMethod == CycleMethod.NO_CYCLE);

        // for use in the quadractic equation
        radiusSq = radius * radius;

        float dX = focusX - centerX;
        float dY = focusY - centerY;

        double distSq = (dX * dX) + (dY * dY);

        // test if distance from focus to center is greater than the radius
        if (distSq > radiusSq * SCALEBACK) {
            // clamp focus to radius
            float scalefactor = (float)Math.sqrt(radiusSq * SCALEBACK / distSq);
            dX = dX * scalefactor;
            dY = dY * scalefactor;
            focusX = centerX + dX;
            focusY = centerY + dY;
        }

        // calculate the solution to be used in the case where X == focusX
        // in cyclicCircularGradientFillRaster()
        trivial = (float)Math.sqrt(radiusSq - (dX * dX));

        // constant parts of X, Y user space coordinates
        constA = a02 - centerX;
        constB = a12 - centerY;

        // constant second order delta for simple loop
        gDeltaDelta = 2 * ( a00 *  a00 +  a10 *  a10) / radiusSq;
    }


    protected void fillRaster(int pixels[], int off, int adjust,
                              int x, int y, int w, int h)
    {
        if (isSimpleFocus && isNonCyclic && isSimpleLookup) {
            simpleNonCyclicFillRaster(pixels, off, adjust, x, y, w, h);
        } else {
            cyclicCircularGradientFillRaster(pixels, off, adjust, x, y, w, h);
        }
    }


    private void simpleNonCyclicFillRaster(int pixels[], int off, int adjust,
                                           int x, int y, int w, int h)
    {

        // coordinates of UL corner in "user space" relative to center
        float rowX = (a00*x) + (a01*y) + constA;
        float rowY = (a10*x) + (a11*y) + constB;

        // second order delta calculated in constructor
        float gDeltaDelta = this.gDeltaDelta;

        // adjust is (scan-w) of pixels array, we need (scan)
        adjust += w;

        // rgb of the 1.0 color used when the distance exceeds gradient radius
        int rgbclip = gradient[fastGradientArraySize];

        for (int j = 0; j < h; j++) {
            // these values depend on the coordinates of the start of the row
            float gRel   =      (rowX * rowX + rowY * rowY) / radiusSq;
            float gDelta = (2 * ( a00 * rowX +  a10 * rowY) / radiusSq +
                            gDeltaDelta/2);


            int i = 0;
            // Quick fill for "out to the left"
            while (i < w && gRel >= 1.0f) {
                pixels[off + i] = rgbclip;
                gRel += gDelta;
                gDelta += gDeltaDelta;
                i++;
            }
            // Slow fill for "in the heart"
            while (i < w && gRel < 1.0f) {
                int gIndex;

                if (gRel <= 0) {
                    gIndex = 0;
                } else {
                    float fIndex = gRel * SQRT_LUT_SIZE;
                    int iIndex = (int) (fIndex);
                    float s0 = sqrtLut[iIndex];
                    float s1 = sqrtLut[iIndex+1] - s0;
                    fIndex = s0 + (fIndex - iIndex) * s1;
                    gIndex = (int) (fIndex * fastGradientArraySize);
                }

                // store the color at this point
                pixels[off + i] = gradient[gIndex];

                // incremental calculation
                gRel += gDelta;
                gDelta += gDeltaDelta;
                i++;
            }
            // Quick fill to end of line for "out to the right"
            while (i < w) {
                pixels[off + i] = rgbclip;
                i++;
            }

            off += adjust;
            rowX += a01;
            rowY += a11;
        }
    }

    // SQRT_LUT_SIZE must be a power of 2 for the test above to work.
    private static final int SQRT_LUT_SIZE = (1 << 11);
    private static float sqrtLut[] = new float[SQRT_LUT_SIZE+1];
    static {
        for (int i = 0; i < sqrtLut.length; i++) {
            sqrtLut[i] = (float) Math.sqrt(i / ((float) SQRT_LUT_SIZE));
        }
    }


    private void cyclicCircularGradientFillRaster(int pixels[], int off,
                                                  int adjust,
                                                  int x, int y,
                                                  int w, int h)
    {
        // constant part of the C factor of the quadratic equation
        final double constC =
            -radiusSq + (centerX * centerX) + (centerY * centerY);

        // coefficients of the quadratic equation (Ax^2 + Bx + C = 0)
        double A, B, C;

        // slope and y-intercept of the focus-perimeter line
        double slope, yintcpt;

        // intersection with circle X,Y coordinate
        double solutionX, solutionY;

        // constant parts of X, Y coordinates
        final float constX = (a00*x) + (a01*y) + a02;
        final float constY = (a10*x) + (a11*y) + a12;

        // constants in inner loop quadratic formula
        final float precalc2 =  2 * centerY;
        final float precalc3 = -2 * centerX;

        // value between 0 and 1 specifying position in the gradient
        float g;

        // determinant of quadratic formula (should always be > 0)
        float det;

        // sq distance from the current point to focus
        float currentToFocusSq;

        // sq distance from the intersect point to focus
        float intersectToFocusSq;

        // temp variables for change in X,Y squared
        float deltaXSq, deltaYSq;

        // used to index pixels array
        int indexer = off;

        // incremental index change for pixels array
        int pixInc = w+adjust;

        // for every row
        for (int j = 0; j < h; j++) {

            // user space point; these are constant from column to column
            float X = (a01*j) + constX;
            float Y = (a11*j) + constY;

            // for every column (inner loop begins here)
            for (int i = 0; i < w; i++) {

                if (X == focusX) {
                    // special case to avoid divide by zero
                    solutionX = focusX;
                    solutionY = centerY;
                    solutionY += (Y > focusY) ? trivial : -trivial;
                } else {
                    // slope and y-intercept of the focus-perimeter line
                    slope = (Y - focusY) / (X - focusX);
                    yintcpt = Y - (slope * X);

                    // use the quadratic formula to calculate the
                    // intersection point
                    A = (slope * slope) + 1;
                    B = precalc3 + (-2 * slope * (centerY - yintcpt));
                    C = constC + (yintcpt* (yintcpt - precalc2));

                    det = (float)Math.sqrt((B * B) - (4 * A * C));
                    solutionX = -B;

                    // choose the positive or negative root depending
                    // on where the X coord lies with respect to the focus
                    solutionX += (X < focusX)? -det : det;
                    solutionX = solutionX / (2 * A); // divisor
                    solutionY = (slope * solutionX) + yintcpt;
                }

                // Calculate the square of the distance from the current point
                // to the focus and the square of the distance from the
                // intersection point to the focus. Want the squares so we can
                // do 1 square root after division instead of 2 before.

                deltaXSq = X - focusX;
                deltaXSq = deltaXSq * deltaXSq;

                deltaYSq = Y - focusY;
                deltaYSq = deltaYSq * deltaYSq;

                currentToFocusSq = deltaXSq + deltaYSq;

                deltaXSq = (float)solutionX - focusX;
                deltaXSq = deltaXSq * deltaXSq;

                deltaYSq = (float)solutionY - focusY;
                deltaYSq = deltaYSq * deltaYSq;

                intersectToFocusSq = deltaXSq + deltaYSq;

                // get the percentage (0-1) of the current point along the
                // focus-circumference line
                g = (float)Math.sqrt(currentToFocusSq / intersectToFocusSq);

                // store the color at this point
                pixels[indexer + i] = indexIntoGradientsArrays(g);

                // incremental change in X, Y
                X += a00;
                Y += a10;
            } //end inner loop

            indexer += pixInc;
        } //end outer loop
    }
}
