
package java.awt;

import java.util.List;
import java.util.ArrayList;
import sun.util.logging.PlatformLogger;


public class ContainerOrderFocusTraversalPolicy extends FocusTraversalPolicy
    implements java.io.Serializable
{
    private static final PlatformLogger log = PlatformLogger.getLogger("java.awt.ContainerOrderFocusTraversalPolicy");

    final private int FORWARD_TRAVERSAL = 0;
    final private int BACKWARD_TRAVERSAL = 1;


    private static final long serialVersionUID = 486933713763926351L;

    private boolean implicitDownCycleTraversal = true;


    transient private Container cachedRoot;
    transient private List<Component> cachedCycle;


     private List<Component> getFocusTraversalCycle(Container aContainer) {
        List<Component> cycle = new ArrayList<Component>();
        enumerateCycle(aContainer, cycle);
        return cycle;
    }
     private int getComponentIndex(List<Component> cycle, Component aComponent) {
        return cycle.indexOf(aComponent);
    }

    private void enumerateCycle(Container container, List<Component> cycle) {
        if (!(container.isVisible() && container.isDisplayable())) {
            return;
        }

        cycle.add(container);

        Component[] components = container.getComponents();
        for (int i = 0; i < components.length; i++) {
            Component comp = components[i];
            if (comp instanceof Container) {
                Container cont = (Container)comp;

                if (!cont.isFocusCycleRoot() && !cont.isFocusTraversalPolicyProvider()) {
                    enumerateCycle(cont, cycle);
                    continue;
                }
            }
            cycle.add(comp);
        }
    }

    private Container getTopmostProvider(Container focusCycleRoot, Component aComponent) {
        Container aCont = aComponent.getParent();
        Container ftp = null;
        while (aCont  != focusCycleRoot && aCont != null) {
            if (aCont.isFocusTraversalPolicyProvider()) {
                ftp = aCont;
            }
            aCont = aCont.getParent();
        }
        if (aCont == null) {
            return null;
        }
        return ftp;
    }


    private Component getComponentDownCycle(Component comp, int traversalDirection) {
        Component retComp = null;

        if (comp instanceof Container) {
            Container cont = (Container)comp;

            if (cont.isFocusCycleRoot()) {
                if (getImplicitDownCycleTraversal()) {
                    retComp = cont.getFocusTraversalPolicy().getDefaultComponent(cont);

                    if (retComp != null && log.isLoggable(PlatformLogger.Level.FINE)) {
                        log.fine("### Transfered focus down-cycle to " + retComp +
                                 " in the focus cycle root " + cont);
                    }
                } else {
                    return null;
                }
            } else if (cont.isFocusTraversalPolicyProvider()) {
                retComp = (traversalDirection == FORWARD_TRAVERSAL ?
                           cont.getFocusTraversalPolicy().getDefaultComponent(cont) :
                           cont.getFocusTraversalPolicy().getLastComponent(cont));

                if (retComp != null && log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("### Transfered focus to " + retComp + " in the FTP provider " + cont);
                }
            }
        }
        return retComp;
    }


    public Component getComponentAfter(Container aContainer, Component aComponent) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("### Searching in " + aContainer + " for component after " + aComponent);
        }

        if (aContainer == null || aComponent == null) {
            throw new IllegalArgumentException("aContainer and aComponent cannot be null");
        }
        if (!aContainer.isFocusTraversalPolicyProvider() && !aContainer.isFocusCycleRoot()) {
            throw new IllegalArgumentException("aContainer should be focus cycle root or focus traversal policy provider");

        } else if (aContainer.isFocusCycleRoot() && !aComponent.isFocusCycleRoot(aContainer)) {
            throw new IllegalArgumentException("aContainer is not a focus cycle root of aComponent");
        }

        synchronized(aContainer.getTreeLock()) {

            if (!(aContainer.isVisible() && aContainer.isDisplayable())) {
                return null;
            }

            // Before all the ckecks below we first see if it's an FTP provider or a focus cycle root.
            // If it's the case just go down cycle (if it's set to "implicit").
            Component comp = getComponentDownCycle(aComponent, FORWARD_TRAVERSAL);
            if (comp != null) {
                return comp;
            }

            // See if the component is inside of policy provider.
            Container provider = getTopmostProvider(aContainer, aComponent);
            if (provider != null) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("### Asking FTP " + provider + " for component after " + aComponent);
                }

                // FTP knows how to find component after the given. We don't.
                FocusTraversalPolicy policy = provider.getFocusTraversalPolicy();
                Component afterComp = policy.getComponentAfter(provider, aComponent);

                // Null result means that we overstepped the limit of the FTP's cycle.
                // In that case we must quit the cycle, otherwise return the component found.
                if (afterComp != null) {
                    if (log.isLoggable(PlatformLogger.Level.FINE)) {
                        log.fine("### FTP returned " + afterComp);
                    }
                    return afterComp;
                }
                aComponent = provider;
            }

            List<Component> cycle = getFocusTraversalCycle(aContainer);

            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("### Cycle is " + cycle + ", component is " + aComponent);
            }

            int index = getComponentIndex(cycle, aComponent);

            if (index < 0) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("### Didn't find component " + aComponent + " in a cycle " + aContainer);
                }
                return getFirstComponent(aContainer);
            }

            for (index++; index < cycle.size(); index++) {
                comp = cycle.get(index);
                if (accept(comp)) {
                    return comp;
                } else if ((comp = getComponentDownCycle(comp, FORWARD_TRAVERSAL)) != null) {
                    return comp;
                }
            }

            if (aContainer.isFocusCycleRoot()) {
                this.cachedRoot = aContainer;
                this.cachedCycle = cycle;

                comp = getFirstComponent(aContainer);

                this.cachedRoot = null;
                this.cachedCycle = null;

                return comp;
            }
        }
        return null;
    }


    public Component getComponentBefore(Container aContainer, Component aComponent) {
        if (aContainer == null || aComponent == null) {
            throw new IllegalArgumentException("aContainer and aComponent cannot be null");
        }
        if (!aContainer.isFocusTraversalPolicyProvider() && !aContainer.isFocusCycleRoot()) {
            throw new IllegalArgumentException("aContainer should be focus cycle root or focus traversal policy provider");

        } else if (aContainer.isFocusCycleRoot() && !aComponent.isFocusCycleRoot(aContainer)) {
            throw new IllegalArgumentException("aContainer is not a focus cycle root of aComponent");
        }

        synchronized(aContainer.getTreeLock()) {

            if (!(aContainer.isVisible() && aContainer.isDisplayable())) {
                return null;
            }

            // See if the component is inside of policy provider.
            Container provider = getTopmostProvider(aContainer, aComponent);
            if (provider != null) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("### Asking FTP " + provider + " for component after " + aComponent);
                }

                // FTP knows how to find component after the given. We don't.
                FocusTraversalPolicy policy = provider.getFocusTraversalPolicy();
                Component beforeComp = policy.getComponentBefore(provider, aComponent);

                // Null result means that we overstepped the limit of the FTP's cycle.
                // In that case we must quit the cycle, otherwise return the component found.
                if (beforeComp != null) {
                    if (log.isLoggable(PlatformLogger.Level.FINE)) {
                        log.fine("### FTP returned " + beforeComp);
                    }
                    return beforeComp;
                }
                aComponent = provider;

                // If the provider is traversable it's returned.
                if (accept(aComponent)) {
                    return aComponent;
                }
            }

            List<Component> cycle = getFocusTraversalCycle(aContainer);

            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("### Cycle is " + cycle + ", component is " + aComponent);
            }

            int index = getComponentIndex(cycle, aComponent);

            if (index < 0) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("### Didn't find component " + aComponent + " in a cycle " + aContainer);
                }
                return getLastComponent(aContainer);
            }

            Component comp = null;
            Component tryComp = null;

            for (index--; index>=0; index--) {
                comp = cycle.get(index);
                if (comp != aContainer && (tryComp = getComponentDownCycle(comp, BACKWARD_TRAVERSAL)) != null) {
                    return tryComp;
                } else if (accept(comp)) {
                    return comp;
                }
            }

            if (aContainer.isFocusCycleRoot()) {
                this.cachedRoot = aContainer;
                this.cachedCycle = cycle;

                comp = getLastComponent(aContainer);

                this.cachedRoot = null;
                this.cachedCycle = null;

                return comp;
            }
        }
        return null;
    }


    public Component getFirstComponent(Container aContainer) {
        List<Component> cycle;

        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("### Getting first component in " + aContainer);
        }
        if (aContainer == null) {
            throw new IllegalArgumentException("aContainer cannot be null");

        }

        synchronized(aContainer.getTreeLock()) {

            if (!(aContainer.isVisible() && aContainer.isDisplayable())) {
                return null;
            }

            if (this.cachedRoot == aContainer) {
                cycle = this.cachedCycle;
            } else {
                cycle = getFocusTraversalCycle(aContainer);
            }

            if (cycle.size() == 0) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("### Cycle is empty");
                }
                return null;
            }
            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("### Cycle is " + cycle);
            }

            for (Component comp : cycle) {
                if (accept(comp)) {
                    return comp;
                } else if (comp != aContainer &&
                           (comp = getComponentDownCycle(comp, FORWARD_TRAVERSAL)) != null)
                {
                    return comp;
                }
            }
        }
        return null;
    }


    public Component getLastComponent(Container aContainer) {
        List<Component> cycle;
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("### Getting last component in " + aContainer);
        }

        if (aContainer == null) {
            throw new IllegalArgumentException("aContainer cannot be null");
        }

        synchronized(aContainer.getTreeLock()) {

            if (!(aContainer.isVisible() && aContainer.isDisplayable())) {
                return null;
            }

            if (this.cachedRoot == aContainer) {
                cycle = this.cachedCycle;
            } else {
                cycle = getFocusTraversalCycle(aContainer);
            }

            if (cycle.size() == 0) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("### Cycle is empty");
                }
                return null;
            }
            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("### Cycle is " + cycle);
            }

            for (int i= cycle.size() - 1; i >= 0; i--) {
                Component comp = cycle.get(i);
                if (accept(comp)) {
                    return comp;
                } else if (comp instanceof Container && comp != aContainer) {
                    Container cont = (Container)comp;
                    if (cont.isFocusTraversalPolicyProvider()) {
                        Component retComp = cont.getFocusTraversalPolicy().getLastComponent(cont);
                        if (retComp != null) {
                            return retComp;
                        }
                    }
                }
            }
        }
        return null;
    }


    public Component getDefaultComponent(Container aContainer) {
        return getFirstComponent(aContainer);
    }


    public void setImplicitDownCycleTraversal(boolean implicitDownCycleTraversal) {
        this.implicitDownCycleTraversal = implicitDownCycleTraversal;
    }


    public boolean getImplicitDownCycleTraversal() {
        return implicitDownCycleTraversal;
    }


    protected boolean accept(Component aComponent) {
        if (!aComponent.canBeFocusOwner()) {
            return false;
        }

        // Verify that the Component is recursively enabled. Disabling a
        // heavyweight Container disables its children, whereas disabling
        // a lightweight Container does not.
        if (!(aComponent instanceof Window)) {
            for (Container enableTest = aComponent.getParent();
                 enableTest != null;
                 enableTest = enableTest.getParent())
            {
                if (!(enableTest.isEnabled() || enableTest.isLightweight())) {
                    return false;
                }
                if (enableTest instanceof Window) {
                    break;
                }
            }
        }

        return true;
    }
}
