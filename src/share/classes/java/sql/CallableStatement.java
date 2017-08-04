

package java.sql;

import java.math.BigDecimal;
import java.util.Calendar;
import java.io.Reader;
import java.io.InputStream;



public interface CallableStatement extends PreparedStatement {


    void registerOutParameter(int parameterIndex, int sqlType)
        throws SQLException;


    void registerOutParameter(int parameterIndex, int sqlType, int scale)
        throws SQLException;


    boolean wasNull() throws SQLException;


    String getString(int parameterIndex) throws SQLException;


    boolean getBoolean(int parameterIndex) throws SQLException;


    byte getByte(int parameterIndex) throws SQLException;


    short getShort(int parameterIndex) throws SQLException;


    int getInt(int parameterIndex) throws SQLException;


    long getLong(int parameterIndex) throws SQLException;


    float getFloat(int parameterIndex) throws SQLException;


    double getDouble(int parameterIndex) throws SQLException;


    @Deprecated
    BigDecimal getBigDecimal(int parameterIndex, int scale)
        throws SQLException;


    byte[] getBytes(int parameterIndex) throws SQLException;


    java.sql.Date getDate(int parameterIndex) throws SQLException;


    java.sql.Time getTime(int parameterIndex) throws SQLException;


    java.sql.Timestamp getTimestamp(int parameterIndex)
        throws SQLException;

    //----------------------------------------------------------------------
    // Advanced features:



    Object getObject(int parameterIndex) throws SQLException;


    //--------------------------JDBC 2.0-----------------------------


    BigDecimal getBigDecimal(int parameterIndex) throws SQLException;


    Object getObject(int parameterIndex, java.util.Map<String,Class<?>> map)
        throws SQLException;


    Ref getRef (int parameterIndex) throws SQLException;


    Blob getBlob (int parameterIndex) throws SQLException;


    Clob getClob (int parameterIndex) throws SQLException;


    Array getArray (int parameterIndex) throws SQLException;


    java.sql.Date getDate(int parameterIndex, Calendar cal)
        throws SQLException;


    java.sql.Time getTime(int parameterIndex, Calendar cal)
        throws SQLException;


    java.sql.Timestamp getTimestamp(int parameterIndex, Calendar cal)
        throws SQLException;



    void registerOutParameter (int parameterIndex, int sqlType, String typeName)
        throws SQLException;

  //--------------------------JDBC 3.0-----------------------------


    void registerOutParameter(String parameterName, int sqlType)
        throws SQLException;


    void registerOutParameter(String parameterName, int sqlType, int scale)
        throws SQLException;


    void registerOutParameter (String parameterName, int sqlType, String typeName)
        throws SQLException;


    java.net.URL getURL(int parameterIndex) throws SQLException;


    void setURL(String parameterName, java.net.URL val) throws SQLException;


    void setNull(String parameterName, int sqlType) throws SQLException;


    void setBoolean(String parameterName, boolean x) throws SQLException;


    void setByte(String parameterName, byte x) throws SQLException;


    void setShort(String parameterName, short x) throws SQLException;


    void setInt(String parameterName, int x) throws SQLException;


    void setLong(String parameterName, long x) throws SQLException;


    void setFloat(String parameterName, float x) throws SQLException;


    void setDouble(String parameterName, double x) throws SQLException;


    void setBigDecimal(String parameterName, BigDecimal x) throws SQLException;


    void setString(String parameterName, String x) throws SQLException;


    void setBytes(String parameterName, byte x[]) throws SQLException;


    void setDate(String parameterName, java.sql.Date x)
        throws SQLException;


    void setTime(String parameterName, java.sql.Time x)
        throws SQLException;


    void setTimestamp(String parameterName, java.sql.Timestamp x)
        throws SQLException;


    void setAsciiStream(String parameterName, java.io.InputStream x, int length)
        throws SQLException;


    void setBinaryStream(String parameterName, java.io.InputStream x,
                         int length) throws SQLException;


    void setObject(String parameterName, Object x, int targetSqlType, int scale)
        throws SQLException;


    void setObject(String parameterName, Object x, int targetSqlType)
        throws SQLException;


    void setObject(String parameterName, Object x) throws SQLException;



    void setCharacterStream(String parameterName,
                            java.io.Reader reader,
                            int length) throws SQLException;


    void setDate(String parameterName, java.sql.Date x, Calendar cal)
        throws SQLException;


    void setTime(String parameterName, java.sql.Time x, Calendar cal)
        throws SQLException;


    void setTimestamp(String parameterName, java.sql.Timestamp x, Calendar cal)
        throws SQLException;


    void setNull (String parameterName, int sqlType, String typeName)
        throws SQLException;


    String getString(String parameterName) throws SQLException;


    boolean getBoolean(String parameterName) throws SQLException;


    byte getByte(String parameterName) throws SQLException;


    short getShort(String parameterName) throws SQLException;


    int getInt(String parameterName) throws SQLException;


    long getLong(String parameterName) throws SQLException;


    float getFloat(String parameterName) throws SQLException;


    double getDouble(String parameterName) throws SQLException;


    byte[] getBytes(String parameterName) throws SQLException;


