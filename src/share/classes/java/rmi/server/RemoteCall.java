

package java.rmi.server;
import java.rmi.*;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.StreamCorruptedException;
import java.io.IOException;


@Deprecated
public interface RemoteCall {


    @Deprecated
    ObjectOutput getOutputStream()  throws IOException;


    @Deprecated
    void releaseOutputStream()  throws IOException;


    @Deprecated
    ObjectInput getInputStream()  throws IOException;



    @Deprecated
    void releaseInputStream() throws IOException;


    @Deprecated
    ObjectOutput getResultStream(boolean success) throws IOException,
        StreamCorruptedException;


    @Deprecated
    void executeCall() throws Exception;


    @Deprecated
    void done() throws IOException;
}
