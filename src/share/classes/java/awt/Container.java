
package java.awt;

import java.awt.dnd.DropTarget;

import java.awt.event.*;

import java.awt.peer.ContainerPeer;
import java.awt.peer.ComponentPeer;
import java.awt.peer.LightweightPeer;

import java.beans.PropertyChangeListener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.lang.ref.WeakReference;
import java.security.AccessController;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;

import javax.accessibility.*;

import sun.util.logging.PlatformLogger;

import sun.awt.AppContext;
import sun.awt.AWTAccessor;
import sun.awt.CausedFocusEvent;
import sun.awt.PeerEvent;
import sun.awt.SunToolkit;

import sun.awt.dnd.SunDropTargetEvent;

import sun.java2d.pipe.Region;

import sun.security.action.GetBooleanAction;


public class Container extends Component {

    private static final PlatformLogger log = PlatformLogger.getLogger("java.awt.Container");
    private static final PlatformLogger eventLog = PlatformLogger.getLogger("java.awt.event.Container");

    private static final Component[] EMPTY_ARRAY = new Component[0];


    private java.util.List<Component> component = new ArrayList<>();


    LayoutManager layoutMgr;


    private LightweightDispatcher dispatcher;


    private transient FocusTraversalPolicy focusTraversalPolicy;


    private boolean focusCycleRoot = false;



    private boolean focusTraversalPolicyProvider;

    // keeps track of the threads that are printing this component
    private transient Set<Thread> printingThreads;
    // True if there is at least one thread that's printing this component
    private transient boolean printing = false;

    transient ContainerListener containerListener;


    transient int listeningChildren;
    transient int listeningBoundsChildren;
    transient int descendantsCount;


    transient Color preserveBackgroundColor = null;


    private static final long serialVersionUID = 4613797578919906343L;


    static final boolean INCLUDE_SELF = true;


    static final boolean SEARCH_HEAVYWEIGHTS = true;


    private transient int numOfHWComponents = 0;
    private transient int numOfLWComponents = 0;

