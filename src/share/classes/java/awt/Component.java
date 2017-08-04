
package java.awt;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.Vector;
import java.util.Locale;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.awt.peer.ComponentPeer;
import java.awt.peer.ContainerPeer;
import java.awt.peer.LightweightPeer;
import java.awt.image.BufferStrategy;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.ColorModel;
import java.awt.image.VolatileImage;
import java.awt.event.*;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.Transient;
import java.awt.im.InputContext;
import java.awt.im.InputMethodRequests;
import java.awt.dnd.DropTarget;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.AccessControlContext;
import javax.accessibility.*;
import java.applet.Applet;

import sun.security.action.GetPropertyAction;
import sun.awt.AppContext;
import sun.awt.AWTAccessor;
import sun.awt.ConstrainableGraphics;
import sun.awt.SubRegionShowable;
import sun.awt.SunToolkit;
import sun.awt.WindowClosingListener;
import sun.awt.CausedFocusEvent;
import sun.awt.EmbeddedFrame;
import sun.awt.dnd.SunDropTargetEvent;
import sun.awt.im.CompositionArea;
import sun.font.FontManager;
import sun.font.FontManagerFactory;
import sun.font.SunFontManager;
import sun.java2d.SunGraphics2D;
import sun.java2d.pipe.Region;
import sun.awt.image.VSyncedBSManager;
import sun.java2d.pipe.hw.ExtendedBufferCapabilities;
import static sun.java2d.pipe.hw.ExtendedBufferCapabilities.VSyncType.*;
import sun.awt.RequestFocusController;
import sun.java2d.SunGraphicsEnvironment;
import sun.util.logging.PlatformLogger;


