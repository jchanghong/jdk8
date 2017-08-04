

package java.rmi.server;

import java.net.MalformedURLException;
import java.net.URL;


public abstract class RMIClassLoaderSpi {


    public abstract Class<?> loadClass(String codebase, String name,
                                       ClassLoader defaultLoader)
        throws MalformedURLException, ClassNotFoundException;


    public abstract Class<?> loadProxyClass(String codebase,
                                            String[] interfaces,
                                            ClassLoader defaultLoader)
        throws MalformedURLException, ClassNotFoundException;


    public abstract ClassLoader getClassLoader(String codebase)
        throws MalformedURLException; // SecurityException


    public abstract String getClassAnnotation(Class<?> cl);
}
