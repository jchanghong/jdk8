

package java.awt;

import java.util.Vector;
import java.awt.peer.SystemTrayPeer;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import sun.awt.AppContext;
import sun.awt.SunToolkit;
import sun.awt.HeadlessToolkit;
import sun.security.util.SecurityConstants;
import sun.awt.AWTAccessor;


public class SystemTray {
    private static SystemTray systemTray;
    private int currentIconID = 0; // each TrayIcon added gets a unique ID

    transient private SystemTrayPeer peer;

    private static final TrayIcon[] EMPTY_TRAY_ARRAY = new TrayIcon[0];

    static {
        AWTAccessor.setSystemTrayAccessor(
            new AWTAccessor.SystemTrayAccessor() {
                public void firePropertyChange(SystemTray tray,
                                               String propertyName,
                                               Object oldValue,
                                               Object newValue) {
                    tray.firePropertyChange(propertyName, oldValue, newValue);
                }
            });
    }


    private SystemTray() {
        addNotify();
    }


    public static SystemTray getSystemTray() {
        checkSystemTrayAllowed();
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }

        initializeSystemTrayIfNeeded();

        if (!isSupported()) {
            throw new UnsupportedOperationException(
                "The system tray is not supported on the current platform.");
        }

        return systemTray;
    }


    public static boolean isSupported() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (toolkit instanceof SunToolkit) {
            // connecting tray to native resource
            initializeSystemTrayIfNeeded();
            return ((SunToolkit)toolkit).isTraySupported();
        } else if (toolkit instanceof HeadlessToolkit) {
            // skip initialization as the init routine
            // throws HeadlessException
            return ((HeadlessToolkit)toolkit).isTraySupported();
        } else {
            return false;
        }
    }


    public void add(TrayIcon trayIcon) throws AWTException {
        if (trayIcon == null) {
            throw new NullPointerException("adding null TrayIcon");
        }
        TrayIcon[] oldArray = null, newArray = null;
        Vector<TrayIcon> icons = null;
        synchronized (this) {
            oldArray = systemTray.getTrayIcons();
            icons = (Vector<TrayIcon>)AppContext.getAppContext().get(TrayIcon.class);
            if (icons == null) {
                icons = new Vector<TrayIcon>(3);
                AppContext.getAppContext().put(TrayIcon.class, icons);

            } else if (icons.contains(trayIcon)) {
                throw new IllegalArgumentException("adding TrayIcon that is already added");
            }
            icons.add(trayIcon);
            newArray = systemTray.getTrayIcons();

            trayIcon.setID(++currentIconID);
        }
        try {
            trayIcon.addNotify();
        } catch (AWTException e) {
            icons.remove(trayIcon);
            throw e;
        }
        firePropertyChange("trayIcons", oldArray, newArray);
    }


    public void remove(TrayIcon trayIcon) {
        if (trayIcon == null) {
            return;
        }
        TrayIcon[] oldArray = null, newArray = null;
        synchronized (this) {
            oldArray = systemTray.getTrayIcons();
            Vector<TrayIcon> icons = (Vector<TrayIcon>)AppContext.getAppContext().get(TrayIcon.class);
            // TrayIcon with no peer is not contained in the array.
            if (icons == null || !icons.remove(trayIcon)) {
                return;
            }
            trayIcon.removeNotify();
            newArray = systemTray.getTrayIcons();
        }
        firePropertyChange("trayIcons", oldArray, newArray);
    }


    public TrayIcon[] getTrayIcons() {
        Vector<TrayIcon> icons = (Vector<TrayIcon>)AppContext.getAppContext().get(TrayIcon.class);
        if (icons != null) {
            return (TrayIcon[])icons.toArray(new TrayIcon[icons.size()]);
        }
        return EMPTY_TRAY_ARRAY;
    }


    public Dimension getTrayIconSize() {
        return peer.getTrayIconSize();
    }


    public synchronized void addPropertyChangeListener(String propertyName,
                                                       PropertyChangeListener listener)
    {
        if (listener == null) {
            return;
        }
        getCurrentChangeSupport().addPropertyChangeListener(propertyName, listener);
    }


    public synchronized void removePropertyChangeListener(String propertyName,
                                                          PropertyChangeListener listener)
    {
        if (listener == null) {
            return;
        }
        getCurrentChangeSupport().removePropertyChangeListener(propertyName, listener);
    }


    public synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return getCurrentChangeSupport().getPropertyChangeListeners(propertyName);
    }


    // ***************************************************************
    // ***************************************************************



    private void firePropertyChange(String propertyName,
                                    Object oldValue, Object newValue)
    {
        if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
            return;
        }
        getCurrentChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
    }


    private synchronized PropertyChangeSupport getCurrentChangeSupport() {
        PropertyChangeSupport changeSupport =
            (PropertyChangeSupport)AppContext.getAppContext().get(SystemTray.class);

        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
            AppContext.getAppContext().put(SystemTray.class, changeSupport);
        }
        return changeSupport;
    }

    synchronized void addNotify() {
        if (peer == null) {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            if (toolkit instanceof SunToolkit) {
                peer = ((SunToolkit)Toolkit.getDefaultToolkit()).createSystemTray(this);
            } else if (toolkit instanceof HeadlessToolkit) {
                peer = ((HeadlessToolkit)Toolkit.getDefaultToolkit()).createSystemTray(this);
            }
        }
    }

    static void checkSystemTrayAllowed() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(SecurityConstants.AWT.ACCESS_SYSTEM_TRAY_PERMISSION);
        }
    }

    private static void initializeSystemTrayIfNeeded() {
        synchronized (SystemTray.class) {
            if (systemTray == null) {
                systemTray = new SystemTray();
            }
        }
    }
}
