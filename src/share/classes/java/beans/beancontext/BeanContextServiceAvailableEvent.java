

package java.beans.beancontext;

import java.beans.beancontext.BeanContextChild;
import java.beans.beancontext.BeanContextEvent;

import java.beans.beancontext.BeanContextServices;

import java.util.Iterator;



public class BeanContextServiceAvailableEvent extends BeanContextEvent {
    private static final long serialVersionUID = -5333985775656400778L;


    public BeanContextServiceAvailableEvent(BeanContextServices bcs, Class sc) {
        super((BeanContext)bcs);

        serviceClass = sc;
    }


    public BeanContextServices getSourceAsBeanContextServices() {
        return (BeanContextServices)getBeanContext();
    }


    public Class getServiceClass() { return serviceClass; }


    public Iterator getCurrentServiceSelectors() {
        return ((BeanContextServices)getSource()).getCurrentServiceSelectors(serviceClass);
    }




    protected Class                      serviceClass;
}
