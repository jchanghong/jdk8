package javax.sql;
public interface RowSetListener extends java.util.EventListener {
  void rowSetChanged(RowSetEvent event);
  void rowChanged(RowSetEvent event);
  void cursorMoved(RowSetEvent event);
}