    private static final PlatformLogger mixingLog = PlatformLogger.getLogger("java.awt.mixing.Container");


    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("ncomponents", Integer.TYPE),
        new ObjectStreamField("component", Component[].class),
        new ObjectStreamField("layoutMgr", LayoutManager.class),
        new ObjectStreamField("dispatcher", LightweightDispatcher.class),
        new ObjectStreamField("maxSize", Dimension.class),
        new ObjectStreamField("focusCycleRoot", Boolean.TYPE),
        new ObjectStreamField("containerSerializedDataVersion", Integer.TYPE),
        new ObjectStreamField("focusTraversalPolicyProvider", Boolean.TYPE),
    };

    static {

        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }

        AWTAccessor.setContainerAccessor(new AWTAccessor.ContainerAccessor() {
            @Override
            public void validateUnconditionally(Container cont) {
                cont.validateUnconditionally();
            }

            @Override
            public Component findComponentAt(Container cont, int x, int y,
                    boolean ignoreEnabled) {
                return cont.findComponentAt(x, y, ignoreEnabled);
            }
        });
    }


    private static native void initIDs();


    public Container() {
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    void initializeFocusTraversalKeys() {
        focusTraversalKeys = new Set[4];
    }


    public int getComponentCount() {
        return countComponents();
    }


    @Deprecated
    public int countComponents() {
        // This method is not synchronized under AWT tree lock.
        // Instead, the calling code is responsible for the
        // synchronization. See 6784816 for details.
        return component.size();
    }


    public Component getComponent(int n) {
        // This method is not synchronized under AWT tree lock.
        // Instead, the calling code is responsible for the
        // synchronization. See 6784816 for details.
        try {
            return component.get(n);
        } catch (IndexOutOfBoundsException z) {
            throw new ArrayIndexOutOfBoundsException("No such child: " + n);
        }
    }


    public Component[] getComponents() {
        // This method is not synchronized under AWT tree lock.
        // Instead, the calling code is responsible for the
        // synchronization. See 6784816 for details.
        return getComponents_NoClientCode();
    }

    // NOTE: This method may be called by privileged threads.
    //       This functionality is implemented in a package-private method
    //       to insure that it cannot be overridden by client subclasses.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    final Component[] getComponents_NoClientCode() {
        return component.toArray(EMPTY_ARRAY);
    }


    Component[] getComponentsSync() {
        synchronized (getTreeLock()) {
            return getComponents();
        }
    }


    public Insets getInsets() {
        return insets();
    }


    @Deprecated
    public Insets insets() {
        ComponentPeer peer = this.peer;
        if (peer instanceof ContainerPeer) {
            ContainerPeer cpeer = (ContainerPeer)peer;
            return (Insets)cpeer.getInsets().clone();
        }
        return new Insets(0, 0, 0, 0);
    }


    public Component add(Component comp) {
        addImpl(comp, null, -1);
        return comp;
    }


    public Component add(String name, Component comp) {
        addImpl(comp, name, -1);
        return comp;
    }


    public Component add(Component comp, int index) {
        addImpl(comp, null, index);
        return comp;
    }


    private void checkAddToSelf(Component comp){
        if (comp instanceof Container) {
            for (Container cn = this; cn != null; cn=cn.parent) {
                if (cn == comp) {
                    throw new IllegalArgumentException("adding container's parent to itself");
                }
            }
        }
    }


    private void checkNotAWindow(Component comp){
        if (comp instanceof Window) {
            throw new IllegalArgumentException("adding a window to a container");
        }
    }


    private void checkAdding(Component comp, int index) {
        checkTreeLock();

        GraphicsConfiguration thisGC = getGraphicsConfiguration();

        if (index > component.size() || index < 0) {
            throw new IllegalArgumentException("illegal component position");
        }
        if (comp.parent == this) {
            if (index == component.size()) {
                throw new IllegalArgumentException("illegal component position " +
                                                   index + " should be less then " + component.size());
            }
        }
        checkAddToSelf(comp);
        checkNotAWindow(comp);

        Window thisTopLevel = getContainingWindow();
        Window compTopLevel = comp.getContainingWindow();
        if (thisTopLevel != compTopLevel) {
            throw new IllegalArgumentException("component and container should be in the same top-level window");
        }
        if (thisGC != null) {
            comp.checkGD(thisGC.getDevice().getIDstring());
        }
    }


    private boolean removeDelicately(Component comp, Container newParent, int newIndex) {
        checkTreeLock();

        int index = getComponentZOrder(comp);
        boolean needRemoveNotify = isRemoveNotifyNeeded(comp, this, newParent);
        if (needRemoveNotify) {
            comp.removeNotify();
        }
        if (newParent != this) {
            if (layoutMgr != null) {
                layoutMgr.removeLayoutComponent(comp);
            }
            adjustListeningChildren(AWTEvent.HIERARCHY_EVENT_MASK,
                                    -comp.numListening(AWTEvent.HIERARCHY_EVENT_MASK));
            adjustListeningChildren(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK,
                                    -comp.numListening(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
            adjustDescendants(-(comp.countHierarchyMembers()));

            comp.parent = null;
            if (needRemoveNotify) {
                comp.setGraphicsConfiguration(null);
            }
            component.remove(index);

            invalidateIfValid();
        } else {
            // We should remove component and then
            // add it by the newIndex without newIndex decrement if even we shift components to the left
            // after remove. Consult the rules below:
            // 2->4: 012345 -> 013425, 2->5: 012345 -> 013452
            // 4->2: 012345 -> 014235
            component.remove(index);
            component.add(newIndex, comp);
        }
        if (comp.parent == null) { // was actually removed
            if (containerListener != null ||
                (eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0 ||
                Toolkit.enabledOnToolkit(AWTEvent.CONTAINER_EVENT_MASK)) {
                ContainerEvent e = new ContainerEvent(this,
                                                      ContainerEvent.COMPONENT_REMOVED,
                                                      comp);
                dispatchEvent(e);

            }
            comp.createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED, comp,
                                       this, HierarchyEvent.PARENT_CHANGED,
                                       Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
            if (peer != null && layoutMgr == null && isVisible()) {
                updateCursorImmediately();
            }
        }
        return needRemoveNotify;
    }


    boolean canContainFocusOwner(Component focusOwnerCandidate) {
        if (!(isEnabled() && isDisplayable()
              && isVisible() && isFocusable()))
        {
            return false;
        }
        if (isFocusCycleRoot()) {
            FocusTraversalPolicy policy = getFocusTraversalPolicy();
            if (policy instanceof DefaultFocusTraversalPolicy) {
                if (!((DefaultFocusTraversalPolicy)policy).accept(focusOwnerCandidate)) {
                    return false;
                }
            }
        }
        synchronized(getTreeLock()) {
            if (parent != null) {
                return parent.canContainFocusOwner(focusOwnerCandidate);
            }
        }
        return true;
    }


    final boolean hasHeavyweightDescendants() {
        checkTreeLock();
        return numOfHWComponents > 0;
    }


    final boolean hasLightweightDescendants() {
        checkTreeLock();
        return numOfLWComponents > 0;
    }


    Container getHeavyweightContainer() {
        checkTreeLock();
        if (peer != null && !(peer instanceof LightweightPeer)) {
            return this;
        } else {
            return getNativeContainer();
        }
    }


    private static boolean isRemoveNotifyNeeded(Component comp, Container oldContainer, Container newContainer) {
        if (oldContainer == null) { // Component didn't have parent - no removeNotify
            return false;
        }
        if (comp.peer == null) { // Component didn't have peer - no removeNotify
            return false;
        }
        if (newContainer.peer == null) {
            // Component has peer but new Container doesn't - call removeNotify
            return true;
        }

        // If component is lightweight non-Container or lightweight Container with all but heavyweight
        // children there is no need to call remove notify
        if (comp.isLightweight()) {
            boolean isContainer = comp instanceof Container;

            if (!isContainer || (isContainer && !((Container)comp).hasHeavyweightDescendants())) {
                return false;
            }
        }

        // If this point is reached, then the comp is either a HW or a LW container with HW descendants.

        // All three components have peers, check for peer change
        Container newNativeContainer = oldContainer.getHeavyweightContainer();
        Container oldNativeContainer = newContainer.getHeavyweightContainer();
        if (newNativeContainer != oldNativeContainer) {
            // Native containers change - check whether or not current platform supports
            // changing of widget hierarchy on native level without recreation.
            // The current implementation forbids reparenting of LW containers with HW descendants
            // into another native container w/o destroying the peers. Actually such an operation
            // is quite rare. If we ever need to save the peers, we'll have to slightly change the
            // addDelicately() method in order to handle such LW containers recursively, reparenting
            // each HW descendant independently.
            return !comp.peer.isReparentSupported();
        } else {
            return false;
        }
    }


    public void setComponentZOrder(Component comp, int index) {
         synchronized (getTreeLock()) {
             // Store parent because remove will clear it
             Container curParent = comp.parent;
             int oldZindex = getComponentZOrder(comp);

             if (curParent == this && index == oldZindex) {
                 return;
             }
             checkAdding(comp, index);

             boolean peerRecreated = (curParent != null) ?
                 curParent.removeDelicately(comp, this, index) : false;

             addDelicately(comp, curParent, index);

             // If the oldZindex == -1, the component gets inserted,
             // rather than it changes its z-order.
             if (!peerRecreated && oldZindex != -1) {
                 // The new 'index' cannot be == -1.
                 // It gets checked at the checkAdding() method.
                 // Therefore both oldZIndex and index denote
                 // some existing positions at this point and
                 // this is actually a Z-order changing.
                 comp.mixOnZOrderChanging(oldZindex, index);
             }
         }
    }


    private void reparentTraverse(ContainerPeer parentPeer, Container child) {
        checkTreeLock();

        for (int i = 0; i < child.getComponentCount(); i++) {
            Component comp = child.getComponent(i);
            if (comp.isLightweight()) {
                // If components is lightweight check if it is container
                // If it is container it might contain heavyweight children we need to reparent
                if (comp instanceof Container) {
                    reparentTraverse(parentPeer, (Container)comp);
                }
            } else {
                // Q: Need to update NativeInLightFixer?
                comp.getPeer().reparent(parentPeer);
            }
        }
    }


    private void reparentChild(Component comp) {
        checkTreeLock();
        if (comp == null) {
            return;
        }
        if (comp.isLightweight()) {
            // If component is lightweight container we need to reparent all its explicit  heavyweight children
            if (comp instanceof Container) {
                // Traverse component's tree till depth-first until encountering heavyweight component
                reparentTraverse((ContainerPeer)getPeer(), (Container)comp);
            }
        } else {
            comp.getPeer().reparent((ContainerPeer)getPeer());
        }
    }


    private void addDelicately(Component comp, Container curParent, int index) {
        checkTreeLock();

        // Check if moving between containers
        if (curParent != this) {
            //index == -1 means add to the end.
            if (index == -1) {
                component.add(comp);
            } else {
                component.add(index, comp);
            }
            comp.parent = this;
            comp.setGraphicsConfiguration(getGraphicsConfiguration());

            adjustListeningChildren(AWTEvent.HIERARCHY_EVENT_MASK,
                                    comp.numListening(AWTEvent.HIERARCHY_EVENT_MASK));
            adjustListeningChildren(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK,
                                    comp.numListening(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
            adjustDescendants(comp.countHierarchyMembers());
        } else {
            if (index < component.size()) {
                component.set(index, comp);
            }
        }

        invalidateIfValid();
        if (peer != null) {
            if (comp.peer == null) { // Remove notify was called or it didn't have peer - create new one
                comp.addNotify();
            } else { // Both container and child have peers, it means child peer should be reparented.
                // In both cases we need to reparent native widgets.
                Container newNativeContainer = getHeavyweightContainer();
                Container oldNativeContainer = curParent.getHeavyweightContainer();
                if (oldNativeContainer != newNativeContainer) {
                    // Native container changed - need to reparent native widgets
                    newNativeContainer.reparentChild(comp);
                }
                comp.updateZOrder();

                if (!comp.isLightweight() && isLightweight()) {
                    // If component is heavyweight and one of the containers is lightweight
                    // the location of the component should be fixed.
                    comp.relocateComponent();
                }
            }
        }
        if (curParent != this) {

            if (layoutMgr != null) {
                if (layoutMgr instanceof LayoutManager2) {
                    ((LayoutManager2)layoutMgr).addLayoutComponent(comp, null);
                } else {
                    layoutMgr.addLayoutComponent(null, comp);
                }
            }
            if (containerListener != null ||
                (eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0 ||
                Toolkit.enabledOnToolkit(AWTEvent.CONTAINER_EVENT_MASK)) {
                ContainerEvent e = new ContainerEvent(this,
                                                      ContainerEvent.COMPONENT_ADDED,
                                                      comp);
                dispatchEvent(e);
            }
            comp.createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED, comp,
                                       this, HierarchyEvent.PARENT_CHANGED,
                                       Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));

            // If component is focus owner or parent container of focus owner check that after reparenting
            // focus owner moved out if new container prohibit this kind of focus owner.
            if (comp.isFocusOwner() && !comp.canBeFocusOwnerRecursively()) {
                comp.transferFocus();
            } else if (comp instanceof Container) {
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner != null && isParentOf(focusOwner) && !focusOwner.canBeFocusOwnerRecursively()) {
                    focusOwner.transferFocus();
                }
            }
        } else {
            comp.createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED, comp,
                                       this, HierarchyEvent.HIERARCHY_CHANGED,
                                       Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
        }

        if (peer != null && layoutMgr == null && isVisible()) {
            updateCursorImmediately();
        }
    }


    public int getComponentZOrder(Component comp) {
        if (comp == null) {
            return -1;
        }
        synchronized(getTreeLock()) {
            // Quick check - container should be immediate parent of the component
            if (comp.parent != this) {
                return -1;
            }
            return component.indexOf(comp);
        }
    }


    public void add(Component comp, Object constraints) {
        addImpl(comp, constraints, -1);
    }


    public void add(Component comp, Object constraints, int index) {
       addImpl(comp, constraints, index);
    }


    protected void addImpl(Component comp, Object constraints, int index) {
        synchronized (getTreeLock()) {

            GraphicsConfiguration thisGC = this.getGraphicsConfiguration();

            if (index > component.size() || (index < 0 && index != -1)) {
                throw new IllegalArgumentException(
                          "illegal component position");
            }
            checkAddToSelf(comp);
            checkNotAWindow(comp);

            if (comp.parent != null) {
                comp.parent.remove(comp);
                if (index > component.size()) {
                    throw new IllegalArgumentException("illegal component position");
                }
            }
            if (thisGC != null) {
                comp.checkGD(thisGC.getDevice().getIDstring());
            }



            //index == -1 means add to the end.
            if (index == -1) {
                component.add(comp);
            } else {
                component.add(index, comp);
            }
            comp.parent = this;
            comp.setGraphicsConfiguration(thisGC);

            adjustListeningChildren(AWTEvent.HIERARCHY_EVENT_MASK,
                comp.numListening(AWTEvent.HIERARCHY_EVENT_MASK));
            adjustListeningChildren(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK,
                comp.numListening(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
            adjustDescendants(comp.countHierarchyMembers());

            invalidateIfValid();
            if (peer != null) {
                comp.addNotify();
            }


            if (layoutMgr != null) {
                if (layoutMgr instanceof LayoutManager2) {
                    ((LayoutManager2)layoutMgr).addLayoutComponent(comp, constraints);
                } else if (constraints instanceof String) {
                    layoutMgr.addLayoutComponent((String)constraints, comp);
                }
            }
            if (containerListener != null ||
                (eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0 ||
                Toolkit.enabledOnToolkit(AWTEvent.CONTAINER_EVENT_MASK)) {
                ContainerEvent e = new ContainerEvent(this,
                                     ContainerEvent.COMPONENT_ADDED,
                                     comp);
                dispatchEvent(e);
            }

            comp.createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED, comp,
                                       this, HierarchyEvent.PARENT_CHANGED,
                                       Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
            if (peer != null && layoutMgr == null && isVisible()) {
                updateCursorImmediately();
            }
        }
    }

    @Override
    boolean updateGraphicsData(GraphicsConfiguration gc) {
        checkTreeLock();

        boolean ret = super.updateGraphicsData(gc);

        for (Component comp : component) {
            if (comp != null) {
                ret |= comp.updateGraphicsData(gc);
            }
        }
        return ret;
    }


    void checkGD(String stringID) {
        for (Component comp : component) {
            if (comp != null) {
                comp.checkGD(stringID);
            }
        }
    }


    public void remove(int index) {
        synchronized (getTreeLock()) {
            if (index < 0  || index >= component.size()) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            Component comp = component.get(index);
            if (peer != null) {
                comp.removeNotify();
            }
            if (layoutMgr != null) {
                layoutMgr.removeLayoutComponent(comp);
            }

            adjustListeningChildren(AWTEvent.HIERARCHY_EVENT_MASK,
                -comp.numListening(AWTEvent.HIERARCHY_EVENT_MASK));
            adjustListeningChildren(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK,
                -comp.numListening(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
            adjustDescendants(-(comp.countHierarchyMembers()));

            comp.parent = null;
            component.remove(index);
            comp.setGraphicsConfiguration(null);

            invalidateIfValid();
            if (containerListener != null ||
                (eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0 ||
                Toolkit.enabledOnToolkit(AWTEvent.CONTAINER_EVENT_MASK)) {
                ContainerEvent e = new ContainerEvent(this,
                                     ContainerEvent.COMPONENT_REMOVED,
                                     comp);
                dispatchEvent(e);
            }

            comp.createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED, comp,
                                       this, HierarchyEvent.PARENT_CHANGED,
                                       Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
            if (peer != null && layoutMgr == null && isVisible()) {
                updateCursorImmediately();
            }
        }
    }


    public void remove(Component comp) {
        synchronized (getTreeLock()) {
            if (comp.parent == this)  {
                int index = component.indexOf(comp);
                if (index >= 0) {
                    remove(index);
                }
            }
        }
    }


    public void removeAll() {
        synchronized (getTreeLock()) {
            adjustListeningChildren(AWTEvent.HIERARCHY_EVENT_MASK,
                                    -listeningChildren);
            adjustListeningChildren(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK,
                                    -listeningBoundsChildren);
            adjustDescendants(-descendantsCount);

            while (!component.isEmpty()) {
                Component comp = component.remove(component.size()-1);

                if (peer != null) {
                    comp.removeNotify();
                }
                if (layoutMgr != null) {
                    layoutMgr.removeLayoutComponent(comp);
                }
                comp.parent = null;
                comp.setGraphicsConfiguration(null);
                if (containerListener != null ||
                   (eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0 ||
                    Toolkit.enabledOnToolkit(AWTEvent.CONTAINER_EVENT_MASK)) {
                    ContainerEvent e = new ContainerEvent(this,
                                     ContainerEvent.COMPONENT_REMOVED,
                                     comp);
                    dispatchEvent(e);
                }

                comp.createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED,
                                           comp, this,
                                           HierarchyEvent.PARENT_CHANGED,
                                           Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
            }
            if (peer != null && layoutMgr == null && isVisible()) {
                updateCursorImmediately();
            }
            invalidateIfValid();
        }
    }

    // Should only be called while holding tree lock
    int numListening(long mask) {
        int superListening = super.numListening(mask);

        if (mask == AWTEvent.HIERARCHY_EVENT_MASK) {
            if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
                // Verify listeningChildren is correct
                int sum = 0;
                for (Component comp : component) {
                    sum += comp.numListening(mask);
                }
                if (listeningChildren != sum) {
                    eventLog.fine("Assertion (listeningChildren == sum) failed");
                }
            }
            return listeningChildren + superListening;
        } else if (mask == AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) {
            if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
                // Verify listeningBoundsChildren is correct
                int sum = 0;
                for (Component comp : component) {
                    sum += comp.numListening(mask);
                }
                if (listeningBoundsChildren != sum) {
                    eventLog.fine("Assertion (listeningBoundsChildren == sum) failed");
                }
            }
            return listeningBoundsChildren + superListening;
        } else {
            // assert false;
            if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
                eventLog.fine("This code must never be reached");
            }
            return superListening;
        }
    }

    // Should only be called while holding tree lock
    void adjustListeningChildren(long mask, int num) {
        if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
            boolean toAssert = (mask == AWTEvent.HIERARCHY_EVENT_MASK ||
                                mask == AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK ||
                                mask == (AWTEvent.HIERARCHY_EVENT_MASK |
                                         AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
            if (!toAssert) {
                eventLog.fine("Assertion failed");
            }
        }

        if (num == 0)
            return;

        if ((mask & AWTEvent.HIERARCHY_EVENT_MASK) != 0) {
            listeningChildren += num;
        }
        if ((mask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) != 0) {
            listeningBoundsChildren += num;
        }

        adjustListeningChildrenOnParent(mask, num);
    }

    // Should only be called while holding tree lock
    void adjustDescendants(int num) {
        if (num == 0)
            return;

        descendantsCount += num;
        adjustDecendantsOnParent(num);
    }

    // Should only be called while holding tree lock
    void adjustDecendantsOnParent(int num) {
        if (parent != null) {
            parent.adjustDescendants(num);
        }
    }

    // Should only be called while holding tree lock
    int countHierarchyMembers() {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            // Verify descendantsCount is correct
            int sum = 0;
            for (Component comp : component) {
                sum += comp.countHierarchyMembers();
            }
            if (descendantsCount != sum) {
                log.fine("Assertion (descendantsCount == sum) failed");
            }
        }
        return descendantsCount + 1;
    }

    private int getListenersCount(int id, boolean enabledOnToolkit) {
        checkTreeLock();
        if (enabledOnToolkit) {
            return descendantsCount;
        }
        switch (id) {
          case HierarchyEvent.HIERARCHY_CHANGED:
            return listeningChildren;
          case HierarchyEvent.ANCESTOR_MOVED:
          case HierarchyEvent.ANCESTOR_RESIZED:
            return listeningBoundsChildren;
          default:
            return 0;
        }
    }

    final int createHierarchyEvents(int id, Component changed,
        Container changedParent, long changeFlags, boolean enabledOnToolkit)
    {
        checkTreeLock();
        int listeners = getListenersCount(id, enabledOnToolkit);

        for (int count = listeners, i = 0; count > 0; i++) {
            count -= component.get(i).createHierarchyEvents(id, changed,
                changedParent, changeFlags, enabledOnToolkit);
        }
        return listeners +
            super.createHierarchyEvents(id, changed, changedParent,
                                        changeFlags, enabledOnToolkit);
    }

    final void createChildHierarchyEvents(int id, long changeFlags,
        boolean enabledOnToolkit)
    {
        checkTreeLock();
        if (component.isEmpty()) {
            return;
        }
        int listeners = getListenersCount(id, enabledOnToolkit);

        for (int count = listeners, i = 0; count > 0; i++) {
            count -= component.get(i).createHierarchyEvents(id, this, parent,
                changeFlags, enabledOnToolkit);
        }
    }


    public LayoutManager getLayout() {
        return layoutMgr;
    }


    public void setLayout(LayoutManager mgr) {
        layoutMgr = mgr;
        invalidateIfValid();
    }


    public void doLayout() {
        layout();
    }


    @Deprecated
    public void layout() {
        LayoutManager layoutMgr = this.layoutMgr;
        if (layoutMgr != null) {
            layoutMgr.layoutContainer(this);
        }
    }


    public boolean isValidateRoot() {
        return false;
    }

    private static final boolean isJavaAwtSmartInvalidate;
    static {
        // Don't lazy-read because every app uses invalidate()
        isJavaAwtSmartInvalidate = AccessController.doPrivileged(
                new GetBooleanAction("java.awt.smartInvalidate"));
    }


    @Override
    void invalidateParent() {
        if (!isJavaAwtSmartInvalidate || !isValidateRoot()) {
            super.invalidateParent();
        }
    }


    @Override
    public void invalidate() {
        LayoutManager layoutMgr = this.layoutMgr;
        if (layoutMgr instanceof LayoutManager2) {
            LayoutManager2 lm = (LayoutManager2) layoutMgr;
            lm.invalidateLayout(this);
        }
        super.invalidate();
    }


    public void validate() {
        boolean updateCur = false;
        synchronized (getTreeLock()) {
            if ((!isValid() || descendUnconditionallyWhenValidating)
                    && peer != null)
            {
                ContainerPeer p = null;
                if (peer instanceof ContainerPeer) {
                    p = (ContainerPeer) peer;
                }
                if (p != null) {
                    p.beginValidate();
                }
                validateTree();
                if (p != null) {
                    p.endValidate();
                    // Avoid updating cursor if this is an internal call.
                    // See validateUnconditionally() for details.
                    if (!descendUnconditionallyWhenValidating) {
                        updateCur = isVisible();
                    }
                }
            }
        }
        if (updateCur) {
            updateCursorImmediately();
        }
    }


    private static boolean descendUnconditionallyWhenValidating = false;


    final void validateUnconditionally() {
        boolean updateCur = false;
        synchronized (getTreeLock()) {
            descendUnconditionallyWhenValidating = true;

            validate();
            if (peer instanceof ContainerPeer) {
                updateCur = isVisible();
            }

            descendUnconditionallyWhenValidating = false;
        }
        if (updateCur) {
            updateCursorImmediately();
        }
    }


    protected void validateTree() {
        checkTreeLock();
        if (!isValid() || descendUnconditionallyWhenValidating) {
            if (peer instanceof ContainerPeer) {
                ((ContainerPeer)peer).beginLayout();
            }
            if (!isValid()) {
                doLayout();
            }
            for (int i = 0; i < component.size(); i++) {
                Component comp = component.get(i);
                if (   (comp instanceof Container)
                       && !(comp instanceof Window)
                       && (!comp.isValid() ||
                           descendUnconditionallyWhenValidating))
                {
                    ((Container)comp).validateTree();
                } else {
                    comp.validate();
                }
            }
            if (peer instanceof ContainerPeer) {
                ((ContainerPeer)peer).endLayout();
            }
        }
        super.validate();
    }


    void invalidateTree() {
        synchronized (getTreeLock()) {
            for (int i = 0; i < component.size(); i++) {
                Component comp = component.get(i);
                if (comp instanceof Container) {
                    ((Container)comp).invalidateTree();
                }
                else {
                    comp.invalidateIfValid();
                }
            }
            invalidateIfValid();
        }
    }


    public void setFont(Font f) {
        boolean shouldinvalidate = false;

        Font oldfont = getFont();
        super.setFont(f);
        Font newfont = getFont();
        if (newfont != oldfont && (oldfont == null ||
                                   !oldfont.equals(newfont))) {
            invalidateTree();
        }
    }


    public Dimension getPreferredSize() {
        return preferredSize();
    }


    @Deprecated
    public Dimension preferredSize() {

        Dimension dim = prefSize;
        if (dim == null || !(isPreferredSizeSet() || isValid())) {
            synchronized (getTreeLock()) {
                prefSize = (layoutMgr != null) ?
                    layoutMgr.preferredLayoutSize(this) :
                    super.preferredSize();
                dim = prefSize;
            }
        }
        if (dim != null){
            return new Dimension(dim);
        }
        else{
            return dim;
        }
    }


    public Dimension getMinimumSize() {
        return minimumSize();
    }


    @Deprecated
    public Dimension minimumSize() {

        Dimension dim = minSize;
        if (dim == null || !(isMinimumSizeSet() || isValid())) {
            synchronized (getTreeLock()) {
                minSize = (layoutMgr != null) ?
                    layoutMgr.minimumLayoutSize(this) :
                    super.minimumSize();
                dim = minSize;
            }
        }
        if (dim != null){
            return new Dimension(dim);
        }
        else{
            return dim;
        }
    }


    public Dimension getMaximumSize() {

        Dimension dim = maxSize;
        if (dim == null || !(isMaximumSizeSet() || isValid())) {
            synchronized (getTreeLock()) {
               if (layoutMgr instanceof LayoutManager2) {
                    LayoutManager2 lm = (LayoutManager2) layoutMgr;
                    maxSize = lm.maximumLayoutSize(this);
               } else {
                    maxSize = super.getMaximumSize();
               }
               dim = maxSize;
            }
        }
        if (dim != null){
            return new Dimension(dim);
        }
        else{
            return dim;
        }
    }


    public float getAlignmentX() {
        float xAlign;
        if (layoutMgr instanceof LayoutManager2) {
            synchronized (getTreeLock()) {
                LayoutManager2 lm = (LayoutManager2) layoutMgr;
                xAlign = lm.getLayoutAlignmentX(this);
            }
        } else {
            xAlign = super.getAlignmentX();
        }
        return xAlign;
    }


    public float getAlignmentY() {
        float yAlign;
        if (layoutMgr instanceof LayoutManager2) {
            synchronized (getTreeLock()) {
                LayoutManager2 lm = (LayoutManager2) layoutMgr;
                yAlign = lm.getLayoutAlignmentY(this);
            }
        } else {
            yAlign = super.getAlignmentY();
        }
        return yAlign;
    }


    public void paint(Graphics g) {
        if (isShowing()) {
            synchronized (getObjectLock()) {
                if (printing) {
                    if (printingThreads.contains(Thread.currentThread())) {
                        return;
                    }
                }
            }

            // The container is showing on screen and
            // this paint() is not called from print().
            // Paint self and forward the paint to lightweight subcomponents.

            // super.paint(); -- Don't bother, since it's a NOP.

            GraphicsCallback.PaintCallback.getInstance().
                runComponents(getComponentsSync(), g, GraphicsCallback.LIGHTWEIGHTS);
        }
    }


    public void update(Graphics g) {
        if (isShowing()) {
            if (! (peer instanceof LightweightPeer)) {
                g.clearRect(0, 0, width, height);
            }
            paint(g);
        }
    }


    public void print(Graphics g) {
        if (isShowing()) {
            Thread t = Thread.currentThread();
            try {
                synchronized (getObjectLock()) {
                    if (printingThreads == null) {
                        printingThreads = new HashSet<>();
                    }
                    printingThreads.add(t);
                    printing = true;
                }
                super.print(g);  // By default, Component.print() calls paint()
            } finally {
                synchronized (getObjectLock()) {
                    printingThreads.remove(t);
                    printing = !printingThreads.isEmpty();
                }
            }

            GraphicsCallback.PrintCallback.getInstance().
                runComponents(getComponentsSync(), g, GraphicsCallback.LIGHTWEIGHTS);
        }
    }


    public void paintComponents(Graphics g) {
        if (isShowing()) {
            GraphicsCallback.PaintAllCallback.getInstance().
                runComponents(getComponentsSync(), g, GraphicsCallback.TWO_PASSES);
        }
    }


    void lightweightPaint(Graphics g) {
        super.lightweightPaint(g);
        paintHeavyweightComponents(g);
    }


    void paintHeavyweightComponents(Graphics g) {
        if (isShowing()) {
            GraphicsCallback.PaintHeavyweightComponentsCallback.getInstance().
                runComponents(getComponentsSync(), g,
                              GraphicsCallback.LIGHTWEIGHTS | GraphicsCallback.HEAVYWEIGHTS);
        }
    }


    public void printComponents(Graphics g) {
        if (isShowing()) {
            GraphicsCallback.PrintAllCallback.getInstance().
                runComponents(getComponentsSync(), g, GraphicsCallback.TWO_PASSES);
        }
    }


    void lightweightPrint(Graphics g) {
        super.lightweightPrint(g);
        printHeavyweightComponents(g);
    }


    void printHeavyweightComponents(Graphics g) {
        if (isShowing()) {
            GraphicsCallback.PrintHeavyweightComponentsCallback.getInstance().
                runComponents(getComponentsSync(), g,
                              GraphicsCallback.LIGHTWEIGHTS | GraphicsCallback.HEAVYWEIGHTS);
        }
    }


    public synchronized void addContainerListener(ContainerListener l) {
        if (l == null) {
            return;
        }
        containerListener = AWTEventMulticaster.add(containerListener, l);
        newEventsOnly = true;
    }


    public synchronized void removeContainerListener(ContainerListener l) {
        if (l == null) {
            return;
        }
        containerListener = AWTEventMulticaster.remove(containerListener, l);
    }


    public synchronized ContainerListener[] getContainerListeners() {
        return getListeners(ContainerListener.class);
    }


    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener l = null;
        if  (listenerType == ContainerListener.class) {
            l = containerListener;
        } else {
            return super.getListeners(listenerType);
        }
        return AWTEventMulticaster.getListeners(l, listenerType);
    }

    // REMIND: remove when filtering is done at lower level
    boolean eventEnabled(AWTEvent e) {
        int id = e.getID();

        if (id == ContainerEvent.COMPONENT_ADDED ||
            id == ContainerEvent.COMPONENT_REMOVED) {
            if ((eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0 ||
                containerListener != null) {
                return true;
            }
            return false;
        }
        return super.eventEnabled(e);
    }


    protected void processEvent(AWTEvent e) {
        if (e instanceof ContainerEvent) {
            processContainerEvent((ContainerEvent)e);
            return;
        }
        super.processEvent(e);
    }


    protected void processContainerEvent(ContainerEvent e) {
        ContainerListener listener = containerListener;
        if (listener != null) {
            switch(e.getID()) {
              case ContainerEvent.COMPONENT_ADDED:
                listener.componentAdded(e);
                break;
              case ContainerEvent.COMPONENT_REMOVED:
                listener.componentRemoved(e);
                break;
            }
        }
    }


    void dispatchEventImpl(AWTEvent e) {
        if ((dispatcher != null) && dispatcher.dispatchEvent(e)) {
            // event was sent to a lightweight component.  The
            // native-produced event sent to the native container
            // must be properly disposed of by the peer, so it
            // gets forwarded.  If the native host has been removed
            // as a result of the sending the lightweight event,
            // the peer reference will be null.
            e.consume();
            if (peer != null) {
                peer.handleEvent(e);
            }
            return;
        }

        super.dispatchEventImpl(e);

        synchronized (getTreeLock()) {
            switch (e.getID()) {
              case ComponentEvent.COMPONENT_RESIZED:
                createChildHierarchyEvents(HierarchyEvent.ANCESTOR_RESIZED, 0,
                                           Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
                break;
              case ComponentEvent.COMPONENT_MOVED:
                createChildHierarchyEvents(HierarchyEvent.ANCESTOR_MOVED, 0,
                                       Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
                break;
              default:
                break;
            }
        }
    }


    void dispatchEventToSelf(AWTEvent e) {
        super.dispatchEventImpl(e);
    }


    Component getMouseEventTarget(int x, int y, boolean includeSelf) {
        return getMouseEventTarget(x, y, includeSelf,
                                   MouseEventTargetFilter.FILTER,
                                   !SEARCH_HEAVYWEIGHTS);
    }


    Component getDropTargetEventTarget(int x, int y, boolean includeSelf) {
        return getMouseEventTarget(x, y, includeSelf,
                                   DropTargetEventTargetFilter.FILTER,
                                   SEARCH_HEAVYWEIGHTS);
    }


    private Component getMouseEventTarget(int x, int y, boolean includeSelf,
                                          EventTargetFilter filter,
                                          boolean searchHeavyweights) {
        Component comp = null;
        if (searchHeavyweights) {
            comp = getMouseEventTargetImpl(x, y, includeSelf, filter,
                                           SEARCH_HEAVYWEIGHTS,
                                           searchHeavyweights);
        }

        if (comp == null || comp == this) {
            comp = getMouseEventTargetImpl(x, y, includeSelf, filter,
                                           !SEARCH_HEAVYWEIGHTS,
                                           searchHeavyweights);
        }

        return comp;
    }


    private Component getMouseEventTargetImpl(int x, int y, boolean includeSelf,
                                         EventTargetFilter filter,
                                         boolean searchHeavyweightChildren,
                                         boolean searchHeavyweightDescendants) {
        synchronized (getTreeLock()) {

            for (int i = 0; i < component.size(); i++) {
                Component comp = component.get(i);
                if (comp != null && comp.visible &&
                    ((!searchHeavyweightChildren &&
                      comp.peer instanceof LightweightPeer) ||
                     (searchHeavyweightChildren &&
                      !(comp.peer instanceof LightweightPeer))) &&
                    comp.contains(x - comp.x, y - comp.y)) {

                    // found a component that intersects the point, see if there
                    // is a deeper possibility.
                    if (comp instanceof Container) {
                        Container child = (Container) comp;
                        Component deeper = child.getMouseEventTarget(
                                x - child.x,
                                y - child.y,
                                includeSelf,
                                filter,
                                searchHeavyweightDescendants);
                        if (deeper != null) {
                            return deeper;
                        }
                    } else {
                        if (filter.accept(comp)) {
                            // there isn't a deeper target, but this component
                            // is a target
                            return comp;
                        }
                    }
                }
            }

            boolean isPeerOK;
            boolean isMouseOverMe;

            isPeerOK = (peer instanceof LightweightPeer) || includeSelf;
            isMouseOverMe = contains(x,y);

            // didn't find a child target, return this component if it's
            // a possible target
            if (isMouseOverMe && isPeerOK && filter.accept(this)) {
                return this;
            }
            // no possible target
            return null;
        }
    }

    static interface EventTargetFilter {
        boolean accept(final Component comp);
    }

    static class MouseEventTargetFilter implements EventTargetFilter {
        static final EventTargetFilter FILTER = new MouseEventTargetFilter();

        private MouseEventTargetFilter() {}

        public boolean accept(final Component comp) {
            return (comp.eventMask & AWTEvent.MOUSE_MOTION_EVENT_MASK) != 0
                || (comp.eventMask & AWTEvent.MOUSE_EVENT_MASK) != 0
                || (comp.eventMask & AWTEvent.MOUSE_WHEEL_EVENT_MASK) != 0
                || comp.mouseListener != null
                || comp.mouseMotionListener != null
                || comp.mouseWheelListener != null;
        }
    }

    static class DropTargetEventTargetFilter implements EventTargetFilter {
        static final EventTargetFilter FILTER = new DropTargetEventTargetFilter();

        private DropTargetEventTargetFilter() {}

        public boolean accept(final Component comp) {
            DropTarget dt = comp.getDropTarget();
            return dt != null && dt.isActive();
        }
    }


    void proxyEnableEvents(long events) {
        if (peer instanceof LightweightPeer) {
            // this container is lightweight.... continue sending it
            // upward.
            if (parent != null) {
                parent.proxyEnableEvents(events);
            }
        } else {
            // This is a native container, so it needs to host
            // one of it's children.  If this function is called before
            // a peer has been created we don't yet have a dispatcher
            // because it has not yet been determined if this instance
            // is lightweight.
            if (dispatcher != null) {
                dispatcher.enableEvents(events);
            }
        }
    }


    @Deprecated
    public void deliverEvent(Event e) {
        Component comp = getComponentAt(e.x, e.y);
        if ((comp != null) && (comp != this)) {
            e.translate(-comp.x, -comp.y);
            comp.deliverEvent(e);
        } else {
            postEvent(e);
        }
    }


    public Component getComponentAt(int x, int y) {
        return locate(x, y);
    }


    @Deprecated
    public Component locate(int x, int y) {
        if (!contains(x, y)) {
            return null;
        }
        Component lightweight = null;
        synchronized (getTreeLock()) {
            // Optimized version of two passes:
            // see comment in sun.awt.SunGraphicsCallback
            for (final Component comp : component) {
                if (comp.contains(x - comp.x, y - comp.y)) {
                    if (!comp.isLightweight()) {
                        // return heavyweight component as soon as possible
                        return comp;
                    }
                    if (lightweight == null) {
                        // save and return later the first lightweight component
                        lightweight = comp;
                    }
                }
            }
        }
        return lightweight != null ? lightweight : this;
    }


    public Component getComponentAt(Point p) {
        return getComponentAt(p.x, p.y);
    }


    public Point getMousePosition(boolean allowChildren) throws HeadlessException {
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
            if (isSameOrAncestorOf(inTheSameWindow, allowChildren)) {
                return  pointRelativeToComponent(pi.getLocation());
            }
            return null;
        }
    }

    boolean isSameOrAncestorOf(Component comp, boolean allowChildren) {
        return this == comp || (allowChildren && isParentOf(comp));
    }


    public Component findComponentAt(int x, int y) {
        return findComponentAt(x, y, true);
    }


    final Component findComponentAt(int x, int y, boolean ignoreEnabled) {
        synchronized (getTreeLock()) {
            if (isRecursivelyVisible()){
                return findComponentAtImpl(x, y, ignoreEnabled);
            }
        }
        return null;
    }

    final Component findComponentAtImpl(int x, int y, boolean ignoreEnabled) {
        // checkTreeLock(); commented for a performance reason

        if (!(contains(x, y) && visible && (ignoreEnabled || enabled))) {
            return null;
        }
        Component lightweight = null;
        // Optimized version of two passes:
        // see comment in sun.awt.SunGraphicsCallback
        for (final Component comp : component) {
            final int x1 = x - comp.x;
            final int y1 = y - comp.y;
            if (!comp.contains(x1, y1)) {
                continue; // fast path
            }
            if (!comp.isLightweight()) {
                final Component child = getChildAt(comp, x1, y1, ignoreEnabled);
                if (child != null) {
                    // return heavyweight component as soon as possible
                    return child;
                }
            } else {
                if (lightweight == null) {
                    // save and return later the first lightweight component
                    lightweight = getChildAt(comp, x1, y1, ignoreEnabled);
                }
            }
        }
        return lightweight != null ? lightweight : this;
    }


    private static Component getChildAt(Component comp, int x, int y,
                                        boolean ignoreEnabled) {
        if (comp instanceof Container) {
            comp = ((Container) comp).findComponentAtImpl(x, y,
                                                          ignoreEnabled);
        } else {
            comp = comp.getComponentAt(x, y);
        }
        if (comp != null && comp.visible &&
                (ignoreEnabled || comp.enabled)) {
            return comp;
        }
        return null;
    }


    public Component findComponentAt(Point p) {
        return findComponentAt(p.x, p.y);
    }


    public void addNotify() {
        synchronized (getTreeLock()) {
            // addNotify() on the children may cause proxy event enabling
            // on this instance, so we first call super.addNotify() and
            // possibly create an lightweight event dispatcher before calling
            // addNotify() on the children which may be lightweight.
            super.addNotify();
            if (! (peer instanceof LightweightPeer)) {
                dispatcher = new LightweightDispatcher(this);
            }

            // We shouldn't use iterator because of the Swing menu
            // implementation specifics:
            // the menu is being assigned as a child to JLayeredPane
            // instead of particular component so always affect
            // collection of component if menu is becoming shown or hidden.
            for (int i = 0; i < component.size(); i++) {
                component.get(i).addNotify();
            }
        }
    }


    public void removeNotify() {
        synchronized (getTreeLock()) {
            // We shouldn't use iterator because of the Swing menu
            // implementation specifics:
            // the menu is being assigned as a child to JLayeredPane
            // instead of particular component so always affect
            // collection of component if menu is becoming shown or hidden.
            for (int i = component.size()-1 ; i >= 0 ; i--) {
                Component comp = component.get(i);
                if (comp != null) {
                    // Fix for 6607170.
                    // We want to suppress focus change on disposal
                    // of the focused component. But because of focus
                    // is asynchronous, we should suppress focus change
                    // on every component in case it receives native focus
                    // in the process of disposal.
                    comp.setAutoFocusTransferOnDisposal(false);
                    comp.removeNotify();
                    comp.setAutoFocusTransferOnDisposal(true);
                 }
             }
            // If some of the children had focus before disposal then it still has.
            // Auto-transfer focus to the next (or previous) component if auto-transfer
            // is enabled.
            if (containsFocus() && KeyboardFocusManager.isAutoFocusTransferEnabledFor(this)) {
                if (!transferFocus(false)) {
                    transferFocusBackward(true);
                }
            }
            if ( dispatcher != null ) {
                dispatcher.dispose();
                dispatcher = null;
            }
            super.removeNotify();
        }
    }


    public boolean isAncestorOf(Component c) {
        Container p;
        if (c == null || ((p = c.getParent()) == null)) {
            return false;
        }
        while (p != null) {
            if (p == this) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }



    transient Component modalComp;
    transient AppContext modalAppContext;

    private void startLWModal() {
        // Store the app context on which this component is being shown.
        // Event dispatch thread of this app context will be sleeping until
        // we wake it by any event from hideAndDisposeHandler().
        modalAppContext = AppContext.getAppContext();

        // keep the KeyEvents from being dispatched
        // until the focus has been transfered
        long time = Toolkit.getEventQueue().getMostRecentKeyEventTime();
        Component predictedFocusOwner = (Component.isInstanceOf(this, "javax.swing.JInternalFrame")) ? ((javax.swing.JInternalFrame)(this)).getMostRecentFocusOwner() : null;
        if (predictedFocusOwner != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                enqueueKeyEvents(time, predictedFocusOwner);
        }
        // We have two mechanisms for blocking: 1. If we're on the
        // EventDispatchThread, start a new event pump. 2. If we're
        // on any other thread, call wait() on the treelock.
        final Container nativeContainer;
        synchronized (getTreeLock()) {
            nativeContainer = getHeavyweightContainer();
            if (nativeContainer.modalComp != null) {
                this.modalComp =  nativeContainer.modalComp;
                nativeContainer.modalComp = this;
                return;
            }
            else {
                nativeContainer.modalComp = this;
            }
        }

        Runnable pumpEventsForHierarchy = new Runnable() {
            public void run() {
                EventDispatchThread dispatchThread =
                    (EventDispatchThread)Thread.currentThread();
                dispatchThread.pumpEventsForHierarchy(
                        new Conditional() {
                        public boolean evaluate() {
                        return ((windowClosingException == null) && (nativeContainer.modalComp != null)) ;
                        }
                        }, Container.this);
            }
        };

        if (EventQueue.isDispatchThread()) {
            SequencedEvent currentSequencedEvent =
                KeyboardFocusManager.getCurrentKeyboardFocusManager().
                getCurrentSequencedEvent();
            if (currentSequencedEvent != null) {
                currentSequencedEvent.dispose();
            }

            pumpEventsForHierarchy.run();
        } else {
            synchronized (getTreeLock()) {
                Toolkit.getEventQueue().
                    postEvent(new PeerEvent(this,
                                pumpEventsForHierarchy,
                                PeerEvent.PRIORITY_EVENT));
                while ((windowClosingException == null) &&
                       (nativeContainer.modalComp != null))
                {
                    try {
                        getTreeLock().wait();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }
        if (windowClosingException != null) {
            windowClosingException.fillInStackTrace();
            throw windowClosingException;
        }
        if (predictedFocusOwner != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                dequeueKeyEvents(time, predictedFocusOwner);
        }
    }

    private void stopLWModal() {
        synchronized (getTreeLock()) {
            if (modalAppContext != null) {
                Container nativeContainer = getHeavyweightContainer();
                if(nativeContainer != null) {
                    if (this.modalComp !=  null) {
                        nativeContainer.modalComp = this.modalComp;
                        this.modalComp = null;
                        return;
                    }
                    else {
                        nativeContainer.modalComp = null;
                    }
                }
                // Wake up event dispatch thread on which the dialog was
                // initially shown
                SunToolkit.postEvent(modalAppContext,
                        new PeerEvent(this,
                                new WakingRunnable(),
                                PeerEvent.PRIORITY_EVENT));
            }
            EventQueue.invokeLater(new WakingRunnable());
            getTreeLock().notifyAll();
        }
    }

    final static class WakingRunnable implements Runnable {
        public void run() {
        }
    }




    protected String paramString() {
        String str = super.paramString();
        LayoutManager layoutMgr = this.layoutMgr;
        if (layoutMgr != null) {
            str += ",layout=" + layoutMgr.getClass().getName();
        }
        return str;
    }


    public void list(PrintStream out, int indent) {
        super.list(out, indent);
        synchronized(getTreeLock()) {
            for (int i = 0; i < component.size(); i++) {
                Component comp = component.get(i);
                if (comp != null) {
                    comp.list(out, indent+1);
                }
            }
        }
    }


    public void list(PrintWriter out, int indent) {
        super.list(out, indent);
        synchronized(getTreeLock()) {
            for (int i = 0; i < component.size(); i++) {
                Component comp = component.get(i);
                if (comp != null) {
                    comp.list(out, indent+1);
                }
            }
        }
    }


    public void setFocusTraversalKeys(int id,
                                      Set<? extends AWTKeyStroke> keystrokes)
    {
        if (id < 0 || id >= KeyboardFocusManager.TRAVERSAL_KEY_LENGTH) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }

        // Don't call super.setFocusTraversalKey. The Component parameter check
        // does not allow DOWN_CYCLE_TRAVERSAL_KEYS, but we do.
        setFocusTraversalKeys_NoIDCheck(id, keystrokes);
    }


    public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
        if (id < 0 || id >= KeyboardFocusManager.TRAVERSAL_KEY_LENGTH) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }

        // Don't call super.getFocusTraversalKey. The Component parameter check
        // does not allow DOWN_CYCLE_TRAVERSAL_KEY, but we do.
        return getFocusTraversalKeys_NoIDCheck(id);
    }


    public boolean areFocusTraversalKeysSet(int id) {
        if (id < 0 || id >= KeyboardFocusManager.TRAVERSAL_KEY_LENGTH) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }

        return (focusTraversalKeys != null && focusTraversalKeys[id] != null);
    }


    public boolean isFocusCycleRoot(Container container) {
        if (isFocusCycleRoot() && container == this) {
            return true;
        } else {
            return super.isFocusCycleRoot(container);
        }
    }

    private Container findTraversalRoot() {
        // I potentially have two roots, myself and my root parent
        // If I am the current root, then use me
        // If none of my parents are roots, then use me
        // If my root parent is the current root, then use my root parent
        // If neither I nor my root parent is the current root, then
        // use my root parent (a guess)

        Container currentFocusCycleRoot = KeyboardFocusManager.
            getCurrentKeyboardFocusManager().getCurrentFocusCycleRoot();
        Container root;

        if (currentFocusCycleRoot == this) {
            root = this;
        } else {
            root = getFocusCycleRootAncestor();
            if (root == null) {
                root = this;
            }
        }

        if (root != currentFocusCycleRoot) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                setGlobalCurrentFocusCycleRootPriv(root);
        }
        return root;
    }

    final boolean containsFocus() {
        final Component focusOwner = KeyboardFocusManager.
            getCurrentKeyboardFocusManager().getFocusOwner();
        return isParentOf(focusOwner);
    }


    private boolean isParentOf(Component comp) {
        synchronized(getTreeLock()) {
            while (comp != null && comp != this && !(comp instanceof Window)) {
                comp = comp.getParent();
            }
            return (comp == this);
        }
    }

    void clearMostRecentFocusOwnerOnHide() {
        boolean reset = false;
        Window window = null;

        synchronized (getTreeLock()) {
            window = getContainingWindow();
            if (window != null) {
                Component comp = KeyboardFocusManager.getMostRecentFocusOwner(window);
                reset = ((comp == this) || isParentOf(comp));
                // This synchronized should always be the second in a pair
                // (tree lock, KeyboardFocusManager.class)
                synchronized(KeyboardFocusManager.class) {
                    Component storedComp = window.getTemporaryLostComponent();
                    if (isParentOf(storedComp) || storedComp == this) {
                        window.setTemporaryLostComponent(null);
                    }
                }
            }
        }

        if (reset) {
            KeyboardFocusManager.setMostRecentFocusOwner(window, null);
        }
    }

    void clearCurrentFocusCycleRootOnHide() {
        KeyboardFocusManager kfm =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Container cont = kfm.getCurrentFocusCycleRoot();

        if (cont == this || isParentOf(cont)) {
            kfm.setGlobalCurrentFocusCycleRootPriv(null);
        }
    }

    final Container getTraversalRoot() {
        if (isFocusCycleRoot()) {
            return findTraversalRoot();
        }

        return super.getTraversalRoot();
    }


    public void setFocusTraversalPolicy(FocusTraversalPolicy policy) {
        FocusTraversalPolicy oldPolicy;
        synchronized (this) {
            oldPolicy = this.focusTraversalPolicy;
            this.focusTraversalPolicy = policy;
        }
        firePropertyChange("focusTraversalPolicy", oldPolicy, policy);
    }


    public FocusTraversalPolicy getFocusTraversalPolicy() {
        if (!isFocusTraversalPolicyProvider() && !isFocusCycleRoot()) {
            return null;
        }

        FocusTraversalPolicy policy = this.focusTraversalPolicy;
        if (policy != null) {
            return policy;
        }

        Container rootAncestor = getFocusCycleRootAncestor();
        if (rootAncestor != null) {
            return rootAncestor.getFocusTraversalPolicy();
        } else {
            return KeyboardFocusManager.getCurrentKeyboardFocusManager().
                getDefaultFocusTraversalPolicy();
        }
    }


    public boolean isFocusTraversalPolicySet() {
        return (focusTraversalPolicy != null);
    }


    public void setFocusCycleRoot(boolean focusCycleRoot) {
        boolean oldFocusCycleRoot;
        synchronized (this) {
            oldFocusCycleRoot = this.focusCycleRoot;
            this.focusCycleRoot = focusCycleRoot;
        }
        firePropertyChange("focusCycleRoot", oldFocusCycleRoot,
                           focusCycleRoot);
    }


    public boolean isFocusCycleRoot() {
        return focusCycleRoot;
    }


    public final void setFocusTraversalPolicyProvider(boolean provider) {
        boolean oldProvider;
        synchronized(this) {
            oldProvider = focusTraversalPolicyProvider;
            focusTraversalPolicyProvider = provider;
        }
        firePropertyChange("focusTraversalPolicyProvider", oldProvider, provider);
    }


    public final boolean isFocusTraversalPolicyProvider() {
        return focusTraversalPolicyProvider;
    }


    public void transferFocusDownCycle() {
        if (isFocusCycleRoot()) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                setGlobalCurrentFocusCycleRootPriv(this);
            Component toFocus = getFocusTraversalPolicy().
                getDefaultComponent(this);
            if (toFocus != null) {
                toFocus.requestFocus(CausedFocusEvent.Cause.TRAVERSAL_DOWN);
            }
        }
    }

    void preProcessKeyEvent(KeyEvent e) {
        Container parent = this.parent;
        if (parent != null) {
            parent.preProcessKeyEvent(e);
        }
    }

    void postProcessKeyEvent(KeyEvent e) {
        Container parent = this.parent;
        if (parent != null) {
            parent.postProcessKeyEvent(e);
        }
    }

    boolean postsOldMouseEvents() {
        return true;
    }


    public void applyComponentOrientation(ComponentOrientation o) {
        super.applyComponentOrientation(o);
        synchronized (getTreeLock()) {
            for (int i = 0; i < component.size(); i++) {
                Component comp = component.get(i);
                comp.applyComponentOrientation(o);
            }
        }
    }


    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
    }


    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener) {
        super.addPropertyChangeListener(propertyName, listener);
    }

    // Serialization support. A Container is responsible for restoring the
    // parent fields of its component children.


    private int containerSerializedDataVersion = 1;


    private void writeObject(ObjectOutputStream s) throws IOException {
        ObjectOutputStream.PutField f = s.putFields();
        f.put("ncomponents", component.size());
        f.put("component", component.toArray(EMPTY_ARRAY));
        f.put("layoutMgr", layoutMgr);
        f.put("dispatcher", dispatcher);
        f.put("maxSize", maxSize);
        f.put("focusCycleRoot", focusCycleRoot);
        f.put("containerSerializedDataVersion", containerSerializedDataVersion);
        f.put("focusTraversalPolicyProvider", focusTraversalPolicyProvider);
        s.writeFields();

        AWTEventMulticaster.save(s, containerListenerK, containerListener);
        s.writeObject(null);

        if (focusTraversalPolicy instanceof java.io.Serializable) {
            s.writeObject(focusTraversalPolicy);
        } else {
            s.writeObject(null);
        }
    }


    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException
    {
        ObjectInputStream.GetField f = s.readFields();
        Component [] tmpComponent = (Component[])f.get("component", EMPTY_ARRAY);
        int ncomponents = (Integer) f.get("ncomponents", 0);
        component = new java.util.ArrayList<Component>(ncomponents);
        for (int i = 0; i < ncomponents; ++i) {
            component.add(tmpComponent[i]);
        }
        layoutMgr = (LayoutManager)f.get("layoutMgr", null);
        dispatcher = (LightweightDispatcher)f.get("dispatcher", null);
        // Old stream. Doesn't contain maxSize among Component's fields.
        if (maxSize == null) {
            maxSize = (Dimension)f.get("maxSize", null);
        }
        focusCycleRoot = f.get("focusCycleRoot", false);
        containerSerializedDataVersion = f.get("containerSerializedDataVersion", 1);
        focusTraversalPolicyProvider = f.get("focusTraversalPolicyProvider", false);
        java.util.List<Component> component = this.component;
        for(Component comp : component) {
            comp.parent = this;
            adjustListeningChildren(AWTEvent.HIERARCHY_EVENT_MASK,
                                    comp.numListening(AWTEvent.HIERARCHY_EVENT_MASK));
            adjustListeningChildren(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK,
                                    comp.numListening(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
            adjustDescendants(comp.countHierarchyMembers());
        }

        Object keyOrNull;
        while(null != (keyOrNull = s.readObject())) {
            String key = ((String)keyOrNull).intern();

            if (containerListenerK == key) {
                addContainerListener((ContainerListener)(s.readObject()));
            } else {
                // skip value for unrecognized key
                s.readObject();
            }
        }

        try {
            Object policy = s.readObject();
            if (policy instanceof FocusTraversalPolicy) {
                focusTraversalPolicy = (FocusTraversalPolicy)policy;
            }
        } catch (java.io.OptionalDataException e) {
            // JDK 1.1/1.2/1.3 instances will not have this optional data.
            // e.eof will be true to indicate that there is no more data
            // available for this object. If e.eof is not true, throw the
            // exception as it might have been caused by reasons unrelated to
            // focusTraversalPolicy.

            if (!e.eof) {
                throw e;
            }
        }
    }




    protected class AccessibleAWTContainer extends AccessibleAWTComponent {


        private static final long serialVersionUID = 5081320404842566097L;


        public int getAccessibleChildrenCount() {
            return Container.this.getAccessibleChildrenCount();
        }


        public Accessible getAccessibleChild(int i) {
            return Container.this.getAccessibleChild(i);
        }


        public Accessible getAccessibleAt(Point p) {
            return Container.this.getAccessibleAt(p);
        }


        private volatile transient int propertyListenersCount = 0;

        protected ContainerListener accessibleContainerHandler = null;


        protected class AccessibleContainerHandler
            implements ContainerListener {
            public void componentAdded(ContainerEvent e) {
                Component c = e.getChild();
                if (c != null && c instanceof Accessible) {
                    AccessibleAWTContainer.this.firePropertyChange(
                        AccessibleContext.ACCESSIBLE_CHILD_PROPERTY,
                        null, ((Accessible) c).getAccessibleContext());
                }
            }
            public void componentRemoved(ContainerEvent e) {
                Component c = e.getChild();
                if (c != null && c instanceof Accessible) {
                    AccessibleAWTContainer.this.firePropertyChange(
                        AccessibleContext.ACCESSIBLE_CHILD_PROPERTY,
                        ((Accessible) c).getAccessibleContext(), null);
                }
            }
        }


        public void addPropertyChangeListener(PropertyChangeListener listener) {
            if (accessibleContainerHandler == null) {
                accessibleContainerHandler = new AccessibleContainerHandler();
            }
            if (propertyListenersCount++ == 0) {
                Container.this.addContainerListener(accessibleContainerHandler);
            }
            super.addPropertyChangeListener(listener);
        }


        public void removePropertyChangeListener(PropertyChangeListener listener) {
            if (--propertyListenersCount == 0) {
                Container.this.removeContainerListener(accessibleContainerHandler);
            }
            super.removePropertyChangeListener(listener);
        }

    } // inner class AccessibleAWTContainer


    Accessible getAccessibleAt(Point p) {
        synchronized (getTreeLock()) {
            if (this instanceof Accessible) {
                Accessible a = (Accessible)this;
                AccessibleContext ac = a.getAccessibleContext();
                if (ac != null) {
                    AccessibleComponent acmp;
                    Point location;
                    int nchildren = ac.getAccessibleChildrenCount();
                    for (int i=0; i < nchildren; i++) {
                        a = ac.getAccessibleChild(i);
                        if ((a != null)) {
                            ac = a.getAccessibleContext();
                            if (ac != null) {
                                acmp = ac.getAccessibleComponent();
                                if ((acmp != null) && (acmp.isShowing())) {
                                    location = acmp.getLocation();
                                    Point np = new Point(p.x-location.x,
                                                         p.y-location.y);
                                    if (acmp.contains(np)){
                                        return a;
                                    }
                                }
                            }
                        }
                    }
                }
                return (Accessible)this;
            } else {
                Component ret = this;
                if (!this.contains(p.x,p.y)) {
                    ret = null;
                } else {
                    int ncomponents = this.getComponentCount();
                    for (int i=0; i < ncomponents; i++) {
                        Component comp = this.getComponent(i);
                        if ((comp != null) && comp.isShowing()) {
                            Point location = comp.getLocation();
                            if (comp.contains(p.x-location.x,p.y-location.y)) {
                                ret = comp;
                            }
                        }
                    }
                }
                if (ret instanceof Accessible) {
                    return (Accessible) ret;
                }
            }
            return null;
        }
    }


    int getAccessibleChildrenCount() {
        synchronized (getTreeLock()) {
            int count = 0;
            Component[] children = this.getComponents();
            for (int i = 0; i < children.length; i++) {
                if (children[i] instanceof Accessible) {
                    count++;
                }
            }
            return count;
        }
    }


    Accessible getAccessibleChild(int i) {
        synchronized (getTreeLock()) {
            Component[] children = this.getComponents();
            int count = 0;
            for (int j = 0; j < children.length; j++) {
                if (children[j] instanceof Accessible) {
                    if (count == i) {
                        return (Accessible) children[j];
                    } else {
                        count++;
                    }
                }
            }
            return null;
        }
    }

    // ************************** MIXING CODE *******************************

    final void increaseComponentCount(Component c) {
        synchronized (getTreeLock()) {
            if (!c.isDisplayable()) {
                throw new IllegalStateException(
                    "Peer does not exist while invoking the increaseComponentCount() method"
                );
            }

            int addHW = 0;
            int addLW = 0;

            if (c instanceof Container) {
                addLW = ((Container)c).numOfLWComponents;
                addHW = ((Container)c).numOfHWComponents;
            }
            if (c.isLightweight()) {
                addLW++;
            } else {
                addHW++;
            }

            for (Container cont = this; cont != null; cont = cont.getContainer()) {
                cont.numOfLWComponents += addLW;
                cont.numOfHWComponents += addHW;
            }
        }
    }

    final void decreaseComponentCount(Component c) {
        synchronized (getTreeLock()) {
            if (!c.isDisplayable()) {
                throw new IllegalStateException(
                    "Peer does not exist while invoking the decreaseComponentCount() method"
                );
            }

            int subHW = 0;
            int subLW = 0;

            if (c instanceof Container) {
                subLW = ((Container)c).numOfLWComponents;
                subHW = ((Container)c).numOfHWComponents;
            }
            if (c.isLightweight()) {
                subLW++;
            } else {
                subHW++;
            }

            for (Container cont = this; cont != null; cont = cont.getContainer()) {
                cont.numOfLWComponents -= subLW;
                cont.numOfHWComponents -= subHW;
            }
        }
    }

    private int getTopmostComponentIndex() {
        checkTreeLock();
        if (getComponentCount() > 0) {
            return 0;
        }
        return -1;
    }

    private int getBottommostComponentIndex() {
        checkTreeLock();
        if (getComponentCount() > 0) {
            return getComponentCount() - 1;
        }
        return -1;
    }


    @Override
    final Region getOpaqueShape() {
        checkTreeLock();
        if (isLightweight() && isNonOpaqueForMixing()
                && hasLightweightDescendants())
        {
            Region s = Region.EMPTY_REGION;
            for (int index = 0; index < getComponentCount(); index++) {
                Component c = getComponent(index);
                if (c.isLightweight() && c.isShowing()) {
                    s = s.getUnion(c.getOpaqueShape());
                }
            }
            return s.getIntersection(getNormalShape());
        }
        return super.getOpaqueShape();
    }

    final void recursiveSubtractAndApplyShape(Region shape) {
        recursiveSubtractAndApplyShape(shape, getTopmostComponentIndex(), getBottommostComponentIndex());
    }

    final void recursiveSubtractAndApplyShape(Region shape, int fromZorder) {
        recursiveSubtractAndApplyShape(shape, fromZorder, getBottommostComponentIndex());
    }

    final void recursiveSubtractAndApplyShape(Region shape, int fromZorder, int toZorder) {
        checkTreeLock();
        if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
            mixingLog.fine("this = " + this +
                "; shape=" + shape + "; fromZ=" + fromZorder + "; toZ=" + toZorder);
        }
        if (fromZorder == -1) {
            return;
        }
        if (shape.isEmpty()) {
            return;
        }
        // An invalid container with not-null layout should be ignored
        // by the mixing code, the container will be validated later
        // and the mixing code will be executed later.
        if (getLayout() != null && !isValid()) {
            return;
        }
        for (int index = fromZorder; index <= toZorder; index++) {
            Component comp = getComponent(index);
            if (!comp.isLightweight()) {
                comp.subtractAndApplyShape(shape);
            } else if (comp instanceof Container &&
                    ((Container)comp).hasHeavyweightDescendants() && comp.isShowing()) {
                ((Container)comp).recursiveSubtractAndApplyShape(shape);
            }
        }
    }

    final void recursiveApplyCurrentShape() {
        recursiveApplyCurrentShape(getTopmostComponentIndex(), getBottommostComponentIndex());
    }

    final void recursiveApplyCurrentShape(int fromZorder) {
        recursiveApplyCurrentShape(fromZorder, getBottommostComponentIndex());
    }

    final void recursiveApplyCurrentShape(int fromZorder, int toZorder) {
        checkTreeLock();
        if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
            mixingLog.fine("this = " + this +
                "; fromZ=" + fromZorder + "; toZ=" + toZorder);
        }
        if (fromZorder == -1) {
            return;
        }
        // An invalid container with not-null layout should be ignored
        // by the mixing code, the container will be validated later
        // and the mixing code will be executed later.
        if (getLayout() != null && !isValid()) {
            return;
        }
        for (int index = fromZorder; index <= toZorder; index++) {
            Component comp = getComponent(index);
            if (!comp.isLightweight()) {
                comp.applyCurrentShape();
            }
            if (comp instanceof Container &&
                    ((Container)comp).hasHeavyweightDescendants()) {
                ((Container)comp).recursiveApplyCurrentShape();
            }
        }
    }

    private void recursiveShowHeavyweightChildren() {
        if (!hasHeavyweightDescendants() || !isVisible()) {
            return;
        }
        for (int index = 0; index < getComponentCount(); index++) {
            Component comp = getComponent(index);
            if (comp.isLightweight()) {
                if  (comp instanceof Container) {
                    ((Container)comp).recursiveShowHeavyweightChildren();
                }
            } else {
                if (comp.isVisible()) {
                    ComponentPeer peer = comp.getPeer();
                    if (peer != null) {
                        peer.setVisible(true);
                    }
                }
            }
        }
    }

    private void recursiveHideHeavyweightChildren() {
        if (!hasHeavyweightDescendants()) {
            return;
        }
        for (int index = 0; index < getComponentCount(); index++) {
            Component comp = getComponent(index);
            if (comp.isLightweight()) {
                if  (comp instanceof Container) {
                    ((Container)comp).recursiveHideHeavyweightChildren();
                }
            } else {
                if (comp.isVisible()) {
                    ComponentPeer peer = comp.getPeer();
                    if (peer != null) {
                        peer.setVisible(false);
                    }
                }
            }
        }
    }

    private void recursiveRelocateHeavyweightChildren(Point origin) {
        for (int index = 0; index < getComponentCount(); index++) {
            Component comp = getComponent(index);
            if (comp.isLightweight()) {
                if  (comp instanceof Container &&
                        ((Container)comp).hasHeavyweightDescendants())
                {
                    final Point newOrigin = new Point(origin);
                    newOrigin.translate(comp.getX(), comp.getY());
                    ((Container)comp).recursiveRelocateHeavyweightChildren(newOrigin);
                }
            } else {
                ComponentPeer peer = comp.getPeer();
                if (peer != null) {
                    peer.setBounds(origin.x + comp.getX(), origin.y + comp.getY(),
                            comp.getWidth(), comp.getHeight(),
                            ComponentPeer.SET_LOCATION);
                }
            }
        }
    }


    final boolean isRecursivelyVisibleUpToHeavyweightContainer() {
        if (!isLightweight()) {
            return true;
        }

        for (Container cont = this;
                cont != null && cont.isLightweight();
                cont = cont.getContainer())
        {
            if (!cont.isVisible()) {
                return false;
            }
        }
        return true;
    }

    @Override
    void mixOnShowing() {
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this);
            }

            boolean isLightweight = isLightweight();

            if (isLightweight && isRecursivelyVisibleUpToHeavyweightContainer()) {
                recursiveShowHeavyweightChildren();
            }

            if (!isMixingNeeded()) {
                return;
            }

            if (!isLightweight || (isLightweight && hasHeavyweightDescendants())) {
                recursiveApplyCurrentShape();
            }

            super.mixOnShowing();
        }
    }

    @Override
    void mixOnHiding(boolean isLightweight) {
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this +
                        "; isLightweight=" + isLightweight);
            }
            if (isLightweight) {
                recursiveHideHeavyweightChildren();
            }
            super.mixOnHiding(isLightweight);
        }
    }

    @Override
    void mixOnReshaping() {
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this);
            }

            boolean isMixingNeeded = isMixingNeeded();

            if (isLightweight() && hasHeavyweightDescendants()) {
                final Point origin = new Point(getX(), getY());
                for (Container cont = getContainer();
                        cont != null && cont.isLightweight();
                        cont = cont.getContainer())
                {
                    origin.translate(cont.getX(), cont.getY());
                }

                recursiveRelocateHeavyweightChildren(origin);

                if (!isMixingNeeded) {
                    return;
                }

                recursiveApplyCurrentShape();
            }

            if (!isMixingNeeded) {
                return;
            }

            super.mixOnReshaping();
        }
    }

    @Override
    void mixOnZOrderChanging(int oldZorder, int newZorder) {
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this +
                    "; oldZ=" + oldZorder + "; newZ=" + newZorder);
            }

            if (!isMixingNeeded()) {
                return;
            }

            boolean becameHigher = newZorder < oldZorder;

            if (becameHigher && isLightweight() && hasHeavyweightDescendants()) {
                recursiveApplyCurrentShape();
            }
            super.mixOnZOrderChanging(oldZorder, newZorder);
        }
    }

    @Override
    void mixOnValidating() {
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this);
            }

            if (!isMixingNeeded()) {
                return;
            }

            if (hasHeavyweightDescendants()) {
                recursiveApplyCurrentShape();
            }

            if (isLightweight() && isNonOpaqueForMixing()) {
                subtractAndApplyShapeBelowMe();
            }

            super.mixOnValidating();
        }
    }

    // ****************** END OF MIXING CODE ********************************
}



