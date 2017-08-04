package java.awt;
import java.awt.peer.MenuItemPeer;
import java.awt.event.*;
import java.util.EventListener;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import javax.accessibility.*;
import sun.awt.AWTAccessor;
public class MenuItem extends MenuComponent implements Accessible {
    static {
        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }
        AWTAccessor.setMenuItemAccessor(
            new AWTAccessor.MenuItemAccessor() {
                public boolean isEnabled(MenuItem item) {
                    return item.enabled;
                }
                public String getLabel(MenuItem item) {
                    return item.label;
                }
                public MenuShortcut getShortcut(MenuItem item) {
                    return item.shortcut;
                }
                public String getActionCommandImpl(MenuItem item) {
                    return item.getActionCommandImpl();
                }
                public boolean isItemEnabled(MenuItem item) {
                    return item.isItemEnabled();
                }
            });
    }
    boolean enabled = true;
    String label;
    String actionCommand;
    long eventMask;
    transient ActionListener actionListener;
    private MenuShortcut shortcut = null;
    private static final String base = "menuitem";
    private static int nameCounter = 0;
    private static final long serialVersionUID = -21757335363267194L;
    public MenuItem() throws HeadlessException {
        this("", null);
    }
    public MenuItem(String label) throws HeadlessException {
        this(label, null);
    }
    public MenuItem(String label, MenuShortcut s) throws HeadlessException {
        this.label = label;
        this.shortcut = s;
    }
    String constructComponentName() {
        synchronized (MenuItem.class) {
            return base + nameCounter++;
        }
    }
    public void addNotify() {
        synchronized (getTreeLock()) {
            if (peer == null)
                peer = Toolkit.getDefaultToolkit().createMenuItem(this);
        }
    }
    public String getLabel() {
        return label;
    }
    public synchronized void setLabel(String label) {
        this.label = label;
        MenuItemPeer peer = (MenuItemPeer)this.peer;
        if (peer != null) {
            peer.setLabel(label);
        }
    }
    public boolean isEnabled() {
        return enabled;
    }
    public synchronized void setEnabled(boolean b) {
        enable(b);
    }
    @Deprecated
    public synchronized void enable() {
        enabled = true;
        MenuItemPeer peer = (MenuItemPeer)this.peer;
        if (peer != null) {
            peer.setEnabled(true);
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
    public synchronized void disable() {
        enabled = false;
        MenuItemPeer peer = (MenuItemPeer)this.peer;
        if (peer != null) {
            peer.setEnabled(false);
        }
    }
    public MenuShortcut getShortcut() {
        return shortcut;
    }
    public void setShortcut(MenuShortcut s) {
        shortcut = s;
        MenuItemPeer peer = (MenuItemPeer)this.peer;
        if (peer != null) {
            peer.setLabel(label);
        }
    }
    public void deleteShortcut() {
        shortcut = null;
        MenuItemPeer peer = (MenuItemPeer)this.peer;
        if (peer != null) {
            peer.setLabel(label);
        }
    }
    void deleteShortcut(MenuShortcut s) {
        if (s.equals(shortcut)) {
            shortcut = null;
            MenuItemPeer peer = (MenuItemPeer)this.peer;
            if (peer != null) {
                peer.setLabel(label);
            }
        }
    }
    void doMenuEvent(long when, int modifiers) {
        Toolkit.getEventQueue().postEvent(
            new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                            getActionCommand(), when, modifiers));
    }
    private final boolean isItemEnabled() {
        // Fix For 6185151: Menu shortcuts of all menuitems within a menu
        // should be disabled when the menu itself is disabled
        if (!isEnabled()) {
            return false;
        }
        MenuContainer container = getParent_NoClientCode();
        do {
            if (!(container instanceof Menu)) {
                return true;
            }
            Menu menu = (Menu)container;
            if (!menu.isEnabled()) {
                return false;
            }
            container = menu.getParent_NoClientCode();
        } while (container != null);
        return true;
    }
    boolean handleShortcut(KeyEvent e) {
        MenuShortcut s = new MenuShortcut(e.getKeyCode(),
                             (e.getModifiers() & InputEvent.SHIFT_MASK) > 0);
        MenuShortcut sE = new MenuShortcut(e.getExtendedKeyCode(),
                             (e.getModifiers() & InputEvent.SHIFT_MASK) > 0);
        // Fix For 6185151: Menu shortcuts of all menuitems within a menu
        // should be disabled when the menu itself is disabled
        if ((s.equals(shortcut) || sE.equals(shortcut)) && isItemEnabled()) {
            // MenuShortcut match -- issue an event on keydown.
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                doMenuEvent(e.getWhen(), e.getModifiers());
            } else {
                // silently eat key release.
            }
            return true;
        }
        return false;
    }
    MenuItem getShortcutMenuItem(MenuShortcut s) {
        return (s.equals(shortcut)) ? this : null;
    }
    protected final void enableEvents(long eventsToEnable) {
        eventMask |= eventsToEnable;
        newEventsOnly = true;
    }
    protected final void disableEvents(long eventsToDisable) {
        eventMask &= ~eventsToDisable;
    }
    public void setActionCommand(String command) {
        actionCommand = command;
    }
    public String getActionCommand() {
        return getActionCommandImpl();
    }
    // This is final so it can be called on the Toolkit thread.
    final String getActionCommandImpl() {
        return (actionCommand == null? label : actionCommand);
    }
    public synchronized void addActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        actionListener = AWTEventMulticaster.add(actionListener, l);
        newEventsOnly = true;
    }
    public synchronized void removeActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        actionListener = AWTEventMulticaster.remove(actionListener, l);
    }
    public synchronized ActionListener[] getActionListeners() {
        return getListeners(ActionListener.class);
    }
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener l = null;
        if  (listenerType == ActionListener.class) {
            l = actionListener;
        }
        return AWTEventMulticaster.getListeners(l, listenerType);
    }
    protected void processEvent(AWTEvent e) {
        if (e instanceof ActionEvent) {
            processActionEvent((ActionEvent)e);
        }
    }
    // REMIND: remove when filtering is done at lower level
    boolean eventEnabled(AWTEvent e) {
        if (e.id == ActionEvent.ACTION_PERFORMED) {
            if ((eventMask & AWTEvent.ACTION_EVENT_MASK) != 0 ||
                actionListener != null) {
                return true;
            }
            return false;
        }
        return super.eventEnabled(e);
    }
    protected void processActionEvent(ActionEvent e) {
        ActionListener listener = actionListener;
        if (listener != null) {
            listener.actionPerformed(e);
        }
    }
    public String paramString() {
        String str = ",label=" + label;
        if (shortcut != null) {
            str += ",shortcut=" + shortcut;
        }
        return super.paramString() + str;
    }
    private int menuItemSerializedDataVersion = 1;
    private void writeObject(ObjectOutputStream s)
      throws IOException
    {
      s.defaultWriteObject();
      AWTEventMulticaster.save(s, actionListenerK, actionListener);
      s.writeObject(null);
    }
    private void readObject(ObjectInputStream s)
      throws ClassNotFoundException, IOException, HeadlessException
    {
      // HeadlessException will be thrown from MenuComponent's readObject
      s.defaultReadObject();
      Object keyOrNull;
      while(null != (keyOrNull = s.readObject())) {
        String key = ((String)keyOrNull).intern();
        if (actionListenerK == key)
          addActionListener((ActionListener)(s.readObject()));
        else // skip value for unrecognized key
          s.readObject();
      }
    }
    private static native void initIDs();
