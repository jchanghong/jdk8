package java.beans;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import static java.util.Locale.ENGLISH;
class NameGenerator {
    private Map<Object, String> valueToName;
    private Map<String, Integer> nameToCount;
    public NameGenerator() {
        valueToName = new IdentityHashMap<>();
        nameToCount = new HashMap<>();
    }
    public void clear() {
        valueToName.clear();
        nameToCount.clear();
    }
    @SuppressWarnings("rawtypes")
    public static String unqualifiedClassName(Class type) {
        if (type.isArray()) {
            return unqualifiedClassName(type.getComponentType())+"Array";
        }
        String name = type.getName();
        return name.substring(name.lastIndexOf('.')+1);
    }
    public static String capitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        return name.substring(0, 1).toUpperCase(ENGLISH) + name.substring(1);
    }
    public String instanceName(Object instance) {
        if (instance == null) {
            return "null";
        }
        if (instance instanceof Class) {
            return unqualifiedClassName((Class)instance);
        }
        else {
            String result = valueToName.get(instance);
            if (result != null) {
                return result;
            }
            Class<?> type = instance.getClass();
            String className = unqualifiedClassName(type);
            Integer size = nameToCount.get(className);
            int instanceNumber = (size == null) ? 0 : (size).intValue() + 1;
            nameToCount.put(className, new Integer(instanceNumber));
            result = className + instanceNumber;
            valueToName.put(instance, result);
            return result;
        }
    }
}
