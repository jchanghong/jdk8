package java.net;
import java.lang.annotation.Native;
public interface SocketOptions {
    public void
        setOption(int optID, Object value) throws SocketException;
    public Object getOption(int optID) throws SocketException;
    @Native public final static int TCP_NODELAY = 0x0001;
    @Native public final static int SO_BINDADDR = 0x000F;
    @Native public final static int SO_REUSEADDR = 0x04;
    @Native public final static int SO_BROADCAST = 0x0020;
    @Native public final static int IP_MULTICAST_IF = 0x10;
    @Native public final static int IP_MULTICAST_IF2 = 0x1f;
    @Native public final static int IP_MULTICAST_LOOP = 0x12;
    @Native public final static int IP_TOS = 0x3;
    @Native public final static int SO_LINGER = 0x0080;
    @Native public final static int SO_TIMEOUT = 0x1006;
    @Native public final static int SO_SNDBUF = 0x1001;
    @Native public final static int SO_RCVBUF = 0x1002;
    @Native public final static int SO_KEEPALIVE = 0x0008;
    @Native public final static int SO_OOBINLINE = 0x1003;
}
