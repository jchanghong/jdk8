package java.awt;
import sun.awt.AWTAccessor;
import java.io.ObjectStreamException;
import java.lang.annotation.Native;
public final class SystemColor extends Color implements java.io.Serializable {
    @Native public final static int DESKTOP = 0;
    @Native public final static int ACTIVE_CAPTION = 1;
    @Native public final static int ACTIVE_CAPTION_TEXT = 2;
    @Native public final static int ACTIVE_CAPTION_BORDER = 3;
    @Native public final static int INACTIVE_CAPTION = 4;
    @Native public final static int INACTIVE_CAPTION_TEXT = 5;
    @Native public final static int INACTIVE_CAPTION_BORDER = 6;
    @Native public final static int WINDOW = 7;
    @Native public final static int WINDOW_BORDER = 8;
    @Native public final static int WINDOW_TEXT = 9;
    @Native public final static int MENU = 10;
    @Native public final static int MENU_TEXT = 11;
    @Native public final static int TEXT = 12;
    @Native public final static int TEXT_TEXT = 13;
    @Native public final static int TEXT_HIGHLIGHT = 14;
    @Native public final static int TEXT_HIGHLIGHT_TEXT = 15;
    @Native public final static int TEXT_INACTIVE_TEXT = 16;
    @Native public final static int CONTROL = 17;
    @Native public final static int CONTROL_TEXT = 18;
    @Native public final static int CONTROL_HIGHLIGHT = 19;
    @Native public final static int CONTROL_LT_HIGHLIGHT = 20;
    @Native public final static int CONTROL_SHADOW = 21;
    @Native public final static int CONTROL_DK_SHADOW = 22;
    @Native public final static int SCROLLBAR = 23;
    @Native public final static int INFO = 24;
    @Native public final static int INFO_TEXT = 25;
    @Native public final static int NUM_COLORS = 26;
    private static int[] systemColors = {
        0xFF005C5C,  // desktop = new Color(0,92,92);
        0xFF000080,  // activeCaption = new Color(0,0,128);
        0xFFFFFFFF,  // activeCaptionText = Color.white;
        0xFFC0C0C0,  // activeCaptionBorder = Color.lightGray;
        0xFF808080,  // inactiveCaption = Color.gray;
        0xFFC0C0C0,  // inactiveCaptionText = Color.lightGray;
        0xFFC0C0C0,  // inactiveCaptionBorder = Color.lightGray;
        0xFFFFFFFF,  // window = Color.white;
        0xFF000000,  // windowBorder = Color.black;
        0xFF000000,  // windowText = Color.black;
        0xFFC0C0C0,  // menu = Color.lightGray;
        0xFF000000,  // menuText = Color.black;
        0xFFC0C0C0,  // text = Color.lightGray;
        0xFF000000,  // textText = Color.black;
        0xFF000080,  // textHighlight = new Color(0,0,128);
        0xFFFFFFFF,  // textHighlightText = Color.white;
        0xFF808080,  // textInactiveText = Color.gray;
        0xFFC0C0C0,  // control = Color.lightGray;
        0xFF000000,  // controlText = Color.black;
        0xFFFFFFFF,  // controlHighlight = Color.white;
        0xFFE0E0E0,  // controlLtHighlight = new Color(224,224,224);
        0xFF808080,  // controlShadow = Color.gray;
        0xFF000000,  // controlDkShadow = Color.black;
        0xFFE0E0E0,  // scrollbar = new Color(224,224,224);
        0xFFE0E000,  // info = new Color(224,224,0);
        0xFF000000,  // infoText = Color.black;
    };
    public final static SystemColor desktop = new SystemColor((byte)DESKTOP);
    public final static SystemColor activeCaption = new SystemColor((byte)ACTIVE_CAPTION);
    public final static SystemColor activeCaptionText = new SystemColor((byte)ACTIVE_CAPTION_TEXT);
    public final static SystemColor activeCaptionBorder = new SystemColor((byte)ACTIVE_CAPTION_BORDER);
    public final static SystemColor inactiveCaption = new SystemColor((byte)INACTIVE_CAPTION);
    public final static SystemColor inactiveCaptionText = new SystemColor((byte)INACTIVE_CAPTION_TEXT);
    public final static SystemColor inactiveCaptionBorder = new SystemColor((byte)INACTIVE_CAPTION_BORDER);
    public final static SystemColor window = new SystemColor((byte)WINDOW);
    public final static SystemColor windowBorder = new SystemColor((byte)WINDOW_BORDER);
    public final static SystemColor windowText = new SystemColor((byte)WINDOW_TEXT);
    public final static SystemColor menu = new SystemColor((byte)MENU);
    public final static SystemColor menuText = new SystemColor((byte)MENU_TEXT);
    public final static SystemColor text = new SystemColor((byte)TEXT);
    public final static SystemColor textText = new SystemColor((byte)TEXT_TEXT);
    public final static SystemColor textHighlight = new SystemColor((byte)TEXT_HIGHLIGHT);
    public final static SystemColor textHighlightText = new SystemColor((byte)TEXT_HIGHLIGHT_TEXT);
    public final static SystemColor textInactiveText = new SystemColor((byte)TEXT_INACTIVE_TEXT);
    public final static SystemColor control = new SystemColor((byte)CONTROL);
    public final static SystemColor controlText = new SystemColor((byte)CONTROL_TEXT);
    public final static SystemColor controlHighlight = new SystemColor((byte)CONTROL_HIGHLIGHT);
    public final static SystemColor controlLtHighlight = new SystemColor((byte)CONTROL_LT_HIGHLIGHT);
    public final static SystemColor controlShadow = new SystemColor((byte)CONTROL_SHADOW);
    public final static SystemColor controlDkShadow = new SystemColor((byte)CONTROL_DK_SHADOW);
    public final static SystemColor scrollbar = new SystemColor((byte)SCROLLBAR);
    public final static SystemColor info = new SystemColor((byte)INFO);
    public final static SystemColor infoText = new SystemColor((byte)INFO_TEXT);
    private static final long serialVersionUID = 4503142729533789064L;
    private transient int index;
    private static SystemColor systemColorObjects [] = {
        SystemColor.desktop,
        SystemColor.activeCaption,
        SystemColor.activeCaptionText,
        SystemColor.activeCaptionBorder,
        SystemColor.inactiveCaption,
        SystemColor.inactiveCaptionText,
        SystemColor.inactiveCaptionBorder,
        SystemColor.window,
        SystemColor.windowBorder,
        SystemColor.windowText,
        SystemColor.menu,
        SystemColor.menuText,
        SystemColor.text,
        SystemColor.textText,
        SystemColor.textHighlight,
        SystemColor.textHighlightText,
        SystemColor.textInactiveText,
        SystemColor.control,
        SystemColor.controlText,
        SystemColor.controlHighlight,
        SystemColor.controlLtHighlight,
        SystemColor.controlShadow,
        SystemColor.controlDkShadow,
        SystemColor.scrollbar,
        SystemColor.info,
        SystemColor.infoText
    };
    static {
        AWTAccessor.setSystemColorAccessor(SystemColor::updateSystemColors);
        updateSystemColors();
    }
    private static void updateSystemColors() {
        if (!GraphicsEnvironment.isHeadless()) {
            Toolkit.getDefaultToolkit().loadSystemColors(systemColors);
        }
        for (int i = 0; i < systemColors.length; i++) {
            systemColorObjects[i].value = systemColors[i];
        }
    }
    private SystemColor(byte index) {
        super(systemColors[index]);
        this.index = index;
    }
    public String toString() {
        return getClass().getName() + "[i=" + (index) + "]";
    }
    private Object readResolve() {
        // The instances of SystemColor are tightly controlled and
        // only the canonical instances appearing above as static
        // constants are allowed.  The serial form of SystemColor
        // objects stores the color index as the value.  Here we
        // map that index back into the canonical instance.
        return systemColorObjects[value];
    }
    private Object writeReplace() throws ObjectStreamException
    {
        // we put an array index in the SystemColor.value while serialize
        // to keep compatibility.
        SystemColor color = new SystemColor((byte)index);
        color.value = index;
        return color;
    }
}