public abstract class Component implements ImageObserver, MenuContainer,
                                           Serializable
{

    private static final PlatformLogger log = PlatformLogger.getLogger("java.awt.Component");
    private static final PlatformLogger eventLog = PlatformLogger.getLogger("java.awt.event.Component");
    private static final PlatformLogger focusLog = PlatformLogger.getLogger("java.awt.focus.Component");
    private static final PlatformLogger mixingLog = PlatformLogger.getLogger("java.awt.mixing.Component");


    transient ComponentPeer peer;


    transient Container parent;


    transient AppContext appContext;


    int x;


    int y;


    int width;


    int height;


    Color       foreground;


    Color       background;


    volatile Font font;


    Font        peerFont;


    Cursor      cursor;


    Locale      locale;


    private transient volatile GraphicsConfiguration graphicsConfig;


    transient BufferStrategy bufferStrategy = null;


    boolean ignoreRepaint = false;


    boolean visible = true;


    boolean enabled = true;


    private volatile boolean valid = false;


    DropTarget dropTarget;


    Vector<PopupMenu> popups;


    private String name;


    private boolean nameExplicitlySet = false;


    private boolean focusable = true;

    private static final int FOCUS_TRAVERSABLE_UNKNOWN = 0;
    private static final int FOCUS_TRAVERSABLE_DEFAULT = 1;
    private static final int FOCUS_TRAVERSABLE_SET = 2;


    private int isFocusTraversableOverridden = FOCUS_TRAVERSABLE_UNKNOWN;


    Set<AWTKeyStroke>[] focusTraversalKeys;

    private static final String[] focusTraversalKeyPropertyNames = {
        "forwardFocusTraversalKeys",
        "backwardFocusTraversalKeys",
        "upCycleFocusTraversalKeys",
        "downCycleFocusTraversalKeys"
    };


    private boolean focusTraversalKeysEnabled = true;


    static final Object LOCK = new AWTTreeLock();
    static class AWTTreeLock {}


    private transient volatile AccessControlContext acc =
        AccessController.getContext();


    Dimension minSize;


    boolean minSizeSet;


    Dimension prefSize;


    boolean prefSizeSet;


    Dimension maxSize;


    boolean maxSizeSet;


    transient ComponentOrientation componentOrientation
    = ComponentOrientation.UNKNOWN;


    boolean newEventsOnly = false;
    transient ComponentListener componentListener;
    transient FocusListener focusListener;
    transient HierarchyListener hierarchyListener;
    transient HierarchyBoundsListener hierarchyBoundsListener;
    transient KeyListener keyListener;
    transient MouseListener mouseListener;
    transient MouseMotionListener mouseMotionListener;
    transient MouseWheelListener mouseWheelListener;
    transient InputMethodListener inputMethodListener;

    transient RuntimeException windowClosingException = null;


    final static String actionListenerK = "actionL";
    final static String adjustmentListenerK = "adjustmentL";
    final static String componentListenerK = "componentL";
    final static String containerListenerK = "containerL";
    final static String focusListenerK = "focusL";
    final static String itemListenerK = "itemL";
    final static String keyListenerK = "keyL";
    final static String mouseListenerK = "mouseL";
    final static String mouseMotionListenerK = "mouseMotionL";
    final static String mouseWheelListenerK = "mouseWheelL";
    final static String textListenerK = "textL";
    final static String ownedWindowK = "ownedL";
    final static String windowListenerK = "windowL";
    final static String inputMethodListenerK = "inputMethodL";
    final static String hierarchyListenerK = "hierarchyL";
    final static String hierarchyBoundsListenerK = "hierarchyBoundsL";
    final static String windowStateListenerK = "windowStateL";
    final static String windowFocusListenerK = "windowFocusL";


    long eventMask = AWTEvent.INPUT_METHODS_ENABLED_MASK;


    static boolean isInc;
    static int incRate;
    static {

        Toolkit.loadLibraries();

        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }

        String s = java.security.AccessController.doPrivileged(
                                                               new GetPropertyAction("awt.image.incrementaldraw"));
        isInc = (s == null || s.equals("true"));

        s = java.security.AccessController.doPrivileged(
                                                        new GetPropertyAction("awt.image.redrawrate"));
        incRate = (s != null) ? Integer.parseInt(s) : 100;
    }


    public static final float TOP_ALIGNMENT = 0.0f;


    public static final float CENTER_ALIGNMENT = 0.5f;


    public static final float BOTTOM_ALIGNMENT = 1.0f;


    public static final float LEFT_ALIGNMENT = 0.0f;


    public static final float RIGHT_ALIGNMENT = 1.0f;


    private static final long serialVersionUID = -7644114512714619750L;


    private PropertyChangeSupport changeSupport;


    private transient Object objectLock = new Object();
    Object getObjectLock() {
        return objectLock;
    }


    final AccessControlContext getAccessControlContext() {
        if (acc == null) {
            throw new SecurityException("Component is missing AccessControlContext");
        }
        return acc;
    }

    boolean isPacked = false;


    private int boundsOp = ComponentPeer.DEFAULT_OPERATION;


    public enum BaselineResizeBehavior {

        CONSTANT_ASCENT,


        CONSTANT_DESCENT,


        CENTER_OFFSET,


        OTHER
    }


    private transient Region compoundShape = null;


    private transient Region mixingCutoutRegion = null;


    private transient boolean isAddNotifyComplete = false;


    int getBoundsOp() {
        assert Thread.holdsLock(getTreeLock());
        return boundsOp;
    }

    void setBoundsOp(int op) {
        assert Thread.holdsLock(getTreeLock());
        if (op == ComponentPeer.RESET_OPERATION) {
            boundsOp = ComponentPeer.DEFAULT_OPERATION;
        } else
            if (boundsOp == ComponentPeer.DEFAULT_OPERATION) {
                boundsOp = op;
            }
    }

    // Whether this Component has had the background erase flag
    // specified via SunToolkit.disableBackgroundErase(). This is
    // needed in order to make this function work on X11 platforms,
    // where currently there is no chance to interpose on the creation
    // of the peer and therefore the call to XSetBackground.
    transient boolean backgroundEraseDisabled;

    static {
        AWTAccessor.setComponentAccessor(new AWTAccessor.ComponentAccessor() {
            public void setBackgroundEraseDisabled(Component comp, boolean disabled) {
                comp.backgroundEraseDisabled = disabled;
            }
            public boolean getBackgroundEraseDisabled(Component comp) {
                return comp.backgroundEraseDisabled;
            }
            public Rectangle getBounds(Component comp) {
                return new Rectangle(comp.x, comp.y, comp.width, comp.height);
            }
            public void setMixingCutoutShape(Component comp, Shape shape) {
                Region region = shape == null ?  null :
                    Region.getInstance(shape, null);

                synchronized (comp.getTreeLock()) {
                    boolean needShowing = false;
                    boolean needHiding = false;

                    if (!comp.isNonOpaqueForMixing()) {
                        needHiding = true;
                    }

                    comp.mixingCutoutRegion = region;

                    if (!comp.isNonOpaqueForMixing()) {
                        needShowing = true;
                    }

                    if (comp.isMixingNeeded()) {
                        if (needHiding) {
                            comp.mixOnHiding(comp.isLightweight());
                        }
                        if (needShowing) {
                            comp.mixOnShowing();
                        }
                    }
                }
            }

            public void setGraphicsConfiguration(Component comp,
                    GraphicsConfiguration gc)
            {
                comp.setGraphicsConfiguration(gc);
            }
            public boolean requestFocus(Component comp, CausedFocusEvent.Cause cause) {
                return comp.requestFocus(cause);
            }
            public boolean canBeFocusOwner(Component comp) {
                return comp.canBeFocusOwner();
            }

            public boolean isVisible(Component comp) {
                return comp.isVisible_NoClientCode();
            }
            public void setRequestFocusController
                (RequestFocusController requestController)
            {
                 Component.setRequestFocusController(requestController);
            }
            public AppContext getAppContext(Component comp) {
                 return comp.appContext;
            }
            public void setAppContext(Component comp, AppContext appContext) {
                 comp.appContext = appContext;
            }
            public Container getParent(Component comp) {
                return comp.getParent_NoClientCode();
            }
            public void setParent(Component comp, Container parent) {
                comp.parent = parent;
            }
            public void setSize(Component comp, int width, int height) {
                comp.width = width;
                comp.height = height;
            }
            public Point getLocation(Component comp) {
                return comp.location_NoClientCode();
            }
            public void setLocation(Component comp, int x, int y) {
                comp.x = x;
                comp.y = y;
            }
            public boolean isEnabled(Component comp) {
                return comp.isEnabledImpl();
            }
            public boolean isDisplayable(Component comp) {
                return comp.peer != null;
            }
            public Cursor getCursor(Component comp) {
                return comp.getCursor_NoClientCode();
            }
            public ComponentPeer getPeer(Component comp) {
                return comp.peer;
            }
            public void setPeer(Component comp, ComponentPeer peer) {
                comp.peer = peer;
            }
            public boolean isLightweight(Component comp) {
                return (comp.peer instanceof LightweightPeer);
            }
            public boolean getIgnoreRepaint(Component comp) {
                return comp.ignoreRepaint;
            }
            public int getWidth(Component comp) {
                return comp.width;
            }
            public int getHeight(Component comp) {
                return comp.height;
            }
            public int getX(Component comp) {
                return comp.x;
            }
            public int getY(Component comp) {
                return comp.y;
            }
            public Color getForeground(Component comp) {
                return comp.foreground;
            }
            public Color getBackground(Component comp) {
                return comp.background;
            }
            public void setBackground(Component comp, Color background) {
                comp.background = background;
            }
            public Font getFont(Component comp) {
                return comp.getFont_NoClientCode();
            }
            public void processEvent(Component comp, AWTEvent e) {
                comp.processEvent(e);
            }

            public AccessControlContext getAccessControlContext(Component comp) {
                return comp.getAccessControlContext();
            }

            public void revalidateSynchronously(Component comp) {
                comp.revalidateSynchronously();
            }
        });
    }


    protected Component() {
        appContext = AppContext.getAppContext();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    void initializeFocusTraversalKeys() {
        focusTraversalKeys = new Set[3];
    }


    String constructComponentName() {
        return null; // For strict compliance with prior platform versions, a Component
                     // that doesn't set its name should return null from
                     // getName()
    }


    public String getName() {
        if (name == null && !nameExplicitlySet) {
            synchronized(getObjectLock()) {
                if (name == null && !nameExplicitlySet)
                    name = constructComponentName();
            }
        }
        return name;
    }


    public void setName(String name) {
        String oldName;
        synchronized(getObjectLock()) {
            oldName = this.name;
            this.name = name;
            nameExplicitlySet = true;
        }
        firePropertyChange("name", oldName, name);
    }


    public Container getParent() {
        return getParent_NoClientCode();
    }

    // NOTE: This method may be called by privileged threads.
    //       This functionality is implemented in a package-private method
    //       to insure that it cannot be overridden by client subclasses.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    final Container getParent_NoClientCode() {
        return parent;
    }

    // This method is overridden in the Window class to return null,
    //    because the parent field of the Window object contains
    //    the owner of the window, not its parent.
    Container getContainer() {
        return getParent_NoClientCode();
    }


    @Deprecated
    public ComponentPeer getPeer() {
        return peer;
    }



    public synchronized void setDropTarget(DropTarget dt) {
        if (dt == dropTarget || (dropTarget != null && dropTarget.equals(dt)))
            return;

        DropTarget old;

        if ((old = dropTarget) != null) {
            if (peer != null) dropTarget.removeNotify(peer);

            DropTarget t = dropTarget;

            dropTarget = null;

            try {
                t.setComponent(null);
            } catch (IllegalArgumentException iae) {
                // ignore it.
            }
        }

        // if we have a new one, and we have a peer, add it!

        if ((dropTarget = dt) != null) {
            try {
                dropTarget.setComponent(this);
                if (peer != null) dropTarget.addNotify(peer);
            } catch (IllegalArgumentException iae) {
                if (old != null) {
                    try {
                        old.setComponent(this);
                        if (peer != null) dropTarget.addNotify(peer);
                    } catch (IllegalArgumentException iae1) {
                        // ignore it!
                    }
                }
            }
        }
    }



    public synchronized DropTarget getDropTarget() { return dropTarget; }


    public GraphicsConfiguration getGraphicsConfiguration() {
        return getGraphicsConfiguration_NoClientCode();
    }

    final GraphicsConfiguration getGraphicsConfiguration_NoClientCode() {
        return graphicsConfig;
    }

    void setGraphicsConfiguration(GraphicsConfiguration gc) {
        synchronized(getTreeLock()) {
            if (updateGraphicsData(gc)) {
                removeNotify();
                addNotify();
            }
        }
    }

    boolean updateGraphicsData(GraphicsConfiguration gc) {
        checkTreeLock();

        if (graphicsConfig == gc) {
            return false;
        }

        graphicsConfig = gc;

        ComponentPeer peer = getPeer();
        if (peer != null) {
            return peer.updateGraphicsData(gc);
        }
        return false;
    }


    void checkGD(String stringID) {
        if (graphicsConfig != null) {
            if (!graphicsConfig.getDevice().getIDstring().equals(stringID)) {
                throw new IllegalArgumentException(
                                                   "adding a container to a container on a different GraphicsDevice");
            }
        }
    }


    public final Object getTreeLock() {
        return LOCK;
    }

    final void checkTreeLock() {
        if (!Thread.holdsLock(getTreeLock())) {
            throw new IllegalStateException("This function should be called while holding treeLock");
        }
    }


    public Toolkit getToolkit() {
        return getToolkitImpl();
    }


    final Toolkit getToolkitImpl() {
        Container parent = this.parent;
        if (parent != null) {
            return parent.getToolkitImpl();
        }
        return Toolkit.getDefaultToolkit();
    }


    public boolean isValid() {
        return (peer != null) && valid;
    }


    public boolean isDisplayable() {
        return getPeer() != null;
    }


    @Transient
    public boolean isVisible() {
        return isVisible_NoClientCode();
    }
    final boolean isVisible_NoClientCode() {
        return visible;
    }


    boolean isRecursivelyVisible() {
        return visible && (parent == null || parent.isRecursivelyVisible());
    }


    private Rectangle getRecursivelyVisibleBounds() {
        final Component container = getContainer();
        final Rectangle bounds = getBounds();
        if (container == null) {
            // we are top level window or haven't a container, return our bounds
            return bounds;
        }
        // translate the container's bounds to our coordinate space
        final Rectangle parentsBounds = container.getRecursivelyVisibleBounds();
        parentsBounds.setLocation(0, 0);
        return parentsBounds.intersection(bounds);
    }


    Point pointRelativeToComponent(Point absolute) {
        Point compCoords = getLocationOnScreen();
        return new Point(absolute.x - compCoords.x,
                         absolute.y - compCoords.y);
    }


    Component findUnderMouseInWindow(PointerInfo pi) {
        if (!isShowing()) {
            return null;
        }
        Window win = getContainingWindow();
        if (!Toolkit.getDefaultToolkit().getMouseInfoPeer().isWindowUnderMouse(win)) {
            return null;
        }
        final boolean INCLUDE_DISABLED = true;
        Point relativeToWindow = win.pointRelativeToComponent(pi.getLocation());
        Component inTheSameWindow = win.findComponentAt(relativeToWindow.x,
                                                        relativeToWindow.y,
                                                        INCLUDE_DISABLED);
        return inTheSameWindow;
    }


    public Point getMousePosition() throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }

        PointerInfo pi = java.security.AccessController.doPrivileged(
                                                                     new java.security.PrivilegedAction<PointerInfo>() {
                                                                         public PointerInfo run() {
                                                                             return MouseInfo.getPointerInfo();
                                                                         }
                                                                     }
                                                                     );

        synchronized (getTreeLock()) {
            Component inTheSameWindow = findUnderMouseInWindow(pi);
            if (!isSameOrAncestorOf(inTheSameWindow, true)) {
                return null;
            }
            return pointRelativeToComponent(pi.getLocation());
        }
    }


    boolean isSameOrAncestorOf(Component comp, boolean allowChildren) {
        return comp == this;
    }


    public boolean isShowing() {
        if (visible && (peer != null)) {
            Container parent = this.parent;
            return (parent == null) || parent.isShowing();
        }
        return false;
    }


    public boolean isEnabled() {
        return isEnabledImpl();
    }


    final boolean isEnabledImpl() {
        return enabled;
    }


    public void setEnabled(boolean b) {
        enable(b);
    }


    @Deprecated
    public void enable() {
        if (!enabled) {
            synchronized (getTreeLock()) {
                enabled = true;
                ComponentPeer peer = this.peer;
                if (peer != null) {
                    peer.setEnabled(true);
                    if (visible && !getRecursivelyVisibleBounds().isEmpty()) {
                        updateCursorImmediately();
                    }
                }
            }
            if (accessibleContext != null) {
                accessibleContext.firePropertyChange(
                                                     AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                     null, AccessibleState.ENABLED);
            }
        }
    }


    @Deprecated
    public void enable(boolean b) {
        if (b) {
            enable();
        } else {
            disable();
        }
    }


    @Deprecated
    public void disable() {
        if (enabled) {
            KeyboardFocusManager.clearMostRecentFocusOwner(this);
            synchronized (getTreeLock()) {
                enabled = false;
                // A disabled lw container is allowed to contain a focus owner.
                if ((isFocusOwner() || (containsFocus() && !isLightweight())) &&
                    KeyboardFocusManager.isAutoFocusTransferEnabled())
                {
                    // Don't clear the global focus owner. If transferFocus
                    // fails, we want the focus to stay on the disabled
                    // Component so that keyboard traversal, et. al. still
                    // makes sense to the user.
                    transferFocus(false);
                }
                ComponentPeer peer = this.peer;
                if (peer != null) {
                    peer.setEnabled(false);
                    if (visible && !getRecursivelyVisibleBounds().isEmpty()) {
                        updateCursorImmediately();
                    }
                }
            }
            if (accessibleContext != null) {
                accessibleContext.firePropertyChange(
                                                     AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                     null, AccessibleState.ENABLED);
            }
        }
    }


    public boolean isDoubleBuffered() {
        return false;
    }


    public void enableInputMethods(boolean enable) {
        if (enable) {
            if ((eventMask & AWTEvent.INPUT_METHODS_ENABLED_MASK) != 0)
                return;

            // If this component already has focus, then activate the
            // input method by dispatching a synthesized focus gained
            // event.
            if (isFocusOwner()) {
                InputContext inputContext = getInputContext();
                if (inputContext != null) {
                    FocusEvent focusGainedEvent =
                        new FocusEvent(this, FocusEvent.FOCUS_GAINED);
                    inputContext.dispatchEvent(focusGainedEvent);
                }
            }

            eventMask |= AWTEvent.INPUT_METHODS_ENABLED_MASK;
        } else {
            if ((eventMask & AWTEvent.INPUT_METHODS_ENABLED_MASK) != 0) {
                InputContext inputContext = getInputContext();
                if (inputContext != null) {
                    inputContext.endComposition();
                    inputContext.removeNotify(this);
                }
            }
            eventMask &= ~AWTEvent.INPUT_METHODS_ENABLED_MASK;
        }
    }


    public void setVisible(boolean b) {
        show(b);
    }


    @Deprecated
    public void show() {
        if (!visible) {
            synchronized (getTreeLock()) {
                visible = true;
                mixOnShowing();
                ComponentPeer peer = this.peer;
                if (peer != null) {
                    peer.setVisible(true);
                    createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED,
                                          this, parent,
                                          HierarchyEvent.SHOWING_CHANGED,
                                          Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
                    if (peer instanceof LightweightPeer) {
                        repaint();
                    }
                    updateCursorImmediately();
                }

                if (componentListener != null ||
                    (eventMask & AWTEvent.COMPONENT_EVENT_MASK) != 0 ||
                    Toolkit.enabledOnToolkit(AWTEvent.COMPONENT_EVENT_MASK)) {
                    ComponentEvent e = new ComponentEvent(this,
                                                          ComponentEvent.COMPONENT_SHOWN);
                    Toolkit.getEventQueue().postEvent(e);
                }
            }
            Container parent = this.parent;
            if (parent != null) {
                parent.invalidate();
            }
        }
    }


    @Deprecated
    public void show(boolean b) {
        if (b) {
            show();
        } else {
            hide();
        }
    }

    boolean containsFocus() {
        return isFocusOwner();
    }

    void clearMostRecentFocusOwnerOnHide() {
        KeyboardFocusManager.clearMostRecentFocusOwner(this);
    }

    void clearCurrentFocusCycleRootOnHide() {

    }


    @Deprecated
    public void hide() {
        isPacked = false;

        if (visible) {
            clearCurrentFocusCycleRootOnHide();
            clearMostRecentFocusOwnerOnHide();
            synchronized (getTreeLock()) {
                visible = false;
                mixOnHiding(isLightweight());
                if (containsFocus() && KeyboardFocusManager.isAutoFocusTransferEnabled()) {
                    transferFocus(true);
                }
                ComponentPeer peer = this.peer;
                if (peer != null) {
                    peer.setVisible(false);
                    createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED,
                                          this, parent,
                                          HierarchyEvent.SHOWING_CHANGED,
                                          Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
                    if (peer instanceof LightweightPeer) {
                        repaint();
                    }
                    updateCursorImmediately();
                }
                if (componentListener != null ||
                    (eventMask & AWTEvent.COMPONENT_EVENT_MASK) != 0 ||
                    Toolkit.enabledOnToolkit(AWTEvent.COMPONENT_EVENT_MASK)) {
                    ComponentEvent e = new ComponentEvent(this,
                                                          ComponentEvent.COMPONENT_HIDDEN);
                    Toolkit.getEventQueue().postEvent(e);
                }
            }
            Container parent = this.parent;
            if (parent != null) {
                parent.invalidate();
            }
        }
    }


    @Transient
    public Color getForeground() {
        Color foreground = this.foreground;
        if (foreground != null) {
            return foreground;
        }
        Container parent = this.parent;
        return (parent != null) ? parent.getForeground() : null;
    }


    public void setForeground(Color c) {
        Color oldColor = foreground;
        ComponentPeer peer = this.peer;
        foreground = c;
        if (peer != null) {
            c = getForeground();
            if (c != null) {
                peer.setForeground(c);
            }
        }
        // This is a bound property, so report the change to
        // any registered listeners.  (Cheap if there are none.)
        firePropertyChange("foreground", oldColor, c);
    }


    public boolean isForegroundSet() {
        return (foreground != null);
    }


    @Transient
    public Color getBackground() {
        Color background = this.background;
        if (background != null) {
            return background;
        }
        Container parent = this.parent;
        return (parent != null) ? parent.getBackground() : null;
    }


    public void setBackground(Color c) {
        Color oldColor = background;
        ComponentPeer peer = this.peer;
        background = c;
        if (peer != null) {
            c = getBackground();
            if (c != null) {
                peer.setBackground(c);
            }
        }
        // This is a bound property, so report the change to
        // any registered listeners.  (Cheap if there are none.)
        firePropertyChange("background", oldColor, c);
    }


    public boolean isBackgroundSet() {
        return (background != null);
    }


    @Transient
    public Font getFont() {
        return getFont_NoClientCode();
    }

    // NOTE: This method may be called by privileged threads.
    //       This functionality is implemented in a package-private method
    //       to insure that it cannot be overridden by client subclasses.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    final Font getFont_NoClientCode() {
        Font font = this.font;
        if (font != null) {
            return font;
        }
        Container parent = this.parent;
        return (parent != null) ? parent.getFont_NoClientCode() : null;
    }


    public void setFont(Font f) {
        Font oldFont, newFont;
        synchronized(getTreeLock()) {
            oldFont = font;
            newFont = font = f;
            ComponentPeer peer = this.peer;
            if (peer != null) {
                f = getFont();
                if (f != null) {
                    peer.setFont(f);
                    peerFont = f;
                }
            }
        }
        // This is a bound property, so report the change to
        // any registered listeners.  (Cheap if there are none.)
        firePropertyChange("font", oldFont, newFont);

        // This could change the preferred size of the Component.
        // Fix for 6213660. Should compare old and new fonts and do not
        // call invalidate() if they are equal.
        if (f != oldFont && (oldFont == null ||
                                      !oldFont.equals(f))) {
            invalidateIfValid();
        }
    }


    public boolean isFontSet() {
        return (font != null);
    }


    public Locale getLocale() {
        Locale locale = this.locale;
        if (locale != null) {
            return locale;
        }
        Container parent = this.parent;

        if (parent == null) {
            throw new IllegalComponentStateException("This component must have a parent in order to determine its locale");
        } else {
            return parent.getLocale();
        }
    }


    public void setLocale(Locale l) {
        Locale oldValue = locale;
        locale = l;

        // This is a bound property, so report the change to
        // any registered listeners.  (Cheap if there are none.)
        firePropertyChange("locale", oldValue, l);

        // This could change the preferred size of the Component.
        invalidateIfValid();
    }


    public ColorModel getColorModel() {
        ComponentPeer peer = this.peer;
        if ((peer != null) && ! (peer instanceof LightweightPeer)) {
            return peer.getColorModel();
        } else if (GraphicsEnvironment.isHeadless()) {
            return ColorModel.getRGBdefault();
        } // else
        return getToolkit().getColorModel();
    }


    public Point getLocation() {
        return location();
    }


    public Point getLocationOnScreen() {
        synchronized (getTreeLock()) {
            return getLocationOnScreen_NoTreeLock();
        }
    }


    final Point getLocationOnScreen_NoTreeLock() {

        if (peer != null && isShowing()) {
            if (peer instanceof LightweightPeer) {
                // lightweight component location needs to be translated
                // relative to a native component.
                Container host = getNativeContainer();
                Point pt = host.peer.getLocationOnScreen();
                for(Component c = this; c != host; c = c.getParent()) {
                    pt.x += c.x;
                    pt.y += c.y;
                }
                return pt;
            } else {
                Point pt = peer.getLocationOnScreen();
                return pt;
            }
        } else {
            throw new IllegalComponentStateException("component must be showing on the screen to determine its location");
        }
    }



    @Deprecated
    public Point location() {
        return location_NoClientCode();
    }

    private Point location_NoClientCode() {
        return new Point(x, y);
    }


    public void setLocation(int x, int y) {
        move(x, y);
    }


    @Deprecated
    public void move(int x, int y) {
        synchronized(getTreeLock()) {
            setBoundsOp(ComponentPeer.SET_LOCATION);
            setBounds(x, y, width, height);
        }
    }


    public void setLocation(Point p) {
        setLocation(p.x, p.y);
    }


    public Dimension getSize() {
        return size();
    }


    @Deprecated
    public Dimension size() {
        return new Dimension(width, height);
    }


    public void setSize(int width, int height) {
        resize(width, height);
    }


    @Deprecated
    public void resize(int width, int height) {
        synchronized(getTreeLock()) {
            setBoundsOp(ComponentPeer.SET_SIZE);
            setBounds(x, y, width, height);
        }
    }


    public void setSize(Dimension d) {
        resize(d);
    }


    @Deprecated
    public void resize(Dimension d) {
        setSize(d.width, d.height);
    }


    public Rectangle getBounds() {
        return bounds();
    }


    @Deprecated
    public Rectangle bounds() {
        return new Rectangle(x, y, width, height);
    }


    public void setBounds(int x, int y, int width, int height) {
        reshape(x, y, width, height);
    }


    @Deprecated
    public void reshape(int x, int y, int width, int height) {
        synchronized (getTreeLock()) {
            try {
                setBoundsOp(ComponentPeer.SET_BOUNDS);
                boolean resized = (this.width != width) || (this.height != height);
                boolean moved = (this.x != x) || (this.y != y);
                if (!resized && !moved) {
                    return;
                }
                int oldX = this.x;
                int oldY = this.y;
                int oldWidth = this.width;
                int oldHeight = this.height;
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;

                if (resized) {
                    isPacked = false;
                }

                boolean needNotify = true;
                mixOnReshaping();
                if (peer != null) {
                    // LightwightPeer is an empty stub so can skip peer.reshape
                    if (!(peer instanceof LightweightPeer)) {
                        reshapeNativePeer(x, y, width, height, getBoundsOp());
                        // Check peer actualy changed coordinates
                        resized = (oldWidth != this.width) || (oldHeight != this.height);
                        moved = (oldX != this.x) || (oldY != this.y);
                        // fix for 5025858: do not send ComponentEvents for toplevel
                        // windows here as it is done from peer or native code when
                        // the window is really resized or moved, otherwise some
                        // events may be sent twice
                        if (this instanceof Window) {
                            needNotify = false;
                        }
                    }
                    if (resized) {
                        invalidate();
                    }
                    if (parent != null) {
                        parent.invalidateIfValid();
                    }
                }
                if (needNotify) {
                    notifyNewBounds(resized, moved);
                }
                repaintParentIfNeeded(oldX, oldY, oldWidth, oldHeight);
            } finally {
                setBoundsOp(ComponentPeer.RESET_OPERATION);
            }
        }
    }

    private void repaintParentIfNeeded(int oldX, int oldY, int oldWidth,
                                       int oldHeight)
    {
        if (parent != null && peer instanceof LightweightPeer && isShowing()) {
            // Have the parent redraw the area this component occupied.
            parent.repaint(oldX, oldY, oldWidth, oldHeight);
            // Have the parent redraw the area this component *now* occupies.
            repaint();
        }
    }

    private void reshapeNativePeer(int x, int y, int width, int height, int op) {
        // native peer might be offset by more than direct
        // parent since parent might be lightweight.
        int nativeX = x;
        int nativeY = y;
        for (Component c = parent;
             (c != null) && (c.peer instanceof LightweightPeer);
             c = c.parent)
        {
            nativeX += c.x;
            nativeY += c.y;
        }
        peer.setBounds(nativeX, nativeY, width, height, op);
    }

    @SuppressWarnings("deprecation")
    private void notifyNewBounds(boolean resized, boolean moved) {
        if (componentListener != null
            || (eventMask & AWTEvent.COMPONENT_EVENT_MASK) != 0
            || Toolkit.enabledOnToolkit(AWTEvent.COMPONENT_EVENT_MASK))
            {
                if (resized) {
                    ComponentEvent e = new ComponentEvent(this,
                                                          ComponentEvent.COMPONENT_RESIZED);
                    Toolkit.getEventQueue().postEvent(e);
                }
                if (moved) {
                    ComponentEvent e = new ComponentEvent(this,
                                                          ComponentEvent.COMPONENT_MOVED);
                    Toolkit.getEventQueue().postEvent(e);
                }
            } else {
                if (this instanceof Container && ((Container)this).countComponents() > 0) {
                    boolean enabledOnToolkit =
                        Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK);
                    if (resized) {

                        ((Container)this).createChildHierarchyEvents(
                                                                     HierarchyEvent.ANCESTOR_RESIZED, 0, enabledOnToolkit);
                    }
                    if (moved) {
                        ((Container)this).createChildHierarchyEvents(
                                                                     HierarchyEvent.ANCESTOR_MOVED, 0, enabledOnToolkit);
                    }
                }
                }
    }


    public void setBounds(Rectangle r) {
        setBounds(r.x, r.y, r.width, r.height);
    }



    public int getX() {
        return x;
    }



    public int getY() {
        return y;
    }



    public int getWidth() {
        return width;
    }



    public int getHeight() {
        return height;
    }


    public Rectangle getBounds(Rectangle rv) {
        if (rv == null) {
            return new Rectangle(getX(), getY(), getWidth(), getHeight());
        }
        else {
            rv.setBounds(getX(), getY(), getWidth(), getHeight());
            return rv;
        }
    }


    public Dimension getSize(Dimension rv) {
        if (rv == null) {
            return new Dimension(getWidth(), getHeight());
        }
        else {
            rv.setSize(getWidth(), getHeight());
            return rv;
        }
    }


    public Point getLocation(Point rv) {
        if (rv == null) {
            return new Point(getX(), getY());
        }
        else {
            rv.setLocation(getX(), getY());
            return rv;
        }
    }


    public boolean isOpaque() {
        if (getPeer() == null) {
            return false;
        }
        else {
            return !isLightweight();
        }
    }



    public boolean isLightweight() {
        return getPeer() instanceof LightweightPeer;
    }



    public void setPreferredSize(Dimension preferredSize) {
        Dimension old;
        // If the preferred size was set, use it as the old value, otherwise
        // use null to indicate we didn't previously have a set preferred
        // size.
        if (prefSizeSet) {
            old = this.prefSize;
        }
        else {
            old = null;
        }
        this.prefSize = preferredSize;
        prefSizeSet = (preferredSize != null);
        firePropertyChange("preferredSize", old, preferredSize);
    }



    public boolean isPreferredSizeSet() {
        return prefSizeSet;
    }



    public Dimension getPreferredSize() {
        return preferredSize();
    }



    @Deprecated
    public Dimension preferredSize() {

        Dimension dim = prefSize;
        if (dim == null || !(isPreferredSizeSet() || isValid())) {
            synchronized (getTreeLock()) {
                prefSize = (peer != null) ?
                    peer.getPreferredSize() :
                    getMinimumSize();
                dim = prefSize;
            }
        }
        return new Dimension(dim);
    }


    public void setMinimumSize(Dimension minimumSize) {
        Dimension old;
        // If the minimum size was set, use it as the old value, otherwise
        // use null to indicate we didn't previously have a set minimum
        // size.
        if (minSizeSet) {
            old = this.minSize;
        }
        else {
            old = null;
        }
        this.minSize = minimumSize;
        minSizeSet = (minimumSize != null);
        firePropertyChange("minimumSize", old, minimumSize);
    }


    public boolean isMinimumSizeSet() {
        return minSizeSet;
    }


    public Dimension getMinimumSize() {
        return minimumSize();
    }


    @Deprecated
    public Dimension minimumSize() {

        Dimension dim = minSize;
        if (dim == null || !(isMinimumSizeSet() || isValid())) {
            synchronized (getTreeLock()) {
                minSize = (peer != null) ?
                    peer.getMinimumSize() :
                    size();
                dim = minSize;
            }
        }
        return new Dimension(dim);
    }


    public void setMaximumSize(Dimension maximumSize) {
        // If the maximum size was set, use it as the old value, otherwise
        // use null to indicate we didn't previously have a set maximum
        // size.
        Dimension old;
        if (maxSizeSet) {
            old = this.maxSize;
        }
        else {
            old = null;
        }
        this.maxSize = maximumSize;
        maxSizeSet = (maximumSize != null);
        firePropertyChange("maximumSize", old, maximumSize);
    }


    public boolean isMaximumSizeSet() {
        return maxSizeSet;
    }


    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return new Dimension(maxSize);
        }
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }


    public float getAlignmentX() {
        return CENTER_ALIGNMENT;
    }


    public float getAlignmentY() {
        return CENTER_ALIGNMENT;
    }


    public int getBaseline(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(
                    "Width and height must be >= 0");
        }
        return -1;
    }


    public BaselineResizeBehavior getBaselineResizeBehavior() {
        return BaselineResizeBehavior.OTHER;
    }


    public void doLayout() {
        layout();
    }


    @Deprecated
    public void layout() {
    }


    public void validate() {
        synchronized (getTreeLock()) {
            ComponentPeer peer = this.peer;
            boolean wasValid = isValid();
            if (!wasValid && peer != null) {
                Font newfont = getFont();
                Font oldfont = peerFont;
                if (newfont != oldfont && (oldfont == null
                                           || !oldfont.equals(newfont))) {
                    peer.setFont(newfont);
                    peerFont = newfont;
                }
                peer.layout();
            }
            valid = true;
            if (!wasValid) {
                mixOnValidating();
            }
        }
    }


    public void invalidate() {
        synchronized (getTreeLock()) {

            valid = false;
            if (!isPreferredSizeSet()) {
                prefSize = null;
            }
            if (!isMinimumSizeSet()) {
                minSize = null;
            }
            if (!isMaximumSizeSet()) {
                maxSize = null;
            }
            invalidateParent();
        }
    }


    void invalidateParent() {
        if (parent != null) {
            parent.invalidateIfValid();
        }
    }


    final void invalidateIfValid() {
        if (isValid()) {
            invalidate();
        }
    }


    public void revalidate() {
        revalidateSynchronously();
    }


    final void revalidateSynchronously() {
        synchronized (getTreeLock()) {
            invalidate();

            Container root = getContainer();
            if (root == null) {
                // There's no parents. Just validate itself.
                validate();
            } else {
                while (!root.isValidateRoot()) {
                    if (root.getContainer() == null) {
                        // If there's no validate roots, we'll validate the
                        // topmost container
                        break;
                    }

                    root = root.getContainer();
                }

                root.validate();
            }
        }
    }


    public Graphics getGraphics() {
        if (peer instanceof LightweightPeer) {
            // This is for a lightweight component, need to
            // translate coordinate spaces and clip relative
            // to the parent.
            if (parent == null) return null;
            Graphics g = parent.getGraphics();
            if (g == null) return null;
            if (g instanceof ConstrainableGraphics) {
                ((ConstrainableGraphics) g).constrain(x, y, width, height);
            } else {
                g.translate(x,y);
                g.setClip(0, 0, width, height);
            }
            g.setFont(getFont());
            return g;
        } else {
            ComponentPeer peer = this.peer;
            return (peer != null) ? peer.getGraphics() : null;
        }
    }

    final Graphics getGraphics_NoClientCode() {
        ComponentPeer peer = this.peer;
        if (peer instanceof LightweightPeer) {
            // This is for a lightweight component, need to
            // translate coordinate spaces and clip relative
            // to the parent.
            Container parent = this.parent;
            if (parent == null) return null;
            Graphics g = parent.getGraphics_NoClientCode();
            if (g == null) return null;
            if (g instanceof ConstrainableGraphics) {
                ((ConstrainableGraphics) g).constrain(x, y, width, height);
            } else {
                g.translate(x,y);
                g.setClip(0, 0, width, height);
            }
            g.setFont(getFont_NoClientCode());
            return g;
        } else {
            return (peer != null) ? peer.getGraphics() : null;
        }
    }


    public FontMetrics getFontMetrics(Font font) {
        // This is an unsupported hack, but left in for a customer.
        // Do not remove.
        FontManager fm = FontManagerFactory.getInstance();
        if (fm instanceof SunFontManager
            && ((SunFontManager) fm).usePlatformFontMetrics()) {

            if (peer != null &&
                !(peer instanceof LightweightPeer)) {
                return peer.getFontMetrics(font);
            }
        }
        return sun.font.FontDesignMetrics.getMetrics(font);
    }


    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
        updateCursorImmediately();
    }


    final void updateCursorImmediately() {
        if (peer instanceof LightweightPeer) {
            Container nativeContainer = getNativeContainer();

            if (nativeContainer == null) return;

            ComponentPeer cPeer = nativeContainer.getPeer();

            if (cPeer != null) {
                cPeer.updateCursorImmediately();
            }
        } else if (peer != null) {
            peer.updateCursorImmediately();
        }
    }


    public Cursor getCursor() {
        return getCursor_NoClientCode();
    }

    final Cursor getCursor_NoClientCode() {
        Cursor cursor = this.cursor;
        if (cursor != null) {
            return cursor;
        }
        Container parent = this.parent;
        if (parent != null) {
            return parent.getCursor_NoClientCode();
        } else {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
    }


    public boolean isCursorSet() {
        return (cursor != null);
    }


    public void paint(Graphics g) {
    }


    public void update(Graphics g) {
        paint(g);
    }


    public void paintAll(Graphics g) {
        if (isShowing()) {
            GraphicsCallback.PeerPaintCallback.getInstance().
                runOneComponent(this, new Rectangle(0, 0, width, height),
                                g, g.getClip(),
                                GraphicsCallback.LIGHTWEIGHTS |
                                GraphicsCallback.HEAVYWEIGHTS);
        }
    }


    void lightweightPaint(Graphics g) {
        paint(g);
    }


    void paintHeavyweightComponents(Graphics g) {
    }


    public void repaint() {
        repaint(0, 0, 0, width, height);
    }


    public void repaint(long tm) {
        repaint(tm, 0, 0, width, height);
    }


    public void repaint(int x, int y, int width, int height) {
        repaint(0, x, y, width, height);
    }


    public void repaint(long tm, int x, int y, int width, int height) {
        if (this.peer instanceof LightweightPeer) {
            // Needs to be translated to parent coordinates since
            // a parent native container provides the actual repaint
            // services.  Additionally, the request is restricted to
            // the bounds of the component.
            if (parent != null) {
                if (x < 0) {
                    width += x;
                    x = 0;
                }
                if (y < 0) {
                    height += y;
                    y = 0;
                }

                int pwidth = (width > this.width) ? this.width : width;
                int pheight = (height > this.height) ? this.height : height;

                if (pwidth <= 0 || pheight <= 0) {
                    return;
                }

                int px = this.x + x;
                int py = this.y + y;
                parent.repaint(tm, px, py, pwidth, pheight);
            }
        } else {
            if (isVisible() && (this.peer != null) &&
                (width > 0) && (height > 0)) {
                PaintEvent e = new PaintEvent(this, PaintEvent.UPDATE,
                                              new Rectangle(x, y, width, height));
                SunToolkit.postEvent(SunToolkit.targetToAppContext(this), e);
            }
        }
    }


    public void print(Graphics g) {
        paint(g);
    }


    public void printAll(Graphics g) {
        if (isShowing()) {
            GraphicsCallback.PeerPrintCallback.getInstance().
                runOneComponent(this, new Rectangle(0, 0, width, height),
                                g, g.getClip(),
                                GraphicsCallback.LIGHTWEIGHTS |
                                GraphicsCallback.HEAVYWEIGHTS);
        }
    }


    void lightweightPrint(Graphics g) {
        print(g);
    }


    void printHeavyweightComponents(Graphics g) {
    }

    private Insets getInsets_NoClientCode() {
        ComponentPeer peer = this.peer;
        if (peer instanceof ContainerPeer) {
            return (Insets)((ContainerPeer)peer).getInsets().clone();
        }
        return new Insets(0, 0, 0, 0);
    }


    public boolean imageUpdate(Image img, int infoflags,
                               int x, int y, int w, int h) {
        int rate = -1;
        if ((infoflags & (FRAMEBITS|ALLBITS)) != 0) {
            rate = 0;
        } else if ((infoflags & SOMEBITS) != 0) {
            if (isInc) {
                rate = incRate;
                if (rate < 0) {
                    rate = 0;
                }
            }
        }
        if (rate >= 0) {
            repaint(rate, 0, 0, width, height);
        }
        return (infoflags & (ALLBITS|ABORT)) == 0;
    }


    public Image createImage(ImageProducer producer) {
        ComponentPeer peer = this.peer;
        if ((peer != null) && ! (peer instanceof LightweightPeer)) {
            return peer.createImage(producer);
        }
        return getToolkit().createImage(producer);
    }


    public Image createImage(int width, int height) {
        ComponentPeer peer = this.peer;
        if (peer instanceof LightweightPeer) {
            if (parent != null) { return parent.createImage(width, height); }
            else { return null;}
        } else {
            return (peer != null) ? peer.createImage(width, height) : null;
        }
    }


    public VolatileImage createVolatileImage(int width, int height) {
        ComponentPeer peer = this.peer;
        if (peer instanceof LightweightPeer) {
            if (parent != null) {
                return parent.createVolatileImage(width, height);
            }
            else { return null;}
        } else {
            return (peer != null) ?
                peer.createVolatileImage(width, height) : null;
        }
    }


    public VolatileImage createVolatileImage(int width, int height,
                                             ImageCapabilities caps) throws AWTException {
        // REMIND : check caps
        return createVolatileImage(width, height);
    }


    public boolean prepareImage(Image image, ImageObserver observer) {
        return prepareImage(image, -1, -1, observer);
    }


    public boolean prepareImage(Image image, int width, int height,
                                ImageObserver observer) {
        ComponentPeer peer = this.peer;
        if (peer instanceof LightweightPeer) {
            return (parent != null)
                ? parent.prepareImage(image, width, height, observer)
                : getToolkit().prepareImage(image, width, height, observer);
        } else {
            return (peer != null)
                ? peer.prepareImage(image, width, height, observer)
                : getToolkit().prepareImage(image, width, height, observer);
        }
    }


    public int checkImage(Image image, ImageObserver observer) {
        return checkImage(image, -1, -1, observer);
    }


    public int checkImage(Image image, int width, int height,
                          ImageObserver observer) {
        ComponentPeer peer = this.peer;
        if (peer instanceof LightweightPeer) {
            return (parent != null)
                ? parent.checkImage(image, width, height, observer)
                : getToolkit().checkImage(image, width, height, observer);
        } else {
            return (peer != null)
                ? peer.checkImage(image, width, height, observer)
                : getToolkit().checkImage(image, width, height, observer);
        }
    }


    void createBufferStrategy(int numBuffers) {
        BufferCapabilities bufferCaps;
        if (numBuffers > 1) {
            // Try to create a page-flipping strategy
            bufferCaps = new BufferCapabilities(new ImageCapabilities(true),
                                                new ImageCapabilities(true),
                                                BufferCapabilities.FlipContents.UNDEFINED);
            try {
                createBufferStrategy(numBuffers, bufferCaps);
                return; // Success
            } catch (AWTException e) {
                // Failed
            }
        }
        // Try a blitting (but still accelerated) strategy
        bufferCaps = new BufferCapabilities(new ImageCapabilities(true),
                                            new ImageCapabilities(true),
                                            null);
        try {
            createBufferStrategy(numBuffers, bufferCaps);
            return; // Success
        } catch (AWTException e) {
            // Failed
        }
        // Try an unaccelerated blitting strategy
        bufferCaps = new BufferCapabilities(new ImageCapabilities(false),
                                            new ImageCapabilities(false),
                                            null);
        try {
            createBufferStrategy(numBuffers, bufferCaps);
            return; // Success
        } catch (AWTException e) {
            // Code should never reach here (an unaccelerated blitting
            // strategy should always work)
            throw new InternalError("Could not create a buffer strategy", e);
        }
    }


    void createBufferStrategy(int numBuffers,
                              BufferCapabilities caps) throws AWTException {
        // Check arguments
        if (numBuffers < 1) {
            throw new IllegalArgumentException(
                "Number of buffers must be at least 1");
        }
        if (caps == null) {
            throw new IllegalArgumentException("No capabilities specified");
        }
        // Destroy old buffers
        if (bufferStrategy != null) {
            bufferStrategy.dispose();
        }
        if (numBuffers == 1) {
            bufferStrategy = new SingleBufferStrategy(caps);
        } else {
            SunGraphicsEnvironment sge = (SunGraphicsEnvironment)
                GraphicsEnvironment.getLocalGraphicsEnvironment();
            if (!caps.isPageFlipping() && sge.isFlipStrategyPreferred(peer)) {
                caps = new ProxyCapabilities(caps);
            }
            // assert numBuffers > 1;
            if (caps.isPageFlipping()) {
                bufferStrategy = new FlipSubRegionBufferStrategy(numBuffers, caps);
            } else {
                bufferStrategy = new BltSubRegionBufferStrategy(numBuffers, caps);
            }
        }
    }


    private class ProxyCapabilities extends ExtendedBufferCapabilities {
        private BufferCapabilities orig;
        private ProxyCapabilities(BufferCapabilities orig) {
            super(orig.getFrontBufferCapabilities(),
                  orig.getBackBufferCapabilities(),
                  orig.getFlipContents() ==
                      BufferCapabilities.FlipContents.BACKGROUND ?
                      BufferCapabilities.FlipContents.BACKGROUND :
                      BufferCapabilities.FlipContents.COPIED);
            this.orig = orig;
        }
    }


    BufferStrategy getBufferStrategy() {
        return bufferStrategy;
    }


    Image getBackBuffer() {
        if (bufferStrategy != null) {
            if (bufferStrategy instanceof BltBufferStrategy) {
                BltBufferStrategy bltBS = (BltBufferStrategy)bufferStrategy;
                return bltBS.getBackBuffer();
            } else if (bufferStrategy instanceof FlipBufferStrategy) {
                FlipBufferStrategy flipBS = (FlipBufferStrategy)bufferStrategy;
                return flipBS.getBackBuffer();
            }
        }
        return null;
    }


    protected class FlipBufferStrategy extends BufferStrategy {

        protected int numBuffers; // = 0

        protected BufferCapabilities caps; // = null

        protected Image drawBuffer; // = null

        protected VolatileImage drawVBuffer; // = null

        protected boolean validatedContents; // = false

        int width;
        int height;


        protected FlipBufferStrategy(int numBuffers, BufferCapabilities caps)
            throws AWTException
        {
            if (!(Component.this instanceof Window) &&
                !(Component.this instanceof Canvas))
            {
                throw new ClassCastException(
                    "Component must be a Canvas or Window");
            }
            this.numBuffers = numBuffers;
            this.caps = caps;
            createBuffers(numBuffers, caps);
        }


        protected void createBuffers(int numBuffers, BufferCapabilities caps)
            throws AWTException
        {
            if (numBuffers < 2) {
                throw new IllegalArgumentException(
                    "Number of buffers cannot be less than two");
            } else if (peer == null) {
                throw new IllegalStateException(
                    "Component must have a valid peer");
            } else if (caps == null || !caps.isPageFlipping()) {
                throw new IllegalArgumentException(
                    "Page flipping capabilities must be specified");
            }

            // save the current bounds
            width = getWidth();
            height = getHeight();

            if (drawBuffer != null) {
                // dispose the existing backbuffers
                drawBuffer = null;
                drawVBuffer = null;
                destroyBuffers();
                // ... then recreate the backbuffers
            }

            if (caps instanceof ExtendedBufferCapabilities) {
                ExtendedBufferCapabilities ebc =
                    (ExtendedBufferCapabilities)caps;
                if (ebc.getVSync() == VSYNC_ON) {
                    // if this buffer strategy is not allowed to be v-synced,
                    // change the caps that we pass to the peer but keep on
                    // trying to create v-synced buffers;
                    // do not throw IAE here in case it is disallowed, see
                    // ExtendedBufferCapabilities for more info
                    if (!VSyncedBSManager.vsyncAllowed(this)) {
                        caps = ebc.derive(VSYNC_DEFAULT);
                    }
                }
            }

            peer.createBuffers(numBuffers, caps);
            updateInternalBuffers();
        }


        private void updateInternalBuffers() {
            // get the images associated with the draw buffer
            drawBuffer = getBackBuffer();
            if (drawBuffer instanceof VolatileImage) {
                drawVBuffer = (VolatileImage)drawBuffer;
            } else {
                drawVBuffer = null;
            }
        }


        protected Image getBackBuffer() {
            if (peer != null) {
                return peer.getBackBuffer();
            } else {
                throw new IllegalStateException(
                    "Component must have a valid peer");
            }
        }


        protected void flip(BufferCapabilities.FlipContents flipAction) {
            if (peer != null) {
                Image backBuffer = getBackBuffer();
                if (backBuffer != null) {
                    peer.flip(0, 0,
                              backBuffer.getWidth(null),
                              backBuffer.getHeight(null), flipAction);
                }
            } else {
                throw new IllegalStateException(
                    "Component must have a valid peer");
            }
        }

        void flipSubRegion(int x1, int y1, int x2, int y2,
                      BufferCapabilities.FlipContents flipAction)
        {
            if (peer != null) {
                peer.flip(x1, y1, x2, y2, flipAction);
            } else {
                throw new IllegalStateException(
                    "Component must have a valid peer");
            }
        }


        protected void destroyBuffers() {
            VSyncedBSManager.releaseVsync(this);
            if (peer != null) {
                peer.destroyBuffers();
            } else {
                throw new IllegalStateException(
                    "Component must have a valid peer");
            }
        }


        public BufferCapabilities getCapabilities() {
            if (caps instanceof ProxyCapabilities) {
                return ((ProxyCapabilities)caps).orig;
            } else {
                return caps;
            }
        }


        public Graphics getDrawGraphics() {
            revalidate();
            return drawBuffer.getGraphics();
        }


        protected void revalidate() {
            revalidate(true);
        }

        void revalidate(boolean checkSize) {
            validatedContents = false;

            if (checkSize && (getWidth() != width || getHeight() != height)) {
                // component has been resized; recreate the backbuffers
                try {
                    createBuffers(numBuffers, caps);
                } catch (AWTException e) {
                    // shouldn't be possible
                }
                validatedContents = true;
            }

            // get the buffers from the peer every time since they
            // might have been replaced in response to a display change event
            updateInternalBuffers();

            // now validate the backbuffer
            if (drawVBuffer != null) {
                GraphicsConfiguration gc =
                        getGraphicsConfiguration_NoClientCode();
                int returnCode = drawVBuffer.validate(gc);
                if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                    try {
                        createBuffers(numBuffers, caps);
                    } catch (AWTException e) {
                        // shouldn't be possible
                    }
                    if (drawVBuffer != null) {
                        // backbuffers were recreated, so validate again
                        drawVBuffer.validate(gc);
                    }
                    validatedContents = true;
                } else if (returnCode == VolatileImage.IMAGE_RESTORED) {
                    validatedContents = true;
                }
            }
        }


        public boolean contentsLost() {
            if (drawVBuffer == null) {
                return false;
            }
            return drawVBuffer.contentsLost();
        }


        public boolean contentsRestored() {
            return validatedContents;
        }


        public void show() {
            flip(caps.getFlipContents());
        }


        void showSubRegion(int x1, int y1, int x2, int y2) {
            flipSubRegion(x1, y1, x2, y2, caps.getFlipContents());
        }


        public void dispose() {
            if (Component.this.bufferStrategy == this) {
                Component.this.bufferStrategy = null;
                if (peer != null) {
                    destroyBuffers();
                }
            }
        }

    } // Inner class FlipBufferStrategy


    protected class BltBufferStrategy extends BufferStrategy {


        protected BufferCapabilities caps; // = null

        protected VolatileImage[] backBuffers; // = null

        protected boolean validatedContents; // = false

        protected int width;
        protected int height;


        private Insets insets;


        protected BltBufferStrategy(int numBuffers, BufferCapabilities caps) {
            this.caps = caps;
            createBackBuffers(numBuffers - 1);
        }


        public void dispose() {
            if (backBuffers != null) {
                for (int counter = backBuffers.length - 1; counter >= 0;
                     counter--) {
                    if (backBuffers[counter] != null) {
                        backBuffers[counter].flush();
                        backBuffers[counter] = null;
                    }
                }
            }
            if (Component.this.bufferStrategy == this) {
                Component.this.bufferStrategy = null;
            }
        }


        protected void createBackBuffers(int numBuffers) {
            if (numBuffers == 0) {
                backBuffers = null;
            } else {
                // save the current bounds
                width = getWidth();
                height = getHeight();
                insets = getInsets_NoClientCode();
                int iWidth = width - insets.left - insets.right;
                int iHeight = height - insets.top - insets.bottom;

                // It is possible for the component's width and/or height
                // to be 0 here.  Force the size of the backbuffers to
                // be > 0 so that creating the image won't fail.
                iWidth = Math.max(1, iWidth);
                iHeight = Math.max(1, iHeight);
                if (backBuffers == null) {
                    backBuffers = new VolatileImage[numBuffers];
                } else {
                    // flush any existing backbuffers
                    for (int i = 0; i < numBuffers; i++) {
                        if (backBuffers[i] != null) {
                            backBuffers[i].flush();
                            backBuffers[i] = null;
                        }
                    }
                }

                // create the backbuffers
                for (int i = 0; i < numBuffers; i++) {
                    backBuffers[i] = createVolatileImage(iWidth, iHeight);
                }
            }
        }


        public BufferCapabilities getCapabilities() {
            return caps;
        }


        public Graphics getDrawGraphics() {
            revalidate();
            Image backBuffer = getBackBuffer();
            if (backBuffer == null) {
                return getGraphics();
            }
            SunGraphics2D g = (SunGraphics2D)backBuffer.getGraphics();
            g.constrain(-insets.left, -insets.top,
                        backBuffer.getWidth(null) + insets.left,
                        backBuffer.getHeight(null) + insets.top);
            return g;
        }


        Image getBackBuffer() {
            if (backBuffers != null) {
                return backBuffers[backBuffers.length - 1];
            } else {
                return null;
            }
        }


        public void show() {
            showSubRegion(insets.left, insets.top,
                          width - insets.right,
                          height - insets.bottom);
        }


        void showSubRegion(int x1, int y1, int x2, int y2) {
            if (backBuffers == null) {
                return;
            }
            // Adjust location to be relative to client area.
            x1 -= insets.left;
            x2 -= insets.left;
            y1 -= insets.top;
            y2 -= insets.top;
            Graphics g = getGraphics_NoClientCode();
            if (g == null) {
                // Not showing, bail
                return;
            }
            try {
                // First image copy is in terms of Frame's coordinates, need
                // to translate to client area.
                g.translate(insets.left, insets.top);
                for (int i = 0; i < backBuffers.length; i++) {
                    g.drawImage(backBuffers[i],
                                x1, y1, x2, y2,
                                x1, y1, x2, y2,
                                null);
                    g.dispose();
                    g = null;
                    g = backBuffers[i].getGraphics();
                }
            } finally {
                if (g != null) {
                    g.dispose();
                }
            }
        }


        protected void revalidate() {
            revalidate(true);
        }

        void revalidate(boolean checkSize) {
            validatedContents = false;

            if (backBuffers == null) {
                return;
            }

            if (checkSize) {
                Insets insets = getInsets_NoClientCode();
                if (getWidth() != width || getHeight() != height ||
                    !insets.equals(this.insets)) {
                    // component has been resized; recreate the backbuffers
                    createBackBuffers(backBuffers.length);
                    validatedContents = true;
                }
            }

            // now validate the backbuffer
            GraphicsConfiguration gc = getGraphicsConfiguration_NoClientCode();
            int returnCode =
                backBuffers[backBuffers.length - 1].validate(gc);
            if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                if (checkSize) {
                    createBackBuffers(backBuffers.length);
                    // backbuffers were recreated, so validate again
                    backBuffers[backBuffers.length - 1].validate(gc);
                }
                // else case means we're called from Swing on the toolkit
                // thread, don't recreate buffers as that'll deadlock
                // (creating VolatileImages invokes getting GraphicsConfig
                // which grabs treelock).
                validatedContents = true;
            } else if (returnCode == VolatileImage.IMAGE_RESTORED) {
                validatedContents = true;
            }
        }


        public boolean contentsLost() {
            if (backBuffers == null) {
                return false;
            } else {
                return backBuffers[backBuffers.length - 1].contentsLost();
            }
        }


        public boolean contentsRestored() {
            return validatedContents;
        }
    } // Inner class BltBufferStrategy


    private class FlipSubRegionBufferStrategy extends FlipBufferStrategy
        implements SubRegionShowable
    {

        protected FlipSubRegionBufferStrategy(int numBuffers,
                                              BufferCapabilities caps)
            throws AWTException
        {
            super(numBuffers, caps);
        }

        public void show(int x1, int y1, int x2, int y2) {
            showSubRegion(x1, y1, x2, y2);
        }

        // This is invoked by Swing on the toolkit thread.
        public boolean showIfNotLost(int x1, int y1, int x2, int y2) {
            if (!contentsLost()) {
                showSubRegion(x1, y1, x2, y2);
                return !contentsLost();
            }
            return false;
        }
    }


    private class BltSubRegionBufferStrategy extends BltBufferStrategy
        implements SubRegionShowable
    {

        protected BltSubRegionBufferStrategy(int numBuffers,
                                             BufferCapabilities caps)
        {
            super(numBuffers, caps);
        }

        public void show(int x1, int y1, int x2, int y2) {
            showSubRegion(x1, y1, x2, y2);
        }

        // This method is called by Swing on the toolkit thread.
        public boolean showIfNotLost(int x1, int y1, int x2, int y2) {
            if (!contentsLost()) {
                showSubRegion(x1, y1, x2, y2);
                return !contentsLost();
            }
            return false;
        }
    }


    private class SingleBufferStrategy extends BufferStrategy {

        private BufferCapabilities caps;

        public SingleBufferStrategy(BufferCapabilities caps) {
            this.caps = caps;
        }
        public BufferCapabilities getCapabilities() {
            return caps;
        }
        public Graphics getDrawGraphics() {
            return getGraphics();
        }
        public boolean contentsLost() {
            return false;
        }
        public boolean contentsRestored() {
            return false;
        }
        public void show() {
            // Do nothing
        }
    } // Inner class SingleBufferStrategy


    public void setIgnoreRepaint(boolean ignoreRepaint) {
        this.ignoreRepaint = ignoreRepaint;
    }


    public boolean getIgnoreRepaint() {
        return ignoreRepaint;
    }


    public boolean contains(int x, int y) {
        return inside(x, y);
    }


    @Deprecated
    public boolean inside(int x, int y) {
        return (x >= 0) && (x < width) && (y >= 0) && (y < height);
    }


    public boolean contains(Point p) {
        return contains(p.x, p.y);
    }


    public Component getComponentAt(int x, int y) {
        return locate(x, y);
    }


    @Deprecated
    public Component locate(int x, int y) {
        return contains(x, y) ? this : null;
    }


    public Component getComponentAt(Point p) {
        return getComponentAt(p.x, p.y);
    }


    @Deprecated
    public void deliverEvent(Event e) {
        postEvent(e);
    }


    public final void dispatchEvent(AWTEvent e) {
        dispatchEventImpl(e);
    }

    @SuppressWarnings("deprecation")
    void dispatchEventImpl(AWTEvent e) {
        int id = e.getID();

        // Check that this component belongs to this app-context
        AppContext compContext = appContext;
        if (compContext != null && !compContext.equals(AppContext.getAppContext())) {
            if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
                eventLog.fine("Event " + e + " is being dispatched on the wrong AppContext");
            }
        }

        if (eventLog.isLoggable(PlatformLogger.Level.FINEST)) {
            eventLog.finest("{0}", e);
        }


        if (!(e instanceof KeyEvent)) {
            // Timestamp of a key event is set later in DKFM.preDispatchKeyEvent(KeyEvent).
            EventQueue.setCurrentEventAndMostRecentTime(e);
        }



        if (e instanceof SunDropTargetEvent) {
            ((SunDropTargetEvent)e).dispatch();
            return;
        }

        if (!e.focusManagerIsDispatching) {
            // Invoke the private focus retargeting method which provides
            // lightweight Component support
            if (e.isPosted) {
                e = KeyboardFocusManager.retargetFocusEvent(e);
                e.isPosted = true;
            }

            // Now, with the event properly targeted to a lightweight
            // descendant if necessary, invoke the public focus retargeting
            // and dispatching function
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().
                dispatchEvent(e))
            {
                return;
            }
        }
        if ((e instanceof FocusEvent) && focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
            focusLog.finest("" + e);
        }
        // MouseWheel may need to be retargeted here so that
        // AWTEventListener sees the event go to the correct
        // Component.  If the MouseWheelEvent needs to go to an ancestor,
        // the event is dispatched to the ancestor, and dispatching here
        // stops.
        if (id == MouseEvent.MOUSE_WHEEL &&
            (!eventTypeEnabled(id)) &&
            (peer != null && !peer.handlesWheelScrolling()) &&
            (dispatchMouseWheelToAncestor((MouseWheelEvent)e)))
        {
            return;
        }


        Toolkit toolkit = Toolkit.getDefaultToolkit();
        toolkit.notifyAWTEventListeners(e);



        if (!e.isConsumed()) {
            if (e instanceof java.awt.event.KeyEvent) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().
                    processKeyEvent(this, (KeyEvent)e);
                if (e.isConsumed()) {
                    return;
                }
            }
        }


        if (areInputMethodsEnabled()) {
            // We need to pass on InputMethodEvents since some host
            // input method adapters send them through the Java
            // event queue instead of directly to the component,
            // and the input context also handles the Java composition window
            if(((e instanceof InputMethodEvent) && !(this instanceof CompositionArea))
               ||
               // Otherwise, we only pass on input and focus events, because
               // a) input methods shouldn't know about semantic or component-level events
               // b) passing on the events takes time
               // c) isConsumed() is always true for semantic events.
               (e instanceof InputEvent) || (e instanceof FocusEvent)) {
                InputContext inputContext = getInputContext();


                if (inputContext != null) {
                    inputContext.dispatchEvent(e);
                    if (e.isConsumed()) {
                        if ((e instanceof FocusEvent) && focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                            focusLog.finest("3579: Skipping " + e);
                        }
                        return;
                    }
                }
            }
        } else {
            // When non-clients get focus, we need to explicitly disable the native
            // input method. The native input method is actually not disabled when
            // the active/passive/peered clients loose focus.
            if (id == FocusEvent.FOCUS_GAINED) {
                InputContext inputContext = getInputContext();
                if (inputContext != null && inputContext instanceof sun.awt.im.InputContext) {
                    ((sun.awt.im.InputContext)inputContext).disableNativeIM();
                }
            }
        }



        switch(id) {
            // Handling of the PAINT and UPDATE events is now done in the
            // peer's handleEvent() method so the background can be cleared
            // selectively for non-native components on Windows only.
            // - Fred.Ecks@Eng.sun.com, 5-8-98

          case KeyEvent.KEY_PRESSED:
          case KeyEvent.KEY_RELEASED:
              Container p = (Container)((this instanceof Container) ? this : parent);
              if (p != null) {
                  p.preProcessKeyEvent((KeyEvent)e);
                  if (e.isConsumed()) {
                        if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                            focusLog.finest("Pre-process consumed event");
                        }
                      return;
                  }
              }
              break;

          case WindowEvent.WINDOW_CLOSING:
              if (toolkit instanceof WindowClosingListener) {
                  windowClosingException = ((WindowClosingListener)
                                            toolkit).windowClosingNotify((WindowEvent)e);
                  if (checkWindowClosingException()) {
                      return;
                  }
              }
              break;

          default:
              break;
        }


        if (newEventsOnly) {
            // Filtering needs to really be moved to happen at a lower
            // level in order to get maximum performance gain;  it is
            // here temporarily to ensure the API spec is honored.
            //
            if (eventEnabled(e)) {
                processEvent(e);
            }
        } else if (id == MouseEvent.MOUSE_WHEEL) {
            // newEventsOnly will be false for a listenerless ScrollPane, but
            // MouseWheelEvents still need to be dispatched to it so scrolling
            // can be done.
            autoProcessMouseWheel((MouseWheelEvent)e);
        } else if (!(e instanceof MouseEvent && !postsOldMouseEvents())) {
            //
            // backward compatibility
            //
            Event olde = e.convertToOld();
            if (olde != null) {
                int key = olde.key;
                int modifiers = olde.modifiers;

                postEvent(olde);
                if (olde.isConsumed()) {
                    e.consume();
                }
                // if target changed key or modifier values, copy them
                // back to original event
                //
                switch(olde.id) {
                  case Event.KEY_PRESS:
                  case Event.KEY_RELEASE:
                  case Event.KEY_ACTION:
                  case Event.KEY_ACTION_RELEASE:
                      if (olde.key != key) {
                          ((KeyEvent)e).setKeyChar(olde.getKeyEventChar());
                      }
                      if (olde.modifiers != modifiers) {
                          ((KeyEvent)e).setModifiers(olde.modifiers);
                      }
                      break;
                  default:
                      break;
                }
            }
        }


        if (id == WindowEvent.WINDOW_CLOSING && !e.isConsumed()) {
            if (toolkit instanceof WindowClosingListener) {
                windowClosingException =
                    ((WindowClosingListener)toolkit).
                    windowClosingDelivered((WindowEvent)e);
                if (checkWindowClosingException()) {
                    return;
                }
            }
        }


        if (!(e instanceof KeyEvent)) {
            ComponentPeer tpeer = peer;
            if (e instanceof FocusEvent && (tpeer == null || tpeer instanceof LightweightPeer)) {
                // if focus owner is lightweight then its native container
                // processes event
                Component source = (Component)e.getSource();
                if (source != null) {
                    Container target = source.getNativeContainer();
                    if (target != null) {
                        tpeer = target.getPeer();
                    }
                }
            }
            if (tpeer != null) {
                tpeer.handleEvent(e);
            }
        }
    } // dispatchEventImpl()


    void autoProcessMouseWheel(MouseWheelEvent e) {}


    boolean dispatchMouseWheelToAncestor(MouseWheelEvent e) {
        int newX, newY;
        newX = e.getX() + getX(); // Coordinates take into account at least
        newY = e.getY() + getY(); // the cursor's position relative to this
                                  // Component (e.getX()), and this Component's
                                  // position relative to its parent.
        MouseWheelEvent newMWE;

        if (eventLog.isLoggable(PlatformLogger.Level.FINEST)) {
            eventLog.finest("dispatchMouseWheelToAncestor");
            eventLog.finest("orig event src is of " + e.getSource().getClass());
        }


        synchronized (getTreeLock()) {
            Container anc = getParent();
            while (anc != null && !anc.eventEnabled(e)) {
                // fix coordinates to be relative to new event source
                newX += anc.getX();
                newY += anc.getY();

                if (!(anc instanceof Window)) {
                    anc = anc.getParent();
                }
                else {
                    break;
                }
            }

            if (eventLog.isLoggable(PlatformLogger.Level.FINEST)) {
                eventLog.finest("new event src is " + anc.getClass());
            }

            if (anc != null && anc.eventEnabled(e)) {
                // Change event to be from new source, with new x,y
                // For now, just create a new event - yucky

                newMWE = new MouseWheelEvent(anc, // new source
                                             e.getID(),
                                             e.getWhen(),
                                             e.getModifiers(),
                                             newX, // x relative to new source
                                             newY, // y relative to new source
                                             e.getXOnScreen(),
                                             e.getYOnScreen(),
                                             e.getClickCount(),
                                             e.isPopupTrigger(),
                                             e.getScrollType(),
                                             e.getScrollAmount(),
                                             e.getWheelRotation(),
                                             e.getPreciseWheelRotation());
                ((AWTEvent)e).copyPrivateDataInto(newMWE);
                // When dispatching a wheel event to
                // ancestor, there is no need trying to find descendant
                // lightweights to dispatch event to.
                // If we dispatch the event to toplevel ancestor,
                // this could encolse the loop: 6480024.
                anc.dispatchEventToSelf(newMWE);
                if (newMWE.isConsumed()) {
                    e.consume();
                }
                return true;
            }
        }
        return false;
    }

    boolean checkWindowClosingException() {
        if (windowClosingException != null) {
            if (this instanceof Dialog) {
                ((Dialog)this).interruptBlocking();
            } else {
                windowClosingException.fillInStackTrace();
                windowClosingException.printStackTrace();
                windowClosingException = null;
            }
            return true;
        }
        return false;
    }

    boolean areInputMethodsEnabled() {
        // in 1.2, we assume input method support is required for all
        // components that handle key events, but components can turn off
        // input methods by calling enableInputMethods(false).
        return ((eventMask & AWTEvent.INPUT_METHODS_ENABLED_MASK) != 0) &&
            ((eventMask & AWTEvent.KEY_EVENT_MASK) != 0 || keyListener != null);
    }

    // REMIND: remove when filtering is handled at lower level
    boolean eventEnabled(AWTEvent e) {
        return eventTypeEnabled(e.id);
    }

    boolean eventTypeEnabled(int type) {
        switch(type) {
          case ComponentEvent.COMPONENT_MOVED:
          case ComponentEvent.COMPONENT_RESIZED:
          case ComponentEvent.COMPONENT_SHOWN:
          case ComponentEvent.COMPONENT_HIDDEN:
              if ((eventMask & AWTEvent.COMPONENT_EVENT_MASK) != 0 ||
                  componentListener != null) {
                  return true;
              }
              break;
          case FocusEvent.FOCUS_GAINED:
          case FocusEvent.FOCUS_LOST:
              if ((eventMask & AWTEvent.FOCUS_EVENT_MASK) != 0 ||
                  focusListener != null) {
                  return true;
              }
              break;
          case KeyEvent.KEY_PRESSED:
          case KeyEvent.KEY_RELEASED:
          case KeyEvent.KEY_TYPED:
              if ((eventMask & AWTEvent.KEY_EVENT_MASK) != 0 ||
                  keyListener != null) {
                  return true;
              }
              break;
          case MouseEvent.MOUSE_PRESSED:
          case MouseEvent.MOUSE_RELEASED:
          case MouseEvent.MOUSE_ENTERED:
          case MouseEvent.MOUSE_EXITED:
          case MouseEvent.MOUSE_CLICKED:
              if ((eventMask & AWTEvent.MOUSE_EVENT_MASK) != 0 ||
                  mouseListener != null) {
                  return true;
              }
              break;
          case MouseEvent.MOUSE_MOVED:
          case MouseEvent.MOUSE_DRAGGED:
              if ((eventMask & AWTEvent.MOUSE_MOTION_EVENT_MASK) != 0 ||
                  mouseMotionListener != null) {
                  return true;
              }
              break;
          case MouseEvent.MOUSE_WHEEL:
              if ((eventMask & AWTEvent.MOUSE_WHEEL_EVENT_MASK) != 0 ||
                  mouseWheelListener != null) {
                  return true;
              }
              break;
          case InputMethodEvent.INPUT_METHOD_TEXT_CHANGED:
          case InputMethodEvent.CARET_POSITION_CHANGED:
              if ((eventMask & AWTEvent.INPUT_METHOD_EVENT_MASK) != 0 ||
                  inputMethodListener != null) {
                  return true;
              }
              break;
          case HierarchyEvent.HIERARCHY_CHANGED:
              if ((eventMask & AWTEvent.HIERARCHY_EVENT_MASK) != 0 ||
                  hierarchyListener != null) {
                  return true;
              }
              break;
          case HierarchyEvent.ANCESTOR_MOVED:
          case HierarchyEvent.ANCESTOR_RESIZED:
              if ((eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) != 0 ||
                  hierarchyBoundsListener != null) {
                  return true;
              }
              break;
          case ActionEvent.ACTION_PERFORMED:
              if ((eventMask & AWTEvent.ACTION_EVENT_MASK) != 0) {
                  return true;
              }
              break;
          case TextEvent.TEXT_VALUE_CHANGED:
              if ((eventMask & AWTEvent.TEXT_EVENT_MASK) != 0) {
                  return true;
              }
              break;
          case ItemEvent.ITEM_STATE_CHANGED:
              if ((eventMask & AWTEvent.ITEM_EVENT_MASK) != 0) {
                  return true;
              }
              break;
          case AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED:
              if ((eventMask & AWTEvent.ADJUSTMENT_EVENT_MASK) != 0) {
                  return true;
              }
              break;
          default:
              break;
        }
        //
        // Always pass on events defined by external programs.
        //
        if (type > AWTEvent.RESERVED_ID_MAX) {
            return true;
        }
        return false;
    }


    @Deprecated
    public boolean postEvent(Event e) {
        ComponentPeer peer = this.peer;

        if (handleEvent(e)) {
            e.consume();
            return true;
        }

        Component parent = this.parent;
        int eventx = e.x;
        int eventy = e.y;
        if (parent != null) {
            e.translate(x, y);
            if (parent.postEvent(e)) {
                e.consume();
                return true;
            }
            // restore coords
            e.x = eventx;
            e.y = eventy;
        }
        return false;
    }

    // Event source interfaces


    public synchronized void addComponentListener(ComponentListener l) {
        if (l == null) {
            return;
        }
        componentListener = AWTEventMulticaster.add(componentListener, l);
        newEventsOnly = true;
    }


    public synchronized void removeComponentListener(ComponentListener l) {
        if (l == null) {
            return;
        }
        componentListener = AWTEventMulticaster.remove(componentListener, l);
    }


    public synchronized ComponentListener[] getComponentListeners() {
        return getListeners(ComponentListener.class);
    }


    public synchronized void addFocusListener(FocusListener l) {
        if (l == null) {
            return;
        }
        focusListener = AWTEventMulticaster.add(focusListener, l);
        newEventsOnly = true;

        // if this is a lightweight component, enable focus events
        // in the native container.
        if (peer instanceof LightweightPeer) {
            parent.proxyEnableEvents(AWTEvent.FOCUS_EVENT_MASK);
        }
    }


    public synchronized void removeFocusListener(FocusListener l) {
        if (l == null) {
            return;
        }
        focusListener = AWTEventMulticaster.remove(focusListener, l);
    }


    public synchronized FocusListener[] getFocusListeners() {
        return getListeners(FocusListener.class);
    }


    public void addHierarchyListener(HierarchyListener l) {
        if (l == null) {
            return;
        }
        boolean notifyAncestors;
        synchronized (this) {
            notifyAncestors =
                (hierarchyListener == null &&
                 (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) == 0);
            hierarchyListener = AWTEventMulticaster.add(hierarchyListener, l);
            notifyAncestors = (notifyAncestors && hierarchyListener != null);
            newEventsOnly = true;
        }
        if (notifyAncestors) {
            synchronized (getTreeLock()) {
                adjustListeningChildrenOnParent(AWTEvent.HIERARCHY_EVENT_MASK,
                                                1);
            }
        }
    }


    public void removeHierarchyListener(HierarchyListener l) {
        if (l == null) {
            return;
        }
        boolean notifyAncestors;
        synchronized (this) {
            notifyAncestors =
                (hierarchyListener != null &&
                 (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) == 0);
            hierarchyListener =
                AWTEventMulticaster.remove(hierarchyListener, l);
            notifyAncestors = (notifyAncestors && hierarchyListener == null);
        }
        if (notifyAncestors) {
            synchronized (getTreeLock()) {
                adjustListeningChildrenOnParent(AWTEvent.HIERARCHY_EVENT_MASK,
                                                -1);
            }
        }
    }


    public synchronized HierarchyListener[] getHierarchyListeners() {
        return getListeners(HierarchyListener.class);
    }


    public void addHierarchyBoundsListener(HierarchyBoundsListener l) {
        if (l == null) {
            return;
        }
        boolean notifyAncestors;
        synchronized (this) {
            notifyAncestors =
                (hierarchyBoundsListener == null &&
                 (eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) == 0);
            hierarchyBoundsListener =
                AWTEventMulticaster.add(hierarchyBoundsListener, l);
            notifyAncestors = (notifyAncestors &&
                               hierarchyBoundsListener != null);
            newEventsOnly = true;
        }
        if (notifyAncestors) {
            synchronized (getTreeLock()) {
                adjustListeningChildrenOnParent(
                                                AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK, 1);
            }
        }
    }


    public void removeHierarchyBoundsListener(HierarchyBoundsListener l) {
        if (l == null) {
            return;
        }
        boolean notifyAncestors;
        synchronized (this) {
            notifyAncestors =
                (hierarchyBoundsListener != null &&
                 (eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) == 0);
            hierarchyBoundsListener =
                AWTEventMulticaster.remove(hierarchyBoundsListener, l);
            notifyAncestors = (notifyAncestors &&
                               hierarchyBoundsListener == null);
        }
        if (notifyAncestors) {
            synchronized (getTreeLock()) {
                adjustListeningChildrenOnParent(
                                                AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK, -1);
            }
        }
    }

    // Should only be called while holding the tree lock
    int numListening(long mask) {
        // One mask or the other, but not neither or both.
        if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
            if ((mask != AWTEvent.HIERARCHY_EVENT_MASK) &&
                (mask != AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK))
            {
                eventLog.fine("Assertion failed");
            }
        }
        if ((mask == AWTEvent.HIERARCHY_EVENT_MASK &&
             (hierarchyListener != null ||
              (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) != 0)) ||
            (mask == AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK &&
             (hierarchyBoundsListener != null ||
              (eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) != 0))) {
            return 1;
        } else {
            return 0;
        }
    }

    // Should only be called while holding tree lock
    int countHierarchyMembers() {
        return 1;
    }
    // Should only be called while holding the tree lock
    int createHierarchyEvents(int id, Component changed,
                              Container changedParent, long changeFlags,
                              boolean enabledOnToolkit) {
        switch (id) {
          case HierarchyEvent.HIERARCHY_CHANGED:
              if (hierarchyListener != null ||
                  (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) != 0 ||
                  enabledOnToolkit) {
                  HierarchyEvent e = new HierarchyEvent(this, id, changed,
                                                        changedParent,
                                                        changeFlags);
                  dispatchEvent(e);
                  return 1;
              }
              break;
          case HierarchyEvent.ANCESTOR_MOVED:
          case HierarchyEvent.ANCESTOR_RESIZED:
              if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
                  if (changeFlags != 0) {
                      eventLog.fine("Assertion (changeFlags == 0) failed");
                  }
              }
              if (hierarchyBoundsListener != null ||
                  (eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) != 0 ||
                  enabledOnToolkit) {
                  HierarchyEvent e = new HierarchyEvent(this, id, changed,
                                                        changedParent);
                  dispatchEvent(e);
                  return 1;
              }
              break;
          default:
              // assert false
              if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
                  eventLog.fine("This code must never be reached");
              }
              break;
        }
        return 0;
    }


    public synchronized HierarchyBoundsListener[] getHierarchyBoundsListeners() {
        return getListeners(HierarchyBoundsListener.class);
    }


    void adjustListeningChildrenOnParent(long mask, int num) {
        if (parent != null) {
            parent.adjustListeningChildren(mask, num);
        }
    }


    public synchronized void addKeyListener(KeyListener l) {
        if (l == null) {
            return;
        }
        keyListener = AWTEventMulticaster.add(keyListener, l);
        newEventsOnly = true;

        // if this is a lightweight component, enable key events
        // in the native container.
        if (peer instanceof LightweightPeer) {
            parent.proxyEnableEvents(AWTEvent.KEY_EVENT_MASK);
        }
    }


    public synchronized void removeKeyListener(KeyListener l) {
        if (l == null) {
            return;
        }
        keyListener = AWTEventMulticaster.remove(keyListener, l);
    }


    public synchronized KeyListener[] getKeyListeners() {
        return getListeners(KeyListener.class);
    }


    public synchronized void addMouseListener(MouseListener l) {
        if (l == null) {
            return;
        }
        mouseListener = AWTEventMulticaster.add(mouseListener,l);
        newEventsOnly = true;

        // if this is a lightweight component, enable mouse events
        // in the native container.
        if (peer instanceof LightweightPeer) {
            parent.proxyEnableEvents(AWTEvent.MOUSE_EVENT_MASK);
        }
    }


    public synchronized void removeMouseListener(MouseListener l) {
        if (l == null) {
            return;
        }
        mouseListener = AWTEventMulticaster.remove(mouseListener, l);
    }


    public synchronized MouseListener[] getMouseListeners() {
        return getListeners(MouseListener.class);
    }


    public synchronized void addMouseMotionListener(MouseMotionListener l) {
        if (l == null) {
            return;
        }
        mouseMotionListener = AWTEventMulticaster.add(mouseMotionListener,l);
        newEventsOnly = true;

        // if this is a lightweight component, enable mouse events
        // in the native container.
        if (peer instanceof LightweightPeer) {
            parent.proxyEnableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }
    }


    public synchronized void removeMouseMotionListener(MouseMotionListener l) {
        if (l == null) {
            return;
        }
        mouseMotionListener = AWTEventMulticaster.remove(mouseMotionListener, l);
    }


    public synchronized MouseMotionListener[] getMouseMotionListeners() {
        return getListeners(MouseMotionListener.class);
    }


    public synchronized void addMouseWheelListener(MouseWheelListener l) {
        if (l == null) {
            return;
        }
        mouseWheelListener = AWTEventMulticaster.add(mouseWheelListener,l);
        newEventsOnly = true;

        // if this is a lightweight component, enable mouse events
        // in the native container.
        if (peer instanceof LightweightPeer) {
            parent.proxyEnableEvents(AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        }
    }


    public synchronized void removeMouseWheelListener(MouseWheelListener l) {
        if (l == null) {
            return;
        }
        mouseWheelListener = AWTEventMulticaster.remove(mouseWheelListener, l);
    }


    public synchronized MouseWheelListener[] getMouseWheelListeners() {
        return getListeners(MouseWheelListener.class);
    }


    public synchronized void addInputMethodListener(InputMethodListener l) {
        if (l == null) {
            return;
        }
        inputMethodListener = AWTEventMulticaster.add(inputMethodListener, l);
        newEventsOnly = true;
    }


    public synchronized void removeInputMethodListener(InputMethodListener l) {
        if (l == null) {
            return;
        }
        inputMethodListener = AWTEventMulticaster.remove(inputMethodListener, l);
    }


    public synchronized InputMethodListener[] getInputMethodListeners() {
        return getListeners(InputMethodListener.class);
    }


    @SuppressWarnings("unchecked")
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener l = null;
        if  (listenerType == ComponentListener.class) {
            l = componentListener;
        } else if (listenerType == FocusListener.class) {
            l = focusListener;
        } else if (listenerType == HierarchyListener.class) {
            l = hierarchyListener;
        } else if (listenerType == HierarchyBoundsListener.class) {
            l = hierarchyBoundsListener;
        } else if (listenerType == KeyListener.class) {
            l = keyListener;
        } else if (listenerType == MouseListener.class) {
            l = mouseListener;
        } else if (listenerType == MouseMotionListener.class) {
            l = mouseMotionListener;
        } else if (listenerType == MouseWheelListener.class) {
            l = mouseWheelListener;
        } else if (listenerType == InputMethodListener.class) {
            l = inputMethodListener;
        } else if (listenerType == PropertyChangeListener.class) {
            return (T[])getPropertyChangeListeners();
        }
        return AWTEventMulticaster.getListeners(l, listenerType);
    }


    public InputMethodRequests getInputMethodRequests() {
        return null;
    }


    public InputContext getInputContext() {
        Container parent = this.parent;
        if (parent == null) {
            return null;
        } else {
            return parent.getInputContext();
        }
    }


    protected final void enableEvents(long eventsToEnable) {
        long notifyAncestors = 0;
        synchronized (this) {
            if ((eventsToEnable & AWTEvent.HIERARCHY_EVENT_MASK) != 0 &&
                hierarchyListener == null &&
                (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) == 0) {
                notifyAncestors |= AWTEvent.HIERARCHY_EVENT_MASK;
            }
            if ((eventsToEnable & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) != 0 &&
                hierarchyBoundsListener == null &&
                (eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) == 0) {
                notifyAncestors |= AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK;
            }
            eventMask |= eventsToEnable;
            newEventsOnly = true;
        }

        // if this is a lightweight component, enable mouse events
        // in the native container.
        if (peer instanceof LightweightPeer) {
            parent.proxyEnableEvents(eventMask);
        }
        if (notifyAncestors != 0) {
            synchronized (getTreeLock()) {
                adjustListeningChildrenOnParent(notifyAncestors, 1);
            }
        }
    }


    protected final void disableEvents(long eventsToDisable) {
        long notifyAncestors = 0;
        synchronized (this) {
            if ((eventsToDisable & AWTEvent.HIERARCHY_EVENT_MASK) != 0 &&
                hierarchyListener == null &&
                (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) != 0) {
                notifyAncestors |= AWTEvent.HIERARCHY_EVENT_MASK;
            }
            if ((eventsToDisable & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK)!=0 &&
                hierarchyBoundsListener == null &&
                (eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) != 0) {
                notifyAncestors |= AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK;
            }
            eventMask &= ~eventsToDisable;
        }
        if (notifyAncestors != 0) {
            synchronized (getTreeLock()) {
                adjustListeningChildrenOnParent(notifyAncestors, -1);
            }
        }
    }

    transient sun.awt.EventQueueItem[] eventCache;


    transient private boolean coalescingEnabled = checkCoalescing();


    private static final Map<Class<?>, Boolean> coalesceMap =
        new java.util.WeakHashMap<Class<?>, Boolean>();


     private boolean checkCoalescing() {
         if (getClass().getClassLoader()==null) {
             return false;
         }
         final Class<? extends Component> clazz = getClass();
         synchronized (coalesceMap) {
             // Check cache.
             Boolean value = coalesceMap.get(clazz);
             if (value != null) {
                 return value;
             }

             // Need to check non-bootstraps.
             Boolean enabled = java.security.AccessController.doPrivileged(
                 new java.security.PrivilegedAction<Boolean>() {
                     public Boolean run() {
                         return isCoalesceEventsOverriden(clazz);
                     }
                 }
                 );
             coalesceMap.put(clazz, enabled);
             return enabled;
         }
     }


    private static final Class[] coalesceEventsParams = {
        AWTEvent.class, AWTEvent.class
    };


    private static boolean isCoalesceEventsOverriden(Class<?> clazz) {
        assert Thread.holdsLock(coalesceMap);

        // First check superclass - we may not need to bother ourselves.
        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null) {
            // Only occurs on implementations that
            //   do not use null to represent the bootsrap class loader.
            return false;
        }
        if (superclass.getClassLoader() != null) {
            Boolean value = coalesceMap.get(superclass);
            if (value == null) {
                // Not done already - recurse.
                if (isCoalesceEventsOverriden(superclass)) {
                    coalesceMap.put(superclass, true);
                    return true;
                }
            } else if (value) {
                return true;
            }
        }

        try {
            // Throws if not overriden.
            clazz.getDeclaredMethod(
                "coalesceEvents", coalesceEventsParams
                );
            return true;
        } catch (NoSuchMethodException e) {
            // Not present in this class.
            return false;
        }
    }


    final boolean isCoalescingEnabled() {
        return coalescingEnabled;
     }



    protected AWTEvent coalesceEvents(AWTEvent existingEvent,
                                      AWTEvent newEvent) {
        return null;
    }


    protected void processEvent(AWTEvent e) {
        if (e instanceof FocusEvent) {
            processFocusEvent((FocusEvent)e);

        } else if (e instanceof MouseEvent) {
            switch(e.getID()) {
              case MouseEvent.MOUSE_PRESSED:
              case MouseEvent.MOUSE_RELEASED:
              case MouseEvent.MOUSE_CLICKED:
              case MouseEvent.MOUSE_ENTERED:
              case MouseEvent.MOUSE_EXITED:
                  processMouseEvent((MouseEvent)e);
                  break;
              case MouseEvent.MOUSE_MOVED:
              case MouseEvent.MOUSE_DRAGGED:
                  processMouseMotionEvent((MouseEvent)e);
                  break;
              case MouseEvent.MOUSE_WHEEL:
                  processMouseWheelEvent((MouseWheelEvent)e);
                  break;
            }

        } else if (e instanceof KeyEvent) {
            processKeyEvent((KeyEvent)e);

        } else if (e instanceof ComponentEvent) {
            processComponentEvent((ComponentEvent)e);
        } else if (e instanceof InputMethodEvent) {
            processInputMethodEvent((InputMethodEvent)e);
        } else if (e instanceof HierarchyEvent) {
            switch (e.getID()) {
              case HierarchyEvent.HIERARCHY_CHANGED:
                  processHierarchyEvent((HierarchyEvent)e);
                  break;
              case HierarchyEvent.ANCESTOR_MOVED:
              case HierarchyEvent.ANCESTOR_RESIZED:
                  processHierarchyBoundsEvent((HierarchyEvent)e);
                  break;
            }
        }
    }


    protected void processComponentEvent(ComponentEvent e) {
        ComponentListener listener = componentListener;
        if (listener != null) {
            int id = e.getID();
            switch(id) {
              case ComponentEvent.COMPONENT_RESIZED:
                  listener.componentResized(e);
                  break;
              case ComponentEvent.COMPONENT_MOVED:
                  listener.componentMoved(e);
                  break;
              case ComponentEvent.COMPONENT_SHOWN:
                  listener.componentShown(e);
                  break;
              case ComponentEvent.COMPONENT_HIDDEN:
                  listener.componentHidden(e);
                  break;
            }
        }
    }


    protected void processFocusEvent(FocusEvent e) {
        FocusListener listener = focusListener;
        if (listener != null) {
            int id = e.getID();
            switch(id) {
              case FocusEvent.FOCUS_GAINED:
                  listener.focusGained(e);
                  break;
              case FocusEvent.FOCUS_LOST:
                  listener.focusLost(e);
                  break;
            }
        }
    }


    protected void processKeyEvent(KeyEvent e) {
        KeyListener listener = keyListener;
        if (listener != null) {
            int id = e.getID();
            switch(id) {
              case KeyEvent.KEY_TYPED:
                  listener.keyTyped(e);
                  break;
              case KeyEvent.KEY_PRESSED:
                  listener.keyPressed(e);
                  break;
              case KeyEvent.KEY_RELEASED:
                  listener.keyReleased(e);
                  break;
            }
        }
    }


    protected void processMouseEvent(MouseEvent e) {
        MouseListener listener = mouseListener;
        if (listener != null) {
            int id = e.getID();
            switch(id) {
              case MouseEvent.MOUSE_PRESSED:
                  listener.mousePressed(e);
                  break;
              case MouseEvent.MOUSE_RELEASED:
                  listener.mouseReleased(e);
                  break;
              case MouseEvent.MOUSE_CLICKED:
                  listener.mouseClicked(e);
                  break;
              case MouseEvent.MOUSE_EXITED:
                  listener.mouseExited(e);
                  break;
              case MouseEvent.MOUSE_ENTERED:
                  listener.mouseEntered(e);
                  break;
            }
        }
    }


    protected void processMouseMotionEvent(MouseEvent e) {
        MouseMotionListener listener = mouseMotionListener;
        if (listener != null) {
            int id = e.getID();
            switch(id) {
              case MouseEvent.MOUSE_MOVED:
                  listener.mouseMoved(e);
                  break;
              case MouseEvent.MOUSE_DRAGGED:
                  listener.mouseDragged(e);
                  break;
            }
        }
    }


    protected void processMouseWheelEvent(MouseWheelEvent e) {
        MouseWheelListener listener = mouseWheelListener;
        if (listener != null) {
            int id = e.getID();
            switch(id) {
              case MouseEvent.MOUSE_WHEEL:
                  listener.mouseWheelMoved(e);
                  break;
            }
        }
    }

    boolean postsOldMouseEvents() {
        return false;
    }


    protected void processInputMethodEvent(InputMethodEvent e) {
        InputMethodListener listener = inputMethodListener;
        if (listener != null) {
            int id = e.getID();
            switch (id) {
              case InputMethodEvent.INPUT_METHOD_TEXT_CHANGED:
                  listener.inputMethodTextChanged(e);
                  break;
              case InputMethodEvent.CARET_POSITION_CHANGED:
                  listener.caretPositionChanged(e);
                  break;
            }
        }
    }


    protected void processHierarchyEvent(HierarchyEvent e) {
        HierarchyListener listener = hierarchyListener;
        if (listener != null) {
            int id = e.getID();
            switch (id) {
              case HierarchyEvent.HIERARCHY_CHANGED:
                  listener.hierarchyChanged(e);
                  break;
            }
        }
    }


    protected void processHierarchyBoundsEvent(HierarchyEvent e) {
        HierarchyBoundsListener listener = hierarchyBoundsListener;
        if (listener != null) {
            int id = e.getID();
            switch (id) {
              case HierarchyEvent.ANCESTOR_MOVED:
                  listener.ancestorMoved(e);
                  break;
              case HierarchyEvent.ANCESTOR_RESIZED:
                  listener.ancestorResized(e);
                  break;
            }
        }
    }


    @Deprecated
    public boolean handleEvent(Event evt) {
        switch (evt.id) {
          case Event.MOUSE_ENTER:
              return mouseEnter(evt, evt.x, evt.y);

          case Event.MOUSE_EXIT:
              return mouseExit(evt, evt.x, evt.y);

          case Event.MOUSE_MOVE:
              return mouseMove(evt, evt.x, evt.y);

          case Event.MOUSE_DOWN:
              return mouseDown(evt, evt.x, evt.y);

          case Event.MOUSE_DRAG:
              return mouseDrag(evt, evt.x, evt.y);

          case Event.MOUSE_UP:
              return mouseUp(evt, evt.x, evt.y);

          case Event.KEY_PRESS:
          case Event.KEY_ACTION:
              return keyDown(evt, evt.key);

          case Event.KEY_RELEASE:
          case Event.KEY_ACTION_RELEASE:
              return keyUp(evt, evt.key);

          case Event.ACTION_EVENT:
              return action(evt, evt.arg);
          case Event.GOT_FOCUS:
              return gotFocus(evt, evt.arg);
          case Event.LOST_FOCUS:
              return lostFocus(evt, evt.arg);
        }
        return false;
    }


    @Deprecated
    public boolean mouseDown(Event evt, int x, int y) {
        return false;
    }


    @Deprecated
    public boolean mouseDrag(Event evt, int x, int y) {
        return false;
    }


    @Deprecated
    public boolean mouseUp(Event evt, int x, int y) {
        return false;
    }


    @Deprecated
    public boolean mouseMove(Event evt, int x, int y) {
        return false;
    }


    @Deprecated
    public boolean mouseEnter(Event evt, int x, int y) {
        return false;
    }


    @Deprecated
    public boolean mouseExit(Event evt, int x, int y) {
        return false;
    }


    @Deprecated
    public boolean keyDown(Event evt, int key) {
        return false;
    }


    @Deprecated
    public boolean keyUp(Event evt, int key) {
        return false;
    }


    @Deprecated
    public boolean action(Event evt, Object what) {
        return false;
    }


    public void addNotify() {
        synchronized (getTreeLock()) {
            ComponentPeer peer = this.peer;
            if (peer == null || peer instanceof LightweightPeer){
                if (peer == null) {
                    // Update both the Component's peer variable and the local
                    // variable we use for thread safety.
                    this.peer = peer = getToolkit().createComponent(this);
                }

                // This is a lightweight component which means it won't be
                // able to get window-related events by itself.  If any
                // have been enabled, then the nearest native container must
                // be enabled.
                if (parent != null) {
                    long mask = 0;
                    if ((mouseListener != null) || ((eventMask & AWTEvent.MOUSE_EVENT_MASK) != 0)) {
                        mask |= AWTEvent.MOUSE_EVENT_MASK;
                    }
                    if ((mouseMotionListener != null) ||
                        ((eventMask & AWTEvent.MOUSE_MOTION_EVENT_MASK) != 0)) {
                        mask |= AWTEvent.MOUSE_MOTION_EVENT_MASK;
                    }
                    if ((mouseWheelListener != null ) ||
                        ((eventMask & AWTEvent.MOUSE_WHEEL_EVENT_MASK) != 0)) {
                        mask |= AWTEvent.MOUSE_WHEEL_EVENT_MASK;
                    }
                    if (focusListener != null || (eventMask & AWTEvent.FOCUS_EVENT_MASK) != 0) {
                        mask |= AWTEvent.FOCUS_EVENT_MASK;
                    }
                    if (keyListener != null || (eventMask & AWTEvent.KEY_EVENT_MASK) != 0) {
                        mask |= AWTEvent.KEY_EVENT_MASK;
                    }
                    if (mask != 0) {
                        parent.proxyEnableEvents(mask);
                    }
                }
            } else {
                // It's native. If the parent is lightweight it will need some
                // help.
                Container parent = getContainer();
                if (parent != null && parent.isLightweight()) {
                    relocateComponent();
                    if (!parent.isRecursivelyVisibleUpToHeavyweightContainer())
                    {
                        peer.setVisible(false);
                    }
                }
            }
            invalidate();

            int npopups = (popups != null? popups.size() : 0);
            for (int i = 0 ; i < npopups ; i++) {
                PopupMenu popup = popups.elementAt(i);
                popup.addNotify();
            }

            if (dropTarget != null) dropTarget.addNotify(peer);

            peerFont = getFont();

            if (getContainer() != null && !isAddNotifyComplete) {
                getContainer().increaseComponentCount(this);
            }


            // Update stacking order
            updateZOrder();

            if (!isAddNotifyComplete) {
                mixOnShowing();
            }

            isAddNotifyComplete = true;

            if (hierarchyListener != null ||
                (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) != 0 ||
                Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK)) {
                HierarchyEvent e =
                    new HierarchyEvent(this, HierarchyEvent.HIERARCHY_CHANGED,
                                       this, parent,
                                       HierarchyEvent.DISPLAYABILITY_CHANGED |
                                       ((isRecursivelyVisible())
                                        ? HierarchyEvent.SHOWING_CHANGED
                                        : 0));
                dispatchEvent(e);
            }
        }
    }


    public void removeNotify() {
        KeyboardFocusManager.clearMostRecentFocusOwner(this);
        if (KeyboardFocusManager.getCurrentKeyboardFocusManager().
            getPermanentFocusOwner() == this)
        {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                setGlobalPermanentFocusOwner(null);
        }

        synchronized (getTreeLock()) {
            if (isFocusOwner() && KeyboardFocusManager.isAutoFocusTransferEnabledFor(this)) {
                transferFocus(true);
            }

            if (getContainer() != null && isAddNotifyComplete) {
                getContainer().decreaseComponentCount(this);
            }

            int npopups = (popups != null? popups.size() : 0);
            for (int i = 0 ; i < npopups ; i++) {
                PopupMenu popup = popups.elementAt(i);
                popup.removeNotify();
            }
            // If there is any input context for this component, notify
            // that this component is being removed. (This has to be done
            // before hiding peer.)
            if ((eventMask & AWTEvent.INPUT_METHODS_ENABLED_MASK) != 0) {
                InputContext inputContext = getInputContext();
                if (inputContext != null) {
                    inputContext.removeNotify(this);
                }
            }

            ComponentPeer p = peer;
            if (p != null) {
                boolean isLightweight = isLightweight();

                if (bufferStrategy instanceof FlipBufferStrategy) {
                    ((FlipBufferStrategy)bufferStrategy).destroyBuffers();
                }

                if (dropTarget != null) dropTarget.removeNotify(peer);

                // Hide peer first to stop system events such as cursor moves.
                if (visible) {
                    p.setVisible(false);
                }

                peer = null; // Stop peer updates.
                peerFont = null;

                Toolkit.getEventQueue().removeSourceEvents(this, false);
                KeyboardFocusManager.getCurrentKeyboardFocusManager().
                    discardKeyEvents(this);

                p.dispose();

                mixOnHiding(isLightweight);

                isAddNotifyComplete = false;
                // Nullifying compoundShape means that the component has normal shape
                // (or has no shape at all).
                this.compoundShape = null;
            }

            if (hierarchyListener != null ||
                (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) != 0 ||
                Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK)) {
                HierarchyEvent e =
                    new HierarchyEvent(this, HierarchyEvent.HIERARCHY_CHANGED,
                                       this, parent,
                                       HierarchyEvent.DISPLAYABILITY_CHANGED |
                                       ((isRecursivelyVisible())
                                        ? HierarchyEvent.SHOWING_CHANGED
                                        : 0));
                dispatchEvent(e);
            }
        }
    }


    @Deprecated
    public boolean gotFocus(Event evt, Object what) {
        return false;
    }


    @Deprecated
    public boolean lostFocus(Event evt, Object what) {
        return false;
    }


    @Deprecated
    public boolean isFocusTraversable() {
        if (isFocusTraversableOverridden == FOCUS_TRAVERSABLE_UNKNOWN) {
            isFocusTraversableOverridden = FOCUS_TRAVERSABLE_DEFAULT;
        }
        return focusable;
    }


    public boolean isFocusable() {
        return isFocusTraversable();
    }


    public void setFocusable(boolean focusable) {
        boolean oldFocusable;
        synchronized (this) {
            oldFocusable = this.focusable;
            this.focusable = focusable;
        }
        isFocusTraversableOverridden = FOCUS_TRAVERSABLE_SET;

        firePropertyChange("focusable", oldFocusable, focusable);
        if (oldFocusable && !focusable) {
            if (isFocusOwner() && KeyboardFocusManager.isAutoFocusTransferEnabled()) {
                transferFocus(true);
            }
            KeyboardFocusManager.clearMostRecentFocusOwner(this);
        }
    }

    final boolean isFocusTraversableOverridden() {
        return (isFocusTraversableOverridden != FOCUS_TRAVERSABLE_DEFAULT);
    }


    public void setFocusTraversalKeys(int id,
                                      Set<? extends AWTKeyStroke> keystrokes)
    {
        if (id < 0 || id >= KeyboardFocusManager.TRAVERSAL_KEY_LENGTH - 1) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }

        setFocusTraversalKeys_NoIDCheck(id, keystrokes);
    }


    public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
        if (id < 0 || id >= KeyboardFocusManager.TRAVERSAL_KEY_LENGTH - 1) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }

        return getFocusTraversalKeys_NoIDCheck(id);
    }

    // We define these methods so that Container does not need to repeat this
    // code. Container cannot call super.<method> because Container allows
    // DOWN_CYCLE_TRAVERSAL_KEY while Component does not. The Component method
    // would erroneously generate an IllegalArgumentException for
    // DOWN_CYCLE_TRAVERSAL_KEY.
    final void setFocusTraversalKeys_NoIDCheck(int id, Set<? extends AWTKeyStroke> keystrokes) {
        Set<AWTKeyStroke> oldKeys;

        synchronized (this) {
            if (focusTraversalKeys == null) {
                initializeFocusTraversalKeys();
            }

            if (keystrokes != null) {
                for (AWTKeyStroke keystroke : keystrokes ) {

                    if (keystroke == null) {
                        throw new IllegalArgumentException("cannot set null focus traversal key");
                    }

                    if (keystroke.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                        throw new IllegalArgumentException("focus traversal keys cannot map to KEY_TYPED events");
                    }

                    for (int i = 0; i < focusTraversalKeys.length; i++) {
                        if (i == id) {
                            continue;
                        }

                        if (getFocusTraversalKeys_NoIDCheck(i).contains(keystroke))
                        {
                            throw new IllegalArgumentException("focus traversal keys must be unique for a Component");
                        }
                    }
                }
            }

            oldKeys = focusTraversalKeys[id];
            focusTraversalKeys[id] = (keystrokes != null)
                ? Collections.unmodifiableSet(new HashSet<AWTKeyStroke>(keystrokes))
                : null;
        }

        firePropertyChange(focusTraversalKeyPropertyNames[id], oldKeys,
                           keystrokes);
    }
    final Set<AWTKeyStroke> getFocusTraversalKeys_NoIDCheck(int id) {
        // Okay to return Set directly because it is an unmodifiable view
        @SuppressWarnings("unchecked")
        Set<AWTKeyStroke> keystrokes = (focusTraversalKeys != null)
            ? focusTraversalKeys[id]
            : null;

        if (keystrokes != null) {
            return keystrokes;
        } else {
            Container parent = this.parent;
            if (parent != null) {
                return parent.getFocusTraversalKeys(id);
            } else {
                return KeyboardFocusManager.getCurrentKeyboardFocusManager().
                    getDefaultFocusTraversalKeys(id);
            }
        }
    }


    public boolean areFocusTraversalKeysSet(int id) {
        if (id < 0 || id >= KeyboardFocusManager.TRAVERSAL_KEY_LENGTH - 1) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }

        return (focusTraversalKeys != null && focusTraversalKeys[id] != null);
    }


    public void setFocusTraversalKeysEnabled(boolean
                                             focusTraversalKeysEnabled) {
        boolean oldFocusTraversalKeysEnabled;
        synchronized (this) {
            oldFocusTraversalKeysEnabled = this.focusTraversalKeysEnabled;
            this.focusTraversalKeysEnabled = focusTraversalKeysEnabled;
        }
        firePropertyChange("focusTraversalKeysEnabled",
                           oldFocusTraversalKeysEnabled,
                           focusTraversalKeysEnabled);
    }


    public boolean getFocusTraversalKeysEnabled() {
        return focusTraversalKeysEnabled;
    }


    public void requestFocus() {
        requestFocusHelper(false, true);
    }

    boolean requestFocus(CausedFocusEvent.Cause cause) {
        return requestFocusHelper(false, true, cause);
    }


    protected boolean requestFocus(boolean temporary) {
        return requestFocusHelper(temporary, true);
    }

    boolean requestFocus(boolean temporary, CausedFocusEvent.Cause cause) {
        return requestFocusHelper(temporary, true, cause);
    }

    public boolean requestFocusInWindow() {
        return requestFocusHelper(false, false);
    }

    boolean requestFocusInWindow(CausedFocusEvent.Cause cause) {
        return requestFocusHelper(false, false, cause);
    }


    protected boolean requestFocusInWindow(boolean temporary) {
        return requestFocusHelper(temporary, false);
    }

    boolean requestFocusInWindow(boolean temporary, CausedFocusEvent.Cause cause) {
        return requestFocusHelper(temporary, false, cause);
    }

    final boolean requestFocusHelper(boolean temporary,
                                     boolean focusedWindowChangeAllowed) {
        return requestFocusHelper(temporary, focusedWindowChangeAllowed, CausedFocusEvent.Cause.UNKNOWN);
    }

    final boolean requestFocusHelper(boolean temporary,
                                     boolean focusedWindowChangeAllowed,
                                     CausedFocusEvent.Cause cause)
    {
        // 1) Check if the event being dispatched is a system-generated mouse event.
        AWTEvent currentEvent = EventQueue.getCurrentEvent();
        if (currentEvent instanceof MouseEvent &&
            SunToolkit.isSystemGenerated(currentEvent))
        {
            // 2) Sanity check: if the mouse event component source belongs to the same containing window.
            Component source = ((MouseEvent)currentEvent).getComponent();
            if (source == null || source.getContainingWindow() == getContainingWindow()) {
                focusLog.finest("requesting focus by mouse event \"in window\"");

                // If both the conditions are fulfilled the focus request should be strictly
                // bounded by the toplevel window. It's assumed that the mouse event activates
                // the window (if it wasn't active) and this makes it possible for a focus
                // request with a strong in-window requirement to change focus in the bounds
                // of the toplevel. If, by any means, due to asynchronous nature of the event
                // dispatching mechanism, the window happens to be natively inactive by the time
                // this focus request is eventually handled, it should not re-activate the
                // toplevel. Otherwise the result may not meet user expectations. See 6981400.
                focusedWindowChangeAllowed = false;
            }
        }
        if (!isRequestFocusAccepted(temporary, focusedWindowChangeAllowed, cause)) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("requestFocus is not accepted");
            }
            return false;
        }
        // Update most-recent map
        KeyboardFocusManager.setMostRecentFocusOwner(this);

        Component window = this;
        while ( (window != null) && !(window instanceof Window)) {
            if (!window.isVisible()) {
                if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                    focusLog.finest("component is recurively invisible");
                }
                return false;
            }
            window = window.parent;
        }

        ComponentPeer peer = this.peer;
        Component heavyweight = (peer instanceof LightweightPeer)
            ? getNativeContainer() : this;
        if (heavyweight == null || !heavyweight.isVisible()) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("Component is not a part of visible hierarchy");
            }
            return false;
        }
        peer = heavyweight.peer;
        if (peer == null) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("Peer is null");
            }
            return false;
        }

        // Focus this Component
        long time = 0;
        if (EventQueue.isDispatchThread()) {
            time = Toolkit.getEventQueue().getMostRecentKeyEventTime();
        } else {
            // A focus request made from outside EDT should not be associated with any event
            // and so its time stamp is simply set to the current time.
            time = System.currentTimeMillis();
        }

        boolean success = peer.requestFocus
            (this, temporary, focusedWindowChangeAllowed, time, cause);
        if (!success) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager
                (appContext).dequeueKeyEvents(time, this);
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("Peer request failed");
            }
        } else {
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("Pass for " + this);
            }
        }
        return success;
    }

    private boolean isRequestFocusAccepted(boolean temporary,
                                           boolean focusedWindowChangeAllowed,
                                           CausedFocusEvent.Cause cause)
    {
        if (!isFocusable() || !isVisible()) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("Not focusable or not visible");
            }
            return false;
        }

        ComponentPeer peer = this.peer;
        if (peer == null) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("peer is null");
            }
            return false;
        }

        Window window = getContainingWindow();
        if (window == null || !window.isFocusableWindow()) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("Component doesn't have toplevel");
            }
            return false;
        }

        // We have passed all regular checks for focus request,
        // now let's call RequestFocusController and see what it says.
        Component focusOwner = KeyboardFocusManager.getMostRecentFocusOwner(window);
        if (focusOwner == null) {
            // sometimes most recent focus owner may be null, but focus owner is not
            // e.g. we reset most recent focus owner if user removes focus owner
            focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner != null && focusOwner.getContainingWindow() != window) {
                focusOwner = null;
            }
        }

        if (focusOwner == this || focusOwner == null) {
            // Controller is supposed to verify focus transfers and for this it
            // should know both from and to components.  And it shouldn't verify
            // transfers from when these components are equal.
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("focus owner is null or this");
            }
            return true;
        }

        if (CausedFocusEvent.Cause.ACTIVATION == cause) {
            // we shouldn't call RequestFocusController in case we are
            // in activation.  We do request focus on component which
            // has got temporary focus lost and then on component which is
            // most recent focus owner.  But most recent focus owner can be
            // changed by requestFocsuXXX() call only, so this transfer has
            // been already approved.
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("cause is activation");
            }
            return true;
        }

        boolean ret = Component.requestFocusController.acceptRequestFocus(focusOwner,
                                                                          this,
                                                                          temporary,
                                                                          focusedWindowChangeAllowed,
                                                                          cause);
        if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
            focusLog.finest("RequestFocusController returns {0}", ret);
        }

        return ret;
    }

    private static RequestFocusController requestFocusController = new DummyRequestFocusController();

    // Swing access this method through reflection to implement InputVerifier's functionality.
    // Perhaps, we should make this method public (later ;)
    private static class DummyRequestFocusController implements RequestFocusController {
        public boolean acceptRequestFocus(Component from, Component to,
                                          boolean temporary, boolean focusedWindowChangeAllowed,
                                          CausedFocusEvent.Cause cause)
        {
            return true;
        }
    };

    synchronized static void setRequestFocusController(RequestFocusController requestController)
    {
        if (requestController == null) {
            requestFocusController = new DummyRequestFocusController();
        } else {
            requestFocusController = requestController;
        }
    }


    public Container getFocusCycleRootAncestor() {
        Container rootAncestor = this.parent;
        while (rootAncestor != null && !rootAncestor.isFocusCycleRoot()) {
            rootAncestor = rootAncestor.parent;
        }
        return rootAncestor;
    }


    public boolean isFocusCycleRoot(Container container) {
        Container rootAncestor = getFocusCycleRootAncestor();
        return (rootAncestor == container);
    }

    Container getTraversalRoot() {
        return getFocusCycleRootAncestor();
    }


    public void transferFocus() {
        nextFocus();
    }


    @Deprecated
    public void nextFocus() {
        transferFocus(false);
    }

    boolean transferFocus(boolean clearOnFailure) {
        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("clearOnFailure = " + clearOnFailure);
        }
        Component toFocus = getNextFocusCandidate();
        boolean res = false;
        if (toFocus != null && !toFocus.isFocusOwner() && toFocus != this) {
            res = toFocus.requestFocusInWindow(CausedFocusEvent.Cause.TRAVERSAL_FORWARD);
        }
        if (clearOnFailure && !res) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                focusLog.finer("clear global focus owner");
            }
            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwnerPriv();
        }
        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("returning result: " + res);
        }
        return res;
    }

    final Component getNextFocusCandidate() {
        Container rootAncestor = getTraversalRoot();
        Component comp = this;
        while (rootAncestor != null &&
               !(rootAncestor.isShowing() && rootAncestor.canBeFocusOwner()))
        {
            comp = rootAncestor;
            rootAncestor = comp.getFocusCycleRootAncestor();
        }
        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("comp = " + comp + ", root = " + rootAncestor);
        }
        Component candidate = null;
        if (rootAncestor != null) {
            FocusTraversalPolicy policy = rootAncestor.getFocusTraversalPolicy();
            Component toFocus = policy.getComponentAfter(rootAncestor, comp);
            if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                focusLog.finer("component after is " + toFocus);
            }
            if (toFocus == null) {
                toFocus = policy.getDefaultComponent(rootAncestor);
                if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                    focusLog.finer("default component is " + toFocus);
                }
            }
            if (toFocus == null) {
                Applet applet = EmbeddedFrame.getAppletIfAncestorOf(this);
                if (applet != null) {
                    toFocus = applet;
                }
            }
            candidate = toFocus;
        }
        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("Focus transfer candidate: " + candidate);
        }
        return candidate;
    }


    public void transferFocusBackward() {
        transferFocusBackward(false);
    }

    boolean transferFocusBackward(boolean clearOnFailure) {
        Container rootAncestor = getTraversalRoot();
        Component comp = this;
        while (rootAncestor != null &&
               !(rootAncestor.isShowing() && rootAncestor.canBeFocusOwner()))
        {
            comp = rootAncestor;
            rootAncestor = comp.getFocusCycleRootAncestor();
        }
        boolean res = false;
        if (rootAncestor != null) {
            FocusTraversalPolicy policy = rootAncestor.getFocusTraversalPolicy();
            Component toFocus = policy.getComponentBefore(rootAncestor, comp);
            if (toFocus == null) {
                toFocus = policy.getDefaultComponent(rootAncestor);
            }
            if (toFocus != null) {
                res = toFocus.requestFocusInWindow(CausedFocusEvent.Cause.TRAVERSAL_BACKWARD);
            }
        }
        if (clearOnFailure && !res) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                focusLog.finer("clear global focus owner");
            }
            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwnerPriv();
        }
        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("returning result: " + res);
        }
        return res;
    }


    public void transferFocusUpCycle() {
        Container rootAncestor;
        for (rootAncestor = getFocusCycleRootAncestor();
             rootAncestor != null && !(rootAncestor.isShowing() &&
                                       rootAncestor.isFocusable() &&
                                       rootAncestor.isEnabled());
             rootAncestor = rootAncestor.getFocusCycleRootAncestor()) {
        }

        if (rootAncestor != null) {
            Container rootAncestorRootAncestor =
                rootAncestor.getFocusCycleRootAncestor();
            Container fcr = (rootAncestorRootAncestor != null) ?
                rootAncestorRootAncestor : rootAncestor;

            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                setGlobalCurrentFocusCycleRootPriv(fcr);
            rootAncestor.requestFocus(CausedFocusEvent.Cause.TRAVERSAL_UP);
        } else {
            Window window = getContainingWindow();

            if (window != null) {
                Component toFocus = window.getFocusTraversalPolicy().
                    getDefaultComponent(window);
                if (toFocus != null) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().
                        setGlobalCurrentFocusCycleRootPriv(window);
                    toFocus.requestFocus(CausedFocusEvent.Cause.TRAVERSAL_UP);
                }
            }
        }
    }


    public boolean hasFocus() {
        return (KeyboardFocusManager.getCurrentKeyboardFocusManager().
                getFocusOwner() == this);
    }


    public boolean isFocusOwner() {
        return hasFocus();
    }


    private boolean autoFocusTransferOnDisposal = true;

    void setAutoFocusTransferOnDisposal(boolean value) {
        autoFocusTransferOnDisposal = value;
    }

    boolean isAutoFocusTransferOnDisposal() {
        return autoFocusTransferOnDisposal;
    }


    public void add(PopupMenu popup) {
        synchronized (getTreeLock()) {
            if (popup.parent != null) {
                popup.parent.remove(popup);
            }
            if (popups == null) {
                popups = new Vector<PopupMenu>();
            }
            popups.addElement(popup);
            popup.parent = this;

            if (peer != null) {
                if (popup.peer == null) {
                    popup.addNotify();
                }
            }
        }
    }


    @SuppressWarnings("unchecked")
    public void remove(MenuComponent popup) {
        synchronized (getTreeLock()) {
            if (popups == null) {
                return;
            }
            int index = popups.indexOf(popup);
            if (index >= 0) {
                PopupMenu pmenu = (PopupMenu)popup;
                if (pmenu.peer != null) {
                    pmenu.removeNotify();
                }
                pmenu.parent = null;
                popups.removeElementAt(index);
                if (popups.size() == 0) {
                    popups = null;
                }
            }
        }
    }


    protected String paramString() {
        final String thisName = Objects.toString(getName(), "");
        final String invalid = isValid() ? "" : ",invalid";
        final String hidden = visible ? "" : ",hidden";
        final String disabled = enabled ? "" : ",disabled";
        return thisName + ',' + x + ',' + y + ',' + width + 'x' + height
                + invalid + hidden + disabled;
    }


    public String toString() {
        return getClass().getName() + '[' + paramString() + ']';
    }


    public void list() {
        list(System.out, 0);
    }


    public void list(PrintStream out) {
        list(out, 0);
    }


    public void list(PrintStream out, int indent) {
        for (int i = 0 ; i < indent ; i++) {
            out.print(" ");
        }
        out.println(this);
    }


    public void list(PrintWriter out) {
        list(out, 0);
    }


    public void list(PrintWriter out, int indent) {
        for (int i = 0 ; i < indent ; i++) {
            out.print(" ");
        }
        out.println(this);
    }


    final Container getNativeContainer() {
        Container p = getContainer();
        while (p != null && p.peer instanceof LightweightPeer) {
            p = p.getContainer();
        }
        return p;
    }


    public void addPropertyChangeListener(
                                                       PropertyChangeListener listener) {
        synchronized (getObjectLock()) {
            if (listener == null) {
                return;
            }
            if (changeSupport == null) {
                changeSupport = new PropertyChangeSupport(this);
            }
            changeSupport.addPropertyChangeListener(listener);
        }
    }


    public void removePropertyChangeListener(
                                                          PropertyChangeListener listener) {
        synchronized (getObjectLock()) {
            if (listener == null || changeSupport == null) {
                return;
            }
            changeSupport.removePropertyChangeListener(listener);
        }
    }


    public PropertyChangeListener[] getPropertyChangeListeners() {
        synchronized (getObjectLock()) {
            if (changeSupport == null) {
                return new PropertyChangeListener[0];
            }
            return changeSupport.getPropertyChangeListeners();
        }
    }


    public void addPropertyChangeListener(
                                                       String propertyName,
                                                       PropertyChangeListener listener) {
        synchronized (getObjectLock()) {
            if (listener == null) {
                return;
            }
            if (changeSupport == null) {
                changeSupport = new PropertyChangeSupport(this);
            }
            changeSupport.addPropertyChangeListener(propertyName, listener);
        }
    }


    public void removePropertyChangeListener(
                                                          String propertyName,
                                                          PropertyChangeListener listener) {
        synchronized (getObjectLock()) {
            if (listener == null || changeSupport == null) {
                return;
            }
            changeSupport.removePropertyChangeListener(propertyName, listener);
        }
    }


    public PropertyChangeListener[] getPropertyChangeListeners(
                                                                            String propertyName) {
        synchronized (getObjectLock()) {
            if (changeSupport == null) {
                return new PropertyChangeListener[0];
            }
            return changeSupport.getPropertyChangeListeners(propertyName);
        }
    }


    protected void firePropertyChange(String propertyName,
                                      Object oldValue, Object newValue) {
        PropertyChangeSupport changeSupport;
        synchronized (getObjectLock()) {
            changeSupport = this.changeSupport;
        }
        if (changeSupport == null ||
            (oldValue != null && newValue != null && oldValue.equals(newValue))) {
            return;
        }
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }


    protected void firePropertyChange(String propertyName,
                                      boolean oldValue, boolean newValue) {
        PropertyChangeSupport changeSupport = this.changeSupport;
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }


    protected void firePropertyChange(String propertyName,
                                      int oldValue, int newValue) {
        PropertyChangeSupport changeSupport = this.changeSupport;
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }


    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        firePropertyChange(propertyName, Byte.valueOf(oldValue), Byte.valueOf(newValue));
    }


    public void firePropertyChange(String propertyName, char oldValue, char newValue) {
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        firePropertyChange(propertyName, new Character(oldValue), new Character(newValue));
    }


    public void firePropertyChange(String propertyName, short oldValue, short newValue) {
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        firePropertyChange(propertyName, Short.valueOf(oldValue), Short.valueOf(newValue));
    }



    public void firePropertyChange(String propertyName, long oldValue, long newValue) {
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        firePropertyChange(propertyName, Long.valueOf(oldValue), Long.valueOf(newValue));
    }


    public void firePropertyChange(String propertyName, float oldValue, float newValue) {
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        firePropertyChange(propertyName, Float.valueOf(oldValue), Float.valueOf(newValue));
    }


    public void firePropertyChange(String propertyName, double oldValue, double newValue) {
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        firePropertyChange(propertyName, Double.valueOf(oldValue), Double.valueOf(newValue));
    }


    // Serialization support.


    private int componentSerializedDataVersion = 4;


    private void doSwingSerialization() {
        Package swingPackage = Package.getPackage("javax.swing");
        // For Swing serialization to correctly work Swing needs to
        // be notified before Component does it's serialization.  This
        // hack accomodates this.
        //
        // Swing classes MUST be loaded by the bootstrap class loader,
        // otherwise we don't consider them.
        for (Class<?> klass = Component.this.getClass(); klass != null;
                   klass = klass.getSuperclass()) {
            if (klass.getPackage() == swingPackage &&
                      klass.getClassLoader() == null) {
                final Class<?> swingClass = klass;
                // Find the first override of the compWriteObjectNotify method
                Method[] methods = AccessController.doPrivileged(
                                                                 new PrivilegedAction<Method[]>() {
                                                                     public Method[] run() {
                                                                         return swingClass.getDeclaredMethods();
                                                                     }
                                                                 });
                for (int counter = methods.length - 1; counter >= 0;
                     counter--) {
                    final Method method = methods[counter];
                    if (method.getName().equals("compWriteObjectNotify")){
                        // We found it, use doPrivileged to make it accessible
                        // to use.
                        AccessController.doPrivileged(new PrivilegedAction<Void>() {
                                public Void run() {
                                    method.setAccessible(true);
                                    return null;
                                }
                            });
                        // Invoke the method
                        try {
                            method.invoke(this, (Object[]) null);
                        } catch (IllegalAccessException iae) {
                        } catch (InvocationTargetException ite) {
                        }
                        // We're done, bail.
                        return;
                    }
                }
            }
        }
    }


    private void writeObject(ObjectOutputStream s)
      throws IOException
    {
        doSwingSerialization();

        s.defaultWriteObject();

        AWTEventMulticaster.save(s, componentListenerK, componentListener);
        AWTEventMulticaster.save(s, focusListenerK, focusListener);
        AWTEventMulticaster.save(s, keyListenerK, keyListener);
        AWTEventMulticaster.save(s, mouseListenerK, mouseListener);
        AWTEventMulticaster.save(s, mouseMotionListenerK, mouseMotionListener);
        AWTEventMulticaster.save(s, inputMethodListenerK, inputMethodListener);

        s.writeObject(null);
        s.writeObject(componentOrientation);

        AWTEventMulticaster.save(s, hierarchyListenerK, hierarchyListener);
        AWTEventMulticaster.save(s, hierarchyBoundsListenerK,
                                 hierarchyBoundsListener);
        s.writeObject(null);

        AWTEventMulticaster.save(s, mouseWheelListenerK, mouseWheelListener);
        s.writeObject(null);

    }


    private void readObject(ObjectInputStream s)
      throws ClassNotFoundException, IOException
    {
        objectLock = new Object();

        acc = AccessController.getContext();

        s.defaultReadObject();

        appContext = AppContext.getAppContext();
        coalescingEnabled = checkCoalescing();
        if (componentSerializedDataVersion < 4) {
            // These fields are non-transient and rely on default
            // serialization. However, the default values are insufficient,
            // so we need to set them explicitly for object data streams prior
            // to 1.4.
            focusable = true;
            isFocusTraversableOverridden = FOCUS_TRAVERSABLE_UNKNOWN;
            initializeFocusTraversalKeys();
            focusTraversalKeysEnabled = true;
        }

        Object keyOrNull;
        while(null != (keyOrNull = s.readObject())) {
            String key = ((String)keyOrNull).intern();

            if (componentListenerK == key)
                addComponentListener((ComponentListener)(s.readObject()));

            else if (focusListenerK == key)
                addFocusListener((FocusListener)(s.readObject()));

            else if (keyListenerK == key)
                addKeyListener((KeyListener)(s.readObject()));

            else if (mouseListenerK == key)
                addMouseListener((MouseListener)(s.readObject()));

            else if (mouseMotionListenerK == key)
                addMouseMotionListener((MouseMotionListener)(s.readObject()));

            else if (inputMethodListenerK == key)
                addInputMethodListener((InputMethodListener)(s.readObject()));

            else // skip value for unrecognized key
                s.readObject();

        }

        // Read the component's orientation if it's present
        Object orient = null;

        try {
            orient = s.readObject();
        } catch (java.io.OptionalDataException e) {
            // JDK 1.1 instances will not have this optional data.
            // e.eof will be true to indicate that there is no more
            // data available for this object.
            // If e.eof is not true, throw the exception as it
            // might have been caused by reasons unrelated to
            // componentOrientation.

            if (!e.eof)  {
                throw (e);
            }
        }

        if (orient != null) {
            componentOrientation = (ComponentOrientation)orient;
        } else {
            componentOrientation = ComponentOrientation.UNKNOWN;
        }

        try {
            while(null != (keyOrNull = s.readObject())) {
                String key = ((String)keyOrNull).intern();

                if (hierarchyListenerK == key) {
                    addHierarchyListener((HierarchyListener)(s.readObject()));
                }
                else if (hierarchyBoundsListenerK == key) {
                    addHierarchyBoundsListener((HierarchyBoundsListener)
                                               (s.readObject()));
                }
                else {
                    // skip value for unrecognized key
                    s.readObject();
                }
            }
        } catch (java.io.OptionalDataException e) {
            // JDK 1.1/1.2 instances will not have this optional data.
            // e.eof will be true to indicate that there is no more
            // data available for this object.
            // If e.eof is not true, throw the exception as it
            // might have been caused by reasons unrelated to
            // hierarchy and hierarchyBounds listeners.

            if (!e.eof)  {
                throw (e);
            }
        }

        try {
            while (null != (keyOrNull = s.readObject())) {
                String key = ((String)keyOrNull).intern();

                if (mouseWheelListenerK == key) {
                    addMouseWheelListener((MouseWheelListener)(s.readObject()));
                }
                else {
                    // skip value for unrecognized key
                    s.readObject();
                }
            }
        } catch (java.io.OptionalDataException e) {
            // pre-1.3 instances will not have this optional data.
            // e.eof will be true to indicate that there is no more
            // data available for this object.
            // If e.eof is not true, throw the exception as it
            // might have been caused by reasons unrelated to
            // mouse wheel listeners

            if (!e.eof)  {
                throw (e);
            }
        }

        if (popups != null) {
            int npopups = popups.size();
            for (int i = 0 ; i < npopups ; i++) {
                PopupMenu popup = popups.elementAt(i);
                popup.parent = this;
            }
        }
    }


    public void setComponentOrientation(ComponentOrientation o) {
        ComponentOrientation oldValue = componentOrientation;
        componentOrientation = o;

        // This is a bound property, so report the change to
        // any registered listeners.  (Cheap if there are none.)
        firePropertyChange("componentOrientation", oldValue, o);

        // This could change the preferred size of the Component.
        invalidateIfValid();
    }


    public ComponentOrientation getComponentOrientation() {
        return componentOrientation;
    }


    public void applyComponentOrientation(ComponentOrientation orientation) {
        if (orientation == null) {
            throw new NullPointerException();
        }
        setComponentOrientation(orientation);
    }

    final boolean canBeFocusOwner() {
        // It is enabled, visible, focusable.
        if (isEnabled() && isDisplayable() && isVisible() && isFocusable()) {
            return true;
        }
        return false;
    }


    final boolean canBeFocusOwnerRecursively() {
        // - it is enabled, visible, focusable
        if (!canBeFocusOwner()) {
            return false;
        }

        // - it's parents are all enabled and showing
        synchronized(getTreeLock()) {
            if (parent != null) {
                return parent.canContainFocusOwner(this);
            }
        }
        return true;
    }


    final void relocateComponent() {
        synchronized (getTreeLock()) {
            if (peer == null) {
                return;
            }
            int nativeX = x;
            int nativeY = y;
            for (Component cont = getContainer();
                    cont != null && cont.isLightweight();
                    cont = cont.getContainer())
            {
                nativeX += cont.x;
                nativeY += cont.y;
            }
            peer.setBounds(nativeX, nativeY, width, height,
                    ComponentPeer.SET_LOCATION);
        }
    }


    Window getContainingWindow() {
        return SunToolkit.getContainingWindow(this);
    }


    private static native void initIDs();




    protected AccessibleContext accessibleContext = null;


    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }


    protected abstract class AccessibleAWTComponent extends AccessibleContext
        implements Serializable, AccessibleComponent {

        private static final long serialVersionUID = 642321655757800191L;


        protected AccessibleAWTComponent() {
        }


        private volatile transient int propertyListenersCount = 0;

        protected ComponentListener accessibleAWTComponentHandler = null;
        protected FocusListener accessibleAWTFocusHandler = null;


        protected class AccessibleAWTComponentHandler implements ComponentListener {
            public void componentHidden(ComponentEvent e)  {
                if (accessibleContext != null) {
                    accessibleContext.firePropertyChange(
                                                         AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                         AccessibleState.VISIBLE, null);
                }
            }

            public void componentShown(ComponentEvent e)  {
                if (accessibleContext != null) {
                    accessibleContext.firePropertyChange(
                                                         AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                         null, AccessibleState.VISIBLE);
                }
            }

            public void componentMoved(ComponentEvent e)  {
            }

            public void componentResized(ComponentEvent e)  {
            }
        } // inner class AccessibleAWTComponentHandler



        protected class AccessibleAWTFocusHandler implements FocusListener {
            public void focusGained(FocusEvent event) {
                if (accessibleContext != null) {
                    accessibleContext.firePropertyChange(
                                                         AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                         null, AccessibleState.FOCUSED);
                }
            }
            public void focusLost(FocusEvent event) {
                if (accessibleContext != null) {
                    accessibleContext.firePropertyChange(
                                                         AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                         AccessibleState.FOCUSED, null);
                }
            }
        }  // inner class AccessibleAWTFocusHandler



        public void addPropertyChangeListener(PropertyChangeListener listener) {
            if (accessibleAWTComponentHandler == null) {
                accessibleAWTComponentHandler = new AccessibleAWTComponentHandler();
            }
            if (accessibleAWTFocusHandler == null) {
                accessibleAWTFocusHandler = new AccessibleAWTFocusHandler();
            }
            if (propertyListenersCount++ == 0) {
                Component.this.addComponentListener(accessibleAWTComponentHandler);
                Component.this.addFocusListener(accessibleAWTFocusHandler);
            }
            super.addPropertyChangeListener(listener);
        }


        public void removePropertyChangeListener(PropertyChangeListener listener) {
            if (--propertyListenersCount == 0) {
                Component.this.removeComponentListener(accessibleAWTComponentHandler);
                Component.this.removeFocusListener(accessibleAWTFocusHandler);
            }
            super.removePropertyChangeListener(listener);
        }

        // AccessibleContext methods
        //

        public String getAccessibleName() {
            return accessibleName;
        }


        public String getAccessibleDescription() {
            return accessibleDescription;
        }


        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.AWT_COMPONENT;
        }


        public AccessibleStateSet getAccessibleStateSet() {
            return Component.this.getAccessibleStateSet();
        }


        public Accessible getAccessibleParent() {
            if (accessibleParent != null) {
                return accessibleParent;
            } else {
                Container parent = getParent();
                if (parent instanceof Accessible) {
                    return (Accessible) parent;
                }
            }
            return null;
        }


        public int getAccessibleIndexInParent() {
            return Component.this.getAccessibleIndexInParent();
        }


        public int getAccessibleChildrenCount() {
            return 0; // Components don't have children
        }


        public Accessible getAccessibleChild(int i) {
            return null; // Components don't have children
        }


        public Locale getLocale() {
            return Component.this.getLocale();
        }


        public AccessibleComponent getAccessibleComponent() {
            return this;
        }


        // AccessibleComponent methods
        //

        public Color getBackground() {
            return Component.this.getBackground();
        }


        public void setBackground(Color c) {
            Component.this.setBackground(c);
        }


        public Color getForeground() {
            return Component.this.getForeground();
        }


        public void setForeground(Color c) {
            Component.this.setForeground(c);
        }


        public Cursor getCursor() {
            return Component.this.getCursor();
        }


        public void setCursor(Cursor cursor) {
            Component.this.setCursor(cursor);
        }


        public Font getFont() {
            return Component.this.getFont();
        }


        public void setFont(Font f) {
            Component.this.setFont(f);
        }


        public FontMetrics getFontMetrics(Font f) {
            if (f == null) {
                return null;
            } else {
                return Component.this.getFontMetrics(f);
            }
        }


        public boolean isEnabled() {
            return Component.this.isEnabled();
        }


        public void setEnabled(boolean b) {
            boolean old = Component.this.isEnabled();
            Component.this.setEnabled(b);
            if (b != old) {
                if (accessibleContext != null) {
                    if (b) {
                        accessibleContext.firePropertyChange(
                                                             AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                             null, AccessibleState.ENABLED);
                    } else {
                        accessibleContext.firePropertyChange(
                                                             AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                             AccessibleState.ENABLED, null);
                    }
                }
            }
        }


        public boolean isVisible() {
            return Component.this.isVisible();
        }


        public void setVisible(boolean b) {
            boolean old = Component.this.isVisible();
            Component.this.setVisible(b);
            if (b != old) {
                if (accessibleContext != null) {
                    if (b) {
                        accessibleContext.firePropertyChange(
                                                             AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                             null, AccessibleState.VISIBLE);
                    } else {
                        accessibleContext.firePropertyChange(
                                                             AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                             AccessibleState.VISIBLE, null);
                    }
                }
            }
        }


        public boolean isShowing() {
            return Component.this.isShowing();
        }


        public boolean contains(Point p) {
            return Component.this.contains(p);
        }


        public Point getLocationOnScreen() {
            synchronized (Component.this.getTreeLock()) {
                if (Component.this.isShowing()) {
                    return Component.this.getLocationOnScreen();
                } else {
                    return null;
                }
            }
        }


        public Point getLocation() {
            return Component.this.getLocation();
        }


        public void setLocation(Point p) {
            Component.this.setLocation(p);
        }


        public Rectangle getBounds() {
            return Component.this.getBounds();
        }


        public void setBounds(Rectangle r) {
            Component.this.setBounds(r);
        }


        public Dimension getSize() {
            return Component.this.getSize();
        }


        public void setSize(Dimension d) {
            Component.this.setSize(d);
        }


        public Accessible getAccessibleAt(Point p) {
            return null; // Components don't have children
        }


        public boolean isFocusTraversable() {
            return Component.this.isFocusTraversable();
        }


        public void requestFocus() {
            Component.this.requestFocus();
        }


        public void addFocusListener(FocusListener l) {
            Component.this.addFocusListener(l);
        }


        public void removeFocusListener(FocusListener l) {
            Component.this.removeFocusListener(l);
        }

    } // inner class AccessibleAWTComponent



    int getAccessibleIndexInParent() {
        synchronized (getTreeLock()) {
            int index = -1;
            Container parent = this.getParent();
            if (parent != null && parent instanceof Accessible) {
                Component ca[] = parent.getComponents();
                for (int i = 0; i < ca.length; i++) {
                    if (ca[i] instanceof Accessible) {
                        index++;
                    }
                    if (this.equals(ca[i])) {
                        return index;
                    }
                }
            }
            return -1;
        }
    }


    AccessibleStateSet getAccessibleStateSet() {
        synchronized (getTreeLock()) {
            AccessibleStateSet states = new AccessibleStateSet();
            if (this.isEnabled()) {
                states.add(AccessibleState.ENABLED);
            }
            if (this.isFocusTraversable()) {
                states.add(AccessibleState.FOCUSABLE);
            }
            if (this.isVisible()) {
                states.add(AccessibleState.VISIBLE);
            }
            if (this.isShowing()) {
                states.add(AccessibleState.SHOWING);
            }
            if (this.isFocusOwner()) {
                states.add(AccessibleState.FOCUSED);
            }
            if (this instanceof Accessible) {
                AccessibleContext ac = ((Accessible) this).getAccessibleContext();
                if (ac != null) {
                    Accessible ap = ac.getAccessibleParent();
                    if (ap != null) {
                        AccessibleContext pac = ap.getAccessibleContext();
                        if (pac != null) {
                            AccessibleSelection as = pac.getAccessibleSelection();
                            if (as != null) {
                                states.add(AccessibleState.SELECTABLE);
                                int i = ac.getAccessibleIndexInParent();
                                if (i >= 0) {
                                    if (as.isAccessibleChildSelected(i)) {
                                        states.add(AccessibleState.SELECTED);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (Component.isInstanceOf(this, "javax.swing.JComponent")) {
                if (((javax.swing.JComponent) this).isOpaque()) {
                    states.add(AccessibleState.OPAQUE);
                }
            }
            return states;
        }
    }


    static boolean isInstanceOf(Object obj, String className) {
        if (obj == null) return false;
        if (className == null) return false;

        Class<?> cls = obj.getClass();
        while (cls != null) {
            if (cls.getName().equals(className)) {
                return true;
            }
            cls = cls.getSuperclass();
        }
        return false;
    }


    // ************************** MIXING CODE *******************************


    final boolean areBoundsValid() {
        Container cont = getContainer();
        return cont == null || cont.isValid() || cont.getLayout() == null;
    }


    void applyCompoundShape(Region shape) {
        checkTreeLock();

        if (!areBoundsValid()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this + "; areBoundsValid = " + areBoundsValid());
            }
            return;
        }

        if (!isLightweight()) {
            ComponentPeer peer = getPeer();
            if (peer != null) {
                // The Region class has some optimizations. That's why
                // we should manually check whether it's empty and
                // substitute the object ourselves. Otherwise we end up
                // with some incorrect Region object with loX being
                // greater than the hiX for instance.
                if (shape.isEmpty()) {
                    shape = Region.EMPTY_REGION;
                }


                // Note: the shape is not really copied/cloned. We create
                // the Region object ourselves, so there's no any possibility
                // to modify the object outside of the mixing code.
                // Nullifying compoundShape means that the component has normal shape
                // (or has no shape at all).
                if (shape.equals(getNormalShape())) {
                    if (this.compoundShape == null) {
                        return;
                    }
                    this.compoundShape = null;
                    peer.applyShape(null);
                } else {
                    if (shape.equals(getAppliedShape())) {
                        return;
                    }
                    this.compoundShape = shape;
                    Point compAbsolute = getLocationOnWindow();
                    if (mixingLog.isLoggable(PlatformLogger.Level.FINER)) {
                        mixingLog.fine("this = " + this +
                                "; compAbsolute=" + compAbsolute + "; shape=" + shape);
                    }
                    peer.applyShape(shape.getTranslatedRegion(-compAbsolute.x, -compAbsolute.y));
                }
            }
        }
    }


    private Region getAppliedShape() {
        checkTreeLock();
        //XXX: if we allow LW components to have a shape, this must be changed
        return (this.compoundShape == null || isLightweight()) ? getNormalShape() : this.compoundShape;
    }

    Point getLocationOnWindow() {
        checkTreeLock();
        Point curLocation = getLocation();

        for (Container parent = getContainer();
                parent != null && !(parent instanceof Window);
                parent = parent.getContainer())
        {
            curLocation.x += parent.getX();
            curLocation.y += parent.getY();
        }

        return curLocation;
    }


    final Region getNormalShape() {
        checkTreeLock();
        //XXX: we may take into account a user-specified shape for this component
        Point compAbsolute = getLocationOnWindow();
        return
            Region.getInstanceXYWH(
                    compAbsolute.x,
                    compAbsolute.y,
                    getWidth(),
                    getHeight()
            );
    }


    Region getOpaqueShape() {
        checkTreeLock();
        if (mixingCutoutRegion != null) {
            return mixingCutoutRegion;
        } else {
            return getNormalShape();
        }
    }

    final int getSiblingIndexAbove() {
        checkTreeLock();
        Container parent = getContainer();
        if (parent == null) {
            return -1;
        }

        int nextAbove = parent.getComponentZOrder(this) - 1;

        return nextAbove < 0 ? -1 : nextAbove;
    }

    final ComponentPeer getHWPeerAboveMe() {
        checkTreeLock();

        Container cont = getContainer();
        int indexAbove = getSiblingIndexAbove();

        while (cont != null) {
            for (int i = indexAbove; i > -1; i--) {
                Component comp = cont.getComponent(i);
                if (comp != null && comp.isDisplayable() && !comp.isLightweight()) {
                    return comp.getPeer();
                }
            }
            // traversing the hierarchy up to the closest HW container;
            // further traversing may return a component that is not actually
            // a native sibling of this component and this kind of z-order
            // request may not be allowed by the underlying system (6852051).
            if (!cont.isLightweight()) {
                break;
            }

            indexAbove = cont.getSiblingIndexAbove();
            cont = cont.getContainer();
        }

        return null;
    }

    final int getSiblingIndexBelow() {
        checkTreeLock();
        Container parent = getContainer();
        if (parent == null) {
            return -1;
        }

        int nextBelow = parent.getComponentZOrder(this) + 1;

        return nextBelow >= parent.getComponentCount() ? -1 : nextBelow;
    }

    final boolean isNonOpaqueForMixing() {
        return mixingCutoutRegion != null &&
            mixingCutoutRegion.isEmpty();
    }

    private Region calculateCurrentShape() {
        checkTreeLock();
        Region s = getNormalShape();

        if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
            mixingLog.fine("this = " + this + "; normalShape=" + s);
        }

        if (getContainer() != null) {
            Component comp = this;
            Container cont = comp.getContainer();

            while (cont != null) {
                for (int index = comp.getSiblingIndexAbove(); index != -1; --index) {

                    Component c = cont.getComponent(index);
                    if (c.isLightweight() && c.isShowing()) {
                        s = s.getDifference(c.getOpaqueShape());
                    }
                }

                if (cont.isLightweight()) {
                    s = s.getIntersection(cont.getNormalShape());
                } else {
                    break;
                }

                comp = cont;
                cont = cont.getContainer();
            }
        }

        if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
            mixingLog.fine("currentShape=" + s);
        }

        return s;
    }

    void applyCurrentShape() {
        checkTreeLock();
        if (!areBoundsValid()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this + "; areBoundsValid = " + areBoundsValid());
            }
            return; // Because applyCompoundShape() ignores such components anyway
        }
        if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
            mixingLog.fine("this = " + this);
        }
        applyCompoundShape(calculateCurrentShape());
    }

    final void subtractAndApplyShape(Region s) {
        checkTreeLock();

        if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
            mixingLog.fine("this = " + this + "; s=" + s);
        }

        applyCompoundShape(getAppliedShape().getDifference(s));
    }

    private final void applyCurrentShapeBelowMe() {
        checkTreeLock();
        Container parent = getContainer();
        if (parent != null && parent.isShowing()) {
            // First, reapply shapes of my siblings
            parent.recursiveApplyCurrentShape(getSiblingIndexBelow());

            // Second, if my container is non-opaque, reapply shapes of siblings of my container
            Container parent2 = parent.getContainer();
            while (!parent.isOpaque() && parent2 != null) {
                parent2.recursiveApplyCurrentShape(parent.getSiblingIndexBelow());

                parent = parent2;
                parent2 = parent.getContainer();
            }
        }
    }

    final void subtractAndApplyShapeBelowMe() {
        checkTreeLock();
        Container parent = getContainer();
        if (parent != null && isShowing()) {
            Region opaqueShape = getOpaqueShape();

            // First, cut my siblings
            parent.recursiveSubtractAndApplyShape(opaqueShape, getSiblingIndexBelow());

            // Second, if my container is non-opaque, cut siblings of my container
            Container parent2 = parent.getContainer();
            while (!parent.isOpaque() && parent2 != null) {
                parent2.recursiveSubtractAndApplyShape(opaqueShape, parent.getSiblingIndexBelow());

                parent = parent2;
                parent2 = parent.getContainer();
            }
        }
    }

    void mixOnShowing() {
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this);
            }
            if (!isMixingNeeded()) {
                return;
            }
            if (isLightweight()) {
                subtractAndApplyShapeBelowMe();
            } else {
                applyCurrentShape();
            }
        }
    }

    void mixOnHiding(boolean isLightweight) {
        // We cannot be sure that the peer exists at this point, so we need the argument
        //    to find out whether the hiding component is (well, actually was) a LW or a HW.
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this + "; isLightweight = " + isLightweight);
            }
            if (!isMixingNeeded()) {
                return;
            }
            if (isLightweight) {
                applyCurrentShapeBelowMe();
            }
        }
    }

    void mixOnReshaping() {
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this);
            }
            if (!isMixingNeeded()) {
                return;
            }
            if (isLightweight()) {
                applyCurrentShapeBelowMe();
            } else {
                applyCurrentShape();
            }
        }
    }

    void mixOnZOrderChanging(int oldZorder, int newZorder) {
        synchronized (getTreeLock()) {
            boolean becameHigher = newZorder < oldZorder;
            Container parent = getContainer();

            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this +
                    "; oldZorder=" + oldZorder + "; newZorder=" + newZorder + "; parent=" + parent);
            }
            if (!isMixingNeeded()) {
                return;
            }
            if (isLightweight()) {
                if (becameHigher) {
                    if (parent != null && isShowing()) {
                        parent.recursiveSubtractAndApplyShape(getOpaqueShape(), getSiblingIndexBelow(), oldZorder);
                    }
                } else {
                    if (parent != null) {
                        parent.recursiveApplyCurrentShape(oldZorder, newZorder);
                    }
                }
            } else {
                if (becameHigher) {
                    applyCurrentShape();
                } else {
                    if (parent != null) {
                        Region shape = getAppliedShape();

                        for (int index = oldZorder; index < newZorder; index++) {
                            Component c = parent.getComponent(index);
                            if (c.isLightweight() && c.isShowing()) {
                                shape = shape.getDifference(c.getOpaqueShape());
                            }
                        }
                        applyCompoundShape(shape);
                    }
                }
            }
        }
    }

    void mixOnValidating() {
        // This method gets overriden in the Container. Obviously, a plain
        // non-container components don't need to handle validation.
    }

    final boolean isMixingNeeded() {
        if (SunToolkit.getSunAwtDisableMixing()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINEST)) {
                mixingLog.finest("this = " + this + "; Mixing disabled via sun.awt.disableMixing");
            }
            return false;
        }
        if (!areBoundsValid()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this + "; areBoundsValid = " + areBoundsValid());
            }
            return false;
        }
        Window window = getContainingWindow();
        if (window != null) {
            if (!window.hasHeavyweightDescendants() || !window.hasLightweightDescendants() || window.isDisposing()) {
                if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                    mixingLog.fine("containing window = " + window +
                            "; has h/w descendants = " + window.hasHeavyweightDescendants() +
                            "; has l/w descendants = " + window.hasLightweightDescendants() +
                            "; disposing = " + window.isDisposing());
                }
                return false;
            }
        } else {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this + "; containing window is null");
            }
            return false;
        }
        return true;
    }

    // ****************** END OF MIXING CODE ********************************

    // Note that the method is overriden in the Window class,
    // a window doesn't need to be updated in the Z-order.
    void updateZOrder() {
        peer.setZOrder(getHWPeerAboveMe());
    }

}
