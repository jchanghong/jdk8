package java.awt;
public class GridBagLayoutInfo implements java.io.Serializable {
    private static final long serialVersionUID = -4899416460737170217L;
    int width, height;
    int startx, starty;
    int minWidth[];
    int minHeight[];
    double weightX[];
    double weightY[];
    boolean hasBaseline;
    // These are only valid if hasBaseline is true and are indexed by
    // row.
    short baselineType[];
    int maxAscent[];
    int maxDescent[];
    GridBagLayoutInfo(int width, int height) {
        this.width = width;
        this.height = height;
    }
    boolean hasConstantDescent(int row) {
        return ((baselineType[row] & (1 << Component.BaselineResizeBehavior.
                                      CONSTANT_DESCENT.ordinal())) != 0);
    }
    boolean hasBaseline(int row) {
        return (hasBaseline && baselineType[row] != 0);
    }
}
