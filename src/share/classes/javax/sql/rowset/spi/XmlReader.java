

package javax.sql.rowset.spi;

import java.sql.SQLException;
import java.io.Reader;

import javax.sql.RowSetReader;
import javax.sql.rowset.*;


public interface XmlReader extends RowSetReader {


  public void readXML(WebRowSet caller, java.io.Reader reader)
    throws SQLException;

}
