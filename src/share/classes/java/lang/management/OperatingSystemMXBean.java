

package java.lang.management;


public interface OperatingSystemMXBean extends PlatformManagedObject {

    public String getName();


    public String getArch();


    public String getVersion();


    public int getAvailableProcessors();


    public double getSystemLoadAverage();
}
