

package javax.sql.rowset;

import java.sql.*;
import javax.sql.*;
import javax.naming.*;
import java.io.*;
import java.math.*;
import java.util.*;

import javax.sql.rowset.*;



public interface JoinRowSet extends WebRowSet {


    public void addRowSet(Joinable rowset) throws SQLException;


    public void addRowSet(RowSet rowset, int columnIdx) throws SQLException;


     public void addRowSet(RowSet rowset,
                           String columnName) throws SQLException;


    public void addRowSet(RowSet[] rowset,
                          int[] columnIdx) throws SQLException;


    public void addRowSet(RowSet[] rowset,
                          String[] columnName) throws SQLException;


    public Collection<?> getRowSets() throws java.sql.SQLException;


    public String[] getRowSetNames() throws java.sql.SQLException;


    public CachedRowSet toCachedRowSet() throws java.sql.SQLException;


    public boolean supportsCrossJoin();


    public boolean supportsInnerJoin();


    public boolean supportsLeftOuterJoin();


    public boolean supportsRightOuterJoin();


    public boolean supportsFullJoin();


    public void setJoinType(int joinType) throws SQLException;


    public String getWhereClause() throws SQLException;


    public int getJoinType() throws SQLException;


    public static int CROSS_JOIN = 0;


    public static int INNER_JOIN = 1;


    public static int LEFT_OUTER_JOIN = 2;


    public static int RIGHT_OUTER_JOIN = 3;


    public static int FULL_JOIN = 4;


}
