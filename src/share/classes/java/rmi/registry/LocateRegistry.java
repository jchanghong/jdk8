package java.rmi.registry;
import java.rmi.RemoteException;
import java.rmi.server.ObjID;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RemoteRef;
import java.rmi.server.UnicastRemoteObject;
import sun.rmi.registry.RegistryImpl;
import sun.rmi.server.UnicastRef2;
import sun.rmi.server.UnicastRef;
import sun.rmi.server.Util;
import sun.rmi.transport.LiveRef;
import sun.rmi.transport.tcp.TCPEndpoint;
public final class LocateRegistry {
    private LocateRegistry() {}
    public static Registry getRegistry()
        throws RemoteException
    {
        return getRegistry(null, Registry.REGISTRY_PORT);
    }
    public static Registry getRegistry(int port)
        throws RemoteException
    {
        return getRegistry(null, port);
    }
    public static Registry getRegistry(String host)
        throws RemoteException
    {
        return getRegistry(host, Registry.REGISTRY_PORT);
    }
    public static Registry getRegistry(String host, int port)
        throws RemoteException
    {
        return getRegistry(host, port, null);
    }
    public static Registry getRegistry(String host, int port,
                                       RMIClientSocketFactory csf)
        throws RemoteException
    {
        Registry registry = null;
        if (port <= 0)
            port = Registry.REGISTRY_PORT;
        if (host == null || host.length() == 0) {
            // If host is blank (as returned by "file:" URL in 1.0.2 used in
            // java.rmi.Naming), try to convert to real local host name so
            // that the RegistryImpl's checkAccess will not fail.
            try {
                host = java.net.InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                // If that failed, at least try "" (localhost) anyway...
                host = "";
            }
        }
        LiveRef liveRef =
            new LiveRef(new ObjID(ObjID.REGISTRY_ID),
                        new TCPEndpoint(host, port, csf, null),
                        false);
        RemoteRef ref =
            (csf == null) ? new UnicastRef(liveRef) : new UnicastRef2(liveRef);
        return (Registry) Util.createProxy(RegistryImpl.class, ref, false);
    }
    public static Registry createRegistry(int port) throws RemoteException {
        return new RegistryImpl(port);
    }
    public static Registry createRegistry(int port,
                                          RMIClientSocketFactory csf,
                                          RMIServerSocketFactory ssf)
        throws RemoteException
    {
        return new RegistryImpl(port, csf, ssf);
    }
}
