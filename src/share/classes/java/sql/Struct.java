package java.sql;
public interface Struct {
  String getSQLTypeName() throws SQLException;
  Object[] getAttributes() throws SQLException;
  Object[] getAttributes(java.util.Map<String,Class<?>> map)
      throws SQLException;
}
