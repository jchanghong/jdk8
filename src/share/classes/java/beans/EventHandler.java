package java.beans;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.reflect.misc.MethodUtil;
import sun.reflect.misc.ReflectUtil;
public class EventHandler implements InvocationHandler {
    private Object target;
    private String action;
    private final String eventPropertyName;
    private final String listenerMethodName;
    private final AccessControlContext acc = AccessController.getContext();
    @ConstructorProperties({"target", "action", "eventPropertyName", "listenerMethodName"})
    public EventHandler(Object target, String action, String eventPropertyName, String listenerMethodName) {
        this.target = target;
        this.action = action;
        if (target == null) {
            throw new NullPointerException("target must be non-null");
        }
        if (action == null) {
            throw new NullPointerException("action must be non-null");
        }
        this.eventPropertyName = eventPropertyName;
        this.listenerMethodName = listenerMethodName;
    }
    public Object getTarget()  {
        return target;
    }
    public String getAction()  {
        return action;
    }
    public String getEventPropertyName()  {
        return eventPropertyName;
    }
    public String getListenerMethodName()  {
        return listenerMethodName;
    }
    private Object applyGetters(Object target, String getters) {
        if (getters == null || getters.equals("")) {
            return target;
        }
        int firstDot = getters.indexOf('.');
        if (firstDot == -1) {
            firstDot = getters.length();
        }
        String first = getters.substring(0, firstDot);
        String rest = getters.substring(Math.min(firstDot + 1, getters.length()));
        try {
            Method getter = null;
            if (target != null) {
                getter = Statement.getMethod(target.getClass(),
                                      "get" + NameGenerator.capitalize(first),
                                      new Class<?>[]{});
                if (getter == null) {
                    getter = Statement.getMethod(target.getClass(),
                                   "is" + NameGenerator.capitalize(first),
                                   new Class<?>[]{});
                }
                if (getter == null) {
                    getter = Statement.getMethod(target.getClass(), first, new Class<?>[]{});
                }
            }
            if (getter == null) {
                throw new RuntimeException("No method called: " + first +
                                           " defined on " + target);
            }
            Object newTarget = MethodUtil.invoke(getter, target, new Object[]{});
            return applyGetters(newTarget, rest);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to call method: " + first +
                                       " on " + target, e);
        }
    }
    public Object invoke(final Object proxy, final Method method, final Object[] arguments) {
        AccessControlContext acc = this.acc;
        if ((acc == null) && (System.getSecurityManager() != null)) {
            throw new SecurityException("AccessControlContext is not set");
        }
        return AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                return invokeInternal(proxy, method, arguments);
            }
        }, acc);
    }
    private Object invokeInternal(Object proxy, Method method, Object[] arguments) {
        String methodName = method.getName();
        if (method.getDeclaringClass() == Object.class)  {
            // Handle the Object public methods.
            if (methodName.equals("hashCode"))  {
                return new Integer(System.identityHashCode(proxy));
            } else if (methodName.equals("equals")) {
                return (proxy == arguments[0] ? Boolean.TRUE : Boolean.FALSE);
            } else if (methodName.equals("toString")) {
                return proxy.getClass().getName() + '@' + Integer.toHexString(proxy.hashCode());
            }
        }
        if (listenerMethodName == null || listenerMethodName.equals(methodName)) {
            Class[] argTypes = null;
            Object[] newArgs = null;
            if (eventPropertyName == null) {     // Nullary method.
                newArgs = new Object[]{};
                argTypes = new Class<?>[]{};
            }
            else {
                Object input = applyGetters(arguments[0], getEventPropertyName());
                newArgs = new Object[]{input};
                argTypes = new Class<?>[]{input == null ? null :
                                       input.getClass()};
            }
            try {
                int lastDot = action.lastIndexOf('.');
                if (lastDot != -1) {
                    target = applyGetters(target, action.substring(0, lastDot));
                    action = action.substring(lastDot + 1);
                }
                Method targetMethod = Statement.getMethod(
                             target.getClass(), action, argTypes);
                if (targetMethod == null) {
                    targetMethod = Statement.getMethod(target.getClass(),
                             "set" + NameGenerator.capitalize(action), argTypes);
                }
                if (targetMethod == null) {
                    String argTypeString = (argTypes.length == 0)
                        ? " with no arguments"
                        : " with argument " + argTypes[0];
                    throw new RuntimeException(
                        "No method called " + action + " on " +
                        target.getClass() + argTypeString);
                }
                return MethodUtil.invoke(targetMethod, target, newArgs);
            }
            catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
            catch (InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                throw (th instanceof RuntimeException)
                        ? (RuntimeException) th
                        : new RuntimeException(th);
            }
        }
        return null;
    }
    public static <T> T create(Class<T> listenerInterface,
                               Object target, String action)
    {
        return create(listenerInterface, target, action, null, null);
    }
    public static <T> T create(Class<T> listenerInterface,
                               Object target, String action,
                               String eventPropertyName)
    {
        return create(listenerInterface, target, action, eventPropertyName, null);
    }
    public static <T> T create(Class<T> listenerInterface,
                               Object target, String action,
                               String eventPropertyName,
                               String listenerMethodName)
    {
        // Create this first to verify target/action are non-null
        final EventHandler handler = new EventHandler(target, action,
                                                     eventPropertyName,
                                                     listenerMethodName);
        if (listenerInterface == null) {
            throw new NullPointerException(
                          "listenerInterface must be non-null");
        }
        final ClassLoader loader = getClassLoader(listenerInterface);
        final Class<?>[] interfaces = {listenerInterface};
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            @SuppressWarnings("unchecked")
            public T run() {
                return (T) Proxy.newProxyInstance(loader, interfaces, handler);
            }
        });
    }
    private static ClassLoader getClassLoader(Class<?> type) {
        ReflectUtil.checkPackageAccess(type);
        ClassLoader loader = type.getClassLoader();
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader(); // avoid use of BCP
            if (loader == null) {
                loader = ClassLoader.getSystemClassLoader();
            }
        }
        return loader;
    }
}
