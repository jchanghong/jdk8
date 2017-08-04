

package java.lang.management;


public interface CompilationMXBean extends PlatformManagedObject {

    public java.lang.String    getName();


    public boolean isCompilationTimeMonitoringSupported();


    public long                getTotalCompilationTime();
}
