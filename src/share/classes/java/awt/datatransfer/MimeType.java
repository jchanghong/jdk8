

package java.awt.datatransfer;

import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;



class MimeType implements Externalizable, Cloneable {



    static final long serialVersionUID = -6568722458793895906L;


    public MimeType() {
    }


    public MimeType(String rawdata) throws MimeTypeParseException {
        parse(rawdata);
    }


    public MimeType(String primary, String sub) throws MimeTypeParseException {
        this(primary, sub, new MimeTypeParameterList());
    }


    public MimeType(String primary, String sub, MimeTypeParameterList mtpl) throws
MimeTypeParseException {
        //    check to see if primary is valid
        if(isValidToken(primary)) {
            primaryType = primary.toLowerCase(Locale.ENGLISH);
        } else {
            throw new MimeTypeParseException("Primary type is invalid.");
        }

        //    check to see if sub is valid
        if(isValidToken(sub)) {
            subType = sub.toLowerCase(Locale.ENGLISH);
        } else {
            throw new MimeTypeParseException("Sub type is invalid.");
        }

        parameters = (MimeTypeParameterList)mtpl.clone();
    }

    public int hashCode() {

        // We sum up the hash codes for all of the strings. This
        // way, the order of the strings is irrelevant
        int code = 0;
        code += primaryType.hashCode();
        code += subType.hashCode();
        code += parameters.hashCode();
        return code;
    } // hashCode()


    public boolean equals(Object thatObject) {
        if (!(thatObject instanceof MimeType)) {
            return false;
        }
        MimeType that = (MimeType)thatObject;
        boolean isIt =
            ((this.primaryType.equals(that.primaryType)) &&
             (this.subType.equals(that.subType)) &&
             (this.parameters.equals(that.parameters)));
        return isIt;
    } // equals()


    private void parse(String rawdata) throws MimeTypeParseException {
        int slashIndex = rawdata.indexOf('/');
        int semIndex = rawdata.indexOf(';');
        if((slashIndex < 0) && (semIndex < 0)) {
            //    neither character is present, so treat it
            //    as an error
            throw new MimeTypeParseException("Unable to find a sub type.");
        } else if((slashIndex < 0) && (semIndex >= 0)) {
            //    we have a ';' (and therefore a parameter list),
            //    but no '/' indicating a sub type is present
            throw new MimeTypeParseException("Unable to find a sub type.");
        } else if((slashIndex >= 0) && (semIndex < 0)) {
            //    we have a primary and sub type but no parameter list
            primaryType = rawdata.substring(0,slashIndex).
                trim().toLowerCase(Locale.ENGLISH);
            subType = rawdata.substring(slashIndex + 1).
                trim().toLowerCase(Locale.ENGLISH);
            parameters = new MimeTypeParameterList();
        } else if (slashIndex < semIndex) {
            //    we have all three items in the proper sequence
            primaryType = rawdata.substring(0, slashIndex).
                trim().toLowerCase(Locale.ENGLISH);
            subType = rawdata.substring(slashIndex + 1,
                semIndex).trim().toLowerCase(Locale.ENGLISH);
            parameters = new
MimeTypeParameterList(rawdata.substring(semIndex));
        } else {
            //    we have a ';' lexically before a '/' which means we have a primary type
            //    & a parameter list but no sub type
            throw new MimeTypeParseException("Unable to find a sub type.");
        }

        //    now validate the primary and sub types

        //    check to see if primary is valid
        if(!isValidToken(primaryType)) {
            throw new MimeTypeParseException("Primary type is invalid.");
        }

        //    check to see if sub is valid
        if(!isValidToken(subType)) {
            throw new MimeTypeParseException("Sub type is invalid.");
        }
    }


    public String getPrimaryType() {
        return primaryType;
    }


    public String getSubType() {
        return subType;
    }


    public MimeTypeParameterList getParameters() {
        return (MimeTypeParameterList)parameters.clone();
    }


    public String getParameter(String name) {
        return parameters.get(name);
    }


    public void setParameter(String name, String value) {
        parameters.set(name, value);
    }


    public void removeParameter(String name) {
        parameters.remove(name);
    }


    public String toString() {
        return getBaseType() + parameters.toString();
    }


    public String getBaseType() {
        return primaryType + "/" + subType;
    }


    public boolean match(MimeType type) {
        if (type == null)
            return false;
        return primaryType.equals(type.getPrimaryType())
                    && (subType.equals("*")
                            || type.getSubType().equals("*")
                            || (subType.equals(type.getSubType())));
    }


    public boolean match(String rawdata) throws MimeTypeParseException {
        if (rawdata == null)
            return false;
        return match(new MimeType(rawdata));
    }


    public void writeExternal(ObjectOutput out) throws IOException {
        String s = toString(); // contains ASCII chars only
        // one-to-one correspondence between ASCII char and byte in UTF string
        if (s.length() <= 65535) { // 65535 is max length of UTF string
            out.writeUTF(s);
        } else {
            out.writeByte(0);
            out.writeByte(0);
            out.writeInt(s.length());
            out.write(s.getBytes());
        }
    }


    public void readExternal(ObjectInput in) throws IOException,
ClassNotFoundException {
        String s = in.readUTF();
        if (s == null || s.length() == 0) { // long mime type
            byte[] ba = new byte[in.readInt()];
            in.readFully(ba);
            s = new String(ba);
        }
        try {
            parse(s);
        } catch(MimeTypeParseException e) {
            throw new IOException(e.toString());
        }
    }



    public Object clone() {
        MimeType newObj = null;
        try {
            newObj = (MimeType)super.clone();
        } catch (CloneNotSupportedException cannotHappen) {
        }
        newObj.parameters = (MimeTypeParameterList)parameters.clone();
        return newObj;
    }

    private String    primaryType;
    private String    subType;
    private MimeTypeParameterList parameters;

    //    below here be scary parsing related things


    private static boolean isTokenChar(char c) {
        return ((c > 040) && (c < 0177)) && (TSPECIALS.indexOf(c) < 0);
    }


    private boolean isValidToken(String s) {
        int len = s.length();
        if(len > 0) {
            for (int i = 0; i < len; ++i) {
                char c = s.charAt(i);
                if (!isTokenChar(c)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }



    private static final String TSPECIALS = "()<>@,;:\\\"/[]?=";

} // class MimeType
