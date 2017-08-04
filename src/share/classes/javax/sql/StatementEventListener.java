


package javax.sql;


public interface StatementEventListener  extends java.util.EventListener{

  void statementClosed(StatementEvent event);


        void statementErrorOccurred(StatementEvent event);

}
