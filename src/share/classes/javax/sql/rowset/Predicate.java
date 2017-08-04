

package javax.sql.rowset;

import javax.sql.*;
import java.sql.*;



 // <h3>3.0 FilteredRowSet Internals</h3>
 // internalNext, Frist, Last. Discuss guidelines on how to approach this
 // and cite examples in reference implementations.
public interface Predicate {

    public boolean evaluate(RowSet rs);



    public boolean evaluate(Object value, int column) throws SQLException;


    public boolean evaluate(Object value, String columnName) throws SQLException;

}
