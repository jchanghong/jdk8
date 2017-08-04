package java.sql;
public interface RowId {
    boolean equals(Object obj);
     byte[] getBytes();
     String toString();
     int hashCode();
}