class LightweightDispatcher implements java.io.Serializable, AWTEventListener {


    private static final long serialVersionUID = 5184291520170872969L;

    private static final int  LWD_MOUSE_DRAGGED_OVER = 1500;

    private static final PlatformLogger eventLog = PlatformLogger.getLogger("java.awt.event.LightweightDispatcher");

    private static final int BUTTONS_DOWN_MASK;

    static {
        int[] buttonsDownMask = AWTAccessor.getInputEventAccessor().
                getButtonDownMasks();
        int mask = 0;
        for (int buttonDownMask : buttonsDownMask) {
            mask |= buttonDownMask;
        }
        BUTTONS_DOWN_MASK = mask;
    }

    LightweightDispatcher(Container nativeContainer) {
        this.nativeContainer = nativeContainer;
        mouseEventTarget = new WeakReference<>(null);
        targetLastEntered = new WeakReference<>(null);
        targetLastEnteredDT = new WeakReference<>(null);
        eventMask = 0;
    }


    void dispose() {
        //System.out.println("Disposing lw dispatcher");
        stopListeningForOtherDrags();
        mouseEventTarget.clear();
        targetLastEntered.clear();
        targetLastEnteredDT.clear();
    }


    void enableEvents(long events) {
        eventMask |= events;
    }


