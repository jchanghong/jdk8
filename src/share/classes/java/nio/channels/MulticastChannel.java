

package java.nio.channels;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.io.IOException;
import java.net.ProtocolFamily;             // javadoc
import java.net.StandardProtocolFamily;     // javadoc
import java.net.StandardSocketOptions;      // javadoc



public interface MulticastChannel
    extends NetworkChannel
{

    @Override void close() throws IOException;


    MembershipKey join(InetAddress group, NetworkInterface interf)
        throws IOException;


    MembershipKey join(InetAddress group, NetworkInterface interf, InetAddress source)
        throws IOException;
}
