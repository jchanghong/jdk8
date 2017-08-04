

package java.rmi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;

import sun.rmi.server.MarshalInputStream;
import sun.rmi.server.MarshalOutputStream;

import sun.misc.ObjectInputFilter;


public final class MarshalledObject<T> implements Serializable {

    private byte[] objBytes = null;


    private byte[] locBytes = null;


    private int hash;


    private transient ObjectInputFilter objectInputFilter = null;


    private static final long serialVersionUID = 8988374069173025854L;


    public MarshalledObject(T obj) throws IOException {
        if (obj == null) {
            hash = 13;
            return;
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream lout = new ByteArrayOutputStream();
        MarshalledObjectOutputStream out =
            new MarshalledObjectOutputStream(bout, lout);
        out.writeObject(obj);
        out.flush();
        objBytes = bout.toByteArray();
        // locBytes is null if no annotations
        locBytes = (out.hadAnnotations() ? lout.toByteArray() : null);


        int h = 0;
        for (int i = 0; i < objBytes.length; i++) {
            h = 31 * h + objBytes[i];
        }
        hash = h;
    }


    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
        stream.defaultReadObject();     // read in all fields
        objectInputFilter = ObjectInputFilter.Config.getObjectInputFilter(stream);
    }


    public T get() throws IOException, ClassNotFoundException {
        if (objBytes == null)   // must have been a null object
            return null;

        ByteArrayInputStream bin = new ByteArrayInputStream(objBytes);
        // locBytes is null if no annotations
        ByteArrayInputStream lin =
            (locBytes == null ? null : new ByteArrayInputStream(locBytes));
        MarshalledObjectInputStream in =
            new MarshalledObjectInputStream(bin, lin, objectInputFilter);
        @SuppressWarnings("unchecked")
        T obj = (T) in.readObject();
        in.close();
        return obj;
    }


    public int hashCode() {
        return hash;
    }


    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj != null && obj instanceof MarshalledObject) {
            MarshalledObject<?> other = (MarshalledObject<?>) obj;

            // if either is a ref to null, both must be
            if (objBytes == null || other.objBytes == null)
                return objBytes == other.objBytes;

            // quick, easy test
            if (objBytes.length != other.objBytes.length)
                return false;

            //!! There is talk about adding an array comparision method
            //!! at 1.2 -- if so, this should be rewritten.  -arnold
            for (int i = 0; i < objBytes.length; ++i) {
                if (objBytes[i] != other.objBytes[i])
                    return false;
            }
            return true;
        } else {
            return false;
        }
    }


    private static class MarshalledObjectOutputStream
        extends MarshalOutputStream
    {

        private ObjectOutputStream locOut;


        private boolean hadAnnotations;


        MarshalledObjectOutputStream(OutputStream objOut, OutputStream locOut)
            throws IOException
        {
            super(objOut);
            this.useProtocolVersion(ObjectStreamConstants.PROTOCOL_VERSION_2);
            this.locOut = new ObjectOutputStream(locOut);
            hadAnnotations = false;
        }


        boolean hadAnnotations() {
            return hadAnnotations;
        }


        protected void writeLocation(String loc) throws IOException {
            hadAnnotations |= (loc != null);
            locOut.writeObject(loc);
        }


        public void flush() throws IOException {
            super.flush();
            locOut.flush();
        }
    }


    private static class MarshalledObjectInputStream
        extends MarshalInputStream
    {

        private ObjectInputStream locIn;


        MarshalledObjectInputStream(InputStream objIn, InputStream locIn,
                    ObjectInputFilter filter)
            throws IOException
        {
            super(objIn);
            this.locIn = (locIn == null ? null : new ObjectInputStream(locIn));
            if (filter != null) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        ObjectInputFilter.Config.setObjectInputFilter(MarshalledObjectInputStream.this, filter);
                        if (MarshalledObjectInputStream.this.locIn != null) {
                            ObjectInputFilter.Config.setObjectInputFilter(MarshalledObjectInputStream.this.locIn, filter);
                        }
                        return null;
                    }
                });
            }
        }


        protected Object readLocation()
            throws IOException, ClassNotFoundException
        {
            return (locIn == null ? null : locIn.readObject());
        }
    }

}
