package java.nio.channels;
import java.io.IOException;
import java.util.concurrent.Future;  // javadoc
public interface AsynchronousChannel
    extends Channel
{
    @Override
    void close() throws IOException;
}
