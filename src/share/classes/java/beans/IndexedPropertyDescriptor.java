package java.beans;
import java.lang.ref.Reference;
import java.lang.reflect.Method;
public class IndexedPropertyDescriptor extends PropertyDescriptor {
    private Reference<? extends Class<?>> indexedPropertyTypeRef;
    private final MethodRef indexedReadMethodRef = new MethodRef();
    private final MethodRef indexedWriteMethodRef = new MethodRef();
    private String indexedReadMethodName;
    private String indexedWriteMethodName;
    public IndexedPropertyDescriptor(String propertyName, Class<?> beanClass)
                throws IntrospectionException {
        this(propertyName, beanClass,
             Introspector.GET_PREFIX + NameGenerator.capitalize(propertyName),
             Introspector.SET_PREFIX + NameGenerator.capitalize(propertyName),
             Introspector.GET_PREFIX + NameGenerator.capitalize(propertyName),
             Introspector.SET_PREFIX + NameGenerator.capitalize(propertyName));
    }
    public IndexedPropertyDescriptor(String propertyName, Class<?> beanClass,
                String readMethodName, String writeMethodName,
                String indexedReadMethodName, String indexedWriteMethodName)
                throws IntrospectionException {
        super(propertyName, beanClass, readMethodName, writeMethodName);
        this.indexedReadMethodName = indexedReadMethodName;
        if (indexedReadMethodName != null && getIndexedReadMethod() == null) {
            throw new IntrospectionException("Method not found: " + indexedReadMethodName);
        }
        this.indexedWriteMethodName = indexedWriteMethodName;
        if (indexedWriteMethodName != null && getIndexedWriteMethod() == null) {
            throw new IntrospectionException("Method not found: " + indexedWriteMethodName);
        }
        // Implemented only for type checking.
        findIndexedPropertyType(getIndexedReadMethod(), getIndexedWriteMethod());
    }
    public IndexedPropertyDescriptor(String propertyName, Method readMethod, Method writeMethod,
                                            Method indexedReadMethod, Method indexedWriteMethod)
                throws IntrospectionException {
        super(propertyName, readMethod, writeMethod);
        setIndexedReadMethod0(indexedReadMethod);
        setIndexedWriteMethod0(indexedWriteMethod);
        // Type checking
        setIndexedPropertyType(findIndexedPropertyType(indexedReadMethod, indexedWriteMethod));
    }
    IndexedPropertyDescriptor(Class<?> bean, String base, Method read, Method write, Method readIndexed, Method writeIndexed) throws IntrospectionException {
        super(bean, base, read, write);
        setIndexedReadMethod0(readIndexed);
        setIndexedWriteMethod0(writeIndexed);
        // Type checking
        setIndexedPropertyType(findIndexedPropertyType(readIndexed, writeIndexed));
    }
    public synchronized Method getIndexedReadMethod() {
        Method indexedReadMethod = this.indexedReadMethodRef.get();
        if (indexedReadMethod == null) {
            Class<?> cls = getClass0();
            if (cls == null ||
                (indexedReadMethodName == null && !this.indexedReadMethodRef.isSet())) {
                // the Indexed readMethod was explicitly set to null.
                return null;
            }
            String nextMethodName = Introspector.GET_PREFIX + getBaseName();
            if (indexedReadMethodName == null) {
                Class<?> type = getIndexedPropertyType0();
                if (type == boolean.class || type == null) {
                    indexedReadMethodName = Introspector.IS_PREFIX + getBaseName();
                } else {
                    indexedReadMethodName = nextMethodName;
                }
            }
            Class<?>[] args = { int.class };
            indexedReadMethod = Introspector.findMethod(cls, indexedReadMethodName, 1, args);
            if ((indexedReadMethod == null) && !indexedReadMethodName.equals(nextMethodName)) {
                // no "is" method, so look for a "get" method.
                indexedReadMethodName = nextMethodName;
                indexedReadMethod = Introspector.findMethod(cls, indexedReadMethodName, 1, args);
            }
            setIndexedReadMethod0(indexedReadMethod);
        }
        return indexedReadMethod;
    }
    public synchronized void setIndexedReadMethod(Method readMethod)
        throws IntrospectionException {
        // the indexed property type is set by the reader.
        setIndexedPropertyType(findIndexedPropertyType(readMethod,
                                                       this.indexedWriteMethodRef.get()));
        setIndexedReadMethod0(readMethod);
    }
    private void setIndexedReadMethod0(Method readMethod) {
        this.indexedReadMethodRef.set(readMethod);
        if (readMethod == null) {
            indexedReadMethodName = null;
            return;
        }
        setClass0(readMethod.getDeclaringClass());
        indexedReadMethodName = readMethod.getName();
        setTransient(readMethod.getAnnotation(Transient.class));
    }
    public synchronized Method getIndexedWriteMethod() {
        Method indexedWriteMethod = this.indexedWriteMethodRef.get();
        if (indexedWriteMethod == null) {
            Class<?> cls = getClass0();
            if (cls == null ||
                (indexedWriteMethodName == null && !this.indexedWriteMethodRef.isSet())) {
                // the Indexed writeMethod was explicitly set to null.
                return null;
            }
            // We need the indexed type to ensure that we get the correct method.
            // Cannot use the getIndexedPropertyType method since that could
            // result in an infinite loop.
            Class<?> type = getIndexedPropertyType0();
            if (type == null) {
                try {
                    type = findIndexedPropertyType(getIndexedReadMethod(), null);
                    setIndexedPropertyType(type);
                } catch (IntrospectionException ex) {
                    // Set iprop type to be the classic type
                    Class<?> propType = getPropertyType();
                    if (propType.isArray()) {
                        type = propType.getComponentType();
                    }
                }
            }
            if (indexedWriteMethodName == null) {
                indexedWriteMethodName = Introspector.SET_PREFIX + getBaseName();
            }
            Class<?>[] args = (type == null) ? null : new Class<?>[] { int.class, type };
            indexedWriteMethod = Introspector.findMethod(cls, indexedWriteMethodName, 2, args);
            if (indexedWriteMethod != null) {
                if (!indexedWriteMethod.getReturnType().equals(void.class)) {
                    indexedWriteMethod = null;
                }
            }
            setIndexedWriteMethod0(indexedWriteMethod);
        }
        return indexedWriteMethod;
    }
    public synchronized void setIndexedWriteMethod(Method writeMethod)
        throws IntrospectionException {
        // If the indexed property type has not been set, then set it.
        Class<?> type = findIndexedPropertyType(getIndexedReadMethod(),
                                             writeMethod);
        setIndexedPropertyType(type);
        setIndexedWriteMethod0(writeMethod);
    }
    private void setIndexedWriteMethod0(Method writeMethod) {
        this.indexedWriteMethodRef.set(writeMethod);
        if (writeMethod == null) {
            indexedWriteMethodName = null;
            return;
        }
        setClass0(writeMethod.getDeclaringClass());
        indexedWriteMethodName = writeMethod.getName();
        setTransient(writeMethod.getAnnotation(Transient.class));
    }
    public synchronized Class<?> getIndexedPropertyType() {
        Class<?> type = getIndexedPropertyType0();
        if (type == null) {
            try {
                type = findIndexedPropertyType(getIndexedReadMethod(),
                                               getIndexedWriteMethod());
                setIndexedPropertyType(type);
            } catch (IntrospectionException ex) {
                // fall
            }
        }
        return type;
    }
    // Private methods which set get/set the Reference objects
    private void setIndexedPropertyType(Class<?> type) {
        this.indexedPropertyTypeRef = getWeakReference(type);
    }
    private Class<?> getIndexedPropertyType0() {
        return (this.indexedPropertyTypeRef != null)
                ? this.indexedPropertyTypeRef.get()
                : null;
    }
    private Class<?> findIndexedPropertyType(Method indexedReadMethod,
                                          Method indexedWriteMethod)
        throws IntrospectionException {
        Class<?> indexedPropertyType = null;
        if (indexedReadMethod != null) {
            Class params[] = getParameterTypes(getClass0(), indexedReadMethod);
            if (params.length != 1) {
                throw new IntrospectionException("bad indexed read method arg count");
            }
            if (params[0] != Integer.TYPE) {
                throw new IntrospectionException("non int index to indexed read method");
            }
            indexedPropertyType = getReturnType(getClass0(), indexedReadMethod);
            if (indexedPropertyType == Void.TYPE) {
                throw new IntrospectionException("indexed read method returns void");
            }
        }
        if (indexedWriteMethod != null) {
            Class params[] = getParameterTypes(getClass0(), indexedWriteMethod);
            if (params.length != 2) {
                throw new IntrospectionException("bad indexed write method arg count");
            }
            if (params[0] != Integer.TYPE) {
                throw new IntrospectionException("non int index to indexed write method");
            }
            if (indexedPropertyType == null || params[1].isAssignableFrom(indexedPropertyType)) {
                indexedPropertyType = params[1];
            } else if (!indexedPropertyType.isAssignableFrom(params[1])) {
                throw new IntrospectionException(
                                                 "type mismatch between indexed read and indexed write methods: "
                                                 + getName());
            }
        }
        Class<?> propertyType = getPropertyType();
        if (propertyType != null && (!propertyType.isArray() ||
                                     propertyType.getComponentType() != indexedPropertyType)) {
            throw new IntrospectionException("type mismatch between indexed and non-indexed methods: "
                                             + getName());
        }
        return indexedPropertyType;
    }
    public boolean equals(Object obj) {
        // Note: This would be identical to PropertyDescriptor but they don't
        // share the same fields.
        if (this == obj) {
            return true;
        }
        if (obj != null && obj instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor other = (IndexedPropertyDescriptor)obj;
            Method otherIndexedReadMethod = other.getIndexedReadMethod();
            Method otherIndexedWriteMethod = other.getIndexedWriteMethod();
            if (!compareMethods(getIndexedReadMethod(), otherIndexedReadMethod)) {
                return false;
            }
            if (!compareMethods(getIndexedWriteMethod(), otherIndexedWriteMethod)) {
                return false;
            }
            if (getIndexedPropertyType() != other.getIndexedPropertyType()) {
                return false;
            }
            return super.equals(obj);
        }
        return false;
    }
    IndexedPropertyDescriptor(PropertyDescriptor x, PropertyDescriptor y) {
        super(x,y);
        if (x instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor ix = (IndexedPropertyDescriptor)x;
            try {
                Method xr = ix.getIndexedReadMethod();
                if (xr != null) {
                    setIndexedReadMethod(xr);
                }
                Method xw = ix.getIndexedWriteMethod();
                if (xw != null) {
                    setIndexedWriteMethod(xw);
                }
            } catch (IntrospectionException ex) {
                // Should not happen
                throw new AssertionError(ex);
            }
        }
        if (y instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor iy = (IndexedPropertyDescriptor)y;
            try {
                Method yr = iy.getIndexedReadMethod();
                if (yr != null && yr.getDeclaringClass() == getClass0()) {
                    setIndexedReadMethod(yr);
                }
                Method yw = iy.getIndexedWriteMethod();
                if (yw != null && yw.getDeclaringClass() == getClass0()) {
                    setIndexedWriteMethod(yw);
                }
            } catch (IntrospectionException ex) {
                // Should not happen
                throw new AssertionError(ex);
            }
        }
    }
    IndexedPropertyDescriptor(IndexedPropertyDescriptor old) {
        super(old);
        this.indexedReadMethodRef.set(old.indexedReadMethodRef.get());
        this.indexedWriteMethodRef.set(old.indexedWriteMethodRef.get());
        indexedPropertyTypeRef = old.indexedPropertyTypeRef;
        indexedWriteMethodName = old.indexedWriteMethodName;
        indexedReadMethodName = old.indexedReadMethodName;
    }
    void updateGenericsFor(Class<?> type) {
        super.updateGenericsFor(type);
        try {
            setIndexedPropertyType(findIndexedPropertyType(this.indexedReadMethodRef.get(), this.indexedWriteMethodRef.get()));
        }
        catch (IntrospectionException exception) {
            setIndexedPropertyType(null);
        }
    }
    public int hashCode() {
        int result = super.hashCode();
        result = 37 * result + ((indexedWriteMethodName == null) ? 0 :
                                indexedWriteMethodName.hashCode());
        result = 37 * result + ((indexedReadMethodName == null) ? 0 :
                                indexedReadMethodName.hashCode());
        result = 37 * result + ((getIndexedPropertyType() == null) ? 0 :
                                getIndexedPropertyType().hashCode());
        return result;
    }
    void appendTo(StringBuilder sb) {
        super.appendTo(sb);
        appendTo(sb, "indexedPropertyType", this.indexedPropertyTypeRef);
        appendTo(sb, "indexedReadMethod", this.indexedReadMethodRef.get());
        appendTo(sb, "indexedWriteMethod", this.indexedWriteMethodRef.get());
    }
}
