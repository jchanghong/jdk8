

package javax.sql.rowset.spi;

import java.sql.SQLException;
import java.io.Reader;

import javax.sql.RowSetWriter;
import javax.sql.rowset.*;
import java.sql.Savepoint;


public interface TransactionalWriter extends RowSetWriter {


    public void commit() throws SQLException;


    public void rollback() throws SQLException;


    public void rollback(Savepoint s) throws SQLException;
}
