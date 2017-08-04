package java.lang.invoke;
import sun.invoke.util.Wrapper;
import static sun.invoke.util.Wrapper.forPrimitiveType;
import static sun.invoke.util.Wrapper.forWrapperType;
import static sun.invoke.util.Wrapper.isWrapperType;
 abstract class AbstractValidatingLambdaMetafactory {
    final Class<?> targetClass;               // The class calling the meta-factory via invokedynamic "class X"
    final MethodType invokedType;             // The type of the invoked method "(CC)II"
    final Class<?> samBase;                   // The type of the returned instance "interface JJ"
    final String samMethodName;               // Name of the SAM method "foo"
    final MethodType samMethodType;           // Type of the SAM method "(Object)Object"
    final MethodHandle implMethod;            // Raw method handle for the implementation method
    final MethodHandleInfo implInfo;          // Info about the implementation method handle "MethodHandleInfo[5 CC.impl(int)String]"
    final int implKind;                       // Invocation kind for implementation "5"=invokevirtual
    final boolean implIsInstanceMethod;       // Is the implementation an instance method "true"
    final Class<?> implDefiningClass;         // Type defining the implementation "class CC"
    final MethodType implMethodType;          // Type of the implementation method "(int)String"
    final MethodType instantiatedMethodType;  // Instantiated erased functional interface method type "(Integer)Object"
    final boolean isSerializable;             // Should the returned instance be serializable
    final Class<?>[] markerInterfaces;        // Additional marker interfaces to be implemented
    final MethodType[] additionalBridges;     // Signatures of additional methods to bridge
    AbstractValidatingLambdaMetafactory(MethodHandles.Lookup caller,
                                       MethodType invokedType,
                                       String samMethodName,
                                       MethodType samMethodType,
                                       MethodHandle implMethod,
                                       MethodType instantiatedMethodType,
                                       boolean isSerializable,
                                       Class<?>[] markerInterfaces,
                                       MethodType[] additionalBridges)
            throws LambdaConversionException {
        if ((caller.lookupModes() & MethodHandles.Lookup.PRIVATE) == 0) {
            throw new LambdaConversionException(String.format(
                    "Invalid caller: %s",
                    caller.lookupClass().getName()));
        }
        this.targetClass = caller.lookupClass();
        this.invokedType = invokedType;
        this.samBase = invokedType.returnType();
        this.samMethodName = samMethodName;
        this.samMethodType  = samMethodType;
        this.implMethod = implMethod;
        this.implInfo = caller.revealDirect(implMethod);
        this.implKind = implInfo.getReferenceKind();
        this.implIsInstanceMethod =
                implKind == MethodHandleInfo.REF_invokeVirtual ||
                implKind == MethodHandleInfo.REF_invokeSpecial ||
                implKind == MethodHandleInfo.REF_invokeInterface;
        this.implDefiningClass = implInfo.getDeclaringClass();
        this.implMethodType = implInfo.getMethodType();
        this.instantiatedMethodType = instantiatedMethodType;
        this.isSerializable = isSerializable;
        this.markerInterfaces = markerInterfaces;
        this.additionalBridges = additionalBridges;
        if (!samBase.isInterface()) {
            throw new LambdaConversionException(String.format(
                    "Functional interface %s is not an interface",
                    samBase.getName()));
        }
        for (Class<?> c : markerInterfaces) {
            if (!c.isInterface()) {
                throw new LambdaConversionException(String.format(
                        "Marker interface %s is not an interface",
                        c.getName()));
            }
        }
    }
    abstract CallSite buildCallSite()
            throws LambdaConversionException;
    void validateMetafactoryArgs() throws LambdaConversionException {
        switch (implKind) {
            case MethodHandleInfo.REF_invokeInterface:
            case MethodHandleInfo.REF_invokeVirtual:
            case MethodHandleInfo.REF_invokeStatic:
            case MethodHandleInfo.REF_newInvokeSpecial:
            case MethodHandleInfo.REF_invokeSpecial:
                break;
            default:
                throw new LambdaConversionException(String.format("Unsupported MethodHandle kind: %s", implInfo));
        }
        // Check arity: optional-receiver + captured + SAM == impl
        final int implArity = implMethodType.parameterCount();
        final int receiverArity = implIsInstanceMethod ? 1 : 0;
        final int capturedArity = invokedType.parameterCount();
        final int samArity = samMethodType.parameterCount();
        final int instantiatedArity = instantiatedMethodType.parameterCount();
        if (implArity + receiverArity != capturedArity + samArity) {
            throw new LambdaConversionException(
                    String.format("Incorrect number of parameters for %s method %s; %d captured parameters, %d functional interface method parameters, %d implementation parameters",
                                  implIsInstanceMethod ? "instance" : "static", implInfo,
                                  capturedArity, samArity, implArity));
        }
        if (instantiatedArity != samArity) {
            throw new LambdaConversionException(
                    String.format("Incorrect number of parameters for %s method %s; %d instantiated parameters, %d functional interface method parameters",
                                  implIsInstanceMethod ? "instance" : "static", implInfo,
                                  instantiatedArity, samArity));
        }
        for (MethodType bridgeMT : additionalBridges) {
            if (bridgeMT.parameterCount() != samArity) {
                throw new LambdaConversionException(
                        String.format("Incorrect number of parameters for bridge signature %s; incompatible with %s",
                                      bridgeMT, samMethodType));
            }
        }
        // If instance: first captured arg (receiver) must be subtype of class where impl method is defined
        final int capturedStart;
        final int samStart;
        if (implIsInstanceMethod) {
            final Class<?> receiverClass;
            // implementation is an instance method, adjust for receiver in captured variables / SAM arguments
            if (capturedArity == 0) {
                // receiver is function parameter
                capturedStart = 0;
                samStart = 1;
                receiverClass = instantiatedMethodType.parameterType(0);
            } else {
                // receiver is a captured variable
                capturedStart = 1;
                samStart = 0;
                receiverClass = invokedType.parameterType(0);
            }
            // check receiver type
            if (!implDefiningClass.isAssignableFrom(receiverClass)) {
                throw new LambdaConversionException(
                        String.format("Invalid receiver type %s; not a subtype of implementation type %s",
                                      receiverClass, implDefiningClass));
            }
           Class<?> implReceiverClass = implMethod.type().parameterType(0);
           if (implReceiverClass != implDefiningClass && !implReceiverClass.isAssignableFrom(receiverClass)) {
               throw new LambdaConversionException(
                       String.format("Invalid receiver type %s; not a subtype of implementation receiver type %s",
                                     receiverClass, implReceiverClass));
            }
        } else {
            // no receiver
            capturedStart = 0;
            samStart = 0;
        }
        // Check for exact match on non-receiver captured arguments
        final int implFromCaptured = capturedArity - capturedStart;
        for (int i=0; i<implFromCaptured; i++) {
            Class<?> implParamType = implMethodType.parameterType(i);
            Class<?> capturedParamType = invokedType.parameterType(i + capturedStart);
            if (!capturedParamType.equals(implParamType)) {
                throw new LambdaConversionException(
                        String.format("Type mismatch in captured lambda parameter %d: expecting %s, found %s",
                                      i, capturedParamType, implParamType));
            }
        }
        // Check for adaptation match on SAM arguments
        final int samOffset = samStart - implFromCaptured;
        for (int i=implFromCaptured; i<implArity; i++) {
            Class<?> implParamType = implMethodType.parameterType(i);
            Class<?> instantiatedParamType = instantiatedMethodType.parameterType(i + samOffset);
            if (!isAdaptableTo(instantiatedParamType, implParamType, true)) {
                throw new LambdaConversionException(
                        String.format("Type mismatch for lambda argument %d: %s is not convertible to %s",
                                      i, instantiatedParamType, implParamType));
            }
        }
        // Adaptation match: return type
        Class<?> expectedType = instantiatedMethodType.returnType();
        Class<?> actualReturnType =
                (implKind == MethodHandleInfo.REF_newInvokeSpecial)
                  ? implDefiningClass
                  : implMethodType.returnType();
        Class<?> samReturnType = samMethodType.returnType();
        if (!isAdaptableToAsReturn(actualReturnType, expectedType)) {
            throw new LambdaConversionException(
                    String.format("Type mismatch for lambda return: %s is not convertible to %s",
                                  actualReturnType, expectedType));
        }
        if (!isAdaptableToAsReturnStrict(expectedType, samReturnType)) {
            throw new LambdaConversionException(
                    String.format("Type mismatch for lambda expected return: %s is not convertible to %s",
                                  expectedType, samReturnType));
        }
        for (MethodType bridgeMT : additionalBridges) {
            if (!isAdaptableToAsReturnStrict(expectedType, bridgeMT.returnType())) {
                throw new LambdaConversionException(
                        String.format("Type mismatch for lambda expected return: %s is not convertible to %s",
                                      expectedType, bridgeMT.returnType()));
            }
        }
     }
    private boolean isAdaptableTo(Class<?> fromType, Class<?> toType, boolean strict) {
        if (fromType.equals(toType)) {
            return true;
        }
        if (fromType.isPrimitive()) {
            Wrapper wfrom = forPrimitiveType(fromType);
            if (toType.isPrimitive()) {
                // both are primitive: widening
                Wrapper wto = forPrimitiveType(toType);
                return wto.isConvertibleFrom(wfrom);
            } else {
                // from primitive to reference: boxing
                return toType.isAssignableFrom(wfrom.wrapperType());
            }
        } else {
            if (toType.isPrimitive()) {
                // from reference to primitive: unboxing
                Wrapper wfrom;
                if (isWrapperType(fromType) && (wfrom = forWrapperType(fromType)).primitiveType().isPrimitive()) {
                    // fromType is a primitive wrapper; unbox+widen
                    Wrapper wto = forPrimitiveType(toType);
                    return wto.isConvertibleFrom(wfrom);
                } else {
                    // must be convertible to primitive
                    return !strict;
                }
            } else {
                // both are reference types: fromType should be a superclass of toType.
                return !strict || toType.isAssignableFrom(fromType);
            }
        }
    }
    private boolean isAdaptableToAsReturn(Class<?> fromType, Class<?> toType) {
        return toType.equals(void.class)
               || !fromType.equals(void.class) && isAdaptableTo(fromType, toType, false);
    }
    private boolean isAdaptableToAsReturnStrict(Class<?> fromType, Class<?> toType) {
        if (fromType.equals(void.class)) return toType.equals(void.class);
        return isAdaptableTo(fromType, toType, true);
    }
}
