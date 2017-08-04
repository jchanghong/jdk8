package javax.sql;
import java.sql.*;
public interface XAConnection extends PooledConnection {
  javax.transaction.xa.XAResource getXAResource() throws SQLException;
 }
