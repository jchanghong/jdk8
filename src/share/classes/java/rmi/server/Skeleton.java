
package java.rmi.server;

import java.rmi.Remote;


@Deprecated
public interface Skeleton {

    @Deprecated
    void dispatch(Remote obj, RemoteCall theCall, int opnum, long hash)
        throws Exception;


    @Deprecated
    Operation[] getOperations();
}