    boolean dispatchEvent(AWTEvent e) {
        boolean ret = false;


        if (e instanceof SunDropTargetEvent) {

            SunDropTargetEvent sdde = (SunDropTargetEvent) e;
            ret = processDropTargetEvent(sdde);

        } else {
            if (e instanceof MouseEvent && (eventMask & MOUSE_MASK) != 0) {
                MouseEvent me = (MouseEvent) e;
                ret = processMouseEvent(me);
            }

            if (e.getID() == MouseEvent.MOUSE_MOVED) {
                nativeContainer.updateCursorImmediately();
            }
        }

        return ret;
    }


    private boolean isMouseGrab(MouseEvent e) {
        int modifiers = e.getModifiersEx();

        if (e.getID() == MouseEvent.MOUSE_PRESSED
                || e.getID() == MouseEvent.MOUSE_RELEASED) {
            modifiers ^= InputEvent.getMaskForButton(e.getButton());
        }

        return ((modifiers & BUTTONS_DOWN_MASK) != 0);
    }


    private boolean processMouseEvent(MouseEvent e) {
        int id = e.getID();
        Component mouseOver =   // sensitive to mouse events
            nativeContainer.getMouseEventTarget(e.getX(), e.getY(),
                                                Container.INCLUDE_SELF);

        trackMouseEnterExit(mouseOver, e);

        Component met = mouseEventTarget.get();
        // 4508327 : MOUSE_CLICKED should only go to the recipient of
        // the accompanying MOUSE_PRESSED, so don't reset mouseEventTarget on a
        // MOUSE_CLICKED.
        if (!isMouseGrab(e) && id != MouseEvent.MOUSE_CLICKED) {
            met = (mouseOver != nativeContainer) ? mouseOver : null;
            mouseEventTarget = new WeakReference<>(met);
        }

        if (met != null) {
            switch (id) {
                case MouseEvent.MOUSE_ENTERED:
                case MouseEvent.MOUSE_EXITED:
                    break;
                case MouseEvent.MOUSE_PRESSED:
                    retargetMouseEvent(met, id, e);
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    retargetMouseEvent(met, id, e);
                    break;
                case MouseEvent.MOUSE_CLICKED:
                    // 4508327: MOUSE_CLICKED should never be dispatched to a Component
                    // other than that which received the MOUSE_PRESSED event.  If the
                    // mouse is now over a different Component, don't dispatch the event.
                    // The previous fix for a similar problem was associated with bug
                    // 4155217.
                    if (mouseOver == met) {
                        retargetMouseEvent(mouseOver, id, e);
                    }
                    break;
                case MouseEvent.MOUSE_MOVED:
                    retargetMouseEvent(met, id, e);
                    break;
                case MouseEvent.MOUSE_DRAGGED:
                    if (isMouseGrab(e)) {
                        retargetMouseEvent(met, id, e);
                    }
                    break;
                case MouseEvent.MOUSE_WHEEL:
                    // This may send it somewhere that doesn't have MouseWheelEvents
                    // enabled.  In this case, Component.dispatchEventImpl() will
                    // retarget the event to a parent that DOES have the events enabled.
                    if (eventLog.isLoggable(PlatformLogger.Level.FINEST) && (mouseOver != null)) {
                        eventLog.finest("retargeting mouse wheel to " +
                                mouseOver.getName() + ", " +
                                mouseOver.getClass());
                    }
                    retargetMouseEvent(mouseOver, id, e);
                    break;
            }
            //Consuming of wheel events is implemented in "retargetMouseEvent".
            if (id != MouseEvent.MOUSE_WHEEL) {
                e.consume();
            }
        }
        return e.isConsumed();
    }

