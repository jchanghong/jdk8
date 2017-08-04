
package java.awt;

import java.awt.peer.CheckboxMenuItemPeer;
import java.awt.event.*;
import java.util.EventListener;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import javax.accessibility.*;
import sun.awt.AWTAccessor;



public class CheckboxMenuItem extends MenuItem implements ItemSelectable, Accessible {

    static {

        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }

        AWTAccessor.setCheckboxMenuItemAccessor(
            new AWTAccessor.CheckboxMenuItemAccessor() {
                public boolean getState(CheckboxMenuItem cmi) {
                    return cmi.state;
                }
            });
    }


    boolean state = false;

    transient ItemListener itemListener;

    private static final String base = "chkmenuitem";
    private static int nameCounter = 0;


     private static final long serialVersionUID = 6190621106981774043L;


    public CheckboxMenuItem() throws HeadlessException {
        this("", false);
    }


    public CheckboxMenuItem(String label) throws HeadlessException {
        this(label, false);
    }


    public CheckboxMenuItem(String label, boolean state)
        throws HeadlessException {
        super(label);
        this.state = state;
    }


    String constructComponentName() {
        synchronized (CheckboxMenuItem.class) {
            return base + nameCounter++;
        }
    }


    public void addNotify() {
        synchronized (getTreeLock()) {
            if (peer == null)
                peer = Toolkit.getDefaultToolkit().createCheckboxMenuItem(this);
            super.addNotify();
        }
    }


    public boolean getState() {
        return state;
    }


    public synchronized void setState(boolean b) {
        state = b;
        CheckboxMenuItemPeer peer = (CheckboxMenuItemPeer)this.peer;
        if (peer != null) {
            peer.setState(b);
        }
    }


    public synchronized Object[] getSelectedObjects() {
        if (state) {
            Object[] items = new Object[1];
            items[0] = label;
            return items;
        }
        return null;
    }


    public synchronized void addItemListener(ItemListener l) {
        if (l == null) {
            return;
        }
        itemListener = AWTEventMulticaster.add(itemListener, l);
        newEventsOnly = true;
    }


    public synchronized void removeItemListener(ItemListener l) {
        if (l == null) {
            return;
        }
        itemListener = AWTEventMulticaster.remove(itemListener, l);
    }


    public synchronized ItemListener[] getItemListeners() {
        return getListeners(ItemListener.class);
    }


    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener l = null;
        if  (listenerType == ItemListener.class) {
            l = itemListener;
        } else {
            return super.getListeners(listenerType);
        }
        return AWTEventMulticaster.getListeners(l, listenerType);
    }

    // REMIND: remove when filtering is done at lower level
    boolean eventEnabled(AWTEvent e) {
        if (e.id == ItemEvent.ITEM_STATE_CHANGED) {
            if ((eventMask & AWTEvent.ITEM_EVENT_MASK) != 0 ||
                itemListener != null) {
                return true;
            }
            return false;
        }
        return super.eventEnabled(e);
    }


    protected void processEvent(AWTEvent e) {
        if (e instanceof ItemEvent) {
            processItemEvent((ItemEvent)e);
            return;
        }
        super.processEvent(e);
    }


    protected void processItemEvent(ItemEvent e) {
        ItemListener listener = itemListener;
        if (listener != null) {
            listener.itemStateChanged(e);
        }
    }


    void doMenuEvent(long when, int modifiers) {
        setState(!state);
        Toolkit.getEventQueue().postEvent(
            new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED,
                          getLabel(),
                          state ? ItemEvent.SELECTED :
                                  ItemEvent.DESELECTED));
    }


    public String paramString() {
        return super.paramString() + ",state=" + state;
    }




    private int checkboxMenuItemSerializedDataVersion = 1;


    private void writeObject(ObjectOutputStream s)
      throws java.io.IOException
    {
      s.defaultWriteObject();

      AWTEventMulticaster.save(s, itemListenerK, itemListener);
      s.writeObject(null);
    }


    private void readObject(ObjectInputStream s)
      throws ClassNotFoundException, IOException
    {
      s.defaultReadObject();

      Object keyOrNull;
      while(null != (keyOrNull = s.readObject())) {
        String key = ((String)keyOrNull).intern();

        if (itemListenerK == key)
          addItemListener((ItemListener)(s.readObject()));

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
            accessibleContext = new AccessibleAWTCheckboxMenuItem();
        }
        return accessibleContext;
    }


    protected class AccessibleAWTCheckboxMenuItem extends AccessibleAWTMenuItem
        implements AccessibleAction, AccessibleValue
    {

        private static final long serialVersionUID = -1122642964303476L;


        public AccessibleAction getAccessibleAction() {
            return this;
        }


        public AccessibleValue getAccessibleValue() {
            return this;
        }


        public int getAccessibleActionCount() {
            return 0;  //  To be fully implemented in a future release
        }


        public String getAccessibleActionDescription(int i) {
            return null;  //  To be fully implemented in a future release
        }


        public boolean doAccessibleAction(int i) {
            return false;    //  To be fully implemented in a future release
        }


        public Number getCurrentAccessibleValue() {
            return null;  //  To be fully implemented in a future release
        }


        public boolean setCurrentAccessibleValue(Number n) {
            return false;  //  To be fully implemented in a future release
        }


        public Number getMinimumAccessibleValue() {
            return null;  //  To be fully implemented in a future release
        }


        public Number getMaximumAccessibleValue() {
            return null;  //  To be fully implemented in a future release
        }


        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.CHECK_BOX;
        }

    } // class AccessibleAWTMenuItem

}
