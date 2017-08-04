

package java.beans;

import java.beans.*;



public class PropertyEditorSupport implements PropertyEditor {


    public PropertyEditorSupport() {
        setSource(this);
    }


    public PropertyEditorSupport(Object source) {
        if (source == null) {
           throw new NullPointerException();
        }
        setSource(source);
    }


    public Object getSource() {
        return source;
    }


    public void setSource(Object source) {
        this.source = source;
    }


    public void setValue(Object value) {
        this.value = value;
        firePropertyChange();
    }


    public Object getValue() {
        return value;
    }

    //----------------------------------------------------------------------



    public boolean isPaintable() {
        return false;
    }


    public void paintValue(java.awt.Graphics gfx, java.awt.Rectangle box) {
    }

    //----------------------------------------------------------------------


    public String getJavaInitializationString() {
        return "???";
    }

    //----------------------------------------------------------------------


    public String getAsText() {
        return (this.value != null)
                ? this.value.toString()
                : null;
    }


    public void setAsText(String text) throws java.lang.IllegalArgumentException {
        if (value instanceof String) {
            setValue(text);
            return;
        }
        throw new java.lang.IllegalArgumentException(text);
    }

    //----------------------------------------------------------------------


    public String[] getTags() {
        return null;
    }

    //----------------------------------------------------------------------



    public java.awt.Component getCustomEditor() {
        return null;
    }


    public boolean supportsCustomEditor() {
        return false;
    }

    //----------------------------------------------------------------------


    public synchronized void addPropertyChangeListener(
                                PropertyChangeListener listener) {
        if (listeners == null) {
            listeners = new java.util.Vector<>();
        }
        listeners.addElement(listener);
    }


    public synchronized void removePropertyChangeListener(
                                PropertyChangeListener listener) {
        if (listeners == null) {
            return;
        }
        listeners.removeElement(listener);
    }


    public void firePropertyChange() {
        java.util.Vector<PropertyChangeListener> targets;
        synchronized (this) {
            if (listeners == null) {
                return;
            }
            targets = unsafeClone(listeners);
        }
        // Tell our listeners that "everything" has changed.
        PropertyChangeEvent evt = new PropertyChangeEvent(source, null, null, null);

        for (int i = 0; i < targets.size(); i++) {
            PropertyChangeListener target = targets.elementAt(i);
            target.propertyChange(evt);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> java.util.Vector<T> unsafeClone(java.util.Vector<T> v) {
        return (java.util.Vector<T>)v.clone();
    }

    //----------------------------------------------------------------------

    private Object value;
    private Object source;
    private java.util.Vector<PropertyChangeListener> listeners;
}
