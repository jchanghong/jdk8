package javax.sql;
import java.sql.*;
public interface RowSetReader {
  void readData(RowSetInternal caller) throws SQLException;
}
