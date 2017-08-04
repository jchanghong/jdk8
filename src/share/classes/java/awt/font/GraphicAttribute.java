



package java.awt.font;

import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


public abstract class GraphicAttribute {

    private int fAlignment;


    public static final int TOP_ALIGNMENT = -1;


    public static final int BOTTOM_ALIGNMENT = -2;


    public static final int ROMAN_BASELINE = Font.ROMAN_BASELINE;


    public static final int CENTER_BASELINE = Font.CENTER_BASELINE;


    public static final int HANGING_BASELINE = Font.HANGING_BASELINE;


    protected GraphicAttribute(int alignment) {
        if (alignment < BOTTOM_ALIGNMENT || alignment > HANGING_BASELINE) {
          throw new IllegalArgumentException("bad alignment");
        }
        fAlignment = alignment;
    }


    public abstract float getAscent();



    public abstract float getDescent();


    public abstract float getAdvance();


    public Rectangle2D getBounds() {
        float ascent = getAscent();
        return new Rectangle2D.Float(0, -ascent,
                                        getAdvance(), ascent+getDescent());
    }


    public Shape getOutline(AffineTransform tx) {
        Shape b = getBounds();
        if (tx != null) {
            b = tx.createTransformedShape(b);
        }
        return b;
    }


    public abstract void draw(Graphics2D graphics, float x, float y);


    public final int getAlignment() {

        return fAlignment;
    }


    public GlyphJustificationInfo getJustificationInfo() {

        // should we cache this?
        float advance = getAdvance();

        return new GlyphJustificationInfo(
                                     advance,   // weight
                                     false,     // growAbsorb
                                     2,         // growPriority
                                     advance/3, // growLeftLimit
                                     advance/3, // growRightLimit
                                     false,     // shrinkAbsorb
                                     1,         // shrinkPriority
                                     0,         // shrinkLeftLimit
                                     0);        // shrinkRightLimit
    }
}
