

package javax.sql;

import java.sql.*;



public interface RowSetWriter {


  boolean writeData(RowSetInternal caller) throws SQLException;

}
