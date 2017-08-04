

package javax.sql.rowset.spi;

import java.sql.SQLException;
import java.io.Writer;

import javax.sql.RowSetWriter;
import javax.sql.rowset.*;


public interface XmlWriter extends RowSetWriter {


  public void writeXML(WebRowSet caller, java.io.Writer writer)
    throws SQLException;



}