    private boolean processDropTargetEvent(SunDropTargetEvent e) {
        int id = e.getID();
        int x = e.getX();
        int y = e.getY();


        if (!nativeContainer.contains(x, y)) {
            final Dimension d = nativeContainer.getSize();
            if (d.width <= x) {
                x = d.width - 1;
            } else if (x < 0) {
                x = 0;
            }
            if (d.height <= y) {
                y = d.height - 1;
            } else if (y < 0) {
                y = 0;
            }
        }
        Component mouseOver =   // not necessarily sensitive to mouse events
            nativeContainer.getDropTargetEventTarget(x, y,
                                                     Container.INCLUDE_SELF);
        trackMouseEnterExit(mouseOver, e);

        if (mouseOver != nativeContainer && mouseOver != null) {
            switch (id) {
            case SunDropTargetEvent.MOUSE_ENTERED:
            case SunDropTargetEvent.MOUSE_EXITED:
                break;
            default:
                retargetMouseEvent(mouseOver, id, e);
                e.consume();
                break;
            }
        }
        return e.isConsumed();
    }


    private void trackDropTargetEnterExit(Component targetOver, MouseEvent e) {
        int id = e.getID();
        if (id == MouseEvent.MOUSE_ENTERED && isMouseDTInNativeContainer) {
            // This can happen if a lightweight component which initiated the
            // drag has an associated drop target. MOUSE_ENTERED comes when the
            // mouse is in the native container already. To propagate this event
            // properly we should null out targetLastEntered.
            targetLastEnteredDT.clear();
        } else if (id == MouseEvent.MOUSE_ENTERED) {
            isMouseDTInNativeContainer = true;
        } else if (id == MouseEvent.MOUSE_EXITED) {
            isMouseDTInNativeContainer = false;
        }
        Component tle = retargetMouseEnterExit(targetOver, e,
                                                     targetLastEnteredDT.get(),
                                                     isMouseDTInNativeContainer);
        targetLastEnteredDT = new WeakReference<>(tle);
    }


