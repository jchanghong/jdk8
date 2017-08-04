package java.rmi.activation;
import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.activation.UnknownGroupException;
import java.rmi.activation.UnknownObjectException;
public interface ActivationMonitor extends Remote {
    public void inactiveObject(ActivationID id)
        throws UnknownObjectException, RemoteException;
    public void activeObject(ActivationID id,
                             MarshalledObject<? extends Remote> obj)
        throws UnknownObjectException, RemoteException;
    public void inactiveGroup(ActivationGroupID id,
                              long incarnation)
        throws UnknownGroupException, RemoteException;
}
