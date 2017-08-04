

package javax.sql.rowset.serial;

import java.sql.*;
import java.io.*;
import java.util.Map;
import java.net.URL;
import java.util.Arrays;



public class SerialArray implements Array, Serializable, Cloneable {


    private Object[] elements;


    private int baseType;


    private String baseTypeName;


    private int len;


     public SerialArray(Array array, Map<String,Class<?>> map)
         throws SerialException, SQLException
     {

        if ((array == null) || (map == null)) {
            throw new SQLException("Cannot instantiate a SerialArray " +
            "object with null parameters");
        }

        if ((elements = (Object[])array.getArray()) == null) {
             throw new SQLException("Invalid Array object. Calls to Array.getArray() " +
                 "return null value which cannot be serialized");
         }

        elements = (Object[])array.getArray(map);
        baseType = array.getBaseType();
        baseTypeName = array.getBaseTypeName();
        len = elements.length;

        switch (baseType) {
            case java.sql.Types.STRUCT:
                for (int i = 0; i < len; i++) {
                    elements[i] = new SerialStruct((Struct)elements[i], map);
                }
            break;

            case java.sql.Types.ARRAY:
                for (int i = 0; i < len; i++) {
                    elements[i] = new SerialArray((Array)elements[i], map);
                }
            break;

            case java.sql.Types.BLOB:
            for (int i = 0; i < len; i++) {
                elements[i] = new SerialBlob((Blob)elements[i]);
            }
            break;

            case java.sql.Types.CLOB:
                for (int i = 0; i < len; i++) {
                    elements[i] = new SerialClob((Clob)elements[i]);
                }
            break;

            case java.sql.Types.DATALINK:
                for (int i = 0; i < len; i++) {
                    elements[i] = new SerialDatalink((URL)elements[i]);
                }
            break;

            case java.sql.Types.JAVA_OBJECT:
                for (int i = 0; i < len; i++) {
                elements[i] = new SerialJavaObject(elements[i]);
            }
        }
  }


    public void free() throws SQLException {
        if (elements != null) {
            elements = null;
            baseTypeName= null;
        }
    }


     public SerialArray(Array array) throws SerialException, SQLException {
         if (array == null) {
             throw new SQLException("Cannot instantiate a SerialArray " +
                 "object with a null Array object");
         }

         if ((elements = (Object[])array.getArray()) == null) {
             throw new SQLException("Invalid Array object. Calls to Array.getArray() " +
                 "return null value which cannot be serialized");
         }

         //elements = (Object[])array.getArray();
         baseType = array.getBaseType();
         baseTypeName = array.getBaseTypeName();
         len = elements.length;

        switch (baseType) {

        case java.sql.Types.BLOB:
            for (int i = 0; i < len; i++) {
                elements[i] = new SerialBlob((Blob)elements[i]);
            }
            break;

        case java.sql.Types.CLOB:
            for (int i = 0; i < len; i++) {
                elements[i] = new SerialClob((Clob)elements[i]);
            }
            break;

        case java.sql.Types.DATALINK:
            for (int i = 0; i < len; i++) {
                elements[i] = new SerialDatalink((URL)elements[i]);
            }
            break;

        case java.sql.Types.JAVA_OBJECT:
            for (int i = 0; i < len; i++) {
                elements[i] = new SerialJavaObject(elements[i]);
            }
            break;

        }


    }


    public Object getArray() throws SerialException {
        isValid();
        Object dst = new Object[len];
        System.arraycopy((Object)elements, 0, dst, 0, len);
        return dst;
    }

 //[if an error occurstype map used??]

    public Object getArray(Map<String, Class<?>> map) throws SerialException {
        isValid();
        Object dst[] = new Object[len];
        System.arraycopy((Object)elements, 0, dst, 0, len);
        return dst;
    }


    public Object getArray(long index, int count) throws SerialException {
        isValid();
        Object dst = new Object[count];
        System.arraycopy((Object)elements, (int)index, dst, 0, count);
        return dst;
    }


    public Object getArray(long index, int count, Map<String,Class<?>> map)
        throws SerialException
    {
        isValid();
        Object dst = new Object[count];
        System.arraycopy((Object)elements, (int)index, dst, 0, count);
        return dst;
    }


    public int getBaseType() throws SerialException {
        isValid();
        return baseType;
    }


    public String getBaseTypeName() throws SerialException {
        isValid();
        return baseTypeName;
    }


    public ResultSet getResultSet(long index, int count) throws SerialException {
        SerialException se = new SerialException();
        se.initCause(new UnsupportedOperationException());
        throw  se;
    }


    public ResultSet getResultSet(Map<String, Class<?>> map)
        throws SerialException
    {
        SerialException se = new SerialException();
        se.initCause(new UnsupportedOperationException());
        throw  se;
    }


    public ResultSet getResultSet() throws SerialException {
        SerialException se = new SerialException();
        se.initCause(new UnsupportedOperationException());
        throw  se;
    }



    public ResultSet getResultSet(long index, int count,
                                  Map<String,Class<?>> map)
        throws SerialException
    {
        SerialException se = new SerialException();
        se.initCause(new UnsupportedOperationException());
        throw  se;
    }



    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof SerialArray) {
            SerialArray sa = (SerialArray)obj;
            return baseType == sa.baseType &&
                    baseTypeName.equals(sa.baseTypeName) &&
                    Arrays.equals(elements, sa.elements);
        }
        return false;
    }


    public int hashCode() {
        return (((31 + Arrays.hashCode(elements)) * 31 + len)  * 31 +
                baseType) * 31 + baseTypeName.hashCode();
    }


    public Object clone() {
        try {
            SerialArray sa = (SerialArray) super.clone();
            sa.elements = (elements != null) ? Arrays.copyOf(elements, len) : null;
            return sa;
        } catch (CloneNotSupportedException ex) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }

    }


    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {

       ObjectInputStream.GetField fields = s.readFields();
       Object[] tmp = (Object[])fields.get("elements", null);
       if (tmp == null)
           throw new InvalidObjectException("elements is null and should not be!");
       elements = tmp.clone();
       len = fields.get("len", 0);
       if(elements.length != len)
           throw new InvalidObjectException("elements is not the expected size");

       baseType = fields.get("baseType", 0);
       baseTypeName = (String)fields.get("baseTypeName", null);
    }


    private void writeObject(ObjectOutputStream s)
            throws IOException, ClassNotFoundException {

        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("elements", elements);
        fields.put("len", len);
        fields.put("baseType", baseType);
        fields.put("baseTypeName", baseTypeName);
        s.writeFields();
    }


    private void isValid() throws SerialException {
        if (elements == null) {
            throw new SerialException("Error: You cannot call a method on a "
                    + "SerialArray instance once free() has been called.");
        }
    }


    static final long serialVersionUID = -8466174297270688520L;
}
