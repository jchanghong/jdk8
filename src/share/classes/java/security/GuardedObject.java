

package java.security;



public class GuardedObject implements java.io.Serializable {

    private static final long serialVersionUID = -5240450096227834308L;

    private Object object; // the object we are guarding
    private Guard guard;   // the guard



    public GuardedObject(Object object, Guard guard)
    {
        this.guard = guard;
        this.object = object;
    }


    public Object getObject()
        throws SecurityException
    {
        if (guard != null)
            guard.checkGuard(object);

        return object;
    }


    private void writeObject(java.io.ObjectOutputStream oos)
        throws java.io.IOException
    {
        if (guard != null)
            guard.checkGuard(object);

        oos.defaultWriteObject();
    }
}