    private void trackMouseEnterExit(Component targetOver, MouseEvent e) {
        if (e instanceof SunDropTargetEvent) {
            trackDropTargetEnterExit(targetOver, e);
            return;
        }
        int id = e.getID();

        if ( id != MouseEvent.MOUSE_EXITED &&
             id != MouseEvent.MOUSE_DRAGGED &&
             id != LWD_MOUSE_DRAGGED_OVER &&
                !isMouseInNativeContainer) {
            // any event but an exit or drag means we're in the native container
            isMouseInNativeContainer = true;
            startListeningForOtherDrags();
        } else if (id == MouseEvent.MOUSE_EXITED) {
            isMouseInNativeContainer = false;
            stopListeningForOtherDrags();
        }
        Component tle = retargetMouseEnterExit(targetOver, e,
                                                   targetLastEntered.get(),
                                                   isMouseInNativeContainer);
        targetLastEntered = new WeakReference<>(tle);
    }

    private Component retargetMouseEnterExit(Component targetOver, MouseEvent e,
                                             Component lastEntered,
                                             boolean inNativeContainer) {
        int id = e.getID();
        Component targetEnter = inNativeContainer ? targetOver : null;

        if (lastEntered != targetEnter) {
            if (lastEntered != null) {
                retargetMouseEvent(lastEntered, MouseEvent.MOUSE_EXITED, e);
            }
            if (id == MouseEvent.MOUSE_EXITED) {
                // consume native exit event if we generate one
                e.consume();
            }

            if (targetEnter != null) {
                retargetMouseEvent(targetEnter, MouseEvent.MOUSE_ENTERED, e);
            }
            if (id == MouseEvent.MOUSE_ENTERED) {
                // consume native enter event if we generate one
                e.consume();
            }
        }
        return targetEnter;
    }


