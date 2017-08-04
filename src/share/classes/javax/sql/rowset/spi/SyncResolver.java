

package javax.sql.rowset.spi;

import javax.sql.RowSet;
import java.sql.SQLException;



public interface SyncResolver extends RowSet {

     public static int UPDATE_ROW_CONFLICT = 0;


    public static int DELETE_ROW_CONFLICT = 1;


    public static int INSERT_ROW_CONFLICT = 2;


    public static int NO_ROW_CONFLICT = 3;


    public int getStatus();


    public Object getConflictValue(int index) throws SQLException;


    public Object getConflictValue(String columnName) throws SQLException;


    public void setResolvedValue(int index, Object obj) throws SQLException;


    public void setResolvedValue(String columnName, Object obj) throws SQLException;


    public boolean nextConflict() throws SQLException;


    public boolean previousConflict() throws SQLException;

}
