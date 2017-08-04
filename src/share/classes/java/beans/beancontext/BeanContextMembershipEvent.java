

package java.beans.beancontext;

import java.util.EventObject;

import java.beans.beancontext.BeanContext;
import java.beans.beancontext.BeanContextEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;


public class BeanContextMembershipEvent extends BeanContextEvent {
    private static final long serialVersionUID = 3499346510334590959L;



    @SuppressWarnings("rawtypes")
    public BeanContextMembershipEvent(BeanContext bc, Collection changes) {
        super(bc);

        if (changes == null) throw new NullPointerException(
            "BeanContextMembershipEvent constructor:  changes is null.");

        children = changes;
    }



    public BeanContextMembershipEvent(BeanContext bc, Object[] changes) {
        super(bc);

        if (changes == null) throw new NullPointerException(
            "BeanContextMembershipEvent:  changes is null.");

        children = Arrays.asList(changes);
    }


    public int size() { return children.size(); }


    public boolean contains(Object child) {
        return children.contains(child);
    }


    public Object[] toArray() { return children.toArray(); }


    @SuppressWarnings("rawtypes")
    public Iterator iterator() { return children.iterator(); }




    @SuppressWarnings("rawtypes")
    protected Collection children;
}
