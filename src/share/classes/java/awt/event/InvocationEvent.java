

package java.awt.event;

import sun.awt.AWTAccessor;

import java.awt.ActiveEvent;
import java.awt.AWTEvent;


public class InvocationEvent extends AWTEvent implements ActiveEvent {

    static {
        AWTAccessor.setInvocationEventAccessor(new AWTAccessor.InvocationEventAccessor() {
            @Override
            public void dispose(InvocationEvent invocationEvent) {
                invocationEvent.finishedDispatching(false);
            }
        });
    }


    public static final int INVOCATION_FIRST = 1200;


    public static final int INVOCATION_DEFAULT = INVOCATION_FIRST;


    public static final int INVOCATION_LAST = INVOCATION_DEFAULT;


    protected Runnable runnable;


    protected volatile Object notifier;


    private final Runnable listener;


    private volatile boolean dispatched = false;


    protected boolean catchExceptions;


    private Exception exception = null;


    private Throwable throwable = null;


    private long when;


    private static final long serialVersionUID = 436056344909459450L;


    public InvocationEvent(Object source, Runnable runnable) {
        this(source, INVOCATION_DEFAULT, runnable, null, null, false);
    }


    public InvocationEvent(Object source, Runnable runnable, Object notifier,
                           boolean catchThrowables) {
        this(source, INVOCATION_DEFAULT, runnable, notifier, null, catchThrowables);
    }


    public InvocationEvent(Object source, Runnable runnable, Runnable listener,
                           boolean catchThrowables)  {
        this(source, INVOCATION_DEFAULT, runnable, null, listener, catchThrowables);
    }


    protected InvocationEvent(Object source, int id, Runnable runnable,
                              Object notifier, boolean catchThrowables) {
        this(source, id, runnable, notifier, null, catchThrowables);
    }

    private InvocationEvent(Object source, int id, Runnable runnable,
                            Object notifier, Runnable listener, boolean catchThrowables) {
        super(source, id);
        this.runnable = runnable;
        this.notifier = notifier;
        this.listener = listener;
        this.catchExceptions = catchThrowables;
        this.when = System.currentTimeMillis();
    }

    public void dispatch() {
        try {
            if (catchExceptions) {
                try {
                    runnable.run();
                }
                catch (Throwable t) {
                    if (t instanceof Exception) {
                        exception = (Exception) t;
                    }
                    throwable = t;
                }
            }
            else {
                runnable.run();
            }
        } finally {
            finishedDispatching(true);
        }
    }


    public Exception getException() {
        return (catchExceptions) ? exception : null;
    }


    public Throwable getThrowable() {
        return (catchExceptions) ? throwable : null;
    }


    public long getWhen() {
        return when;
    }


    public boolean isDispatched() {
        return dispatched;
    }


    private void finishedDispatching(boolean dispatched) {
        this.dispatched = dispatched;

        if (notifier != null) {
            synchronized (notifier) {
                notifier.notifyAll();
            }
        }

        if (listener != null) {
            listener.run();
        }
    }


    public String paramString() {
        String typeStr;
        switch(id) {
            case INVOCATION_DEFAULT:
                typeStr = "INVOCATION_DEFAULT";
                break;
            default:
                typeStr = "unknown type";
        }
        return typeStr + ",runnable=" + runnable + ",notifier=" + notifier +
            ",catchExceptions=" + catchExceptions + ",when=" + when;
    }
}
