

package javax.sql.rowset;

import java.sql.*;
import javax.sql.*;
import javax.naming.*;
import java.io.*;
import java.math.*;



public interface FilteredRowSet extends WebRowSet {


    public void setFilter(Predicate p) throws SQLException;


    public Predicate getFilter() ;
}
