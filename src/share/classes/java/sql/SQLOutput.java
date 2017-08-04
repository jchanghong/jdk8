package java.sql;
 public interface SQLOutput {
  //================================================================
  // Methods for writing attributes to the stream of SQL data.
  // These methods correspond to the column-accessor methods of
  // java.sql.ResultSet.
  //================================================================
  void writeString(String x) throws SQLException;
  void writeBoolean(boolean x) throws SQLException;
  void writeByte(byte x) throws SQLException;
  void writeShort(short x) throws SQLException;
  void writeInt(int x) throws SQLException;
  void writeLong(long x) throws SQLException;
  void writeFloat(float x) throws SQLException;
  void writeDouble(double x) throws SQLException;
  void writeBigDecimal(java.math.BigDecimal x) throws SQLException;
  void writeBytes(byte[] x) throws SQLException;
  void writeDate(java.sql.Date x) throws SQLException;
  void writeTime(java.sql.Time x) throws SQLException;
  void writeTimestamp(java.sql.Timestamp x) throws SQLException;
  void writeCharacterStream(java.io.Reader x) throws SQLException;
  void writeAsciiStream(java.io.InputStream x) throws SQLException;
  void writeBinaryStream(java.io.InputStream x) throws SQLException;
  //================================================================
  // Methods for writing items of SQL user-defined types to the stream.
  // These methods pass objects to the database as values of SQL
  // Structured Types, Distinct Types, Constructed Types, and Locator
  // Types.  They decompose the Java object(s) and write leaf data
  // items using the methods above.
  //================================================================
  void writeObject(SQLData x) throws SQLException;
  void writeRef(Ref x) throws SQLException;
  void writeBlob(Blob x) throws SQLException;
  void writeClob(Clob x) throws SQLException;
  void writeStruct(Struct x) throws SQLException;
  void writeArray(Array x) throws SQLException;
     //--------------------------- JDBC 3.0 ------------------------
     void writeURL(java.net.URL x) throws SQLException;
     //--------------------------- JDBC 4.0 ------------------------
  void writeNString(String x) throws SQLException;
  void writeNClob(NClob x) throws SQLException;
  void writeRowId(RowId x) throws SQLException;
  void writeSQLXML(SQLXML x) throws SQLException;
  //--------------------------JDBC 4.2 -----------------------------
  default void writeObject(Object x, SQLType targetSqlType) throws SQLException {
        throw new SQLFeatureNotSupportedException();
  }
}
