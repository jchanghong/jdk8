package java.lang.reflect;
public interface WildcardType extends Type {
    Type[] getUpperBounds();
    Type[] getLowerBounds();
    // one or many? Up to language spec; currently only one, but this API
    // allows for generalization.
}
