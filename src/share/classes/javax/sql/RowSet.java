

package javax.sql;

import java.sql.*;
import java.io.*;
import java.math.*;
import java.util.*;



public interface RowSet extends ResultSet {

  //-----------------------------------------------------------------------
  // Properties
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  // The following properties may be used to create a Connection.
  //-----------------------------------------------------------------------


  String getUrl() throws SQLException;


  void setUrl(String url) throws SQLException;


  String getDataSourceName();


  void setDataSourceName(String name) throws SQLException;


  String getUsername();


  void setUsername(String name) throws SQLException;


  String getPassword();


  void setPassword(String password) throws SQLException;


  int getTransactionIsolation();


  void setTransactionIsolation(int level) throws SQLException;


   java.util.Map<String,Class<?>> getTypeMap() throws SQLException;


   void setTypeMap(java.util.Map<String,Class<?>> map) throws SQLException;

  //-----------------------------------------------------------------------
  // The following properties may be used to create a Statement.
  //-----------------------------------------------------------------------


  String getCommand();


  void setCommand(String cmd) throws SQLException;


  boolean isReadOnly();


  void setReadOnly(boolean value) throws SQLException;


  int getMaxFieldSize() throws SQLException;


  void setMaxFieldSize(int max) throws SQLException;


  int getMaxRows() throws SQLException;


  void setMaxRows(int max) throws SQLException;


  boolean getEscapeProcessing() throws SQLException;


  void setEscapeProcessing(boolean enable) throws SQLException;


  int getQueryTimeout() throws SQLException;


  void setQueryTimeout(int seconds) throws SQLException;


  void setType(int type) throws SQLException;


  void setConcurrency(int concurrency) throws SQLException;

  //-----------------------------------------------------------------------
  // Parameters
  //-----------------------------------------------------------------------




  void setNull(int parameterIndex, int sqlType) throws SQLException;


    void setNull(String parameterName, int sqlType) throws SQLException;


  void setNull (int paramIndex, int sqlType, String typeName)
    throws SQLException;


    void setNull (String parameterName, int sqlType, String typeName)
        throws SQLException;


  void setBoolean(int parameterIndex, boolean x) throws SQLException;


    void setBoolean(String parameterName, boolean x) throws SQLException;


  void setByte(int parameterIndex, byte x) throws SQLException;


    void setByte(String parameterName, byte x) throws SQLException;


  void setShort(int parameterIndex, short x) throws SQLException;


    void setShort(String parameterName, short x) throws SQLException;


  void setInt(int parameterIndex, int x) throws SQLException;


    void setInt(String parameterName, int x) throws SQLException;


  void setLong(int parameterIndex, long x) throws SQLException;


    void setLong(String parameterName, long x) throws SQLException;


  void setFloat(int parameterIndex, float x) throws SQLException;


    void setFloat(String parameterName, float x) throws SQLException;


  void setDouble(int parameterIndex, double x) throws SQLException;


    void setDouble(String parameterName, double x) throws SQLException;


  void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException;


    void setBigDecimal(String parameterName, BigDecimal x) throws SQLException;


  void setString(int parameterIndex, String x) throws SQLException;


    void setString(String parameterName, String x) throws SQLException;


  void setBytes(int parameterIndex, byte x[]) throws SQLException;


    void setBytes(String parameterName, byte x[]) throws SQLException;


  void setDate(int parameterIndex, java.sql.Date x) throws SQLException;


  void setTime(int parameterIndex, java.sql.Time x) throws SQLException;


  void setTimestamp(int parameterIndex, java.sql.Timestamp x)
    throws SQLException;


    void setTimestamp(String parameterName, java.sql.Timestamp x)
        throws SQLException;


  void setAsciiStream(int parameterIndex, java.io.InputStream x, int length)
    throws SQLException;


    void setAsciiStream(String parameterName, java.io.InputStream x, int length)
        throws SQLException;


  void setBinaryStream(int parameterIndex, java.io.InputStream x,
                       int length) throws SQLException;


    void setBinaryStream(String parameterName, java.io.InputStream x,
                         int length) throws SQLException;


  void setCharacterStream(int parameterIndex,
                          Reader reader,
                          int length) throws SQLException;


    void setCharacterStream(String parameterName,
                            java.io.Reader reader,
                            int length) throws SQLException;


