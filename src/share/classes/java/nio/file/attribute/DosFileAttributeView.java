package java.nio.file.attribute;
import java.io.IOException;
public interface DosFileAttributeView
    extends BasicFileAttributeView
{
    @Override
    String name();
    @Override
    DosFileAttributes readAttributes() throws IOException;
    void setReadOnly(boolean value) throws IOException;
    void setHidden(boolean value) throws IOException;
    void setSystem(boolean value) throws IOException;
    void setArchive(boolean value) throws IOException;
}
