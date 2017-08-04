package java.awt;
import java.beans.PropertyChangeEvent;
import java.awt.event.*;
import java.awt.peer.*;
import java.awt.im.InputMethodHighlight;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.ColorModel;
import java.awt.datatransfer.Clipboard;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.net.URL;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import sun.awt.AppContext;
import sun.awt.HeadlessToolkit;
import sun.awt.NullComponentPeer;
import sun.awt.PeerEvent;
import sun.awt.SunToolkit;
import sun.awt.AWTAccessor;
import sun.security.util.SecurityConstants;
import sun.util.CoreResourceBundleControl;
public abstract class Toolkit {
    protected abstract DesktopPeer createDesktopPeer(Desktop target)
      throws HeadlessException;
    protected abstract ButtonPeer createButton(Button target)
        throws HeadlessException;
    protected abstract TextFieldPeer createTextField(TextField target)
        throws HeadlessException;
    protected abstract LabelPeer createLabel(Label target)
        throws HeadlessException;
    protected abstract ListPeer createList(java.awt.List target)
        throws HeadlessException;
    protected abstract CheckboxPeer createCheckbox(Checkbox target)
        throws HeadlessException;
    protected abstract ScrollbarPeer createScrollbar(Scrollbar target)
        throws HeadlessException;
    protected abstract ScrollPanePeer createScrollPane(ScrollPane target)
        throws HeadlessException;
    protected abstract TextAreaPeer createTextArea(TextArea target)
        throws HeadlessException;
    protected abstract ChoicePeer createChoice(Choice target)
        throws HeadlessException;
    protected abstract FramePeer createFrame(Frame target)
        throws HeadlessException;
    protected abstract CanvasPeer       createCanvas(Canvas target);
    protected abstract PanelPeer        createPanel(Panel target);
    protected abstract WindowPeer createWindow(Window target)
        throws HeadlessException;
    protected abstract DialogPeer createDialog(Dialog target)
        throws HeadlessException;
    protected abstract MenuBarPeer createMenuBar(MenuBar target)
        throws HeadlessException;
    protected abstract MenuPeer createMenu(Menu target)
        throws HeadlessException;
    protected abstract PopupMenuPeer createPopupMenu(PopupMenu target)
        throws HeadlessException;
    protected abstract MenuItemPeer createMenuItem(MenuItem target)
        throws HeadlessException;
    protected abstract FileDialogPeer createFileDialog(FileDialog target)
        throws HeadlessException;
    protected abstract CheckboxMenuItemPeer createCheckboxMenuItem(
        CheckboxMenuItem target) throws HeadlessException;
    protected MouseInfoPeer getMouseInfoPeer() {
        throw new UnsupportedOperationException("Not implemented");
    }
    private static LightweightPeer lightweightMarker;
    protected LightweightPeer createComponent(Component target) {
        if (lightweightMarker == null) {
            lightweightMarker = new NullComponentPeer();
        }
        return lightweightMarker;
    }
    @Deprecated
    protected abstract FontPeer getFontPeer(String name, int style);
    // The following method is called by the private method
    // <code>updateSystemColors</code> in <code>SystemColor</code>.
    protected void loadSystemColors(int[] systemColors)
        throws HeadlessException {
        GraphicsEnvironment.checkHeadless();
    }
    public void setDynamicLayout(final boolean dynamic)
        throws HeadlessException {
        GraphicsEnvironment.checkHeadless();
        if (this != getDefaultToolkit()) {
            getDefaultToolkit().setDynamicLayout(dynamic);
        }
    }
    protected boolean isDynamicLayoutSet()
        throws HeadlessException {
        GraphicsEnvironment.checkHeadless();
        if (this != Toolkit.getDefaultToolkit()) {
            return Toolkit.getDefaultToolkit().isDynamicLayoutSet();
        } else {
            return false;
        }
    }
    public boolean isDynamicLayoutActive()
        throws HeadlessException {
        GraphicsEnvironment.checkHeadless();
        if (this != Toolkit.getDefaultToolkit()) {
            return Toolkit.getDefaultToolkit().isDynamicLayoutActive();
        } else {
            return false;
        }
    }
    public abstract Dimension getScreenSize()
        throws HeadlessException;
    public abstract int getScreenResolution()
        throws HeadlessException;
    public Insets getScreenInsets(GraphicsConfiguration gc)
        throws HeadlessException {
        GraphicsEnvironment.checkHeadless();
        if (this != Toolkit.getDefaultToolkit()) {
            return Toolkit.getDefaultToolkit().getScreenInsets(gc);
        } else {
            return new Insets(0, 0, 0, 0);
        }
    }
    public abstract ColorModel getColorModel()
        throws HeadlessException;
    @Deprecated
    public abstract String[] getFontList();
    @Deprecated
    public abstract FontMetrics getFontMetrics(Font font);
    public abstract void sync();
    private static Toolkit toolkit;
    private static String atNames;
    private static void initAssistiveTechnologies() {
        // Get accessibility properties
        final String sep = File.separator;
        final Properties properties = new Properties();
        atNames = java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<String>() {
            public String run() {
                // Try loading the per-user accessibility properties file.
                try {
                    File propsFile = new File(
                      System.getProperty("user.home") +
                      sep + ".accessibility.properties");
                    FileInputStream in =
                        new FileInputStream(propsFile);
                    // Inputstream has been buffered in Properties class
                    properties.load(in);
                    in.close();
                } catch (Exception e) {
                    // Per-user accessibility properties file does not exist
                }
                // Try loading the system-wide accessibility properties
                // file only if a per-user accessibility properties
                // file does not exist or is empty.
                if (properties.size() == 0) {
                    try {
                        File propsFile = new File(
                            System.getProperty("java.home") + sep + "lib" +
                            sep + "accessibility.properties");
                        FileInputStream in =
                            new FileInputStream(propsFile);
                        // Inputstream has been buffered in Properties class
                        properties.load(in);
                        in.close();
                    } catch (Exception e) {
                        // System-wide accessibility properties file does
                        // not exist;
                    }
                }
                // Get whether a screen magnifier is present.  First check
                // the system property and then check the properties file.
                String magPresent = System.getProperty("javax.accessibility.screen_magnifier_present");
                if (magPresent == null) {
                    magPresent = properties.getProperty("screen_magnifier_present", null);
                    if (magPresent != null) {
                        System.setProperty("javax.accessibility.screen_magnifier_present", magPresent);
                    }
                }
                // Get the names of any assistive technolgies to load.  First
                // check the system property and then check the properties
                // file.
                String classNames = System.getProperty("javax.accessibility.assistive_technologies");
                if (classNames == null) {
                    classNames = properties.getProperty("assistive_technologies", null);
                    if (classNames != null) {
                        System.setProperty("javax.accessibility.assistive_technologies", classNames);
                    }
                }
                return classNames;
            }
        });
    }
    private static void loadAssistiveTechnologies() {
        // Load any assistive technologies
        if (atNames != null) {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            StringTokenizer parser = new StringTokenizer(atNames," ,");
            String atName;
            while (parser.hasMoreTokens()) {
                atName = parser.nextToken();
                try {
                    Class<?> clazz;
                    if (cl != null) {
                        clazz = cl.loadClass(atName);
                    } else {
                        clazz = Class.forName(atName);
                    }
                    clazz.newInstance();
                } catch (ClassNotFoundException e) {
                    throw new AWTError("Assistive Technology not found: "
                            + atName);
                } catch (InstantiationException e) {
                    throw new AWTError("Could not instantiate Assistive"
                            + " Technology: " + atName);
                } catch (IllegalAccessException e) {
                    throw new AWTError("Could not access Assistive"
                            + " Technology: " + atName);
                } catch (Exception e) {
                    throw new AWTError("Error trying to install Assistive"
                            + " Technology: " + atName + " " + e);
                }
            }
        }
    }
    public static synchronized Toolkit getDefaultToolkit() {
        if (toolkit == null) {
            java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Void>() {
                public Void run() {
                    Class<?> cls = null;
                    String nm = System.getProperty("awt.toolkit");
                    try {
                        cls = Class.forName(nm);
                    } catch (ClassNotFoundException e) {
                        ClassLoader cl = ClassLoader.getSystemClassLoader();
                        if (cl != null) {
                            try {
                                cls = cl.loadClass(nm);
                            } catch (final ClassNotFoundException ignored) {
                                throw new AWTError("Toolkit not found: " + nm);
                            }
                        }
                    }
                    try {
                        if (cls != null) {
                            toolkit = (Toolkit)cls.newInstance();
                            if (GraphicsEnvironment.isHeadless()) {
                                toolkit = new HeadlessToolkit(toolkit);
                            }
                        }
                    } catch (final InstantiationException ignored) {
                        throw new AWTError("Could not instantiate Toolkit: " + nm);
                    } catch (final IllegalAccessException ignored) {
                        throw new AWTError("Could not access Toolkit: " + nm);
                    }
                    return null;
                }
            });
            loadAssistiveTechnologies();
        }
        return toolkit;
    }
    public abstract Image getImage(String filename);
    public abstract Image getImage(URL url);
    public abstract Image createImage(String filename);
    public abstract Image createImage(URL url);
    public abstract boolean prepareImage(Image image, int width, int height,
                                         ImageObserver observer);
    public abstract int checkImage(Image image, int width, int height,
                                   ImageObserver observer);
    public abstract Image createImage(ImageProducer producer);
    public Image createImage(byte[] imagedata) {
        return createImage(imagedata, 0, imagedata.length);
    }
    public abstract Image createImage(byte[] imagedata,
                                      int imageoffset,
                                      int imagelength);
    public abstract PrintJob getPrintJob(Frame frame, String jobtitle,
                                         Properties props);
    public PrintJob getPrintJob(Frame frame, String jobtitle,
                                JobAttributes jobAttributes,
                                PageAttributes pageAttributes) {
        // Override to add printing support with new job/page control classes
        if (this != Toolkit.getDefaultToolkit()) {
            return Toolkit.getDefaultToolkit().getPrintJob(frame, jobtitle,
                                                           jobAttributes,
                                                           pageAttributes);
        } else {
            return getPrintJob(frame, jobtitle, null);
        }
    }
    public abstract void beep();
    public abstract Clipboard getSystemClipboard()
        throws HeadlessException;
    public Clipboard getSystemSelection() throws HeadlessException {
        GraphicsEnvironment.checkHeadless();
        if (this != Toolkit.getDefaultToolkit()) {
            return Toolkit.getDefaultToolkit().getSystemSelection();
        } else {
            GraphicsEnvironment.checkHeadless();
            return null;
        }
    }
    public int getMenuShortcutKeyMask() throws HeadlessException {
        GraphicsEnvironment.checkHeadless();
        return Event.CTRL_MASK;
    }
    public boolean getLockingKeyState(int keyCode)
        throws UnsupportedOperationException
    {
        GraphicsEnvironment.checkHeadless();
        if (! (keyCode == KeyEvent.VK_CAPS_LOCK || keyCode == KeyEvent.VK_NUM_LOCK ||
               keyCode == KeyEvent.VK_SCROLL_LOCK || keyCode == KeyEvent.VK_KANA_LOCK)) {
            throw new IllegalArgumentException("invalid key for Toolkit.getLockingKeyState");
        }
        throw new UnsupportedOperationException("Toolkit.getLockingKeyState");
    }
    public void setLockingKeyState(int keyCode, boolean on)
        throws UnsupportedOperationException
    {
        GraphicsEnvironment.checkHeadless();
        if (! (keyCode == KeyEvent.VK_CAPS_LOCK || keyCode == KeyEvent.VK_NUM_LOCK ||
               keyCode == KeyEvent.VK_SCROLL_LOCK || keyCode == KeyEvent.VK_KANA_LOCK)) {
            throw new IllegalArgumentException("invalid key for Toolkit.setLockingKeyState");
        }
        throw new UnsupportedOperationException("Toolkit.setLockingKeyState");
    }
    protected static Container getNativeContainer(Component c) {
        return c.getNativeContainer();
    }
    public Cursor createCustomCursor(Image cursor, Point hotSpot, String name)
        throws IndexOutOfBoundsException, HeadlessException
    {
        // Override to implement custom cursor support.
        if (this != Toolkit.getDefaultToolkit()) {
            return Toolkit.getDefaultToolkit().
                createCustomCursor(cursor, hotSpot, name);
        } else {
            return new Cursor(Cursor.DEFAULT_CURSOR);
        }
    }
    public Dimension getBestCursorSize(int preferredWidth,
        int preferredHeight) throws HeadlessException {
        GraphicsEnvironment.checkHeadless();
        // Override to implement custom cursor support.
        if (this != Toolkit.getDefaultToolkit()) {
            return Toolkit.getDefaultToolkit().
                getBestCursorSize(preferredWidth, preferredHeight);
        } else {
            return new Dimension(0, 0);
        }
    }
    public int getMaximumCursorColors() throws HeadlessException {
        GraphicsEnvironment.checkHeadless();
        // Override to implement custom cursor support.
        if (this != Toolkit.getDefaultToolkit()) {
            return Toolkit.getDefaultToolkit().getMaximumCursorColors();
        } else {
            return 0;
        }
    }
    public boolean isFrameStateSupported(int state)
        throws HeadlessException
    {
        GraphicsEnvironment.checkHeadless();
        if (this != Toolkit.getDefaultToolkit()) {
            return Toolkit.getDefaultToolkit().
                isFrameStateSupported(state);
        } else {
            return (state == Frame.NORMAL); // others are not guaranteed
        }
    }
    private static ResourceBundle resources;
    private static ResourceBundle platformResources;
    // called by platform toolkit
    private static void setPlatformResources(ResourceBundle bundle) {
        platformResources = bundle;
    }
    private static native void initIDs();
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
    static {
        AWTAccessor.setToolkitAccessor(
                new AWTAccessor.ToolkitAccessor() {
                    @Override
                    public void setPlatformResources(ResourceBundle bundle) {
                        Toolkit.setPlatformResources(bundle);
                    }
                });
        java.security.AccessController.doPrivileged(
                                 new java.security.PrivilegedAction<Void>() {
            public Void run() {
                try {
                    resources =
                        ResourceBundle.getBundle("sun.awt.resources.awt",
                                                 CoreResourceBundleControl.getRBControlInstance());
                } catch (MissingResourceException e) {
                    // No resource file; defaults will be used.
                }
                return null;
            }
        });
        // ensure that the proper libraries are loaded
        loadLibraries();
        initAssistiveTechnologies();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }
    }
    public static String getProperty(String key, String defaultValue) {
        // first try platform specific bundle
        if (platformResources != null) {
            try {
                return platformResources.getString(key);
            }
            catch (MissingResourceException e) {}
        }
        // then shared one
        if (resources != null) {
            try {
                return resources.getString(key);
            }
            catch (MissingResourceException e) {}
        }
        return defaultValue;
    }
    public final EventQueue getSystemEventQueue() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(SecurityConstants.AWT.CHECK_AWT_EVENTQUEUE_PERMISSION);
        }
        return getSystemEventQueueImpl();
    }
    protected abstract EventQueue getSystemEventQueueImpl();
    static EventQueue getEventQueue() {
        return getDefaultToolkit().getSystemEventQueueImpl();
    }
    public abstract DragSourceContextPeer createDragSourceContextPeer(DragGestureEvent dge) throws InvalidDnDOperationException;
    public <T extends DragGestureRecognizer> T
        createDragGestureRecognizer(Class<T> abstractRecognizerClass,
                                    DragSource ds, Component c, int srcActions,
                                    DragGestureListener dgl)
    {
        return null;
    }
    public final synchronized Object getDesktopProperty(String propertyName) {
        // This is a workaround for headless toolkits.  It would be
        // better to override this method but it is declared final.
        // "this instanceof" syntax defeats polymorphism.
        // --mm, 03/03/00
        if (this instanceof HeadlessToolkit) {
            return ((HeadlessToolkit)this).getUnderlyingToolkit()
                .getDesktopProperty(propertyName);
        }
        if (desktopProperties.isEmpty()) {
            initializeDesktopProperties();
        }
        Object value;
        // This property should never be cached
        if (propertyName.equals("awt.dynamicLayoutSupported")) {
            return getDefaultToolkit().lazilyLoadDesktopProperty(propertyName);
        }
        value = desktopProperties.get(propertyName);
        if (value == null) {
            value = lazilyLoadDesktopProperty(propertyName);
            if (value != null) {
                setDesktopProperty(propertyName, value);
            }
        }
        if (value instanceof RenderingHints) {
            value = ((RenderingHints)value).clone();
        }
        return value;
    }
    protected final void setDesktopProperty(String name, Object newValue) {
        // This is a workaround for headless toolkits.  It would be
        // better to override this method but it is declared final.
        // "this instanceof" syntax defeats polymorphism.
        // --mm, 03/03/00
        if (this instanceof HeadlessToolkit) {
            ((HeadlessToolkit)this).getUnderlyingToolkit()
                .setDesktopProperty(name, newValue);
            return;
        }
        Object oldValue;
        synchronized (this) {
            oldValue = desktopProperties.get(name);
            desktopProperties.put(name, newValue);
        }
        // Don't fire change event if old and new values are null.
        // It helps to avoid recursive resending of WM_THEMECHANGED
        if (oldValue != null || newValue != null) {
            desktopPropsSupport.firePropertyChange(name, oldValue, newValue);
        }
    }
    protected Object lazilyLoadDesktopProperty(String name) {
        return null;
    }
    protected void initializeDesktopProperties() {
    }
    public void addPropertyChangeListener(String name, PropertyChangeListener pcl) {
        desktopPropsSupport.addPropertyChangeListener(name, pcl);
    }
    public void removePropertyChangeListener(String name, PropertyChangeListener pcl) {
        desktopPropsSupport.removePropertyChangeListener(name, pcl);
    }
    public PropertyChangeListener[] getPropertyChangeListeners() {
        return desktopPropsSupport.getPropertyChangeListeners();
    }
    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return desktopPropsSupport.getPropertyChangeListeners(propertyName);
    }
    protected final Map<String,Object> desktopProperties =
            new HashMap<String,Object>();
    protected final PropertyChangeSupport desktopPropsSupport =
            Toolkit.createPropertyChangeSupport(this);
    public boolean isAlwaysOnTopSupported() {
        return true;
    }
    public abstract boolean isModalityTypeSupported(Dialog.ModalityType modalityType);
    public abstract boolean isModalExclusionTypeSupported(Dialog.ModalExclusionType modalExclusionType);
    // 8014718: logging has been removed from SunToolkit
    private static final int LONG_BITS = 64;
    private int[] calls = new int[LONG_BITS];
    private static volatile long enabledOnToolkitMask;
    private AWTEventListener eventListener = null;
    private WeakHashMap<AWTEventListener, SelectiveAWTEventListener> listener2SelectiveListener = new WeakHashMap<>();
    static private AWTEventListener deProxyAWTEventListener(AWTEventListener l)
    {
        AWTEventListener localL = l;
        if (localL == null) {
            return null;
        }
        // if user passed in a AWTEventListenerProxy object, extract
        // the listener
        if (l instanceof AWTEventListenerProxy) {
            localL = ((AWTEventListenerProxy)l).getListener();
        }
        return localL;
    }
    public void addAWTEventListener(AWTEventListener listener, long eventMask) {
        AWTEventListener localL = deProxyAWTEventListener(listener);
        if (localL == null) {
            return;
        }
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
          security.checkPermission(SecurityConstants.AWT.ALL_AWT_EVENTS_PERMISSION);
        }
        synchronized (this) {
            SelectiveAWTEventListener selectiveListener =
                listener2SelectiveListener.get(localL);
            if (selectiveListener == null) {
                // Create a new selectiveListener.
                selectiveListener = new SelectiveAWTEventListener(localL,
                                                                 eventMask);
                listener2SelectiveListener.put(localL, selectiveListener);
                eventListener = ToolkitEventMulticaster.add(eventListener,
                                                            selectiveListener);
            }
            // OR the eventMask into the selectiveListener's event mask.
            selectiveListener.orEventMasks(eventMask);
            enabledOnToolkitMask |= eventMask;
            long mask = eventMask;
            for (int i=0; i<LONG_BITS; i++) {
                // If no bits are set, break out of loop.
                if (mask == 0) {
                    break;
                }
                if ((mask & 1L) != 0) {  // Always test bit 0.
                    calls[i]++;
                }
                mask >>>= 1;  // Right shift, fill with zeros on left.
            }
        }
    }
    public void removeAWTEventListener(AWTEventListener listener) {
        AWTEventListener localL = deProxyAWTEventListener(listener);
        if (listener == null) {
            return;
        }
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(SecurityConstants.AWT.ALL_AWT_EVENTS_PERMISSION);
        }
        synchronized (this) {
            SelectiveAWTEventListener selectiveListener =
                listener2SelectiveListener.get(localL);
            if (selectiveListener != null) {
                listener2SelectiveListener.remove(localL);
                int[] listenerCalls = selectiveListener.getCalls();
                for (int i=0; i<LONG_BITS; i++) {
                    calls[i] -= listenerCalls[i];
                    assert calls[i] >= 0: "Negative Listeners count";
                    if (calls[i] == 0) {
                        enabledOnToolkitMask &= ~(1L<<i);
                    }
                }
            }
            eventListener = ToolkitEventMulticaster.remove(eventListener,
            (selectiveListener == null) ? localL : selectiveListener);
        }
    }
    static boolean enabledOnToolkit(long eventMask) {
        return (enabledOnToolkitMask & eventMask) != 0;
        }
    synchronized int countAWTEventListeners(long eventMask) {
        int ci = 0;
        for (; eventMask != 0; eventMask >>>= 1, ci++) {
        }
        ci--;
        return calls[ci];
    }
    public AWTEventListener[] getAWTEventListeners() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(SecurityConstants.AWT.ALL_AWT_EVENTS_PERMISSION);
        }
        synchronized (this) {
            EventListener[] la = ToolkitEventMulticaster.getListeners(eventListener,AWTEventListener.class);
            AWTEventListener[] ret = new AWTEventListener[la.length];
            for (int i = 0; i < la.length; i++) {
                SelectiveAWTEventListener sael = (SelectiveAWTEventListener)la[i];
                AWTEventListener tempL = sael.getListener();
                //assert tempL is not an AWTEventListenerProxy - we should
                // have weeded them all out
                // don't want to wrap a proxy inside a proxy
                ret[i] = new AWTEventListenerProxy(sael.getEventMask(), tempL);
            }
            return ret;
        }
    }
    public AWTEventListener[] getAWTEventListeners(long eventMask) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(SecurityConstants.AWT.ALL_AWT_EVENTS_PERMISSION);
        }
        synchronized (this) {
            EventListener[] la = ToolkitEventMulticaster.getListeners(eventListener,AWTEventListener.class);
            java.util.List<AWTEventListenerProxy> list = new ArrayList<>(la.length);
            for (int i = 0; i < la.length; i++) {
                SelectiveAWTEventListener sael = (SelectiveAWTEventListener)la[i];
                if ((sael.getEventMask() & eventMask) == eventMask) {
                    //AWTEventListener tempL = sael.getListener();
                    list.add(new AWTEventListenerProxy(sael.getEventMask(),
                                                       sael.getListener()));
                }
            }
            return list.toArray(new AWTEventListener[0]);
        }
    }
    void notifyAWTEventListeners(AWTEvent theEvent) {
        // This is a workaround for headless toolkits.  It would be
        // better to override this method but it is declared package private.
        // "this instanceof" syntax defeats polymorphism.
        // --mm, 03/03/00
        if (this instanceof HeadlessToolkit) {
            ((HeadlessToolkit)this).getUnderlyingToolkit()
                .notifyAWTEventListeners(theEvent);
            return;
        }
        AWTEventListener eventListener = this.eventListener;
        if (eventListener != null) {
            eventListener.eventDispatched(theEvent);
        }
    }
    static private class ToolkitEventMulticaster extends AWTEventMulticaster
        implements AWTEventListener {
        // Implementation cloned from AWTEventMulticaster.
        ToolkitEventMulticaster(AWTEventListener a, AWTEventListener b) {
            super(a, b);
        }
        static AWTEventListener add(AWTEventListener a,
                                    AWTEventListener b) {
            if (a == null)  return b;
            if (b == null)  return a;
            return new ToolkitEventMulticaster(a, b);
        }
        static AWTEventListener remove(AWTEventListener l,
                                       AWTEventListener oldl) {
            return (AWTEventListener) removeInternal(l, oldl);
        }
        // #4178589: must overload remove(EventListener) to call our add()
        // instead of the static addInternal() so we allocate a
        // ToolkitEventMulticaster instead of an AWTEventMulticaster.
        // Note: this method is called by AWTEventListener.removeInternal(),
        // so its method signature must match AWTEventListener.remove().
        protected EventListener remove(EventListener oldl) {
            if (oldl == a)  return b;
            if (oldl == b)  return a;
            AWTEventListener a2 = (AWTEventListener)removeInternal(a, oldl);
            AWTEventListener b2 = (AWTEventListener)removeInternal(b, oldl);
            if (a2 == a && b2 == b) {
                return this;    // it's not here
            }
            return add(a2, b2);
        }
        public void eventDispatched(AWTEvent event) {
            ((AWTEventListener)a).eventDispatched(event);
            ((AWTEventListener)b).eventDispatched(event);
        }
    }
    private class SelectiveAWTEventListener implements AWTEventListener {
        AWTEventListener listener;
        private long eventMask;
        // This array contains the number of times to call the eventlistener
        // for each event type.
        int[] calls = new int[Toolkit.LONG_BITS];
        public AWTEventListener getListener() {return listener;}
        public long getEventMask() {return eventMask;}
        public int[] getCalls() {return calls;}
        public void orEventMasks(long mask) {
            eventMask |= mask;
            // For each event bit set in mask, increment its call count.
            for (int i=0; i<Toolkit.LONG_BITS; i++) {
                // If no bits are set, break out of loop.
                if (mask == 0) {
                    break;
                }
                if ((mask & 1L) != 0) {  // Always test bit 0.
                    calls[i]++;
                }
                mask >>>= 1;  // Right shift, fill with zeros on left.
            }
        }
        SelectiveAWTEventListener(AWTEventListener l, long mask) {
            listener = l;
            eventMask = mask;
        }
        public void eventDispatched(AWTEvent event) {
            long eventBit = 0; // Used to save the bit of the event type.
            if (((eventBit = eventMask & AWTEvent.COMPONENT_EVENT_MASK) != 0 &&
                 event.id >= ComponentEvent.COMPONENT_FIRST &&
                 event.id <= ComponentEvent.COMPONENT_LAST)
             || ((eventBit = eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0 &&
                 event.id >= ContainerEvent.CONTAINER_FIRST &&
                 event.id <= ContainerEvent.CONTAINER_LAST)
             || ((eventBit = eventMask & AWTEvent.FOCUS_EVENT_MASK) != 0 &&
                 event.id >= FocusEvent.FOCUS_FIRST &&
                 event.id <= FocusEvent.FOCUS_LAST)
             || ((eventBit = eventMask & AWTEvent.KEY_EVENT_MASK) != 0 &&
                 event.id >= KeyEvent.KEY_FIRST &&
                 event.id <= KeyEvent.KEY_LAST)
             || ((eventBit = eventMask & AWTEvent.MOUSE_WHEEL_EVENT_MASK) != 0 &&
                 event.id == MouseEvent.MOUSE_WHEEL)
             || ((eventBit = eventMask & AWTEvent.MOUSE_MOTION_EVENT_MASK) != 0 &&
                 (event.id == MouseEvent.MOUSE_MOVED ||
                  event.id == MouseEvent.MOUSE_DRAGGED))
             || ((eventBit = eventMask & AWTEvent.MOUSE_EVENT_MASK) != 0 &&
                 event.id != MouseEvent.MOUSE_MOVED &&
                 event.id != MouseEvent.MOUSE_DRAGGED &&
                 event.id != MouseEvent.MOUSE_WHEEL &&
                 event.id >= MouseEvent.MOUSE_FIRST &&
                 event.id <= MouseEvent.MOUSE_LAST)
             || ((eventBit = eventMask & AWTEvent.WINDOW_EVENT_MASK) != 0 &&
                 (event.id >= WindowEvent.WINDOW_FIRST &&
                 event.id <= WindowEvent.WINDOW_LAST))
             || ((eventBit = eventMask & AWTEvent.ACTION_EVENT_MASK) != 0 &&
                 event.id >= ActionEvent.ACTION_FIRST &&
                 event.id <= ActionEvent.ACTION_LAST)
             || ((eventBit = eventMask & AWTEvent.ADJUSTMENT_EVENT_MASK) != 0 &&
                 event.id >= AdjustmentEvent.ADJUSTMENT_FIRST &&
                 event.id <= AdjustmentEvent.ADJUSTMENT_LAST)
             || ((eventBit = eventMask & AWTEvent.ITEM_EVENT_MASK) != 0 &&
                 event.id >= ItemEvent.ITEM_FIRST &&
                 event.id <= ItemEvent.ITEM_LAST)
             || ((eventBit = eventMask & AWTEvent.TEXT_EVENT_MASK) != 0 &&
                 event.id >= TextEvent.TEXT_FIRST &&
                 event.id <= TextEvent.TEXT_LAST)
             || ((eventBit = eventMask & AWTEvent.INPUT_METHOD_EVENT_MASK) != 0 &&
                 event.id >= InputMethodEvent.INPUT_METHOD_FIRST &&
                 event.id <= InputMethodEvent.INPUT_METHOD_LAST)
             || ((eventBit = eventMask & AWTEvent.PAINT_EVENT_MASK) != 0 &&
                 event.id >= PaintEvent.PAINT_FIRST &&
                 event.id <= PaintEvent.PAINT_LAST)
             || ((eventBit = eventMask & AWTEvent.INVOCATION_EVENT_MASK) != 0 &&
                 event.id >= InvocationEvent.INVOCATION_FIRST &&
                 event.id <= InvocationEvent.INVOCATION_LAST)
             || ((eventBit = eventMask & AWTEvent.HIERARCHY_EVENT_MASK) != 0 &&
                 event.id == HierarchyEvent.HIERARCHY_CHANGED)
             || ((eventBit = eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) != 0 &&
                 (event.id == HierarchyEvent.ANCESTOR_MOVED ||
                  event.id == HierarchyEvent.ANCESTOR_RESIZED))
             || ((eventBit = eventMask & AWTEvent.WINDOW_STATE_EVENT_MASK) != 0 &&
                 event.id == WindowEvent.WINDOW_STATE_CHANGED)
             || ((eventBit = eventMask & AWTEvent.WINDOW_FOCUS_EVENT_MASK) != 0 &&
                 (event.id == WindowEvent.WINDOW_GAINED_FOCUS ||
                  event.id == WindowEvent.WINDOW_LOST_FOCUS))
                || ((eventBit = eventMask & sun.awt.SunToolkit.GRAB_EVENT_MASK) != 0 &&
                    (event instanceof sun.awt.UngrabEvent))) {
                // Get the index of the call count for this event type.
                // Instead of using Math.log(...) we will calculate it with
                // bit shifts. That's what previous implementation looked like:
                //
                // int ci = (int) (Math.log(eventBit)/Math.log(2));
                int ci = 0;
                for (long eMask = eventBit; eMask != 0; eMask >>>= 1, ci++) {
                }
                ci--;
                // Call the listener as many times as it was added for this
                // event type.
                for (int i=0; i<calls[ci]; i++) {
                    listener.eventDispatched(event);
                }
            }
        }
    }
    public abstract Map<java.awt.font.TextAttribute,?>
        mapInputMethodHighlight(InputMethodHighlight highlight)
        throws HeadlessException;
    private static PropertyChangeSupport createPropertyChangeSupport(Toolkit toolkit) {
        if (toolkit instanceof SunToolkit || toolkit instanceof HeadlessToolkit) {
            return new DesktopPropertyChangeSupport(toolkit);
        } else {
            return new PropertyChangeSupport(toolkit);
        }
    }
    @SuppressWarnings("serial")
    private static class DesktopPropertyChangeSupport extends PropertyChangeSupport {
        private static final StringBuilder PROP_CHANGE_SUPPORT_KEY =
                new StringBuilder("desktop property change support key");
        private final Object source;
        public DesktopPropertyChangeSupport(Object sourceBean) {
            super(sourceBean);
            source = sourceBean;
        }
        @Override
        public synchronized void addPropertyChangeListener(
                String propertyName,
                PropertyChangeListener listener)
        {
            PropertyChangeSupport pcs = (PropertyChangeSupport)
                    AppContext.getAppContext().get(PROP_CHANGE_SUPPORT_KEY);
            if (null == pcs) {
                pcs = new PropertyChangeSupport(source);
                AppContext.getAppContext().put(PROP_CHANGE_SUPPORT_KEY, pcs);
            }
            pcs.addPropertyChangeListener(propertyName, listener);
        }
        @Override
        public synchronized void removePropertyChangeListener(
                String propertyName,
                PropertyChangeListener listener)
        {
            PropertyChangeSupport pcs = (PropertyChangeSupport)
                    AppContext.getAppContext().get(PROP_CHANGE_SUPPORT_KEY);
            if (null != pcs) {
                pcs.removePropertyChangeListener(propertyName, listener);
            }
        }
        @Override
        public synchronized PropertyChangeListener[] getPropertyChangeListeners()
        {
            PropertyChangeSupport pcs = (PropertyChangeSupport)
                    AppContext.getAppContext().get(PROP_CHANGE_SUPPORT_KEY);
            if (null != pcs) {
                return pcs.getPropertyChangeListeners();
            } else {
                return new PropertyChangeListener[0];
            }
        }
        @Override
        public synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName)
        {
            PropertyChangeSupport pcs = (PropertyChangeSupport)
                    AppContext.getAppContext().get(PROP_CHANGE_SUPPORT_KEY);
            if (null != pcs) {
                return pcs.getPropertyChangeListeners(propertyName);
            } else {
                return new PropertyChangeListener[0];
            }
        }
        @Override
        public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
            PropertyChangeSupport pcs = (PropertyChangeSupport)
                    AppContext.getAppContext().get(PROP_CHANGE_SUPPORT_KEY);
            if (null == pcs) {
                pcs = new PropertyChangeSupport(source);
                AppContext.getAppContext().put(PROP_CHANGE_SUPPORT_KEY, pcs);
            }
            pcs.addPropertyChangeListener(listener);
        }
        @Override
        public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
            PropertyChangeSupport pcs = (PropertyChangeSupport)
                    AppContext.getAppContext().get(PROP_CHANGE_SUPPORT_KEY);
            if (null != pcs) {
                pcs.removePropertyChangeListener(listener);
            }
        }
        @Override
        public void firePropertyChange(final PropertyChangeEvent evt) {
            Object oldValue = evt.getOldValue();
            Object newValue = evt.getNewValue();
            String propertyName = evt.getPropertyName();
            if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
                return;
            }
            Runnable updater = new Runnable() {
                public void run() {
                    PropertyChangeSupport pcs = (PropertyChangeSupport)
                            AppContext.getAppContext().get(PROP_CHANGE_SUPPORT_KEY);
                    if (null != pcs) {
                        pcs.firePropertyChange(evt);
                    }
                }
            };
            final AppContext currentAppContext = AppContext.getAppContext();
            for (AppContext appContext : AppContext.getAppContexts()) {
                if (null == appContext || appContext.isDisposed()) {
                    continue;
                }
                if (currentAppContext == appContext) {
                    updater.run();
                } else {
                    final PeerEvent e = new PeerEvent(source, updater, PeerEvent.ULTIMATE_PRIORITY_EVENT);
                    SunToolkit.postEvent(appContext, e);
                }
            }
        }
    }
    public boolean areExtraMouseButtonsEnabled() throws HeadlessException {
        GraphicsEnvironment.checkHeadless();
        return Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled();
    }
}
