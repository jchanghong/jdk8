
package java.rmi.server;

import java.rmi.*;
import sun.rmi.server.UnicastServerRef;
import sun.rmi.runtime.Log;


public abstract class RemoteServer extends RemoteObject
{

    private static final long serialVersionUID = -4100238210092549637L;


    protected RemoteServer() {
        super();
    }


    protected RemoteServer(RemoteRef ref) {
        super(ref);
    }


    public static String getClientHost() throws ServerNotActiveException {
        return sun.rmi.transport.tcp.TCPTransport.getClientHost();
    }


    public static void setLog(java.io.OutputStream out)
    {
        logNull = (out == null);
        UnicastServerRef.callLog.setOutputStream(out);
    }


    public static java.io.PrintStream getLog()
    {
        return (logNull ? null : UnicastServerRef.callLog.getPrintStream());
    }

    // initialize log status
    private static boolean logNull = !UnicastServerRef.logCalls;
}
