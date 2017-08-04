



package java.util;

import sun.util.ResourceBundleEnumeration;


public abstract class ListResourceBundle extends ResourceBundle {

    public ListResourceBundle() {
    }

    // Implements java.util.ResourceBundle.handleGetObject; inherits javadoc specification.
    public final Object handleGetObject(String key) {
        // lazily load the lookup hashtable.
        if (lookup == null) {
            loadLookup();
        }
        if (key == null) {
            throw new NullPointerException();
        }
        return lookup.get(key); // this class ignores locales
    }


    public Enumeration<String> getKeys() {
        // lazily load the lookup hashtable.
        if (lookup == null) {
            loadLookup();
        }

        ResourceBundle parent = this.parent;
        return new ResourceBundleEnumeration(lookup.keySet(),
                (parent != null) ? parent.getKeys() : null);
    }


    protected Set<String> handleKeySet() {
        if (lookup == null) {
            loadLookup();
        }
        return lookup.keySet();
    }


    abstract protected Object[][] getContents();

    // ==================privates====================


    private synchronized void loadLookup() {
        if (lookup != null)
            return;

        Object[][] contents = getContents();
        HashMap<String,Object> temp = new HashMap<>(contents.length);
        for (int i = 0; i < contents.length; ++i) {
            // key must be non-null String, value must be non-null
            String key = (String) contents[i][0];
            Object value = contents[i][1];
            if (key == null || value == null) {
                throw new NullPointerException();
            }
            temp.put(key, value);
        }
        lookup = temp;
    }

    private Map<String,Object> lookup = null;
}
