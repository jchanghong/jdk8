

package javax.sql.rowset.serial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Map;
import java.util.Vector;


public class SQLOutputImpl implements SQLOutput {


    @SuppressWarnings("rawtypes")
    private Vector attribs;


    @SuppressWarnings("rawtypes")
    private Map map;


    public SQLOutputImpl(Vector<?> attributes, Map<String,?> map)
        throws SQLException
    {
        if ((attributes == null) || (map == null)) {
            throw new SQLException("Cannot instantiate a SQLOutputImpl " +
            "instance with null parameters");
        }
        this.attribs = attributes;
        this.map = map;
    }

    //================================================================
    // Methods for writing attributes to the stream of SQL data.
    // These methods correspond to the column-accessor methods of
    // java.sql.ResultSet.
    //================================================================


    @SuppressWarnings("unchecked")
    public void writeString(String x) throws SQLException {
        //System.out.println("Adding :"+x);
        attribs.add(x);
    }


    @SuppressWarnings("unchecked")
    public void writeBoolean(boolean x) throws SQLException {
        attribs.add(Boolean.valueOf(x));
    }


    @SuppressWarnings("unchecked")
    public void writeByte(byte x) throws SQLException {
        attribs.add(Byte.valueOf(x));
    }


    @SuppressWarnings("unchecked")
    public void writeShort(short x) throws SQLException {
        attribs.add(Short.valueOf(x));
    }


    @SuppressWarnings("unchecked")
    public void writeInt(int x) throws SQLException {
        attribs.add(Integer.valueOf(x));
    }


    @SuppressWarnings("unchecked")
    public void writeLong(long x) throws SQLException {
        attribs.add(Long.valueOf(x));
    }


    @SuppressWarnings("unchecked")
    public void writeFloat(float x) throws SQLException {
        attribs.add(Float.valueOf(x));
    }


    @SuppressWarnings("unchecked")
    public void writeDouble(double x) throws SQLException{
        attribs.add(Double.valueOf(x));
    }


    @SuppressWarnings("unchecked")
    public void writeBigDecimal(java.math.BigDecimal x) throws SQLException{
        attribs.add(x);
    }


    @SuppressWarnings("unchecked")
    public void writeBytes(byte[] x) throws SQLException {
        attribs.add(x);
    }


    @SuppressWarnings("unchecked")
    public void writeDate(java.sql.Date x) throws SQLException {
        attribs.add(x);
    }


    @SuppressWarnings("unchecked")
    public void writeTime(java.sql.Time x) throws SQLException {
        attribs.add(x);
    }


    @SuppressWarnings("unchecked")
    public void writeTimestamp(java.sql.Timestamp x) throws SQLException {
        attribs.add(x);
    }


    @SuppressWarnings("unchecked")
    public void writeCharacterStream(java.io.Reader x) throws SQLException {
         BufferedReader bufReader = new BufferedReader(x);
         try {
             int i;
             while( (i = bufReader.read()) != -1 ) {
                char ch = (char)i;
                StringBuffer strBuf = new StringBuffer();
                strBuf.append(ch);

                String str = new String(strBuf);
                String strLine = bufReader.readLine();

                writeString(str.concat(strLine));
             }
         } catch(IOException ioe) {

         }
    }


    @SuppressWarnings("unchecked")
    public void writeAsciiStream(java.io.InputStream x) throws SQLException {
         BufferedReader bufReader = new BufferedReader(new InputStreamReader(x));
         try {
               int i;
               while( (i=bufReader.read()) != -1 ) {
                char ch = (char)i;

                StringBuffer strBuf = new StringBuffer();
                strBuf.append(ch);

                String str = new String(strBuf);
                String strLine = bufReader.readLine();

                writeString(str.concat(strLine));
            }
          }catch(IOException ioe) {
            throw new SQLException(ioe.getMessage());
        }
    }


    @SuppressWarnings("unchecked")
    public void writeBinaryStream(java.io.InputStream x) throws SQLException {
         BufferedReader bufReader = new BufferedReader(new InputStreamReader(x));
         try {
               int i;
             while( (i=bufReader.read()) != -1 ) {
                char ch = (char)i;

                StringBuffer strBuf = new StringBuffer();
                strBuf.append(ch);

                String str = new String(strBuf);
                String strLine = bufReader.readLine();

                writeString(str.concat(strLine));
             }
        } catch(IOException ioe) {
            throw new SQLException(ioe.getMessage());
        }
    }

    //================================================================
    // Methods for writing items of SQL user-defined types to the stream.
    // These methods pass objects to the database as values of SQL
    // Structured Types, Distinct Types, Constructed Types, and Locator
    // Types.  They decompose the Java object(s) and write leaf data
    // items using the methods above.
    //================================================================


    @SuppressWarnings("unchecked")
    public void writeObject(SQLData x) throws SQLException {


        if (x == null) {
            attribs.add(null);
        } else {

            attribs.add(new SerialStruct(x, map));
        }
    }


    @SuppressWarnings("unchecked")
    public void writeRef(Ref x) throws SQLException {
        if (x == null) {
            attribs.add(null);
        } else {
            attribs.add(new SerialRef(x));
        }
    }


    @SuppressWarnings("unchecked")
    public void writeBlob(Blob x) throws SQLException {
        if (x == null) {
            attribs.add(null);
        } else {
            attribs.add(new SerialBlob(x));
        }
    }


    @SuppressWarnings("unchecked")
    public void writeClob(Clob x) throws SQLException {
        if (x == null) {
            attribs.add(null);
        } else {
            attribs.add(new SerialClob(x));
        }
    }


    @SuppressWarnings("unchecked")
    public void writeStruct(Struct x) throws SQLException {
        SerialStruct s = new SerialStruct(x,map);;
        attribs.add(s);
    }


    @SuppressWarnings("unchecked")
    public void writeArray(Array x) throws SQLException {
        if (x == null) {
            attribs.add(null);
        } else {
            attribs.add(new SerialArray(x, map));
        }
    }


    @SuppressWarnings("unchecked")
    public void writeURL(java.net.URL url) throws SQLException {
        if (url == null) {
            attribs.add(null);
        } else {
            attribs.add(new SerialDatalink(url));
        }
    }



   @SuppressWarnings("unchecked")
   public void writeNString(String x) throws SQLException {
       attribs.add(x);
   }


   @SuppressWarnings("unchecked")
   public void writeNClob(NClob x) throws SQLException {
           attribs.add(x);
   }



   @SuppressWarnings("unchecked")
   public void writeRowId(RowId x) throws SQLException {
        attribs.add(x);
   }



   @SuppressWarnings("unchecked")
   public void writeSQLXML(SQLXML x) throws SQLException {
        attribs.add(x);
    }

}