  void setAsciiStream(int parameterIndex, java.io.InputStream x)
                      throws SQLException;


    void setAsciiStream(String parameterName, java.io.InputStream x)
            throws SQLException;


  void setBinaryStream(int parameterIndex, java.io.InputStream x)
                       throws SQLException;


    void setBinaryStream(String parameterName, java.io.InputStream x)
    throws SQLException;


  void setCharacterStream(int parameterIndex,
                          java.io.Reader reader) throws SQLException;


    void setCharacterStream(String parameterName,
                          java.io.Reader reader) throws SQLException;


   void setNCharacterStream(int parameterIndex, Reader value) throws SQLException;




  void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
            throws SQLException;


    void setObject(String parameterName, Object x, int targetSqlType, int scale)
        throws SQLException;


  void setObject(int parameterIndex, Object x,
                 int targetSqlType) throws SQLException;


    void setObject(String parameterName, Object x, int targetSqlType)
        throws SQLException;


    void setObject(String parameterName, Object x) throws SQLException;


  void setObject(int parameterIndex, Object x) throws SQLException;



  void setRef (int i, Ref x) throws SQLException;


  void setBlob (int i, Blob x) throws SQLException;


     void setBlob(int parameterIndex, InputStream inputStream, long length)
        throws SQLException;


     void setBlob(int parameterIndex, InputStream inputStream)
        throws SQLException;


     void setBlob(String parameterName, InputStream inputStream, long length)
        throws SQLException;


    void setBlob (String parameterName, Blob x) throws SQLException;


     void setBlob(String parameterName, InputStream inputStream)
        throws SQLException;


  void setClob (int i, Clob x) throws SQLException;


     void setClob(int parameterIndex, Reader reader, long length)
       throws SQLException;


     void setClob(int parameterIndex, Reader reader)
       throws SQLException;


     void setClob(String parameterName, Reader reader, long length)
       throws SQLException;


    void setClob (String parameterName, Clob x) throws SQLException;


     void setClob(String parameterName, Reader reader)
       throws SQLException;


  void setArray (int i, Array x) throws SQLException;


  void setDate(int parameterIndex, java.sql.Date x, Calendar cal)
    throws SQLException;


    void setDate(String parameterName, java.sql.Date x)
        throws SQLException;


    void setDate(String parameterName, java.sql.Date x, Calendar cal)
        throws SQLException;


  void setTime(int parameterIndex, java.sql.Time x, Calendar cal)
    throws SQLException;


    void setTime(String parameterName, java.sql.Time x)
        throws SQLException;


    void setTime(String parameterName, java.sql.Time x, Calendar cal)
        throws SQLException;


  void setTimestamp(int parameterIndex, java.sql.Timestamp x, Calendar cal)
    throws SQLException;


    void setTimestamp(String parameterName, java.sql.Timestamp x, Calendar cal)
        throws SQLException;


  void clearParameters() throws SQLException;

  //---------------------------------------------------------------------
  // Reading and writing data
  //---------------------------------------------------------------------


  void execute() throws SQLException;

  //--------------------------------------------------------------------
  // Events
  //--------------------------------------------------------------------


  void addRowSetListener(RowSetListener listener);


  void removeRowSetListener(RowSetListener listener);


     void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException;


    void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException;


    void setRowId(int parameterIndex, RowId x) throws SQLException;


   void setRowId(String parameterName, RowId x) throws SQLException;


     void setNString(int parameterIndex, String value) throws SQLException;


    public void setNString(String parameterName, String value)
            throws SQLException;


     void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException;


    public void setNCharacterStream(String parameterName, Reader value, long length)
            throws SQLException;


     void setNCharacterStream(String parameterName, Reader value) throws SQLException;


    void setNClob(String parameterName, NClob value) throws SQLException;


     void setNClob(String parameterName, Reader reader, long length)
       throws SQLException;


     void setNClob(String parameterName, Reader reader)
       throws SQLException;


     void setNClob(int parameterIndex, Reader reader, long length)
       throws SQLException;


     void setNClob(int parameterIndex, NClob value) throws SQLException;


     void setNClob(int parameterIndex, Reader reader)
       throws SQLException;


    void setURL(int parameterIndex, java.net.URL x) throws SQLException;



}
