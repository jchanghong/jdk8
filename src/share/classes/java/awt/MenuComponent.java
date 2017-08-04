package java.awt;
import java.awt.peer.MenuComponentPeer;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import sun.awt.AppContext;
import sun.awt.AWTAccessor;
import javax.accessibility.*;
import java.security.AccessControlContext;
import java.security.AccessController;
public abstract class MenuComponent implements java.io.Serializable {
    static {
        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }
    }
    transient MenuComponentPeer peer;
    transient MenuContainer parent;
    transient AppContext appContext;
    volatile Font font;
    private String name;
    private boolean nameExplicitlySet = false;
    boolean newEventsOnly = false;
    private transient volatile AccessControlContext acc =
            AccessController.getContext();
    final AccessControlContext getAccessControlContext() {
        if (acc == null) {
            throw new SecurityException(
                    "MenuComponent is missing AccessControlContext");
        }
        return acc;
    }
    final static String actionListenerK = Component.actionListenerK;
    final static String itemListenerK = Component.itemListenerK;
    private static final long serialVersionUID = -4536902356223894379L;
    static {
        AWTAccessor.setMenuComponentAccessor(
            new AWTAccessor.MenuComponentAccessor() {
                public AppContext getAppContext(MenuComponent menuComp) {
                    return menuComp.appContext;
                }
                public void setAppContext(MenuComponent menuComp,
                                          AppContext appContext) {
                    menuComp.appContext = appContext;
                }
                public MenuContainer getParent(MenuComponent menuComp) {
                    return menuComp.parent;
                }
                public Font getFont_NoClientCode(MenuComponent menuComp) {
                    return menuComp.getFont_NoClientCode();
                }
                @SuppressWarnings("unchecked")
                public <T extends MenuComponentPeer> T getPeer(MenuComponent menuComp) {
                    return (T) menuComp.peer;
                }
            });
    }
    public MenuComponent() throws HeadlessException {
        GraphicsEnvironment.checkHeadless();
        appContext = AppContext.getAppContext();
    }
    String constructComponentName() {
        return null; // For strict compliance with prior platform versions, a MenuComponent
                     // that doesn't set its name should return null from
                     // getName()
    }
    public String getName() {
        if (name == null && !nameExplicitlySet) {
            synchronized(this) {
                if (name == null && !nameExplicitlySet)
                    name = constructComponentName();
            }
        }
        return name;
    }
    public void setName(String name) {
        synchronized(this) {
            this.name = name;
            nameExplicitlySet = true;
        }
    }
    public MenuContainer getParent() {
        return getParent_NoClientCode();
    }
    // NOTE: This method may be called by privileged threads.
    //       This functionality is implemented in a package-private method
    //       to insure that it cannot be overridden by client subclasses.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    final MenuContainer getParent_NoClientCode() {
        return parent;
    }
    @Deprecated
    public MenuComponentPeer getPeer() {
        return peer;
    }
    public Font getFont() {
        Font font = this.font;
        if (font != null) {
            return font;
        }
        MenuContainer parent = this.parent;
        if (parent != null) {
            return parent.getFont();
        }
        return null;
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
        // The MenuContainer interface does not have getFont_NoClientCode()
        // and it cannot, because it must be package-private. Because of
        // this, we must manually cast classes that implement
        // MenuContainer.
        Object parent = this.parent;
        if (parent != null) {
            if (parent instanceof Component) {
                font = ((Component)parent).getFont_NoClientCode();
            } else if (parent instanceof MenuComponent) {
                font = ((MenuComponent)parent).getFont_NoClientCode();
            }
        }
        return font;
    } // getFont_NoClientCode()
    public void setFont(Font f) {
        synchronized (getTreeLock()) {
            font = f;
            //Fixed 6312943: NullPointerException in method MenuComponent.setFont(Font)
            MenuComponentPeer peer = this.peer;
            if (peer != null) {
                peer.setFont(f);
            }
        }
    }
    public void removeNotify() {
        synchronized (getTreeLock()) {
            MenuComponentPeer p = this.peer;
            if (p != null) {
                Toolkit.getEventQueue().removeSourceEvents(this, true);
                this.peer = null;
                p.dispose();
            }
        }
    }
    @Deprecated
    public boolean postEvent(Event evt) {
        MenuContainer parent = this.parent;
        if (parent != null) {
            parent.postEvent(evt);
        }
        return false;
    }
    public final void dispatchEvent(AWTEvent e) {
        dispatchEventImpl(e);
    }
    void dispatchEventImpl(AWTEvent e) {
        EventQueue.setCurrentEventAndMostRecentTime(e);
        Toolkit.getDefaultToolkit().notifyAWTEventListeners(e);
        if (newEventsOnly ||
            (parent != null && parent instanceof MenuComponent &&
             ((MenuComponent)parent).newEventsOnly)) {
            if (eventEnabled(e)) {
                processEvent(e);
            } else if (e instanceof ActionEvent && parent != null) {
                e.setSource(parent);
                ((MenuComponent)parent).dispatchEvent(e);
            }
        } else { // backward compatibility
            Event olde = e.convertToOld();
            if (olde != null) {
                postEvent(olde);
            }
        }
    }
    // REMIND: remove when filtering is done at lower level
    boolean eventEnabled(AWTEvent e) {
        return false;
    }
    protected void processEvent(AWTEvent e) {
    }
    protected String paramString() {
        String thisName = getName();
        return (thisName != null? thisName : "");
    }
    public String toString() {
        return getClass().getName() + "[" + paramString() + "]";
    }
    protected final Object getTreeLock() {
        return Component.LOCK;
    }
    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException, HeadlessException
    {
        GraphicsEnvironment.checkHeadless();
        acc = AccessController.getContext();
        s.defaultReadObject();
        appContext = AppContext.getAppContext();
    }
    private static native void initIDs();
    AccessibleContext accessibleContext = null;
    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
    protected abstract class AccessibleAWTMenuComponent
        extends AccessibleContext
        implements java.io.Serializable, AccessibleComponent,
                   AccessibleSelection
    {
        private static final long serialVersionUID = -4269533416223798698L;
        protected AccessibleAWTMenuComponent() {
        }
        // AccessibleContext methods
        //
        public AccessibleSelection getAccessibleSelection() {
            return this;
        }
        public String getAccessibleName() {
            return accessibleName;
        }
        public String getAccessibleDescription() {
            return accessibleDescription;
        }
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.AWT_COMPONENT; // Non-specific -- overridden in subclasses
        }
        public AccessibleStateSet getAccessibleStateSet() {
            return MenuComponent.this.getAccessibleStateSet();
        }
        public Accessible getAccessibleParent() {
            if (accessibleParent != null) {
                return accessibleParent;
            } else {
                MenuContainer parent = MenuComponent.this.getParent();
                if (parent instanceof Accessible) {
                    return (Accessible) parent;
                }
            }
            return null;
        }
        public int getAccessibleIndexInParent() {
            return MenuComponent.this.getAccessibleIndexInParent();
        }
        public int getAccessibleChildrenCount() {
            return 0; // MenuComponents don't have children
        }
        public Accessible getAccessibleChild(int i) {
            return null; // MenuComponents don't have children
        }
        public java.util.Locale getLocale() {
            MenuContainer parent = MenuComponent.this.getParent();
            if (parent instanceof Component)
                return ((Component)parent).getLocale();
            else
                return java.util.Locale.getDefault();
        }
        public AccessibleComponent getAccessibleComponent() {
            return this;
        }
        // AccessibleComponent methods
        //
        public Color getBackground() {
            return null; // Not supported for MenuComponents
        }
        public void setBackground(Color c) {
            // Not supported for MenuComponents
        }
        public Color getForeground() {
            return null; // Not supported for MenuComponents
        }
        public void setForeground(Color c) {
            // Not supported for MenuComponents
        }
        public Cursor getCursor() {
            return null; // Not supported for MenuComponents
        }
        public void setCursor(Cursor cursor) {
            // Not supported for MenuComponents
        }
        public Font getFont() {
            return MenuComponent.this.getFont();
        }
        public void setFont(Font f) {
            MenuComponent.this.setFont(f);
        }
        public FontMetrics getFontMetrics(Font f) {
            return null; // Not supported for MenuComponents
        }
        public boolean isEnabled() {
            return true; // Not supported for MenuComponents
        }
        public void setEnabled(boolean b) {
            // Not supported for MenuComponents
        }
        public boolean isVisible() {
            return true; // Not supported for MenuComponents
        }
        public void setVisible(boolean b) {
            // Not supported for MenuComponents
        }
        public boolean isShowing() {
            return true; // Not supported for MenuComponents
        }
        public boolean contains(Point p) {
            return false; // Not supported for MenuComponents
        }
        public Point getLocationOnScreen() {
            return null; // Not supported for MenuComponents
        }
        public Point getLocation() {
            return null; // Not supported for MenuComponents
        }
        public void setLocation(Point p) {
            // Not supported for MenuComponents
        }
        public Rectangle getBounds() {
            return null; // Not supported for MenuComponents
        }
        public void setBounds(Rectangle r) {
            // Not supported for MenuComponents
        }
        public Dimension getSize() {
            return null; // Not supported for MenuComponents
        }
        public void setSize(Dimension d) {
            // Not supported for MenuComponents
        }
        public Accessible getAccessibleAt(Point p) {
            return null; // MenuComponents don't have children
        }
        public boolean isFocusTraversable() {
            return true; // Not supported for MenuComponents
        }
        public void requestFocus() {
            // Not supported for MenuComponents
        }
        public void addFocusListener(java.awt.event.FocusListener l) {
            // Not supported for MenuComponents
        }
        public void removeFocusListener(java.awt.event.FocusListener l) {
            // Not supported for MenuComponents
        }
        // AccessibleSelection methods
        //
         public int getAccessibleSelectionCount() {
             return 0;  //  To be fully implemented in a future release
         }
         public Accessible getAccessibleSelection(int i) {
             return null;  //  To be fully implemented in a future release
         }
         public boolean isAccessibleChildSelected(int i) {
             return false;  //  To be fully implemented in a future release
         }
         public void addAccessibleSelection(int i) {
               //  To be fully implemented in a future release
         }
         public void removeAccessibleSelection(int i) {
               //  To be fully implemented in a future release
         }
         public void clearAccessibleSelection() {
               //  To be fully implemented in a future release
         }
         public void selectAllAccessibleSelection() {
               //  To be fully implemented in a future release
         }
    } // inner class AccessibleAWTComponent
    int getAccessibleIndexInParent() {
        MenuContainer localParent = parent;
        if (!(localParent instanceof MenuComponent)) {
            // MenuComponents only have accessible index when inside MenuComponents
            return -1;
        }
        MenuComponent localParentMenu = (MenuComponent)localParent;
        return localParentMenu.getAccessibleChildIndex(this);
    }
    int getAccessibleChildIndex(MenuComponent child) {
        return -1; // Overridden in subclasses.
    }
    AccessibleStateSet getAccessibleStateSet() {
        AccessibleStateSet states = new AccessibleStateSet();
        return states;
    }
}
