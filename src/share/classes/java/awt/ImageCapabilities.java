

package java.awt;


public class ImageCapabilities implements Cloneable {

    private boolean accelerated = false;


    public ImageCapabilities(boolean accelerated) {
        this.accelerated = accelerated;
    }


    public boolean isAccelerated() {
        return accelerated;
    }


    public boolean isTrueVolatile() {
        return false;
    }


    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // Since we implement Cloneable, this should never happen
            throw new InternalError(e);
        }
    }

}
