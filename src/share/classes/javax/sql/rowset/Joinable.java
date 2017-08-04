

package javax.sql.rowset;

import java.sql.SQLException;


public interface Joinable {


    public void setMatchColumn(int columnIdx) throws SQLException;


    public void setMatchColumn(int[] columnIdxes) throws SQLException;


    public void setMatchColumn(String columnName) throws SQLException;


    public void setMatchColumn(String[] columnNames) throws SQLException;


    public int[] getMatchColumnIndexes() throws SQLException;


    public String[] getMatchColumnNames() throws SQLException;


    public void unsetMatchColumn(int columnIdx) throws SQLException;


    public void unsetMatchColumn(int[] columnIdxes) throws SQLException;


    public void unsetMatchColumn(String columnName) throws SQLException;


    public void unsetMatchColumn(String[] columnName) throws SQLException;
}
