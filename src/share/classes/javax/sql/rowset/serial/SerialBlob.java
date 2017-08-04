

package javax.sql.rowset.serial;

import java.sql.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.Arrays;



public class SerialBlob implements Blob, Serializable, Cloneable {


    private byte[] buf;


    private Blob blob;


    private long len;


    private long origLen;


    public SerialBlob(byte[] b)
            throws SerialException, SQLException {

        len = b.length;
        buf = new byte[(int)len];
        for(int i = 0; i < len; i++) {
            buf[i] = b[i];
        }
        origLen = len;
    }



    public SerialBlob (Blob blob)
            throws SerialException, SQLException {

        if (blob == null) {
            throw new SQLException(
                    "Cannot instantiate a SerialBlob object with a null Blob object");
        }

        len = blob.length();
        buf = blob.getBytes(1, (int)len );
        this.blob = blob;
        origLen = len;
    }


    public byte[] getBytes(long pos, int length) throws SerialException {
        isValid();
        if (length > len) {
            length = (int)len;
        }

        if (pos < 1 || len - pos < 0 ) {
            throw new SerialException("Invalid arguments: position cannot be "
                    + "less than 1 or greater than the length of the SerialBlob");
        }

        pos--; // correct pos to array index

        byte[] b = new byte[length];

        for (int i = 0; i < length; i++) {
            b[i] = this.buf[(int)pos];
            pos++;
        }
        return b;
    }


    public long length() throws SerialException {
        isValid();
        return len;
    }


    public java.io.InputStream getBinaryStream() throws SerialException {
        isValid();
        InputStream stream = new ByteArrayInputStream(buf);
        return stream;
    }


    public long position(byte[] pattern, long start)
            throws SerialException, SQLException {

        isValid();
        if (start < 1 || start > len) {
            return -1;
        }

        int pos = (int)start-1; // internally Blobs are stored as arrays.
        int i = 0;
        long patlen = pattern.length;

        while (pos < len) {
            if (pattern[i] == buf[pos]) {
                if (i + 1 == patlen) {
                    return (pos + 1) - (patlen - 1);
                }
                i++; pos++; // increment pos, and i
            } else if (pattern[i] != buf[pos]) {
                pos++; // increment pos only
            }
        }
        return -1; // not found
    }


    public long position(Blob pattern, long start)
            throws SerialException, SQLException {
        isValid();
        return position(pattern.getBytes(1, (int)(pattern.length())), start);
    }


    public int setBytes(long pos, byte[] bytes)
            throws SerialException, SQLException {
        return setBytes(pos, bytes, 0, bytes.length);
    }


    public int setBytes(long pos, byte[] bytes, int offset, int length)
            throws SerialException, SQLException {

        isValid();
        if (offset < 0 || offset > bytes.length) {
            throw new SerialException("Invalid offset in byte array set");
        }

        if (pos < 1 || pos > this.length()) {
            throw new SerialException("Invalid position in BLOB object set");
        }

        if ((long)(length) > origLen) {
            throw new SerialException("Buffer is not sufficient to hold the value");
        }

        if ((length + offset) > bytes.length) {
            throw new SerialException("Invalid OffSet. Cannot have combined offset " +
                    "and length that is greater that the Blob buffer");
        }

        int i = 0;
        pos--; // correct to array indexing
        while ( i < length || (offset + i +1) < (bytes.length-offset) ) {
            this.buf[(int)pos + i] = bytes[offset + i ];
            i++;
        }
        return i;
    }


    public java.io.OutputStream setBinaryStream(long pos)
            throws SerialException, SQLException {

        isValid();
        if (this.blob != null) {
            return this.blob.setBinaryStream(pos);
        } else {
            throw new SerialException("Unsupported operation. SerialBlob cannot " +
                "return a writable binary stream, unless instantiated with a Blob object " +
                "that provides a setBinaryStream() implementation");
        }
    }


    public void truncate(long length) throws SerialException {
        isValid();
        if (length > len) {
            throw new SerialException(
                    "Length more than what can be truncated");
        } else if((int)length == 0) {
            buf = new byte[0];
            len = length;
        } else {
            len = length;
            buf = this.getBytes(1, (int)len);
        }
    }



    public InputStream getBinaryStream(long pos, long length) throws SQLException {
        isValid();
        if (pos < 1 || pos > this.length()) {
            throw new SerialException("Invalid position in BLOB object set");
        }
        if (length < 1 || length > len - pos + 1) {
            throw new SerialException(
                    "length is < 1 or pos + length > total number of bytes");
        }
        return new ByteArrayInputStream(buf, (int) pos - 1, (int) length);
    }



    public void free() throws SQLException {
        if (buf != null) {
            buf = null;
            if (blob != null) {
                blob.free();
            }
            blob = null;
        }
    }


    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SerialBlob) {
            SerialBlob sb = (SerialBlob)obj;
            if (this.len == sb.len) {
                return Arrays.equals(buf, sb.buf);
            }
        }
        return false;
    }


    public int hashCode() {
       return ((31 + Arrays.hashCode(buf)) * 31 + (int)len) * 31 + (int)origLen;
    }


    public Object clone() {
        try {
            SerialBlob sb = (SerialBlob) super.clone();
            sb.buf = (buf != null) ? Arrays.copyOf(buf, (int)len) : null;
            sb.blob = null;
            return sb;
        } catch (CloneNotSupportedException ex) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }


    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {

        ObjectInputStream.GetField fields = s.readFields();
        byte[] tmp = (byte[])fields.get("buf", null);
        if (tmp == null)
            throw new InvalidObjectException("buf is null and should not be!");
        buf = tmp.clone();
        len = fields.get("len", 0L);
        if (buf.length != len)
            throw new InvalidObjectException("buf is not the expected size");
        origLen = fields.get("origLen", 0L);
        blob = (Blob) fields.get("blob", null);
    }


    private void writeObject(ObjectOutputStream s)
            throws IOException, ClassNotFoundException {

        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("buf", buf);
        fields.put("len", len);
        fields.put("origLen", origLen);
        // Note: this check to see if it is an instance of Serializable
        // is for backwards compatibiity
        fields.put("blob", blob instanceof Serializable ? blob : null);
        s.writeFields();
    }


    private void isValid() throws SerialException {
        if (buf == null) {
            throw new SerialException("Error: You cannot call a method on a " +
                    "SerialBlob instance once free() has been called.");
        }
    }


    static final long serialVersionUID = -8144641928112860441L;
}
