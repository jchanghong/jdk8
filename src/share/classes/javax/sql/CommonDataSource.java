

package javax.sql;

import java.sql.SQLException;
import java.io.PrintWriter;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;


public interface CommonDataSource {


    java.io.PrintWriter getLogWriter() throws SQLException;


    void setLogWriter(java.io.PrintWriter out) throws SQLException;


    void setLoginTimeout(int seconds) throws SQLException;


    int getLoginTimeout() throws SQLException;

    //------------------------- JDBC 4.1 -----------------------------------


    public Logger getParentLogger() throws SQLFeatureNotSupportedException;
}
