

package java.sql;

import java.math.BigDecimal;
import java.util.Calendar;
import java.io.Reader;
import java.io.InputStream;



public interface PreparedStatement extends Statement {


    ResultSet executeQuery() throws SQLException;


    int executeUpdate() throws SQLException;


    void setNull(int parameterIndex, int sqlType) throws SQLException;


    void setBoolean(int parameterIndex, boolean x) throws SQLException;


    void setByte(int parameterIndex, byte x) throws SQLException;


    void setShort(int parameterIndex, short x) throws SQLException;


    void setInt(int parameterIndex, int x) throws SQLException;


    void setLong(int parameterIndex, long x) throws SQLException;


    void setFloat(int parameterIndex, float x) throws SQLException;


    void setDouble(int parameterIndex, double x) throws SQLException;


    void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException;


    void setString(int parameterIndex, String x) throws SQLException;


    void setBytes(int parameterIndex, byte x[]) throws SQLException;


    void setDate(int parameterIndex, java.sql.Date x)
            throws SQLException;


    void setTime(int parameterIndex, java.sql.Time x)
            throws SQLException;


    void setTimestamp(int parameterIndex, java.sql.Timestamp x)
            throws SQLException;


    void setAsciiStream(int parameterIndex, java.io.InputStream x, int length)
            throws SQLException;


    @Deprecated
    void setUnicodeStream(int parameterIndex, java.io.InputStream x,
                          int length) throws SQLException;


    void setBinaryStream(int parameterIndex, java.io.InputStream x,
                         int length) throws SQLException;


    void clearParameters() throws SQLException;

    //----------------------------------------------------------------------
    // Advanced features:


    void setObject(int parameterIndex, Object x, int targetSqlType)
      throws SQLException;


    void setObject(int parameterIndex, Object x) throws SQLException;


    boolean execute() throws SQLException;

    //--------------------------JDBC 2.0-----------------------------


    void addBatch() throws SQLException;


    void setCharacterStream(int parameterIndex,
                          java.io.Reader reader,
                          int length) throws SQLException;


    void setRef (int parameterIndex, Ref x) throws SQLException;


    void setBlob (int parameterIndex, Blob x) throws SQLException;


    void setClob (int parameterIndex, Clob x) throws SQLException;


    void setArray (int parameterIndex, Array x) throws SQLException;


    ResultSetMetaData getMetaData() throws SQLException;


    void setDate(int parameterIndex, java.sql.Date x, Calendar cal)
            throws SQLException;


    void setTime(int parameterIndex, java.sql.Time x, Calendar cal)
            throws SQLException;


    void setTimestamp(int parameterIndex, java.sql.Timestamp x, Calendar cal)
            throws SQLException;


  void setNull (int parameterIndex, int sqlType, String typeName)
    throws SQLException;

    //------------------------- JDBC 3.0 -----------------------------------


    void setURL(int parameterIndex, java.net.URL x) throws SQLException;


    ParameterMetaData getParameterMetaData() throws SQLException;

    //------------------------- JDBC 4.0 -----------------------------------


    void setRowId(int parameterIndex, RowId x) throws SQLException;



     void setNString(int parameterIndex, String value) throws SQLException;


     void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException;


     void setNClob(int parameterIndex, NClob value) throws SQLException;


     void setClob(int parameterIndex, Reader reader, long length)
       throws SQLException;


     void setBlob(int parameterIndex, InputStream inputStream, long length)
        throws SQLException;

     void setNClob(int parameterIndex, Reader reader, long length)
       throws SQLException;


     void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException;


    void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
            throws SQLException;

    void setAsciiStream(int parameterIndex, java.io.InputStream x, long length)
            throws SQLException;

    void setBinaryStream(int parameterIndex, java.io.InputStream x,
                         long length) throws SQLException;

    void setCharacterStream(int parameterIndex,
                          java.io.Reader reader,
                          long length) throws SQLException;
    //-----

    void setAsciiStream(int parameterIndex, java.io.InputStream x)
            throws SQLException;

    void setBinaryStream(int parameterIndex, java.io.InputStream x)
    throws SQLException;

    void setCharacterStream(int parameterIndex,
                          java.io.Reader reader) throws SQLException;

     void setNCharacterStream(int parameterIndex, Reader value) throws SQLException;


     void setClob(int parameterIndex, Reader reader)
       throws SQLException;


     void setBlob(int parameterIndex, InputStream inputStream)
        throws SQLException;

     void setNClob(int parameterIndex, Reader reader)
       throws SQLException;

    //------------------------- JDBC 4.2 -----------------------------------


    default void setObject(int parameterIndex, Object x, SQLType targetSqlType,
             int scaleOrLength) throws SQLException {
        throw new SQLFeatureNotSupportedException("setObject not implemented");
    }


    default void setObject(int parameterIndex, Object x, SQLType targetSqlType)
      throws SQLException {
        throw new SQLFeatureNotSupportedException("setObject not implemented");
    }


    default long executeLargeUpdate() throws SQLException {
        throw new UnsupportedOperationException("executeLargeUpdate not implemented");
    }
}
