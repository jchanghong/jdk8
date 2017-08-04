

package java.lang.management;


public interface ClassLoadingMXBean extends PlatformManagedObject {


    public long getTotalLoadedClassCount();


    public int getLoadedClassCount();


    public long getUnloadedClassCount();


    public boolean isVerbose();


    public void setVerbose(boolean value);

}
