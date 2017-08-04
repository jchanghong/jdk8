package java.nio.channels.spi;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.nio.ch.Interruptible;
public abstract class AbstractInterruptibleChannel
    implements Channel, InterruptibleChannel
{
    private final Object closeLock = new Object();
    private volatile boolean open = true;
    protected AbstractInterruptibleChannel() { }
    public final void close() throws IOException {
        synchronized (closeLock) {
            if (!open)
                return;
            open = false;
            implCloseChannel();
        }
    }
    protected abstract void implCloseChannel() throws IOException;
    public final boolean isOpen() {
        return open;
    }
    // -- Interruption machinery --
    private Interruptible interruptor;
    private volatile Thread interrupted;
    protected final void begin() {
        if (interruptor == null) {
            interruptor = new Interruptible() {
                    public void interrupt(Thread target) {
                        synchronized (closeLock) {
                            if (!open)
                                return;
                            open = false;
                            interrupted = target;
                            try {
                                AbstractInterruptibleChannel.this.implCloseChannel();
                            } catch (IOException x) { }
                        }
                    }};
        }
        blockedOn(interruptor);
        Thread me = Thread.currentThread();
        if (me.isInterrupted())
            interruptor.interrupt(me);
    }
    protected final void end(boolean completed)
        throws AsynchronousCloseException
    {
        blockedOn(null);
        Thread interrupted = this.interrupted;
        if (interrupted != null && interrupted == Thread.currentThread()) {
            interrupted = null;
            throw new ClosedByInterruptException();
        }
        if (!completed && !open)
            throw new AsynchronousCloseException();
    }
    // -- sun.misc.SharedSecrets --
    static void blockedOn(Interruptible intr) {         // package-private
        sun.misc.SharedSecrets.getJavaLangAccess().blockedOn(Thread.currentThread(),
                                                             intr);
    }
}
