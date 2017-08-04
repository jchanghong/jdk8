

package java.nio.file.attribute;

import java.io.IOException;



public interface FileOwnerAttributeView
    extends FileAttributeView
{

    @Override
    String name();


    UserPrincipal getOwner() throws IOException;


    void setOwner(UserPrincipal owner) throws IOException;
}
