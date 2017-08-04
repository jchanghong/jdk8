package java.rmi.server;
import java.rmi.*;
@Deprecated
public interface ServerRef extends RemoteRef {
    static final long serialVersionUID = -4557750989390278438L;
    RemoteStub exportObject(Remote obj, Object data)
        throws RemoteException;
    String getClientHost() throws ServerNotActiveException;
}
