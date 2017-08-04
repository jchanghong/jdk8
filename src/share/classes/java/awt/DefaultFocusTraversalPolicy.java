
package java.awt;

import java.awt.peer.ComponentPeer;



public class DefaultFocusTraversalPolicy
    extends ContainerOrderFocusTraversalPolicy
{

    private static final long serialVersionUID = 8876966522510157497L;


    protected boolean accept(Component aComponent) {
        if (!(aComponent.isVisible() && aComponent.isDisplayable() &&
              aComponent.isEnabled()))
        {
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

        boolean focusable = aComponent.isFocusable();
        if (aComponent.isFocusTraversableOverridden()) {
            return focusable;
        }

        ComponentPeer peer = aComponent.getPeer();
        return (peer != null && peer.isFocusable());
    }
}
