package javax.sql;
import java.sql.*;
public interface RowSetInternal {
  Object[] getParams() throws SQLException;
  Connection getConnection() throws SQLException;
  void setMetaData(RowSetMetaData md) throws SQLException;
  public ResultSet getOriginal() throws SQLException;
  public ResultSet getOriginalRow() throws SQLException;
}
