package java.rmi.registry;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
@Deprecated
public interface RegistryHandler {
    @Deprecated
    Registry registryStub(String host, int port)
        throws RemoteException, UnknownHostException;
    @Deprecated
    Registry registryImpl(int port) throws RemoteException;
}
