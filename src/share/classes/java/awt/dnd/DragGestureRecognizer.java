package java.awt.dnd;
import java.awt.event.InputEvent;
import java.awt.Component;
import java.awt.Point;
import java.io.InvalidObjectException;
import java.util.Collections;
import java.util.TooManyListenersException;
import java.util.ArrayList;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
public abstract class DragGestureRecognizer implements Serializable {
    private static final long serialVersionUID = 8996673345831063337L;
    protected DragGestureRecognizer(DragSource ds, Component c, int sa, DragGestureListener dgl) {
        super();
        if (ds == null) throw new IllegalArgumentException("null DragSource");
        dragSource    = ds;
        component     = c;
        sourceActions = sa & (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK);
        try {
            if (dgl != null) addDragGestureListener(dgl);
        } catch (TooManyListenersException tmle) {
            // cant happen ...
        }
    }
    protected DragGestureRecognizer(DragSource ds, Component c, int sa) {
        this(ds, c, sa, null);
    }
    protected DragGestureRecognizer(DragSource ds, Component c) {
        this(ds, c, DnDConstants.ACTION_NONE);
    }
    protected DragGestureRecognizer(DragSource ds) {
        this(ds, null);
    }
    protected abstract void registerListeners();
    protected abstract void unregisterListeners();
    public DragSource getDragSource() { return dragSource; }
    public synchronized Component getComponent() { return component; }
    public synchronized void setComponent(Component c) {
        if (component != null && dragGestureListener != null)
            unregisterListeners();
        component = c;
        if (component != null && dragGestureListener != null)
            registerListeners();
    }
    public synchronized int getSourceActions() { return sourceActions; }
    public synchronized void setSourceActions(int actions) {
        sourceActions = actions & (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK);
    }
    public InputEvent getTriggerEvent() { return events.isEmpty() ? null : events.get(0); }
    public void resetRecognizer() { events.clear(); }
    public synchronized void addDragGestureListener(DragGestureListener dgl) throws TooManyListenersException {
        if (dragGestureListener != null)
            throw new TooManyListenersException();
        else {
            dragGestureListener = dgl;
            if (component != null) registerListeners();
        }
    }
    public synchronized void removeDragGestureListener(DragGestureListener dgl) {
        if (dragGestureListener == null || !dragGestureListener.equals(dgl))
            throw new IllegalArgumentException();
        else {
            dragGestureListener = null;
            if (component != null) unregisterListeners();
        }
    }
    protected synchronized void fireDragGestureRecognized(int dragAction, Point p) {
        try {
            if (dragGestureListener != null) {
                dragGestureListener.dragGestureRecognized(new DragGestureEvent(this, dragAction, p, events));
            }
        } finally {
            events.clear();
        }
    }
    protected synchronized void appendEvent(InputEvent awtie) {
        events.add(awtie);
    }
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeObject(SerializationTester.test(dragGestureListener)
                      ? dragGestureListener : null);
    }
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException
    {
        ObjectInputStream.GetField f = s.readFields();
        DragSource newDragSource = (DragSource)f.get("dragSource", null);
        if (newDragSource == null) {
            throw new InvalidObjectException("null DragSource");
        }
        dragSource = newDragSource;
        component = (Component)f.get("component", null);
        sourceActions = f.get("sourceActions", 0) & (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK);
        events = (ArrayList<InputEvent>)f.get("events", new ArrayList<>(1));
        dragGestureListener = (DragGestureListener)s.readObject();
    }
    protected DragSource          dragSource;
    protected Component           component;
    protected transient DragGestureListener dragGestureListener;
  protected int  sourceActions;
   protected ArrayList<InputEvent> events = new ArrayList<InputEvent>(1);
}
