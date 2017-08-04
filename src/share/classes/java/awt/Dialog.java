
package java.awt;

import java.awt.peer.DialogPeer;
import java.awt.event.*;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.accessibility.*;
import sun.awt.AppContext;
import sun.awt.SunToolkit;
import sun.awt.PeerEvent;
import sun.awt.util.IdentityArrayList;
import sun.awt.util.IdentityLinkedList;
import sun.security.util.SecurityConstants;
import java.security.AccessControlException;


public class Dialog extends Window {

    static {

        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }
    }


    boolean resizable = true;



    boolean undecorated = false;

    private transient boolean initialized = false;


    public static enum ModalityType {

        MODELESS,

        DOCUMENT_MODAL,

        APPLICATION_MODAL,

        TOOLKIT_MODAL
    };


    public final static ModalityType DEFAULT_MODALITY_TYPE = ModalityType.APPLICATION_MODAL;


    boolean modal;


    ModalityType modalityType;


    public static enum ModalExclusionType {

        NO_EXCLUDE,

        APPLICATION_EXCLUDE,

        TOOLKIT_EXCLUDE
    };


    transient static IdentityArrayList<Dialog> modalDialogs = new IdentityArrayList<Dialog>();

    transient IdentityArrayList<Window> blockedWindows = new IdentityArrayList<Window>();


    String title;

    private transient ModalEventFilter modalFilter;
    private transient volatile SecondaryLoop secondaryLoop;


    transient volatile boolean isInHide = false;


    transient volatile boolean isInDispose = false;

    private static final String base = "dialog";
    private static int nameCounter = 0;


    private static final long serialVersionUID = 5920926903803293709L;


     public Dialog(Frame owner) {
         this(owner, "", false);
     }


     public Dialog(Frame owner, boolean modal) {
         this(owner, "", modal);
     }


     public Dialog(Frame owner, String title) {
         this(owner, title, false);
     }


     public Dialog(Frame owner, String title, boolean modal) {
         this(owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS);
     }


     public Dialog(Frame owner, String title, boolean modal,
                   GraphicsConfiguration gc) {
         this(owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS, gc);
     }


     public Dialog(Dialog owner) {
         this(owner, "", false);
     }


     public Dialog(Dialog owner, String title) {
         this(owner, title, false);
     }


     public Dialog(Dialog owner, String title, boolean modal) {
         this(owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS);
     }


     public Dialog(Dialog owner, String title, boolean modal,
                   GraphicsConfiguration gc) {
         this(owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS, gc);
     }


    public Dialog(Window owner) {
        this(owner, "", ModalityType.MODELESS);
    }


    public Dialog(Window owner, String title) {
        this(owner, title, ModalityType.MODELESS);
    }


    public Dialog(Window owner, ModalityType modalityType) {
        this(owner, "", modalityType);
    }


    public Dialog(Window owner, String title, ModalityType modalityType) {
        super(owner);

        if ((owner != null) &&
            !(owner instanceof Frame) &&
            !(owner instanceof Dialog))
        {
            throw new IllegalArgumentException("Wrong parent window");
        }

        this.title = title;
        setModalityType(modalityType);
        SunToolkit.checkAndSetPolicy(this);
        initialized = true;
    }


    public Dialog(Window owner, String title, ModalityType modalityType,
                  GraphicsConfiguration gc) {
        super(owner, gc);

        if ((owner != null) &&
            !(owner instanceof Frame) &&
            !(owner instanceof Dialog))
        {
            throw new IllegalArgumentException("wrong owner window");
        }

        this.title = title;
        setModalityType(modalityType);
        SunToolkit.checkAndSetPolicy(this);
        initialized = true;
    }


    String constructComponentName() {
        synchronized (Dialog.class) {
            return base + nameCounter++;
        }
    }


    public void addNotify() {
        synchronized (getTreeLock()) {
            if (parent != null && parent.getPeer() == null) {
                parent.addNotify();
            }

            if (peer == null) {
                peer = getToolkit().createDialog(this);
            }
            super.addNotify();
        }
    }


    public boolean isModal() {
        return isModal_NoClientCode();
    }
    final boolean isModal_NoClientCode() {
        return modalityType != ModalityType.MODELESS;
    }


    public void setModal(boolean modal) {
        this.modal = modal;
        setModalityType(modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS);
    }


    public ModalityType getModalityType() {
        return modalityType;
    }


    public void setModalityType(ModalityType type) {
        if (type == null) {
            type = Dialog.ModalityType.MODELESS;
        }
        if (!Toolkit.getDefaultToolkit().isModalityTypeSupported(type)) {
            type = Dialog.ModalityType.MODELESS;
        }
        if (modalityType == type) {
            return;
        }

        checkModalityPermission(type);

        modalityType = type;
        modal = (modalityType != ModalityType.MODELESS);
    }


    public String getTitle() {
        return title;
    }


    public void setTitle(String title) {
        String oldTitle = this.title;

        synchronized(this) {
            this.title = title;
            DialogPeer peer = (DialogPeer)this.peer;
            if (peer != null) {
                peer.setTitle(title);
            }
        }
        firePropertyChange("title", oldTitle, title);
    }


    private boolean conditionalShow(Component toFocus, AtomicLong time) {
        boolean retval;

        closeSplashScreen();

        synchronized (getTreeLock()) {
            if (peer == null) {
                addNotify();
            }
            validateUnconditionally();
            if (visible) {
                toFront();
                retval = false;
            } else {
                visible = retval = true;

                // check if this dialog should be modal blocked BEFORE calling peer.show(),
                // otherwise, a pair of FOCUS_GAINED and FOCUS_LOST may be mistakenly
                // generated for the dialog
                if (!isModal()) {
                    checkShouldBeBlocked(this);
                } else {
                    modalDialogs.add(this);
                    modalShow();
                }

                if (toFocus != null && time != null && isFocusable() &&
                    isEnabled() && !isModalBlocked()) {
                    // keep the KeyEvents from being dispatched
                    // until the focus has been transfered
                    time.set(Toolkit.getEventQueue().getMostRecentKeyEventTime());
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().
                        enqueueKeyEvents(time.get(), toFocus);
                }

                // This call is required as the show() method of the Dialog class
                // does not invoke the super.show(). So wried... :(
                mixOnShowing();

                peer.setVisible(true); // now guaranteed never to block
                if (isModalBlocked()) {
                    modalBlocker.toFront();
                }

                setLocationByPlatform(false);
                for (int i = 0; i < ownedWindowList.size(); i++) {
                    Window child = ownedWindowList.elementAt(i).get();
                    if ((child != null) && child.showWithParent) {
                        child.show();
                        child.showWithParent = false;
                    }       // endif
                }   // endfor
                Window.updateChildFocusableWindowState(this);

                createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED,
                                      this, parent,
                                      HierarchyEvent.SHOWING_CHANGED,
                                      Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
                if (componentListener != null ||
                        (eventMask & AWTEvent.COMPONENT_EVENT_MASK) != 0 ||
                        Toolkit.enabledOnToolkit(AWTEvent.COMPONENT_EVENT_MASK)) {
                    ComponentEvent e =
                        new ComponentEvent(this, ComponentEvent.COMPONENT_SHOWN);
                    Toolkit.getEventQueue().postEvent(e);
                }
            }
        }

        if (retval && (state & OPENED) == 0) {
            postWindowEvent(WindowEvent.WINDOW_OPENED);
            state |= OPENED;
        }

        return retval;
    }


    public void setVisible(boolean b) {
        super.setVisible(b);
    }


    @Deprecated
    public void show() {
        if (!initialized) {
            throw new IllegalStateException("The dialog component " +
                "has not been initialized properly");
        }

        beforeFirstShow = false;
        if (!isModal()) {
            conditionalShow(null, null);
        } else {
            AppContext showAppContext = AppContext.getAppContext();

            AtomicLong time = new AtomicLong();
            Component predictedFocusOwner = null;
            try {
                predictedFocusOwner = getMostRecentFocusOwner();
                if (conditionalShow(predictedFocusOwner, time)) {
                    modalFilter = ModalEventFilter.createFilterForDialog(this);
                    final Conditional cond = new Conditional() {
                        @Override
                        public boolean evaluate() {
                            return windowClosingException == null;
                        }
                    };

                    // if this dialog is toolkit-modal, the filter should be added
                    // to all EDTs (for all AppContexts)
                    if (modalityType == ModalityType.TOOLKIT_MODAL) {
                        Iterator<AppContext> it = AppContext.getAppContexts().iterator();
                        while (it.hasNext()) {
                            AppContext appContext = it.next();
                            if (appContext == showAppContext) {
                                continue;
                            }
                            EventQueue eventQueue = (EventQueue)appContext.get(AppContext.EVENT_QUEUE_KEY);
                            // it may occur that EDT for appContext hasn't been started yet, so
                            // we post an empty invocation event to trigger EDT initialization
                            Runnable createEDT = new Runnable() {
                                public void run() {};
                            };
                            eventQueue.postEvent(new InvocationEvent(this, createEDT));
                            EventDispatchThread edt = eventQueue.getDispatchThread();
                            edt.addEventFilter(modalFilter);
                        }
                    }

                    modalityPushed();
                    try {
                        final EventQueue eventQueue = AccessController.doPrivileged(
                            new PrivilegedAction<EventQueue>() {
                                public EventQueue run() {
                                    return Toolkit.getDefaultToolkit().getSystemEventQueue();
                                }
                        });
                        secondaryLoop = eventQueue.createSecondaryLoop(cond, modalFilter, 0);
                        if (!secondaryLoop.enter()) {
                            secondaryLoop = null;
                        }
                    } finally {
                        modalityPopped();
                    }

                    // if this dialog is toolkit-modal, its filter must be removed
                    // from all EDTs (for all AppContexts)
                    if (modalityType == ModalityType.TOOLKIT_MODAL) {
                        Iterator<AppContext> it = AppContext.getAppContexts().iterator();
                        while (it.hasNext()) {
                            AppContext appContext = it.next();
                            if (appContext == showAppContext) {
                                continue;
                            }
                            EventQueue eventQueue = (EventQueue)appContext.get(AppContext.EVENT_QUEUE_KEY);
                            EventDispatchThread edt = eventQueue.getDispatchThread();
                            edt.removeEventFilter(modalFilter);
                        }
                    }

                    if (windowClosingException != null) {
                        windowClosingException.fillInStackTrace();
                        throw windowClosingException;
                    }
                }
            } finally {
                if (predictedFocusOwner != null) {
                    // Restore normal key event dispatching
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().
                        dequeueKeyEvents(time.get(), predictedFocusOwner);
                }
            }
        }
    }

    final void modalityPushed() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        if (tk instanceof SunToolkit) {
            SunToolkit stk = (SunToolkit)tk;
            stk.notifyModalityPushed(this);
        }
    }

    final void modalityPopped() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        if (tk instanceof SunToolkit) {
            SunToolkit stk = (SunToolkit)tk;
            stk.notifyModalityPopped(this);
        }
    }

    void interruptBlocking() {
        if (isModal()) {
            disposeImpl();
        } else if (windowClosingException != null) {
            windowClosingException.fillInStackTrace();
            windowClosingException.printStackTrace();
            windowClosingException = null;
        }
    }

    private void hideAndDisposePreHandler() {
        isInHide = true;
        synchronized (getTreeLock()) {
            if (secondaryLoop != null) {
                modalHide();
                // dialog can be shown and then disposed before its
                // modal filter is created
                if (modalFilter != null) {
                    modalFilter.disable();
                }
                modalDialogs.remove(this);
            }
        }
    }
    private void hideAndDisposeHandler() {
        if (secondaryLoop != null) {
            secondaryLoop.exit();
            secondaryLoop = null;
        }
        isInHide = false;
    }


    @Deprecated
    public void hide() {
        hideAndDisposePreHandler();
        super.hide();
        // fix for 5048370: if hide() is called from super.doDispose(), then
        // hideAndDisposeHandler() should not be called here as it will be called
        // at the end of doDispose()
        if (!isInDispose) {
            hideAndDisposeHandler();
        }
    }


    void doDispose() {
        // fix for 5048370: set isInDispose flag to true to prevent calling
        // to hideAndDisposeHandler() from hide()
        isInDispose = true;
        super.doDispose();
        hideAndDisposeHandler();
        isInDispose = false;
    }


    public void toBack() {
        super.toBack();
        if (visible) {
            synchronized (getTreeLock()) {
                for (Window w : blockedWindows) {
                    w.toBack_NoClientCode();
                }
            }
        }
    }


    public boolean isResizable() {
        return resizable;
    }


    public void setResizable(boolean resizable) {
        boolean testvalid = false;

        synchronized (this) {
            this.resizable = resizable;
            DialogPeer peer = (DialogPeer)this.peer;
            if (peer != null) {
                peer.setResizable(resizable);
                testvalid = true;
            }
        }

        // On some platforms, changing the resizable state affects
        // the insets of the Dialog. If we could, we'd call invalidate()
        // from the peer, but we need to guarantee that we're not holding
        // the Dialog lock when we call invalidate().
        if (testvalid) {
            invalidateIfValid();
        }
    }



    public void setUndecorated(boolean undecorated) {

        synchronized (getTreeLock()) {
            if (isDisplayable()) {
                throw new IllegalComponentStateException("The dialog is displayable.");
            }
            if (!undecorated) {
                if (getOpacity() < 1.0f) {
                    throw new IllegalComponentStateException("The dialog is not opaque");
                }
                if (getShape() != null) {
                    throw new IllegalComponentStateException("The dialog does not have a default shape");
                }
                Color bg = getBackground();
                if ((bg != null) && (bg.getAlpha() < 255)) {
                    throw new IllegalComponentStateException("The dialog background color is not opaque");
                }
            }
            this.undecorated = undecorated;
        }
    }


    public boolean isUndecorated() {
        return undecorated;
    }


    @Override
    public void setOpacity(float opacity) {
        synchronized (getTreeLock()) {
            if ((opacity < 1.0f) && !isUndecorated()) {
                throw new IllegalComponentStateException("The dialog is decorated");
            }
            super.setOpacity(opacity);
        }
    }


    @Override
    public void setShape(Shape shape) {
        synchronized (getTreeLock()) {
            if ((shape != null) && !isUndecorated()) {
                throw new IllegalComponentStateException("The dialog is decorated");
            }
            super.setShape(shape);
        }
    }


    @Override
    public void setBackground(Color bgColor) {
        synchronized (getTreeLock()) {
            if ((bgColor != null) && (bgColor.getAlpha() < 255) && !isUndecorated()) {
                throw new IllegalComponentStateException("The dialog is decorated");
            }
            super.setBackground(bgColor);
        }
    }


    protected String paramString() {
        String str = super.paramString() + "," + modalityType;
        if (title != null) {
            str += ",title=" + title;
        }
        return str;
    }


    private static native void initIDs();




    void modalShow() {
        // find all the dialogs that block this one
        IdentityArrayList<Dialog> blockers = new IdentityArrayList<Dialog>();
        for (Dialog d : modalDialogs) {
            if (d.shouldBlock(this)) {
                Window w = d;
                while ((w != null) && (w != this)) {
                    w = w.getOwner_NoClientCode();
                }
                if ((w == this) || !shouldBlock(d) || (modalityType.compareTo(d.getModalityType()) < 0)) {
                    blockers.add(d);
                }
            }
        }

        // add all blockers' blockers to blockers :)
        for (int i = 0; i < blockers.size(); i++) {
            Dialog blocker = blockers.get(i);
            if (blocker.isModalBlocked()) {
                Dialog blockerBlocker = blocker.getModalBlocker();
                if (!blockers.contains(blockerBlocker)) {
                    blockers.add(i + 1, blockerBlocker);
                }
            }
        }

        if (blockers.size() > 0) {
            blockers.get(0).blockWindow(this);
        }

        // find all windows from blockers' hierarchies
        IdentityArrayList<Window> blockersHierarchies = new IdentityArrayList<Window>(blockers);
        int k = 0;
        while (k < blockersHierarchies.size()) {
            Window w = blockersHierarchies.get(k);
            Window[] ownedWindows = w.getOwnedWindows_NoClientCode();
            for (Window win : ownedWindows) {
                blockersHierarchies.add(win);
            }
            k++;
        }

        java.util.List<Window> toBlock = new IdentityLinkedList<Window>();
        // block all windows from scope of blocking except from blockers' hierarchies
        IdentityArrayList<Window> unblockedWindows = Window.getAllUnblockedWindows();
        for (Window w : unblockedWindows) {
            if (shouldBlock(w) && !blockersHierarchies.contains(w)) {
                if ((w instanceof Dialog) && ((Dialog)w).isModal_NoClientCode()) {
                    Dialog wd = (Dialog)w;
                    if (wd.shouldBlock(this) && (modalDialogs.indexOf(wd) > modalDialogs.indexOf(this))) {
                        continue;
                    }
                }
                toBlock.add(w);
            }
        }
        blockWindows(toBlock);

        if (!isModalBlocked()) {
            updateChildrenBlocking();
        }
    }


    void modalHide() {
        // we should unblock all the windows first...
        IdentityArrayList<Window> save = new IdentityArrayList<Window>();
        int blockedWindowsCount = blockedWindows.size();
        for (int i = 0; i < blockedWindowsCount; i++) {
            Window w = blockedWindows.get(0);
            save.add(w);
            unblockWindow(w); // also removes w from blockedWindows
        }
        // ... and only after that check if they should be blocked
        // by another dialogs
        for (int i = 0; i < blockedWindowsCount; i++) {
            Window w = save.get(i);
            if ((w instanceof Dialog) && ((Dialog)w).isModal_NoClientCode()) {
                Dialog d = (Dialog)w;
                d.modalShow();
            } else {
                checkShouldBeBlocked(w);
            }
        }
    }


    boolean shouldBlock(Window w) {
        if (!isVisible_NoClientCode() ||
            (!w.isVisible_NoClientCode() && !w.isInShow) ||
            isInHide ||
            (w == this) ||
            !isModal_NoClientCode())
        {
            return false;
        }
        if ((w instanceof Dialog) && ((Dialog)w).isInHide) {
            return false;
        }
        // check if w is from children hierarchy
        // fix for 6271546: we should also take into consideration child hierarchies
        // of this dialog's blockers
        Window blockerToCheck = this;
        while (blockerToCheck != null) {
            Component c = w;
            while ((c != null) && (c != blockerToCheck)) {
                c = c.getParent_NoClientCode();
            }
            if (c == blockerToCheck) {
                return false;
            }
            blockerToCheck = blockerToCheck.getModalBlocker();
        }
        switch (modalityType) {
            case MODELESS:
                return false;
            case DOCUMENT_MODAL:
                if (w.isModalExcluded(ModalExclusionType.APPLICATION_EXCLUDE)) {
                    // application- and toolkit-excluded windows are not blocked by
                    // document-modal dialogs from outside their children hierarchy
                    Component c = this;
                    while ((c != null) && (c != w)) {
                        c = c.getParent_NoClientCode();
                    }
                    return c == w;
                } else {
                    return getDocumentRoot() == w.getDocumentRoot();
                }
            case APPLICATION_MODAL:
                return !w.isModalExcluded(ModalExclusionType.APPLICATION_EXCLUDE) &&
                    (appContext == w.appContext);
            case TOOLKIT_MODAL:
                return !w.isModalExcluded(ModalExclusionType.TOOLKIT_EXCLUDE);
        }

        return false;
    }


    void blockWindow(Window w) {
        if (!w.isModalBlocked()) {
            w.setModalBlocked(this, true, true);
            blockedWindows.add(w);
        }
    }

    void blockWindows(java.util.List<Window> toBlock) {
        DialogPeer dpeer = (DialogPeer)peer;
        if (dpeer == null) {
            return;
        }
        Iterator<Window> it = toBlock.iterator();
        while (it.hasNext()) {
            Window w = it.next();
            if (!w.isModalBlocked()) {
                w.setModalBlocked(this, true, false);
            } else {
                it.remove();
            }
        }
        dpeer.blockWindows(toBlock);
        blockedWindows.addAll(toBlock);
    }


    void unblockWindow(Window w) {
        if (w.isModalBlocked() && blockedWindows.contains(w)) {
            blockedWindows.remove(w);
            w.setModalBlocked(this, false, true);
        }
    }


    static void checkShouldBeBlocked(Window w) {
        synchronized (w.getTreeLock()) {
            for (int i = 0; i < modalDialogs.size(); i++) {
                Dialog modalDialog = modalDialogs.get(i);
                if (modalDialog.shouldBlock(w)) {
                    modalDialog.blockWindow(w);
                    break;
                }
            }
        }
    }

    private void checkModalityPermission(ModalityType mt) {
        if (mt == ModalityType.TOOLKIT_MODAL) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(
                    SecurityConstants.AWT.TOOLKIT_MODALITY_PERMISSION
                );
            }
        }
    }

    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException, HeadlessException
    {
        GraphicsEnvironment.checkHeadless();

        java.io.ObjectInputStream.GetField fields =
            s.readFields();

        ModalityType localModalityType = (ModalityType)fields.get("modalityType", null);

        try {
            checkModalityPermission(localModalityType);
        } catch (AccessControlException ace) {
            localModalityType = DEFAULT_MODALITY_TYPE;
        }

        // in 1.5 or earlier modalityType was absent, so use "modal" instead
        if (localModalityType == null) {
            this.modal = fields.get("modal", false);
            setModal(modal);
        } else {
            this.modalityType = localModalityType;
        }

        this.resizable = fields.get("resizable", true);
        this.undecorated = fields.get("undecorated", false);
        this.title = (String)fields.get("title", "");

        blockedWindows = new IdentityArrayList<>();

        SunToolkit.checkAndSetPolicy(this);

        initialized = true;

    }




    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleAWTDialog();
        }
        return accessibleContext;
    }


    protected class AccessibleAWTDialog extends AccessibleAWTWindow
    {

        private static final long serialVersionUID = 4837230331833941201L;


        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.DIALOG;
        }


        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();
            if (getFocusOwner() != null) {
                states.add(AccessibleState.ACTIVE);
            }
            if (isModal()) {
                states.add(AccessibleState.MODAL);
            }
            if (isResizable()) {
                states.add(AccessibleState.RESIZABLE);
            }
            return states;
        }

    } // inner class AccessibleAWTDialog
}
