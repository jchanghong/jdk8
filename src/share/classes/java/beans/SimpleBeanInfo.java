

package java.beans;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageProducer;
import java.net.URL;



public class SimpleBeanInfo implements BeanInfo {


    public BeanDescriptor getBeanDescriptor() {
        return null;
    }


    public PropertyDescriptor[] getPropertyDescriptors() {
        return null;
    }


    public int getDefaultPropertyIndex() {
        return -1;
    }


    public EventSetDescriptor[] getEventSetDescriptors() {
        return null;
    }


    public int getDefaultEventIndex() {
        return -1;
    }


    public MethodDescriptor[] getMethodDescriptors() {
        return null;
    }


    public BeanInfo[] getAdditionalBeanInfo() {
        return null;
    }


    public Image getIcon(int iconKind) {
        return null;
    }


    public Image loadImage(final String resourceName) {
        try {
            final URL url = getClass().getResource(resourceName);
            if (url != null) {
                final ImageProducer ip = (ImageProducer) url.getContent();
                if (ip != null) {
                    return Toolkit.getDefaultToolkit().createImage(ip);
                }
            }
        } catch (final Exception ignored) {
        }
        return null;
    }
}
