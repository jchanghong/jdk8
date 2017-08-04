

package java.awt;

import java.lang.annotation.Native;


public interface Transparency {


    @Native public final static int OPAQUE            = 1;


    @Native public final static int BITMASK = 2;


    @Native public final static int TRANSLUCENT        = 3;


    public int getTransparency();
}
