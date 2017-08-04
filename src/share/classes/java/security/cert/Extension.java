

package java.security.cert;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;


public interface Extension {


    String getId();


    boolean isCritical();


    byte[] getValue();


    void encode(OutputStream out) throws IOException;
}