    private void startListeningForOtherDrags() {
        //System.out.println("Adding AWTEventListener");
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Object>() {
                public Object run() {
                    nativeContainer.getToolkit().addAWTEventListener(
                        LightweightDispatcher.this,
                        AWTEvent.MOUSE_EVENT_MASK |
                        AWTEvent.MOUSE_MOTION_EVENT_MASK);
                    return null;
                }
            }
        );
    }

    private void stopListeningForOtherDrags() {
        //System.out.println("Removing AWTEventListener");
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Object>() {
                public Object run() {
                    nativeContainer.getToolkit().removeAWTEventListener(LightweightDispatcher.this);
                    return null;
                }
            }
        );
    }


    public void eventDispatched(AWTEvent e) {
        boolean isForeignDrag = (e instanceof MouseEvent) &&
                                !(e instanceof SunDropTargetEvent) &&
                                (e.id == MouseEvent.MOUSE_DRAGGED) &&
                                (e.getSource() != nativeContainer);

        if (!isForeignDrag) {
            // only interested in drags from other hw components
            return;
        }

        MouseEvent      srcEvent = (MouseEvent)e;
        MouseEvent      me;

        synchronized (nativeContainer.getTreeLock()) {
            Component srcComponent = srcEvent.getComponent();

            // component may have disappeared since drag event posted
            // (i.e. Swing hierarchical menus)
            if ( !srcComponent.isShowing() ) {
                return;
            }

            // see 5083555
            // check if srcComponent is in any modal blocked window
            Component c = nativeContainer;
            while ((c != null) && !(c instanceof Window)) {
                c = c.getParent_NoClientCode();
            }
            if ((c == null) || ((Window)c).isModalBlocked()) {
                return;
            }

            //
            // create an internal 'dragged-over' event indicating
            // we are being dragged over from another hw component
            //
            me = new MouseEvent(nativeContainer,
                               LWD_MOUSE_DRAGGED_OVER,
                               srcEvent.getWhen(),
                               srcEvent.getModifiersEx() | srcEvent.getModifiers(),
                               srcEvent.getX(),
                               srcEvent.getY(),
                               srcEvent.getXOnScreen(),
                               srcEvent.getYOnScreen(),
                               srcEvent.getClickCount(),
                               srcEvent.isPopupTrigger(),
                               srcEvent.getButton());
            ((AWTEvent)srcEvent).copyPrivateDataInto(me);
            // translate coordinates to this native container
            final Point ptSrcOrigin = srcComponent.getLocationOnScreen();

            if (AppContext.getAppContext() != nativeContainer.appContext) {
                final MouseEvent mouseEvent = me;
                Runnable r = new Runnable() {
                        public void run() {
                            if (!nativeContainer.isShowing() ) {
                                return;
                            }

                            Point       ptDstOrigin = nativeContainer.getLocationOnScreen();
                            mouseEvent.translatePoint(ptSrcOrigin.x - ptDstOrigin.x,
                                              ptSrcOrigin.y - ptDstOrigin.y );
                            Component targetOver =
                                nativeContainer.getMouseEventTarget(mouseEvent.getX(),
                                                                    mouseEvent.getY(),
                                                                    Container.INCLUDE_SELF);
                            trackMouseEnterExit(targetOver, mouseEvent);
                        }
                    };
                SunToolkit.executeOnEventHandlerThread(nativeContainer, r);
                return;
            } else {
                if (!nativeContainer.isShowing() ) {
                    return;
                }

                Point   ptDstOrigin = nativeContainer.getLocationOnScreen();
                me.translatePoint( ptSrcOrigin.x - ptDstOrigin.x, ptSrcOrigin.y - ptDstOrigin.y );
            }
        }
        //System.out.println("Track event: " + me);
        // feed the 'dragged-over' event directly to the enter/exit
        // code (not a real event so don't pass it to dispatchEvent)
        Component targetOver =
            nativeContainer.getMouseEventTarget(me.getX(), me.getY(),
                                                Container.INCLUDE_SELF);
        trackMouseEnterExit(targetOver, me);
    }


    void retargetMouseEvent(Component target, int id, MouseEvent e) {
        if (target == null) {
            return; // mouse is over another hw component or target is disabled
        }

        int x = e.getX(), y = e.getY();
        Component component;

        for(component = target;
            component != null && component != nativeContainer;
            component = component.getParent()) {
            x -= component.x;
            y -= component.y;
        }
        MouseEvent retargeted;
        if (component != null) {
            if (e instanceof SunDropTargetEvent) {
                retargeted = new SunDropTargetEvent(target,
                                                    id,
                                                    x,
                                                    y,
                                                    ((SunDropTargetEvent)e).getDispatcher());
            } else if (id == MouseEvent.MOUSE_WHEEL) {
                retargeted = new MouseWheelEvent(target,
                                      id,
                                       e.getWhen(),
                                       e.getModifiersEx() | e.getModifiers(),
                                       x,
                                       y,
                                       e.getXOnScreen(),
                                       e.getYOnScreen(),
                                       e.getClickCount(),
                                       e.isPopupTrigger(),
                                       ((MouseWheelEvent)e).getScrollType(),
                                       ((MouseWheelEvent)e).getScrollAmount(),
                                       ((MouseWheelEvent)e).getWheelRotation(),
                                       ((MouseWheelEvent)e).getPreciseWheelRotation());
            }
            else {
                retargeted = new MouseEvent(target,
                                            id,
                                            e.getWhen(),
                                            e.getModifiersEx() | e.getModifiers(),
                                            x,
                                            y,
                                            e.getXOnScreen(),
                                            e.getYOnScreen(),
                                            e.getClickCount(),
                                            e.isPopupTrigger(),
                                            e.getButton());
            }

            ((AWTEvent)e).copyPrivateDataInto(retargeted);

            if (target == nativeContainer) {
                // avoid recursively calling LightweightDispatcher...
                ((Container)target).dispatchEventToSelf(retargeted);
            } else {
                assert AppContext.getAppContext() == target.appContext;

                if (nativeContainer.modalComp != null) {
                    if (((Container)nativeContainer.modalComp).isAncestorOf(target)) {
                        target.dispatchEvent(retargeted);
                    } else {
                        e.consume();
                    }
                } else {
                    target.dispatchEvent(retargeted);
                }
            }
            if (id == MouseEvent.MOUSE_WHEEL && retargeted.isConsumed()) {
                //An exception for wheel bubbling to the native system.
                //In "processMouseEvent" total event consuming for wheel events is skipped.
                //Protection from bubbling of Java-accepted wheel events.
                e.consume();
            }
        }
    }

    // --- member variables -------------------------------


    private Container nativeContainer;


    private Component focus;


    private transient WeakReference<Component> mouseEventTarget;


    private transient  WeakReference<Component> targetLastEntered;


    private transient  WeakReference<Component> targetLastEnteredDT;


    private transient boolean isMouseInNativeContainer = false;


    private transient boolean isMouseDTInNativeContainer = false;


    private Cursor nativeCursor;


    private long eventMask;


    private static final long PROXY_EVENT_MASK =
        AWTEvent.FOCUS_EVENT_MASK |
        AWTEvent.KEY_EVENT_MASK |
        AWTEvent.MOUSE_EVENT_MASK |
        AWTEvent.MOUSE_MOTION_EVENT_MASK |
        AWTEvent.MOUSE_WHEEL_EVENT_MASK;

    private static final long MOUSE_MASK =
        AWTEvent.MOUSE_EVENT_MASK |
        AWTEvent.MOUSE_MOTION_EVENT_MASK |
        AWTEvent.MOUSE_WHEEL_EVENT_MASK;
}
