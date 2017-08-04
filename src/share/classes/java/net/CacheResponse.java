package java.net;
import java.io.InputStream;
import java.util.Map;
import java.util.List;
import java.io.IOException;
public abstract class CacheResponse {
    public abstract Map<String, List<String>> getHeaders() throws IOException;
    public abstract InputStream getBody() throws IOException;
}
