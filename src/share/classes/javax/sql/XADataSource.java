package javax.sql;
import java.sql.*;
public interface XADataSource extends CommonDataSource {
  XAConnection getXAConnection() throws SQLException;
  XAConnection getXAConnection(String user, String password)
    throws SQLException;
 }
