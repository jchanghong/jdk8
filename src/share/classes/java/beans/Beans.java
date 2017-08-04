package java.beans;
import com.sun.beans.finder.ClassFinder;
import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;
import java.awt.Image;
import java.beans.beancontext.BeanContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
public class Beans {
    public static Object instantiate(ClassLoader cls, String beanName) throws IOException, ClassNotFoundException {
        return Beans.instantiate(cls, beanName, null, null);
    }
    public static Object instantiate(ClassLoader cls, String beanName, BeanContext beanContext) throws IOException, ClassNotFoundException {
        return Beans.instantiate(cls, beanName, beanContext, null);
    }
    public static Object instantiate(ClassLoader cls, String beanName, BeanContext beanContext, AppletInitializer initializer)
                        throws IOException, ClassNotFoundException {
        InputStream ins;
        ObjectInputStream oins = null;
        Object result = null;
        boolean serialized = false;
        IOException serex = null;
        // If the given classloader is null, we check if an
        // system classloader is available and (if so)
        // use that instead.
        // Note that calls on the system class loader will
        // look in the bootstrap class loader first.
        if (cls == null) {
            try {
                cls = ClassLoader.getSystemClassLoader();
            } catch (SecurityException ex) {
                // We're not allowed to access the system class loader.
                // Drop through.
            }
        }
        // Try to find a serialized object with this name
        final String serName = beanName.replace('.','/').concat(".ser");
        if (cls == null)
            ins =  ClassLoader.getSystemResourceAsStream(serName);
        else
            ins =  cls.getResourceAsStream(serName);
        if (ins != null) {
            try {
                if (cls == null) {
                    oins = new ObjectInputStream(ins);
                } else {
                    oins = new ObjectInputStreamWithLoader(ins, cls);
                }
                result = oins.readObject();
                serialized = true;
                oins.close();
            } catch (IOException ex) {
                ins.close();
                // Drop through and try opening the class.  But remember
                // the exception in case we can't find the class either.
                serex = ex;
            } catch (ClassNotFoundException ex) {
                ins.close();
                throw ex;
            }
        }
        if (result == null) {
            // No serialized object, try just instantiating the class
            Class<?> cl;
            try {
                cl = ClassFinder.findClass(beanName, cls);
            } catch (ClassNotFoundException ex) {
                // There is no appropriate class.  If we earlier tried to
                // deserialize an object and got an IO exception, throw that,
                // otherwise rethrow the ClassNotFoundException.
                if (serex != null) {
                    throw serex;
                }
                throw ex;
            }
            if (!Modifier.isPublic(cl.getModifiers())) {
                throw new ClassNotFoundException("" + cl + " : no public access");
            }
            try {
                result = cl.newInstance();
            } catch (Exception ex) {
                // We have to remap the exception to one in our signature.
                // But we pass extra information in the detail message.
                throw new ClassNotFoundException("" + cl + " : " + ex, ex);
            }
        }
        if (result != null) {
            // Ok, if the result is an applet initialize it.
            AppletStub stub = null;
            if (result instanceof Applet) {
                Applet  applet      = (Applet) result;
                boolean needDummies = initializer == null;
                if (needDummies) {
                    // Figure our the codebase and docbase URLs.  We do this
                    // by locating the URL for a known resource, and then
                    // massaging the URL.
                    // First find the "resource name" corresponding to the bean
                    // itself.  So a serialzied bean "a.b.c" would imply a
                    // resource name of "a/b/c.ser" and a classname of "x.y"
                    // would imply a resource name of "x/y.class".
                    final String resourceName;
                    if (serialized) {
                        // Serialized bean
                        resourceName = beanName.replace('.','/').concat(".ser");
                    } else {
                        // Regular class
                        resourceName = beanName.replace('.','/').concat(".class");
                    }
                    URL objectUrl = null;
                    URL codeBase  = null;
                    URL docBase   = null;
                    // Now get the URL correponding to the resource name.
                    if (cls == null) {
                        objectUrl = ClassLoader.getSystemResource(resourceName);
                    } else
                        objectUrl = cls.getResource(resourceName);
                    // If we found a URL, we try to locate the docbase by taking
                    // of the final path name component, and the code base by taking
                    // of the complete resourceName.
                    // So if we had a resourceName of "a/b/c.class" and we got an
                    // objectURL of "file://bert/classes/a/b/c.class" then we would
                    // want to set the codebase to "file://bert/classes/" and the
                    // docbase to "file://bert/classes/a/b/"
                    if (objectUrl != null) {
                        String s = objectUrl.toExternalForm();
                        if (s.endsWith(resourceName)) {
                            int ix   = s.length() - resourceName.length();
                            codeBase = new URL(s.substring(0,ix));
                            docBase  = codeBase;
                            ix = s.lastIndexOf('/');
                            if (ix >= 0) {
                                docBase = new URL(s.substring(0,ix+1));
                            }
                        }
                    }
                    // Setup a default context and stub.
                    BeansAppletContext context = new BeansAppletContext(applet);
                    stub = (AppletStub)new BeansAppletStub(applet, context, codeBase, docBase);
                    applet.setStub(stub);
                } else {
                    initializer.initialize(applet, beanContext);
                }
                // now, if there is a BeanContext, add the bean, if applicable.
                if (beanContext != null) {
                    unsafeBeanContextAdd(beanContext, result);
                }
                // If it was deserialized then it was already init-ed.
                // Otherwise we need to initialize it.
                if (!serialized) {
                    // We need to set a reasonable initial size, as many
                    // applets are unhappy if they are started without
                    // having been explicitly sized.
                    applet.setSize(100,100);
                    applet.init();
                }
                if (needDummies) {
                  ((BeansAppletStub)stub).active = true;
                } else initializer.activate(applet);
            } else if (beanContext != null) unsafeBeanContextAdd(beanContext, result);
        }
        return result;
    }
    @SuppressWarnings("unchecked")
    private static void unsafeBeanContextAdd(BeanContext beanContext, Object res) {
        beanContext.add(res);
    }
    public static Object getInstanceOf(Object bean, Class<?> targetType) {
        return bean;
    }
    public static boolean isInstanceOf(Object bean, Class<?> targetType) {
        return Introspector.isSubclass(bean.getClass(), targetType);
    }
    public static boolean isDesignTime() {
        return ThreadGroupContext.getContext().isDesignTime();
    }
    public static boolean isGuiAvailable() {
        return ThreadGroupContext.getContext().isGuiAvailable();
    }
    public static void setDesignTime(boolean isDesignTime)
                        throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPropertiesAccess();
        }
        ThreadGroupContext.getContext().setDesignTime(isDesignTime);
    }
    public static void setGuiAvailable(boolean isGuiAvailable)
                        throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPropertiesAccess();
        }
        ThreadGroupContext.getContext().setGuiAvailable(isGuiAvailable);
    }
}
class ObjectInputStreamWithLoader extends ObjectInputStream
{
    private ClassLoader loader;
    public ObjectInputStreamWithLoader(InputStream in, ClassLoader loader)
            throws IOException, StreamCorruptedException {
        super(in);
        if (loader == null) {
            throw new IllegalArgumentException("Illegal null argument to ObjectInputStreamWithLoader");
        }
        this.loader = loader;
    }
    @SuppressWarnings("rawtypes")
    protected Class resolveClass(ObjectStreamClass classDesc)
        throws IOException, ClassNotFoundException {
        String cname = classDesc.getName();
        return ClassFinder.resolveClass(cname, this.loader);
    }
}
class BeansAppletContext implements AppletContext {
    Applet target;
    Hashtable<URL,Object> imageCache = new Hashtable<>();
    BeansAppletContext(Applet target) {
        this.target = target;
    }
    public AudioClip getAudioClip(URL url) {
        // We don't currently support audio clips in the Beans.instantiate
        // applet context, unless by some luck there exists a URL content
        // class that can generate an AudioClip from the audio URL.
        try {
            return (AudioClip) url.getContent();
        } catch (Exception ex) {
            return null;
        }
    }
    public synchronized Image getImage(URL url) {
        Object o = imageCache.get(url);
        if (o != null) {
            return (Image)o;
        }
        try {
            o = url.getContent();
            if (o == null) {
                return null;
            }
            if (o instanceof Image) {
                imageCache.put(url, o);
                return (Image) o;
            }
            // Otherwise it must be an ImageProducer.
            Image img = target.createImage((java.awt.image.ImageProducer)o);
            imageCache.put(url, img);
            return img;
        } catch (Exception ex) {
            return null;
        }
    }
    public Applet getApplet(String name) {
        return null;
    }
    public Enumeration<Applet> getApplets() {
        Vector<Applet> applets = new Vector<>();
        applets.addElement(target);
        return applets.elements();
    }
    public void showDocument(URL url) {
        // We do nothing.
    }
    public void showDocument(URL url, String target) {
        // We do nothing.
    }
    public void showStatus(String status) {
        // We do nothing.
    }
    public void setStream(String key, InputStream stream)throws IOException{
        // We do nothing.
    }
    public InputStream getStream(String key){
        // We do nothing.
        return null;
    }
    public Iterator<String> getStreamKeys(){
        // We do nothing.
        return null;
    }
}
class BeansAppletStub implements AppletStub {
    transient boolean active;
    transient Applet target;
    transient AppletContext context;
    transient URL codeBase;
    transient URL docBase;
    BeansAppletStub(Applet target,
                AppletContext context, URL codeBase,
                                URL docBase) {
        this.target = target;
        this.context = context;
        this.codeBase = codeBase;
        this.docBase = docBase;
    }
    public boolean isActive() {
        return active;
    }
    public URL getDocumentBase() {
        // use the root directory of the applet's class-loader
        return docBase;
    }
    public URL getCodeBase() {
        // use the directory where we found the class or serialized object.
        return codeBase;
    }
    public String getParameter(String name) {
        return null;
    }
    public AppletContext getAppletContext() {
        return context;
    }
    public void appletResize(int width, int height) {
        // we do nothing.
    }
}
