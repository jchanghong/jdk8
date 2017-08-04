

package javax.sql.rowset.serial;

import java.sql.*;
import javax.sql.*;
import java.io.*;
import java.math.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;

import javax.sql.rowset.*;


public class SerialStruct implements Struct, Serializable, Cloneable {



    private String SQLTypeName;


    private Object attribs[];


     public SerialStruct(Struct in, Map<String,Class<?>> map)
         throws SerialException
     {

        try {

        // get the type name
        SQLTypeName = in.getSQLTypeName();
        System.out.println("SQLTypeName: " + SQLTypeName);

        // get the attributes of the struct
        attribs = in.getAttributes(map);


        mapToSerial(map);

        } catch (SQLException e) {
            throw new SerialException(e.getMessage());
        }
    }


    public SerialStruct(SQLData in, Map<String,Class<?>> map)
        throws SerialException
    {

        try {

        //set the type name
        SQLTypeName = in.getSQLTypeName();

        Vector<Object> tmp = new Vector<>();
        in.writeSQL(new SQLOutputImpl(tmp, map));
        attribs = tmp.toArray();

        } catch (SQLException e) {
            throw new SerialException(e.getMessage());
        }
    }



    public String getSQLTypeName() throws SerialException {
        return SQLTypeName;
    }


    public Object[]  getAttributes() throws SerialException {
        Object[] val = this.attribs;
        return (val == null) ? null : Arrays.copyOf(val, val.length);
    }


    public Object[] getAttributes(Map<String,Class<?>> map)
        throws SerialException
    {
        Object[] val = this.attribs;
        return (val == null) ? null : Arrays.copyOf(val, val.length);
    }



    private void mapToSerial(Map<String,Class<?>> map) throws SerialException {

        try {

        for (int i = 0; i < attribs.length; i++) {
            if (attribs[i] instanceof Struct) {
                attribs[i] = new SerialStruct((Struct)attribs[i], map);
            } else if (attribs[i] instanceof SQLData) {
                attribs[i] = new SerialStruct((SQLData)attribs[i], map);
            } else if (attribs[i] instanceof Blob) {
                attribs[i] = new SerialBlob((Blob)attribs[i]);
            } else if (attribs[i] instanceof Clob) {
                attribs[i] = new SerialClob((Clob)attribs[i]);
            } else if (attribs[i] instanceof Ref) {
                attribs[i] = new SerialRef((Ref)attribs[i]);
            } else if (attribs[i] instanceof java.sql.Array) {
                attribs[i] = new SerialArray((java.sql.Array)attribs[i], map);
            }
        }

        } catch (SQLException e) {
            throw new SerialException(e.getMessage());
        }
        return;
    }


    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SerialStruct) {
            SerialStruct ss = (SerialStruct)obj;
            return SQLTypeName.equals(ss.SQLTypeName) &&
                    Arrays.equals(attribs, ss.attribs);
        }
        return false;
    }


    public int hashCode() {
        return ((31 + Arrays.hashCode(attribs)) * 31) * 31
                + SQLTypeName.hashCode();
    }


    public Object clone() {
        try {
            SerialStruct ss = (SerialStruct) super.clone();
            ss.attribs = Arrays.copyOf(attribs, attribs.length);
            return ss;
        } catch (CloneNotSupportedException ex) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }

    }


    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {

       ObjectInputStream.GetField fields = s.readFields();
       Object[] tmp = (Object[])fields.get("attribs", null);
       attribs = tmp == null ? null : tmp.clone();
       SQLTypeName = (String)fields.get("SQLTypeName", null);
    }


    private void writeObject(ObjectOutputStream s)
            throws IOException, ClassNotFoundException {

        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("attribs", attribs);
        fields.put("SQLTypeName", SQLTypeName);
        s.writeFields();
    }


    static final long serialVersionUID = -8322445504027483372L;
}
