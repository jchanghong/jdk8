package java.sql;
public interface SQLInput {
    //================================================================
    // Methods for reading attributes from the stream of SQL data.
    // These methods correspond to the column-accessor methods of
    // java.sql.ResultSet.
    //================================================================
    String readString() throws SQLException;
    boolean readBoolean() throws SQLException;
    byte readByte() throws SQLException;
    short readShort() throws SQLException;
    int readInt() throws SQLException;
    long readLong() throws SQLException;
    float readFloat() throws SQLException;
    double readDouble() throws SQLException;
    java.math.BigDecimal readBigDecimal() throws SQLException;
    byte[] readBytes() throws SQLException;
    java.sql.Date readDate() throws SQLException;
    java.sql.Time readTime() throws SQLException;
    java.sql.Timestamp readTimestamp() throws SQLException;
    java.io.Reader readCharacterStream() throws SQLException;
    java.io.InputStream readAsciiStream() throws SQLException;
    java.io.InputStream readBinaryStream() throws SQLException;
    //================================================================
    // Methods for reading items of SQL user-defined types from the stream.
    //================================================================
    Object readObject() throws SQLException;
    Ref readRef() throws SQLException;
    Blob readBlob() throws SQLException;
    Clob readClob() throws SQLException;
    Array readArray() throws SQLException;
    boolean wasNull() throws SQLException;
    //---------------------------- JDBC 3.0 -------------------------
    java.net.URL readURL() throws SQLException;
     //---------------------------- JDBC 4.0 -------------------------
    NClob readNClob() throws SQLException;
    String readNString() throws SQLException;
    SQLXML readSQLXML() throws SQLException;
    RowId readRowId() throws SQLException;
    //--------------------------JDBC 4.2 -----------------------------
    default <T> T readObject(Class<T> type) throws SQLException {
       throw new SQLFeatureNotSupportedException();
    }
}
