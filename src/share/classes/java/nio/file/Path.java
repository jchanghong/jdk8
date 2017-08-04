package java.nio.file;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
public interface Path
    extends Comparable<Path>, Iterable<Path>, Watchable
{
    FileSystem getFileSystem();
    boolean isAbsolute();
    Path getRoot();
    Path getFileName();
    Path getParent();
    int getNameCount();
    Path getName(int index);
    Path subpath(int beginIndex, int endIndex);
    boolean startsWith(Path other);
    boolean startsWith(String other);
    boolean endsWith(Path other);
    boolean endsWith(String other);
    Path normalize();
    // -- resolution and relativization --
    Path resolve(Path other);
    Path resolve(String other);
    Path resolveSibling(Path other);
    Path resolveSibling(String other);
    Path relativize(Path other);
    URI toUri();
    Path toAbsolutePath();
    Path toRealPath(LinkOption... options) throws IOException;
    File toFile();
    // -- watchable --
    @Override
    WatchKey register(WatchService watcher,
                      WatchEvent.Kind<?>[] events,
                      WatchEvent.Modifier... modifiers)
        throws IOException;
    @Override
    WatchKey register(WatchService watcher,
                      WatchEvent.Kind<?>... events)
        throws IOException;
    // -- Iterable --
    @Override
    Iterator<Path> iterator();
    // -- compareTo/equals/hashCode --
    @Override
    int compareTo(Path other);
    boolean equals(Object other);
    int hashCode();
    String toString();
}
