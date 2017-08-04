

package java.awt.im;

import java.awt.Component;
import java.util.Locale;
import java.awt.AWTEvent;
import java.beans.Transient;
import java.lang.Character.Subset;
import sun.awt.im.InputMethodContext;



public class InputContext {


    protected InputContext() {
        // real implementation is in sun.awt.im.InputContext
    }


    public static InputContext getInstance() {
        return new sun.awt.im.InputMethodContext();
    }


    public boolean selectInputMethod(Locale locale) {
        // real implementation is in sun.awt.im.InputContext
        return false;
    }


    public Locale getLocale() {
        // real implementation is in sun.awt.im.InputContext
        return null;
    }


    public void setCharacterSubsets(Subset[] subsets) {
        // real implementation is in sun.awt.im.InputContext
    }


    public void setCompositionEnabled(boolean enable) {
        // real implementation is in sun.awt.im.InputContext
    }


    @Transient
    public boolean isCompositionEnabled() {
        // real implementation is in sun.awt.im.InputContext
        return false;
    }


    public void reconvert() {
        // real implementation is in sun.awt.im.InputContext
    }


    public void dispatchEvent(AWTEvent event) {
        // real implementation is in sun.awt.im.InputContext
    }


    public void removeNotify(Component client) {
        // real implementation is in sun.awt.im.InputContext
    }


    public void endComposition() {
        // real implementation is in sun.awt.im.InputContext
    }


    public void dispose() {
        // real implementation is in sun.awt.im.InputContext
    }


    public Object getInputMethodControlObject() {
        // real implementation is in sun.awt.im.InputContext
        return null;
    }

}