/////////////////
// Accessibility support
////////////////
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleAWTMenuItem();
        }
        return accessibleContext;
    }
    protected class AccessibleAWTMenuItem extends AccessibleAWTMenuComponent
        implements AccessibleAction, AccessibleValue
    {
        private static final long serialVersionUID = -217847831945965825L;
        public String getAccessibleName() {
            if (accessibleName != null) {
                return accessibleName;
            } else {
                if (getLabel() == null) {
                    return super.getAccessibleName();
                } else {
                    return getLabel();
                }
            }
        }
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU_ITEM;
        }
        public AccessibleAction getAccessibleAction() {
            return this;
        }
        public AccessibleValue getAccessibleValue() {
            return this;
        }
        public int getAccessibleActionCount() {
            return 1;
        }
        public String getAccessibleActionDescription(int i) {
            if (i == 0) {
                // [[[PENDING:  WDW -- need to provide a localized string]]]
                return "click";
            } else {
                return null;
            }
        }
        public boolean doAccessibleAction(int i) {
            if (i == 0) {
                // Simulate a button click
                Toolkit.getEventQueue().postEvent(
                        new ActionEvent(MenuItem.this,
                                        ActionEvent.ACTION_PERFORMED,
                                        MenuItem.this.getActionCommand(),
                                        EventQueue.getMostRecentEventTime(),
                                        0));
                return true;
            } else {
                return false;
            }
        }
        public Number getCurrentAccessibleValue() {
            return Integer.valueOf(0);
        }
        public boolean setCurrentAccessibleValue(Number n) {
            return false;
        }
        public Number getMinimumAccessibleValue() {
            return Integer.valueOf(0);
        }
        public Number getMaximumAccessibleValue() {
            return Integer.valueOf(0);
        }
    } // class AccessibleAWTMenuItem
}
