

package java.lang.management;

import javax.management.openmbean.CompositeData;


public interface MemoryMXBean extends PlatformManagedObject {

    public int getObjectPendingFinalizationCount();


    public MemoryUsage getHeapMemoryUsage();


    public MemoryUsage getNonHeapMemoryUsage();


    public boolean isVerbose();


    public void setVerbose(boolean value);


    public void gc();

}
