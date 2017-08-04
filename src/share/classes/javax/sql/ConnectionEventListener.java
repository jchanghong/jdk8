

package javax.sql;



public interface ConnectionEventListener extends java.util.EventListener {


  void connectionClosed(ConnectionEvent event);


  void connectionErrorOccurred(ConnectionEvent event);

 }
