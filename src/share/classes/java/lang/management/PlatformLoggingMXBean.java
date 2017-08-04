package java.lang.management;
public interface PlatformLoggingMXBean extends PlatformManagedObject {
    java.util.List<String> getLoggerNames();
    String getLoggerLevel(String loggerName);
    void setLoggerLevel(String loggerName, String levelName);
    String getParentLoggerName(String loggerName);
}
