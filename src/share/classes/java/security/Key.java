

package java.security;



public interface Key extends java.io.Serializable {

    // Declare serialVersionUID to be compatible with JDK1.1


    static final long serialVersionUID = 6603384152749567654L;


    public String getAlgorithm();


    public String getFormat();


    public byte[] getEncoded();
}
