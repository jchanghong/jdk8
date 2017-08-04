package java.awt;
public class PointerInfo {
    private final GraphicsDevice device;
    private final Point location;
    PointerInfo(final GraphicsDevice device, final Point location) {
        this.device = device;
        this.location = location;
    }
    public GraphicsDevice getDevice() {
        return device;
    }
    public Point getLocation() {
        return location;
    }
}
