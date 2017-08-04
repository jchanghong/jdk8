

package java.rmi.server;

import java.rmi.*;


public interface RemoteRef extends java.io.Externalizable {


    static final long serialVersionUID = 3632638527362204081L;


    final static String packagePrefix = "sun.rmi.server";


    Object invoke(Remote obj,
                  java.lang.reflect.Method method,
                  Object[] params,
                  long opnum)
        throws Exception;


    @Deprecated
    RemoteCall newCall(RemoteObject obj, Operation[] op, int opnum, long hash)
        throws RemoteException;


    @Deprecated
    void invoke(RemoteCall call) throws Exception;


    @Deprecated
    void done(RemoteCall call) throws RemoteException;


    String getRefClass(java.io.ObjectOutput out);


    int remoteHashCode();


    boolean remoteEquals(RemoteRef obj);


    String remoteToString();

}
