

package java.lang.management;


public interface MemoryManagerMXBean extends PlatformManagedObject {

    public String getName();


    public boolean isValid();


    public String[] getMemoryPoolNames();
}
