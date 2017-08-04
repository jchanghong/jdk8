

package javax.sql.rowset;

import java.sql.*;
import javax.sql.*;
import javax.naming.*;
import java.io.*;
import java.math.*;
import java.io.*;



public interface JdbcRowSet extends RowSet, Joinable {


    public boolean getShowDeleted() throws SQLException;


    public void setShowDeleted(boolean b) throws SQLException;


    public RowSetWarning getRowSetWarnings() throws SQLException;


    public void commit() throws SQLException;



    public boolean getAutoCommit() throws SQLException;



    public void setAutoCommit(boolean autoCommit) throws SQLException;


     public void rollback() throws SQLException;



    public void rollback(Savepoint s) throws SQLException;

}
