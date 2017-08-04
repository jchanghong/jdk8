package java.awt.dnd;
import java.lang.annotation.Native;
public final class DnDConstants {
    private DnDConstants() {} // define null private constructor.
    @Native public static final int ACTION_NONE         = 0x0;
    @Native public static final int ACTION_COPY         = 0x1;
    @Native public static final int ACTION_MOVE         = 0x2;
    @Native public static final int ACTION_COPY_OR_MOVE = ACTION_COPY | ACTION_MOVE;
    @Native public static final int ACTION_LINK         = 0x40000000;
    @Native public static final int ACTION_REFERENCE    = ACTION_LINK;
}
