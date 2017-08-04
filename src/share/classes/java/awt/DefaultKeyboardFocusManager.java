package java.awt;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.peer.ComponentPeer;
import java.awt.peer.LightweightPeer;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;
import sun.util.logging.PlatformLogger;
import sun.awt.AppContext;
import sun.awt.SunToolkit;
import sun.awt.AWTAccessor;
import sun.awt.CausedFocusEvent;
import sun.awt.TimedWindowEvent;
public class DefaultKeyboardFocusManager extends KeyboardFocusManager {
    private static final PlatformLogger focusLog = PlatformLogger.getLogger("java.awt.focus.DefaultKeyboardFocusManager");
    // null weak references to not create too many objects
    private static final WeakReference<Window> NULL_WINDOW_WR =
        new WeakReference<Window>(null);
    private static final WeakReference<Component> NULL_COMPONENT_WR =
        new WeakReference<Component>(null);
    private WeakReference<Window> realOppositeWindowWR = NULL_WINDOW_WR;
    private WeakReference<Component> realOppositeComponentWR = NULL_COMPONENT_WR;
    private int inSendMessage;
    private LinkedList<KeyEvent> enqueuedKeyEvents = new LinkedList<KeyEvent>();
    private LinkedList<TypeAheadMarker> typeAheadMarkers = new LinkedList<TypeAheadMarker>();
    private boolean consumeNextKeyTyped;
    static {
        AWTAccessor.setDefaultKeyboardFocusManagerAccessor(
            new AWTAccessor.DefaultKeyboardFocusManagerAccessor() {
                public void consumeNextKeyTyped(DefaultKeyboardFocusManager dkfm, KeyEvent e) {
                    dkfm.consumeNextKeyTyped(e);
                }
            });
    }
    private static class TypeAheadMarker {
        long after;
        Component untilFocused;
        TypeAheadMarker(long after, Component untilFocused) {
            this.after = after;
            this.untilFocused = untilFocused;
        }
        public String toString() {
            return ">>> Marker after " + after + " on " + untilFocused;
        }
    }
    private Window getOwningFrameDialog(Window window) {
        while (window != null && !(window instanceof Frame ||
                                   window instanceof Dialog)) {
            window = (Window)window.getParent();
        }
        return window;
    }
    private void restoreFocus(FocusEvent fe, Window newFocusedWindow) {
        Component realOppositeComponent = this.realOppositeComponentWR.get();
        Component vetoedComponent = fe.getComponent();
        if (newFocusedWindow != null && restoreFocus(newFocusedWindow,
                                                     vetoedComponent, false))
        {
        } else if (realOppositeComponent != null &&
                   doRestoreFocus(realOppositeComponent, vetoedComponent, false)) {
        } else if (fe.getOppositeComponent() != null &&
                   doRestoreFocus(fe.getOppositeComponent(), vetoedComponent, false)) {
        } else {
            clearGlobalFocusOwnerPriv();
        }
    }
    private void restoreFocus(WindowEvent we) {
        Window realOppositeWindow = this.realOppositeWindowWR.get();
        if (realOppositeWindow != null
            && restoreFocus(realOppositeWindow, null, false))
        {
            // do nothing, everything is done in restoreFocus()
        } else if (we.getOppositeWindow() != null &&
                   restoreFocus(we.getOppositeWindow(), null, false))
        {
            // do nothing, everything is done in restoreFocus()
        } else {
            clearGlobalFocusOwnerPriv();
        }
    }
    private boolean restoreFocus(Window aWindow, Component vetoedComponent,
                                 boolean clearOnFailure) {
        Component toFocus =
            KeyboardFocusManager.getMostRecentFocusOwner(aWindow);
        if (toFocus != null && toFocus != vetoedComponent && doRestoreFocus(toFocus, vetoedComponent, false)) {
            return true;
        } else if (clearOnFailure) {
            clearGlobalFocusOwnerPriv();
            return true;
        } else {
            return false;
        }
    }
    private boolean restoreFocus(Component toFocus, boolean clearOnFailure) {
        return doRestoreFocus(toFocus, null, clearOnFailure);
    }
    private boolean doRestoreFocus(Component toFocus, Component vetoedComponent,
                                   boolean clearOnFailure)
    {
        if (toFocus != vetoedComponent && toFocus.isShowing() && toFocus.canBeFocusOwner() &&
            toFocus.requestFocus(false, CausedFocusEvent.Cause.ROLLBACK))
        {
            return true;
        } else {
            Component nextFocus = toFocus.getNextFocusCandidate();
            if (nextFocus != null && nextFocus != vetoedComponent &&
                nextFocus.requestFocusInWindow(CausedFocusEvent.Cause.ROLLBACK))
            {
                return true;
            } else if (clearOnFailure) {
                clearGlobalFocusOwnerPriv();
                return true;
            } else {
                return false;
            }
        }
    }
    private static class DefaultKeyboardFocusManagerSentEvent
        extends SentEvent
    {
        private static final long serialVersionUID = -2924743257508701758L;
        public DefaultKeyboardFocusManagerSentEvent(AWTEvent nested,
                                                    AppContext toNotify) {
            super(nested, toNotify);
        }
        public final void dispatch() {
            KeyboardFocusManager manager =
                KeyboardFocusManager.getCurrentKeyboardFocusManager();
            DefaultKeyboardFocusManager defaultManager =
                (manager instanceof DefaultKeyboardFocusManager)
                ? (DefaultKeyboardFocusManager)manager
                : null;
            if (defaultManager != null) {
                synchronized (defaultManager) {
                    defaultManager.inSendMessage++;
                }
            }
            super.dispatch();
            if (defaultManager != null) {
                synchronized (defaultManager) {
                    defaultManager.inSendMessage--;
                }
            }
        }
    }
    static boolean sendMessage(Component target, AWTEvent e) {
        e.isPosted = true;
        AppContext myAppContext = AppContext.getAppContext();
        final AppContext targetAppContext = target.appContext;
        final SentEvent se =
            new DefaultKeyboardFocusManagerSentEvent(e, myAppContext);
        if (myAppContext == targetAppContext) {
            se.dispatch();
        } else {
            if (targetAppContext.isDisposed()) {
                return false;
            }
            SunToolkit.postEvent(targetAppContext, se);
            if (EventQueue.isDispatchThread()) {
                EventDispatchThread edt = (EventDispatchThread)
                    Thread.currentThread();
                edt.pumpEvents(SentEvent.ID, new Conditional() {
                        public boolean evaluate() {
                            return !se.dispatched && !targetAppContext.isDisposed();
                        }
                    });
            } else {
                synchronized (se) {
                    while (!se.dispatched && !targetAppContext.isDisposed()) {
                        try {
                            se.wait(1000);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                }
            }
        }
        return se.dispatched;
    }
    private boolean repostIfFollowsKeyEvents(WindowEvent e) {
        if (!(e instanceof TimedWindowEvent)) {
            return false;
        }
        TimedWindowEvent we = (TimedWindowEvent)e;
        long time = we.getWhen();
        synchronized (this) {
            KeyEvent ke = enqueuedKeyEvents.isEmpty() ? null : enqueuedKeyEvents.getFirst();
            if (ke != null && time >= ke.getWhen()) {
                TypeAheadMarker marker = typeAheadMarkers.isEmpty() ? null : typeAheadMarkers.getFirst();
                if (marker != null) {
                    Window toplevel = marker.untilFocused.getContainingWindow();
                    // Check that the component awaiting focus belongs to
                    // the current focused window. See 8015454.
                    if (toplevel != null && toplevel.isFocused()) {
                        SunToolkit.postEvent(AppContext.getAppContext(), new SequencedEvent(e));
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public boolean dispatchEvent(AWTEvent e) {
        if (focusLog.isLoggable(PlatformLogger.Level.FINE) && (e instanceof WindowEvent || e instanceof FocusEvent)) {
            focusLog.fine("" + e);
        }
        switch (e.getID()) {
            case WindowEvent.WINDOW_GAINED_FOCUS: {
                if (repostIfFollowsKeyEvents((WindowEvent)e)) {
                    break;
                }
                WindowEvent we = (WindowEvent)e;
                Window oldFocusedWindow = getGlobalFocusedWindow();
                Window newFocusedWindow = we.getWindow();
                if (newFocusedWindow == oldFocusedWindow) {
                    break;
                }
                if (!(newFocusedWindow.isFocusableWindow()
                      && newFocusedWindow.isVisible()
                      && newFocusedWindow.isDisplayable()))
                {
                    // we can not accept focus on such window, so reject it.
                    restoreFocus(we);
                    break;
                }
                // If there exists a current focused window, then notify it
                // that it has lost focus.
                if (oldFocusedWindow != null) {
                    boolean isEventDispatched =
                        sendMessage(oldFocusedWindow,
                                new WindowEvent(oldFocusedWindow,
                                                WindowEvent.WINDOW_LOST_FOCUS,
                                                newFocusedWindow));
                    // Failed to dispatch, clear by ourselfves
                    if (!isEventDispatched) {
                        setGlobalFocusOwner(null);
                        setGlobalFocusedWindow(null);
                    }
                }
                // Because the native libraries do not post WINDOW_ACTIVATED
                // events, we need to synthesize one if the active Window
                // changed.
                Window newActiveWindow =
                    getOwningFrameDialog(newFocusedWindow);
                Window currentActiveWindow = getGlobalActiveWindow();
                if (newActiveWindow != currentActiveWindow) {
                    sendMessage(newActiveWindow,
                                new WindowEvent(newActiveWindow,
                                                WindowEvent.WINDOW_ACTIVATED,
                                                currentActiveWindow));
                    if (newActiveWindow != getGlobalActiveWindow()) {
                        // Activation change was rejected. Unlikely, but
                        // possible.
                        restoreFocus(we);
                        break;
                    }
                }
                setGlobalFocusedWindow(newFocusedWindow);
                if (newFocusedWindow != getGlobalFocusedWindow()) {
                    // Focus change was rejected. Will happen if
                    // newFocusedWindow is not a focusable Window.
                    restoreFocus(we);
                    break;
                }
                // Restore focus to the Component which last held it. We do
                // this here so that client code can override our choice in
                // a WINDOW_GAINED_FOCUS handler.
                //
                // Make sure that the focus change request doesn't change the
                // focused Window in case we are no longer the focused Window
                // when the request is handled.
                if (inSendMessage == 0) {
                    // Identify which Component should initially gain focus
                    // in the Window.
                    //
                    // * If we're in SendMessage, then this is a synthetic
                    //   WINDOW_GAINED_FOCUS message which was generated by a
                    //   the FOCUS_GAINED handler. Allow the Component to
                    //   which the FOCUS_GAINED message was targeted to
                    //   receive the focus.
                    // * Otherwise, look up the correct Component here.
                    //   We don't use Window.getMostRecentFocusOwner because
                    //   window is focused now and 'null' will be returned
                    // Calculating of most recent focus owner and focus
                    // request should be synchronized on KeyboardFocusManager.class
                    // to prevent from thread race when user will request
                    // focus between calculation and our request.
                    // But if focus transfer is synchronous, this synchronization
                    // may cause deadlock, thus we don't synchronize this block.
                    Component toFocus = KeyboardFocusManager.
                        getMostRecentFocusOwner(newFocusedWindow);
                    if ((toFocus == null) &&
                        newFocusedWindow.isFocusableWindow())
                    {
                        toFocus = newFocusedWindow.getFocusTraversalPolicy().
                            getInitialComponent(newFocusedWindow);
                    }
                    Component tempLost = null;
                    synchronized(KeyboardFocusManager.class) {
                        tempLost = newFocusedWindow.setTemporaryLostComponent(null);
                    }
                    // The component which last has the focus when this window was focused
                    // should receive focus first
                    if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                        focusLog.finer("tempLost {0}, toFocus {1}",
                                       tempLost, toFocus);
                    }
                    if (tempLost != null) {
                        tempLost.requestFocusInWindow(CausedFocusEvent.Cause.ACTIVATION);
                    }
                    if (toFocus != null && toFocus != tempLost) {
                        // If there is a component which requested focus when this window
                        // was inactive it expects to receive focus after activation.
                        toFocus.requestFocusInWindow(CausedFocusEvent.Cause.ACTIVATION);
                    }
                }
                Window realOppositeWindow = this.realOppositeWindowWR.get();
                if (realOppositeWindow != we.getOppositeWindow()) {
                    we = new WindowEvent(newFocusedWindow,
                                         WindowEvent.WINDOW_GAINED_FOCUS,
                                         realOppositeWindow);
                }
                return typeAheadAssertions(newFocusedWindow, we);
            }
            case WindowEvent.WINDOW_ACTIVATED: {
                WindowEvent we = (WindowEvent)e;
                Window oldActiveWindow = getGlobalActiveWindow();
                Window newActiveWindow = we.getWindow();
                if (oldActiveWindow == newActiveWindow) {
                    break;
                }
                // If there exists a current active window, then notify it that
                // it has lost activation.
                if (oldActiveWindow != null) {
                    boolean isEventDispatched =
                        sendMessage(oldActiveWindow,
                                new WindowEvent(oldActiveWindow,
                                                WindowEvent.WINDOW_DEACTIVATED,
                                                newActiveWindow));
                    // Failed to dispatch, clear by ourselfves
                    if (!isEventDispatched) {
                        setGlobalActiveWindow(null);
                    }
                    if (getGlobalActiveWindow() != null) {
                        // Activation change was rejected. Unlikely, but
                        // possible.
                        break;
                    }
                }
                setGlobalActiveWindow(newActiveWindow);
                if (newActiveWindow != getGlobalActiveWindow()) {
                    // Activation change was rejected. Unlikely, but
                    // possible.
                    break;
                }
                return typeAheadAssertions(newActiveWindow, we);
            }
            case FocusEvent.FOCUS_GAINED: {
                FocusEvent fe = (FocusEvent)e;
                CausedFocusEvent.Cause cause = (fe instanceof CausedFocusEvent) ?
                    ((CausedFocusEvent)fe).getCause() : CausedFocusEvent.Cause.UNKNOWN;
                Component oldFocusOwner = getGlobalFocusOwner();
                Component newFocusOwner = fe.getComponent();
                if (oldFocusOwner == newFocusOwner) {
                    if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
                        focusLog.fine("Skipping {0} because focus owner is the same", e);
                    }
                    // We can't just drop the event - there could be
                    // type-ahead markers associated with it.
                    dequeueKeyEvents(-1, newFocusOwner);
                    break;
                }
                // If there exists a current focus owner, then notify it that
                // it has lost focus.
                if (oldFocusOwner != null) {
                    boolean isEventDispatched =
                        sendMessage(oldFocusOwner,
                                    new CausedFocusEvent(oldFocusOwner,
                                                   FocusEvent.FOCUS_LOST,
                                                   fe.isTemporary(),
                                                   newFocusOwner, cause));
                    // Failed to dispatch, clear by ourselfves
                    if (!isEventDispatched) {
                        setGlobalFocusOwner(null);
                        if (!fe.isTemporary()) {
                            setGlobalPermanentFocusOwner(null);
                        }
                    }
                }
                // Because the native windowing system has a different notion
                // of the current focus and activation states, it is possible
                // that a Component outside of the focused Window receives a
                // FOCUS_GAINED event. We synthesize a WINDOW_GAINED_FOCUS
                // event in that case.
                final Window newFocusedWindow = SunToolkit.getContainingWindow(newFocusOwner);
                final Window currentFocusedWindow = getGlobalFocusedWindow();
                if (newFocusedWindow != null &&
                    newFocusedWindow != currentFocusedWindow)
                {
                    sendMessage(newFocusedWindow,
                                new WindowEvent(newFocusedWindow,
                                        WindowEvent.WINDOW_GAINED_FOCUS,
                                                currentFocusedWindow));
                    if (newFocusedWindow != getGlobalFocusedWindow()) {
                        // Focus change was rejected. Will happen if
                        // newFocusedWindow is not a focusable Window.
                        // Need to recover type-ahead, but don't bother
                        // restoring focus. That was done by the
                        // WINDOW_GAINED_FOCUS handler
                        dequeueKeyEvents(-1, newFocusOwner);
                        break;
                    }
                }
                if (!(newFocusOwner.isFocusable() && newFocusOwner.isShowing() &&
                    // Refuse focus on a disabled component if the focus event
                    // isn't of UNKNOWN reason (i.e. not a result of a direct request
                    // but traversal, activation or system generated).
                    (newFocusOwner.isEnabled() || cause.equals(CausedFocusEvent.Cause.UNKNOWN))))
                {
                    // we should not accept focus on such component, so reject it.
                    dequeueKeyEvents(-1, newFocusOwner);
                    if (KeyboardFocusManager.isAutoFocusTransferEnabled()) {
                        // If FOCUS_GAINED is for a disposed component (however
                        // it shouldn't happen) its toplevel parent is null. In this
                        // case we have to try to restore focus in the current focused
                        // window (for the details: 6607170).
                        if (newFocusedWindow == null) {
                            restoreFocus(fe, currentFocusedWindow);
                        } else {
                            restoreFocus(fe, newFocusedWindow);
                        }
                        setMostRecentFocusOwner(newFocusedWindow, null); // see: 8013773
                    }
                    break;
                }
                setGlobalFocusOwner(newFocusOwner);
                if (newFocusOwner != getGlobalFocusOwner()) {
                    // Focus change was rejected. Will happen if
                    // newFocusOwner is not focus traversable.
                    dequeueKeyEvents(-1, newFocusOwner);
                    if (KeyboardFocusManager.isAutoFocusTransferEnabled()) {
                        restoreFocus(fe, (Window)newFocusedWindow);
                    }
                    break;
                }
                if (!fe.isTemporary()) {
                    setGlobalPermanentFocusOwner(newFocusOwner);
                    if (newFocusOwner != getGlobalPermanentFocusOwner()) {
                        // Focus change was rejected. Unlikely, but possible.
                        dequeueKeyEvents(-1, newFocusOwner);
                        if (KeyboardFocusManager.isAutoFocusTransferEnabled()) {
                            restoreFocus(fe, (Window)newFocusedWindow);
                        }
                        break;
                    }
                }
                setNativeFocusOwner(getHeavyweight(newFocusOwner));
                Component realOppositeComponent = this.realOppositeComponentWR.get();
                if (realOppositeComponent != null &&
                    realOppositeComponent != fe.getOppositeComponent()) {
                    fe = new CausedFocusEvent(newFocusOwner,
                                        FocusEvent.FOCUS_GAINED,
                                        fe.isTemporary(),
                                        realOppositeComponent, cause);
                    ((AWTEvent) fe).isPosted = true;
                }
                return typeAheadAssertions(newFocusOwner, fe);
            }
            case FocusEvent.FOCUS_LOST: {
                FocusEvent fe = (FocusEvent)e;
                Component currentFocusOwner = getGlobalFocusOwner();
                if (currentFocusOwner == null) {
                    if (focusLog.isLoggable(PlatformLogger.Level.FINE))
                        focusLog.fine("Skipping {0} because focus owner is null", e);
                    break;
                }
                // Ignore cases where a Component loses focus to itself.
                // If we make a mistake because of retargeting, then the
                // FOCUS_GAINED handler will correct it.
                if (currentFocusOwner == fe.getOppositeComponent()) {
                    if (focusLog.isLoggable(PlatformLogger.Level.FINE))
                        focusLog.fine("Skipping {0} because current focus owner is equal to opposite", e);
                    break;
                }
                setGlobalFocusOwner(null);
                if (getGlobalFocusOwner() != null) {
                    // Focus change was rejected. Unlikely, but possible.
                    restoreFocus(currentFocusOwner, true);
                    break;
                }
                if (!fe.isTemporary()) {
                    setGlobalPermanentFocusOwner(null);
                    if (getGlobalPermanentFocusOwner() != null) {
                        // Focus change was rejected. Unlikely, but possible.
                        restoreFocus(currentFocusOwner, true);
                        break;
                    }
                } else {
                    Window owningWindow = currentFocusOwner.getContainingWindow();
                    if (owningWindow != null) {
                        owningWindow.setTemporaryLostComponent(currentFocusOwner);
                    }
                }
                setNativeFocusOwner(null);
                fe.setSource(currentFocusOwner);
                realOppositeComponentWR = (fe.getOppositeComponent() != null)
                    ? new WeakReference<Component>(currentFocusOwner)
                    : NULL_COMPONENT_WR;
                return typeAheadAssertions(currentFocusOwner, fe);
            }
            case WindowEvent.WINDOW_DEACTIVATED: {
                WindowEvent we = (WindowEvent)e;
                Window currentActiveWindow = getGlobalActiveWindow();
                if (currentActiveWindow == null) {
                    break;
                }
                if (currentActiveWindow != e.getSource()) {
                    // The event is lost in time.
                    // Allow listeners to precess the event but do not
                    // change any global states
                    break;
                }
                setGlobalActiveWindow(null);
                if (getGlobalActiveWindow() != null) {
                    // Activation change was rejected. Unlikely, but possible.
                    break;
                }
                we.setSource(currentActiveWindow);
                return typeAheadAssertions(currentActiveWindow, we);
            }
            case WindowEvent.WINDOW_LOST_FOCUS: {
                if (repostIfFollowsKeyEvents((WindowEvent)e)) {
                    break;
                }
                WindowEvent we = (WindowEvent)e;
                Window currentFocusedWindow = getGlobalFocusedWindow();
                Window losingFocusWindow = we.getWindow();
                Window activeWindow = getGlobalActiveWindow();
                Window oppositeWindow = we.getOppositeWindow();
                if (focusLog.isLoggable(PlatformLogger.Level.FINE))
                    focusLog.fine("Active {0}, Current focused {1}, losing focus {2} opposite {3}",
                                  activeWindow, currentFocusedWindow,
                                  losingFocusWindow, oppositeWindow);
                if (currentFocusedWindow == null) {
                    break;
                }
                // Special case -- if the native windowing system posts an
                // event claiming that the active Window has lost focus to the
                // focused Window, then discard the event. This is an artifact
                // of the native windowing system not knowing which Window is
                // really focused.
                if (inSendMessage == 0 && losingFocusWindow == activeWindow &&
                    oppositeWindow == currentFocusedWindow)
                {
                    break;
                }
                Component currentFocusOwner = getGlobalFocusOwner();
                if (currentFocusOwner != null) {
                    // The focus owner should always receive a FOCUS_LOST event
                    // before the Window is defocused.
                    Component oppositeComp = null;
                    if (oppositeWindow != null) {
                        oppositeComp = oppositeWindow.getTemporaryLostComponent();
                        if (oppositeComp == null) {
                            oppositeComp = oppositeWindow.getMostRecentFocusOwner();
                        }
                    }
                    if (oppositeComp == null) {
                        oppositeComp = oppositeWindow;
                    }
                    sendMessage(currentFocusOwner,
                                new CausedFocusEvent(currentFocusOwner,
                                               FocusEvent.FOCUS_LOST,
                                               true,
                                               oppositeComp, CausedFocusEvent.Cause.ACTIVATION));
                }
                setGlobalFocusedWindow(null);
                if (getGlobalFocusedWindow() != null) {
                    // Focus change was rejected. Unlikely, but possible.
                    restoreFocus(currentFocusedWindow, null, true);
                    break;
                }
                we.setSource(currentFocusedWindow);
                realOppositeWindowWR = (oppositeWindow != null)
                    ? new WeakReference<Window>(currentFocusedWindow)
                    : NULL_WINDOW_WR;
                typeAheadAssertions(currentFocusedWindow, we);
                if (oppositeWindow == null) {
                    // Then we need to deactive the active Window as well.
                    // No need to synthesize in other cases, because
                    // WINDOW_ACTIVATED will handle it if necessary.
                    sendMessage(activeWindow,
                                new WindowEvent(activeWindow,
                                                WindowEvent.WINDOW_DEACTIVATED,
                                                null));
                    if (getGlobalActiveWindow() != null) {
                        // Activation change was rejected. Unlikely,
                        // but possible.
                        restoreFocus(currentFocusedWindow, null, true);
                    }
                }
                break;
            }
            case KeyEvent.KEY_TYPED:
            case KeyEvent.KEY_PRESSED:
            case KeyEvent.KEY_RELEASED:
                return typeAheadAssertions(null, e);
            default:
                return false;
        }
        return true;
    }
    public boolean dispatchKeyEvent(KeyEvent e) {
        Component focusOwner = (((AWTEvent)e).isPosted) ? getFocusOwner() : e.getComponent();
        if (focusOwner != null && focusOwner.isShowing() && focusOwner.canBeFocusOwner()) {
            if (!e.isConsumed()) {
                Component comp = e.getComponent();
                if (comp != null && comp.isEnabled()) {
                    redispatchEvent(comp, e);
                }
            }
        }
        boolean stopPostProcessing = false;
        java.util.List<KeyEventPostProcessor> processors = getKeyEventPostProcessors();
        if (processors != null) {
            for (java.util.Iterator<KeyEventPostProcessor> iter = processors.iterator();
                 !stopPostProcessing && iter.hasNext(); )
            {
                stopPostProcessing = iter.next().
                            postProcessKeyEvent(e);
            }
        }
        if (!stopPostProcessing) {
            postProcessKeyEvent(e);
        }
        // Allow the peer to process KeyEvent
        Component source = e.getComponent();
        ComponentPeer peer = source.getPeer();
        if (peer == null || peer instanceof LightweightPeer) {
            // if focus owner is lightweight then its native container
            // processes event
            Container target = source.getNativeContainer();
            if (target != null) {
                peer = target.getPeer();
            }
        }
        if (peer != null) {
            peer.handleEvent(e);
        }
        return true;
    }
    public boolean postProcessKeyEvent(KeyEvent e) {
        if (!e.isConsumed()) {
            Component target = e.getComponent();
            Container p = (Container)
                (target instanceof Container ? target : target.getParent());
            if (p != null) {
                p.postProcessKeyEvent(e);
            }
        }
        return true;
    }
    private void pumpApprovedKeyEvents() {
        KeyEvent ke;
        do {
            ke = null;
            synchronized (this) {
                if (enqueuedKeyEvents.size() != 0) {
                    ke = enqueuedKeyEvents.getFirst();
                    if (typeAheadMarkers.size() != 0) {
                        TypeAheadMarker marker = typeAheadMarkers.getFirst();
                        // Fixed 5064013: may appears that the events have the same time
                        // if (ke.getWhen() >= marker.after) {
                        // The fix is rolled out.
                        if (ke.getWhen() > marker.after) {
                            ke = null;
                        }
                    }
                    if (ke != null) {
                        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                            focusLog.finer("Pumping approved event {0}", ke);
                        }
                        enqueuedKeyEvents.removeFirst();
                    }
                }
            }
            if (ke != null) {
                preDispatchKeyEvent(ke);
            }
        } while (ke != null);
    }
    void dumpMarkers() {
        if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
            focusLog.finest(">>> Markers dump, time: {0}", System.currentTimeMillis());
            synchronized (this) {
                if (typeAheadMarkers.size() != 0) {
                    Iterator<TypeAheadMarker> iter = typeAheadMarkers.iterator();
                    while (iter.hasNext()) {
                        TypeAheadMarker marker = iter.next();
                        focusLog.finest("    {0}", marker);
                    }
                }
            }
        }
    }
    private boolean typeAheadAssertions(Component target, AWTEvent e) {
        // Clear any pending events here as well as in the FOCUS_GAINED
        // handler. We need this call here in case a marker was removed in
        // response to a call to dequeueKeyEvents.
        pumpApprovedKeyEvents();
        switch (e.getID()) {
            case KeyEvent.KEY_TYPED:
            case KeyEvent.KEY_PRESSED:
            case KeyEvent.KEY_RELEASED: {
                KeyEvent ke = (KeyEvent)e;
                synchronized (this) {
                    if (e.isPosted && typeAheadMarkers.size() != 0) {
                        TypeAheadMarker marker = typeAheadMarkers.getFirst();
                        // Fixed 5064013: may appears that the events have the same time
                        // if (ke.getWhen() >= marker.after) {
                        // The fix is rolled out.
                        if (ke.getWhen() > marker.after) {
                            if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                                focusLog.finer("Storing event {0} because of marker {1}", ke, marker);
                            }
                            enqueuedKeyEvents.addLast(ke);
                            return true;
                        }
                    }
                }
                // KeyEvent was posted before focus change request
                return preDispatchKeyEvent(ke);
            }
            case FocusEvent.FOCUS_GAINED:
                if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                    focusLog.finest("Markers before FOCUS_GAINED on {0}", target);
                }
                dumpMarkers();
                // Search the marker list for the first marker tied to
                // the Component which just gained focus. Then remove
                // that marker, any markers which immediately follow
                // and are tied to the same component, and all markers
                // that preceed it. This handles the case where
                // multiple focus requests were made for the same
                // Component in a row and when we lost some of the
                // earlier requests. Since FOCUS_GAINED events will
                // not be generated for these additional requests, we
                // need to clear those markers too.
                synchronized (this) {
                    boolean found = false;
                    if (hasMarker(target)) {
                        for (Iterator<TypeAheadMarker> iter = typeAheadMarkers.iterator();
                             iter.hasNext(); )
                        {
                            if (iter.next().untilFocused == target) {
                                found = true;
                            } else if (found) {
                                break;
                            }
                            iter.remove();
                        }
                    } else {
                        // Exception condition - event without marker
                        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                            focusLog.finer("Event without marker {0}", e);
                        }
                    }
                }
                focusLog.finest("Markers after FOCUS_GAINED");
                dumpMarkers();
                redispatchEvent(target, e);
                // Now, dispatch any pending KeyEvents which have been
                // released because of the FOCUS_GAINED event so that we don't
                // have to wait for another event to be posted to the queue.
                pumpApprovedKeyEvents();
                return true;
            default:
                redispatchEvent(target, e);
                return true;
        }
    }
    private boolean hasMarker(Component comp) {
        for (Iterator<TypeAheadMarker> iter = typeAheadMarkers.iterator(); iter.hasNext(); ) {
            if (iter.next().untilFocused == comp) {
                return true;
            }
        }
        return false;
    }
    void clearMarkers() {
        synchronized(this) {
            typeAheadMarkers.clear();
        }
    }
    private boolean preDispatchKeyEvent(KeyEvent ke) {
        if (((AWTEvent) ke).isPosted) {
            Component focusOwner = getFocusOwner();
            ke.setSource(((focusOwner != null) ? focusOwner : getFocusedWindow()));
        }
        if (ke.getSource() == null) {
            return true;
        }
        // Explicitly set the key event timestamp here (not in Component.dispatchEventImpl):
        // - A key event is anyway passed to this method which starts its actual dispatching.
        // - If a key event is put to the type ahead queue, its time stamp should not be registered
        //   until its dispatching actually starts (by this method).
        EventQueue.setCurrentEventAndMostRecentTime(ke);
        if (KeyboardFocusManager.isProxyActive(ke)) {
            Component source = (Component)ke.getSource();
            Container target = source.getNativeContainer();
            if (target != null) {
                ComponentPeer peer = target.getPeer();
                if (peer != null) {
                    peer.handleEvent(ke);
                    ke.consume();
                }
            }
            return true;
        }
        java.util.List<KeyEventDispatcher> dispatchers = getKeyEventDispatchers();
        if (dispatchers != null) {
            for (java.util.Iterator<KeyEventDispatcher> iter = dispatchers.iterator();
                 iter.hasNext(); )
             {
                 if (iter.next().
                     dispatchKeyEvent(ke))
                 {
                     return true;
                 }
             }
        }
        return dispatchKeyEvent(ke);
    }
    private void consumeNextKeyTyped(KeyEvent e) {
        consumeNextKeyTyped = true;
    }
    private void consumeTraversalKey(KeyEvent e) {
        e.consume();
        consumeNextKeyTyped = (e.getID() == KeyEvent.KEY_PRESSED) &&
                              !e.isActionKey();
    }
    private boolean consumeProcessedKeyEvent(KeyEvent e) {
        if ((e.getID() == KeyEvent.KEY_TYPED) && consumeNextKeyTyped) {
            e.consume();
            consumeNextKeyTyped = false;
            return true;
        }
        return false;
    }
    public void processKeyEvent(Component focusedComponent, KeyEvent e) {
        // consume processed event if needed
        if (consumeProcessedKeyEvent(e)) {
            return;
        }
        // KEY_TYPED events cannot be focus traversal keys
        if (e.getID() == KeyEvent.KEY_TYPED) {
            return;
        }
        if (focusedComponent.getFocusTraversalKeysEnabled() &&
            !e.isConsumed())
        {
            AWTKeyStroke stroke = AWTKeyStroke.getAWTKeyStrokeForEvent(e),
                oppStroke = AWTKeyStroke.getAWTKeyStroke(stroke.getKeyCode(),
                                                 stroke.getModifiers(),
                                                 !stroke.isOnKeyRelease());
            Set<AWTKeyStroke> toTest;
            boolean contains, containsOpp;
            toTest = focusedComponent.getFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
            contains = toTest.contains(stroke);
            containsOpp = toTest.contains(oppStroke);
            if (contains || containsOpp) {
                consumeTraversalKey(e);
                if (contains) {
                    focusNextComponent(focusedComponent);
                }
                return;
            } else if (e.getID() == KeyEvent.KEY_PRESSED) {
                // Fix for 6637607: consumeNextKeyTyped should be reset.
                consumeNextKeyTyped = false;
            }
            toTest = focusedComponent.getFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);
            contains = toTest.contains(stroke);
            containsOpp = toTest.contains(oppStroke);
            if (contains || containsOpp) {
                consumeTraversalKey(e);
                if (contains) {
                    focusPreviousComponent(focusedComponent);
                }
                return;
            }
            toTest = focusedComponent.getFocusTraversalKeys(
                KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS);
            contains = toTest.contains(stroke);
            containsOpp = toTest.contains(oppStroke);
            if (contains || containsOpp) {
                consumeTraversalKey(e);
                if (contains) {
                    upFocusCycle(focusedComponent);
                }
                return;
            }
            if (!((focusedComponent instanceof Container) &&
                  ((Container)focusedComponent).isFocusCycleRoot())) {
                return;
            }
            toTest = focusedComponent.getFocusTraversalKeys(
                KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS);
            contains = toTest.contains(stroke);
            containsOpp = toTest.contains(oppStroke);
            if (contains || containsOpp) {
                consumeTraversalKey(e);
                if (contains) {
                    downFocusCycle((Container)focusedComponent);
                }
            }
        }
    }
    protected synchronized void enqueueKeyEvents(long after,
                                                 Component untilFocused) {
        if (untilFocused == null) {
            return;
        }
        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("Enqueue at {0} for {1}",
                       after, untilFocused);
        }
        int insertionIndex = 0,
            i = typeAheadMarkers.size();
        ListIterator<TypeAheadMarker> iter = typeAheadMarkers.listIterator(i);
        for (; i > 0; i--) {
            TypeAheadMarker marker = iter.previous();
            if (marker.after <= after) {
                insertionIndex = i;
                break;
            }
        }
        typeAheadMarkers.add(insertionIndex,
                             new TypeAheadMarker(after, untilFocused));
    }
    protected synchronized void dequeueKeyEvents(long after,
                                                 Component untilFocused) {
        if (untilFocused == null) {
            return;
        }
        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("Dequeue at {0} for {1}",
                       after, untilFocused);
        }
        TypeAheadMarker marker;
        ListIterator<TypeAheadMarker> iter = typeAheadMarkers.listIterator
            ((after >= 0) ? typeAheadMarkers.size() : 0);
        if (after < 0) {
            while (iter.hasNext()) {
                marker = iter.next();
                if (marker.untilFocused == untilFocused)
                {
                    iter.remove();
                    return;
                }
            }
        } else {
            while (iter.hasPrevious()) {
                marker = iter.previous();
                if (marker.untilFocused == untilFocused &&
                    marker.after == after)
                {
                    iter.remove();
                    return;
                }
            }
        }
    }
    protected synchronized void discardKeyEvents(Component comp) {
        if (comp == null) {
            return;
        }
        long start = -1;
        for (Iterator<TypeAheadMarker> iter = typeAheadMarkers.iterator(); iter.hasNext(); ) {
            TypeAheadMarker marker = iter.next();
            Component toTest = marker.untilFocused;
            boolean match = (toTest == comp);
            while (!match && toTest != null && !(toTest instanceof Window)) {
                toTest = toTest.getParent();
                match = (toTest == comp);
            }
            if (match) {
                if (start < 0) {
                    start = marker.after;
                }
                iter.remove();
            } else if (start >= 0) {
                purgeStampedEvents(start, marker.after);
                start = -1;
            }
        }
        purgeStampedEvents(start, -1);
    }
    // Notes:
    //   * must be called inside a synchronized block
    //   * if 'start' is < 0, then this function does nothing
    //   * if 'end' is < 0, then all KeyEvents from 'start' to the end of the
    //     queue will be removed
    private void purgeStampedEvents(long start, long end) {
        if (start < 0) {
            return;
        }
        for (Iterator<KeyEvent> iter = enqueuedKeyEvents.iterator(); iter.hasNext(); ) {
            KeyEvent ke = iter.next();
            long time = ke.getWhen();
            if (start < time && (end < 0 || time <= end)) {
                iter.remove();
            }
            if (end >= 0 && time > end) {
                break;
            }
        }
    }
    public void focusPreviousComponent(Component aComponent) {
        if (aComponent != null) {
            aComponent.transferFocusBackward();
        }
    }
    public void focusNextComponent(Component aComponent) {
        if (aComponent != null) {
            aComponent.transferFocus();
        }
    }
    public void upFocusCycle(Component aComponent) {
        if (aComponent != null) {
            aComponent.transferFocusUpCycle();
        }
    }
    public void downFocusCycle(Container aContainer) {
        if (aContainer != null && aContainer.isFocusCycleRoot()) {
            aContainer.transferFocusDownCycle();
        }
    }
}