    java.sql.Date getDate(String parameterName) throws SQLException;


    java.sql.Time getTime(String parameterName) throws SQLException;


    java.sql.Timestamp getTimestamp(String parameterName) throws SQLException;


    Object getObject(String parameterName) throws SQLException;


    BigDecimal getBigDecimal(String parameterName) throws SQLException;


    Object getObject(String parameterName, java.util.Map<String,Class<?>> map)
      throws SQLException;


    Ref getRef (String parameterName) throws SQLException;


    Blob getBlob (String parameterName) throws SQLException;


    Clob getClob (String parameterName) throws SQLException;


    Array getArray (String parameterName) throws SQLException;


    java.sql.Date getDate(String parameterName, Calendar cal)
        throws SQLException;


    java.sql.Time getTime(String parameterName, Calendar cal)
        throws SQLException;


    java.sql.Timestamp getTimestamp(String parameterName, Calendar cal)
        throws SQLException;


    java.net.URL getURL(String parameterName) throws SQLException;

    //------------------------- JDBC 4.0 -----------------------------------


    RowId getRowId(int parameterIndex) throws SQLException;


    RowId getRowId(String parameterName) throws SQLException;


    void setRowId(String parameterName, RowId x) throws SQLException;


    void setNString(String parameterName, String value)
            throws SQLException;


    void setNCharacterStream(String parameterName, Reader value, long length)
            throws SQLException;


     void setNClob(String parameterName, NClob value) throws SQLException;


     void setClob(String parameterName, Reader reader, long length)
       throws SQLException;


     void setBlob(String parameterName, InputStream inputStream, long length)
        throws SQLException;

     void setNClob(String parameterName, Reader reader, long length)
       throws SQLException;


    NClob getNClob (int parameterIndex) throws SQLException;



    NClob getNClob (String parameterName) throws SQLException;


    void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException;


    SQLXML getSQLXML(int parameterIndex) throws SQLException;


    SQLXML getSQLXML(String parameterName) throws SQLException;


    String getNString(int parameterIndex) throws SQLException;



    String getNString(String parameterName) throws SQLException;


    java.io.Reader getNCharacterStream(int parameterIndex) throws SQLException;


    java.io.Reader getNCharacterStream(String parameterName) throws SQLException;


    java.io.Reader getCharacterStream(int parameterIndex) throws SQLException;


    java.io.Reader getCharacterStream(String parameterName) throws SQLException;


    void setBlob (String parameterName, Blob x) throws SQLException;


    void setClob (String parameterName, Clob x) throws SQLException;

    void setAsciiStream(String parameterName, java.io.InputStream x, long length)
        throws SQLException;


    void setBinaryStream(String parameterName, java.io.InputStream x,
                         long length) throws SQLException;

    void setCharacterStream(String parameterName,
                            java.io.Reader reader,
                            long length) throws SQLException;
     //--

    void setAsciiStream(String parameterName, java.io.InputStream x)
            throws SQLException;

    void setBinaryStream(String parameterName, java.io.InputStream x)
    throws SQLException;

    void setCharacterStream(String parameterName,
                          java.io.Reader reader) throws SQLException;

     void setNCharacterStream(String parameterName, Reader value) throws SQLException;


     void setClob(String parameterName, Reader reader)
       throws SQLException;


     void setBlob(String parameterName, InputStream inputStream)
        throws SQLException;

     void setNClob(String parameterName, Reader reader)
       throws SQLException;

    //------------------------- JDBC 4.1 -----------------------------------



     public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException;



     public <T> T getObject(String parameterName, Class<T> type) throws SQLException;

     //------------------------- JDBC 4.2 -----------------------------------


     default void setObject(String parameterName, Object x, SQLType targetSqlType,
             int scaleOrLength) throws SQLException {
        throw new SQLFeatureNotSupportedException("setObject not implemented");
    }

     default void setObject(String parameterName, Object x, SQLType targetSqlType)
        throws SQLException {
        throw new SQLFeatureNotSupportedException("setObject not implemented");
    }


    default void registerOutParameter(int parameterIndex, SQLType sqlType)
        throws SQLException {
        throw new SQLFeatureNotSupportedException("registerOutParameter not implemented");
    }


    default void registerOutParameter(int parameterIndex, SQLType sqlType,
            int scale) throws SQLException {
        throw new SQLFeatureNotSupportedException("registerOutParameter not implemented");
    }

    default void registerOutParameter (int parameterIndex, SQLType sqlType,
            String typeName) throws SQLException {
        throw new SQLFeatureNotSupportedException("registerOutParameter not implemented");
    }


    default void registerOutParameter(String parameterName, SQLType sqlType)
        throws SQLException {
        throw new SQLFeatureNotSupportedException("registerOutParameter not implemented");
    }


    default void registerOutParameter(String parameterName, SQLType sqlType,
            int scale) throws SQLException {
        throw new SQLFeatureNotSupportedException("registerOutParameter not implemented");
    }


    default void registerOutParameter (String parameterName, SQLType sqlType,
            String typeName) throws SQLException {
        throw new SQLFeatureNotSupportedException("registerOutParameter not implemented");
    }
}
