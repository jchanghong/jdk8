

package java.awt.dnd;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import java.awt.dnd.peer.DragSourceContextPeer;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import java.util.TooManyListenersException;



public class DragSourceContext
    implements DragSourceListener, DragSourceMotionListener, Serializable {

    private static final long serialVersionUID = -115407898692194719L;

    // used by updateCurrentCursor


    protected static final int DEFAULT = 0;


    protected static final int ENTER   = 1;


    protected static final int OVER    = 2;



    protected static final int CHANGED = 3;


    public DragSourceContext(DragSourceContextPeer dscp,
                             DragGestureEvent trigger, Cursor dragCursor,
                             Image dragImage, Point offset, Transferable t,
                             DragSourceListener dsl) {
        if (dscp == null) {
            throw new NullPointerException("DragSourceContextPeer");
        }

        if (trigger == null) {
            throw new NullPointerException("Trigger");
        }

        if (trigger.getDragSource() == null) {
            throw new IllegalArgumentException("DragSource");
        }

        if (trigger.getComponent() == null) {
            throw new IllegalArgumentException("Component");
        }

        if (trigger.getSourceAsDragGestureRecognizer().getSourceActions() ==
                 DnDConstants.ACTION_NONE) {
            throw new IllegalArgumentException("source actions");
        }

        if (trigger.getDragAction() == DnDConstants.ACTION_NONE) {
            throw new IllegalArgumentException("no drag action");
        }

        if (t == null) {
            throw new NullPointerException("Transferable");
        }

        if (dragImage != null && offset == null) {
            throw new NullPointerException("offset");
        }

        peer         = dscp;
        this.trigger = trigger;
        cursor       = dragCursor;
        transferable = t;
        listener     = dsl;
        sourceActions =
            trigger.getSourceAsDragGestureRecognizer().getSourceActions();

        useCustomCursor = (dragCursor != null);

        updateCurrentCursor(trigger.getDragAction(), getSourceActions(), DEFAULT);
    }



    public DragSource   getDragSource() { return trigger.getDragSource(); }



    public Component    getComponent() { return trigger.getComponent(); }



    public DragGestureEvent getTrigger() { return trigger; }


    public int  getSourceActions() {
        return sourceActions;
    }



    public synchronized void setCursor(Cursor c) {
        useCustomCursor = (c != null);
        setCursorImpl(c);
    }



    public Cursor getCursor() { return cursor; }



    public synchronized void addDragSourceListener(DragSourceListener dsl) throws TooManyListenersException {
        if (dsl == null) return;

        if (equals(dsl)) throw new IllegalArgumentException("DragSourceContext may not be its own listener");

        if (listener != null)
            throw new TooManyListenersException();
        else
            listener = dsl;
    }



    public synchronized void removeDragSourceListener(DragSourceListener dsl) {
        if (listener != null && listener.equals(dsl)) {
            listener = null;
        } else
            throw new IllegalArgumentException();
    }



    public void transferablesFlavorsChanged() {
        if (peer != null) peer.transferablesFlavorsChanged();
    }


    public void dragEnter(DragSourceDragEvent dsde) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dragEnter(dsde);
        }
        getDragSource().processDragEnter(dsde);

        updateCurrentCursor(getSourceActions(), dsde.getTargetActions(), ENTER);
    }


    public void dragOver(DragSourceDragEvent dsde) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dragOver(dsde);
        }
        getDragSource().processDragOver(dsde);

        updateCurrentCursor(getSourceActions(), dsde.getTargetActions(), OVER);
    }


    public void dragExit(DragSourceEvent dse) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dragExit(dse);
        }
        getDragSource().processDragExit(dse);

        updateCurrentCursor(DnDConstants.ACTION_NONE, DnDConstants.ACTION_NONE, DEFAULT);
    }


    public void dropActionChanged(DragSourceDragEvent dsde) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dropActionChanged(dsde);
        }
        getDragSource().processDropActionChanged(dsde);

        updateCurrentCursor(getSourceActions(), dsde.getTargetActions(), CHANGED);
    }


    public void dragDropEnd(DragSourceDropEvent dsde) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dragDropEnd(dsde);
        }
        getDragSource().processDragDropEnd(dsde);
    }


    public void dragMouseMoved(DragSourceDragEvent dsde) {
        getDragSource().processDragMouseMoved(dsde);
    }


    public Transferable getTransferable() { return transferable; }



    protected synchronized void updateCurrentCursor(int sourceAct, int targetAct, int status) {

        // if the cursor has been previously set then don't do any defaults
        // processing.

        if (useCustomCursor) {
            return;
        }

        // do defaults processing

        Cursor c = null;

        switch (status) {
            default:
                targetAct = DnDConstants.ACTION_NONE;
            case ENTER:
            case OVER:
            case CHANGED:
                int    ra = sourceAct & targetAct;

                if (ra == DnDConstants.ACTION_NONE) { // no drop possible
                    if ((sourceAct & DnDConstants.ACTION_LINK) == DnDConstants.ACTION_LINK)
                        c = DragSource.DefaultLinkNoDrop;
                    else if ((sourceAct & DnDConstants.ACTION_MOVE) == DnDConstants.ACTION_MOVE)
                        c = DragSource.DefaultMoveNoDrop;
                    else
                        c = DragSource.DefaultCopyNoDrop;
                } else { // drop possible
                    if ((ra & DnDConstants.ACTION_LINK) == DnDConstants.ACTION_LINK)
                        c = DragSource.DefaultLinkDrop;
                    else if ((ra & DnDConstants.ACTION_MOVE) == DnDConstants.ACTION_MOVE)
                        c = DragSource.DefaultMoveDrop;
                    else
                        c = DragSource.DefaultCopyDrop;
                }
        }

        setCursorImpl(c);
    }

    private void setCursorImpl(Cursor c) {
        if (cursor == null || !cursor.equals(c)) {
            cursor = c;
            if (peer != null) peer.setCursor(cursor);
        }
    }


    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();

        s.writeObject(SerializationTester.test(transferable)
                      ? transferable : null);
        s.writeObject(SerializationTester.test(listener)
                      ? listener : null);
    }


    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException
    {
        ObjectInputStream.GetField f = s.readFields();

        DragGestureEvent newTrigger = (DragGestureEvent)f.get("trigger", null);
        if (newTrigger == null) {
            throw new InvalidObjectException("Null trigger");
        }
        if (newTrigger.getDragSource() == null) {
            throw new InvalidObjectException("Null DragSource");
        }
        if (newTrigger.getComponent() == null) {
            throw new InvalidObjectException("Null trigger component");
        }

        int newSourceActions = f.get("sourceActions", 0)
                & (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK);
        if (newSourceActions == DnDConstants.ACTION_NONE) {
            throw new InvalidObjectException("Invalid source actions");
        }
        int triggerActions = newTrigger.getDragAction();
        if (triggerActions != DnDConstants.ACTION_COPY &&
                triggerActions != DnDConstants.ACTION_MOVE &&
                triggerActions != DnDConstants.ACTION_LINK) {
            throw new InvalidObjectException("No drag action");
        }
        trigger = newTrigger;

        cursor = (Cursor)f.get("cursor", null);
        useCustomCursor = f.get("useCustomCursor", false);
        sourceActions = newSourceActions;

        transferable = (Transferable)s.readObject();
        listener = (DragSourceListener)s.readObject();

        // Implementation assumes 'transferable' is never null.
        if (transferable == null) {
            if (emptyTransferable == null) {
                emptyTransferable = new Transferable() {
                        public DataFlavor[] getTransferDataFlavors() {
                            return new DataFlavor[0];
                        }
                        public boolean isDataFlavorSupported(DataFlavor flavor)
                        {
                            return false;
                        }
                        public Object getTransferData(DataFlavor flavor)
                            throws UnsupportedFlavorException
                        {
                            throw new UnsupportedFlavorException(flavor);
                        }
                    };
            }
            transferable = emptyTransferable;
        }
    }

    private static Transferable emptyTransferable;



    private transient DragSourceContextPeer peer;


    private DragGestureEvent    trigger;


    private Cursor              cursor;

    private transient Transferable      transferable;

    private transient DragSourceListener    listener;


    private boolean useCustomCursor;


    private int sourceActions;
}
