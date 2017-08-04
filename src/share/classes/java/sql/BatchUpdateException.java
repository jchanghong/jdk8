package java.sql;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
public class BatchUpdateException extends SQLException {
  public BatchUpdateException( String reason, String SQLState, int vendorCode,
                               int[] updateCounts ) {
      super(reason, SQLState, vendorCode);
      this.updateCounts  = (updateCounts == null) ? null : Arrays.copyOf(updateCounts, updateCounts.length);
      this.longUpdateCounts = (updateCounts == null) ? null : copyUpdateCount(updateCounts);
  }
  public BatchUpdateException(String reason, String SQLState,
                              int[] updateCounts) {
      this(reason, SQLState, 0, updateCounts);
  }
  public  BatchUpdateException(String reason, int[] updateCounts) {
      this(reason, null, 0, updateCounts);
  }
  public BatchUpdateException(int[] updateCounts) {
      this(null, null, 0, updateCounts);
  }
  public BatchUpdateException() {
        this(null, null, 0, null);
  }
  public BatchUpdateException(Throwable cause) {
      this((cause == null ? null : cause.toString()), null, 0, (int[])null, cause);
  }
  public BatchUpdateException(int []updateCounts , Throwable cause) {
      this((cause == null ? null : cause.toString()), null, 0, updateCounts, cause);
  }
  public BatchUpdateException(String reason, int []updateCounts, Throwable cause) {
      this(reason, null, 0, updateCounts, cause);
  }
  public BatchUpdateException(String reason, String SQLState,
          int []updateCounts, Throwable cause) {
      this(reason, SQLState, 0, updateCounts, cause);
  }
  public BatchUpdateException(String reason, String SQLState, int vendorCode,
                                int []updateCounts,Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
        this.updateCounts  = (updateCounts == null) ? null : Arrays.copyOf(updateCounts, updateCounts.length);
        this.longUpdateCounts = (updateCounts == null) ? null : copyUpdateCount(updateCounts);
  }
  public int[] getUpdateCounts() {
      return (updateCounts == null) ? null : Arrays.copyOf(updateCounts, updateCounts.length);
  }
  public BatchUpdateException(String reason, String SQLState, int vendorCode,
          long []updateCounts,Throwable cause) {
      super(reason, SQLState, vendorCode, cause);
      this.longUpdateCounts  = (updateCounts == null) ? null : Arrays.copyOf(updateCounts, updateCounts.length);
      this.updateCounts = (longUpdateCounts == null) ? null : copyUpdateCount(longUpdateCounts);
  }
  public long[] getLargeUpdateCounts() {
      return (longUpdateCounts == null) ? null :
              Arrays.copyOf(longUpdateCounts, longUpdateCounts.length);
  }
  private  int[] updateCounts;
  private  long[] longUpdateCounts;
  private static final long serialVersionUID = 5977529877145521757L;
  private static long[] copyUpdateCount(int[] uc) {
      long[] copy = new long[uc.length];
      for(int i= 0; i< uc.length; i++) {
          copy[i] = uc[i];
      }
      return copy;
  }
  private static int[] copyUpdateCount(long[] uc) {
      int[] copy = new int[uc.length];
      for(int i= 0; i< uc.length; i++) {
          copy[i] = (int) uc[i];
      }
      return copy;
  }
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
       ObjectInputStream.GetField fields = s.readFields();
       int[] tmp = (int[])fields.get("updateCounts", null);
       long[] tmp2 = (long[])fields.get("longUpdateCounts", null);
       if(tmp != null && tmp2 != null && tmp.length != tmp2.length)
           throw new InvalidObjectException("update counts are not the expected size");
       if (tmp != null)
           updateCounts = tmp.clone();
       if (tmp2 != null)
           longUpdateCounts = tmp2.clone();
       if(updateCounts == null && longUpdateCounts != null)
           updateCounts = copyUpdateCount(longUpdateCounts);
       if(longUpdateCounts == null && updateCounts != null)
           longUpdateCounts = copyUpdateCount(updateCounts);
    }
    private void writeObject(ObjectOutputStream s)
            throws IOException, ClassNotFoundException {
        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("updateCounts", updateCounts);
        fields.put("longUpdateCounts", longUpdateCounts);
        s.writeFields();
    }
}
