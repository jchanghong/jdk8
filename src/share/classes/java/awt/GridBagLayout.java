package java.awt;
import java.util.Hashtable;
import java.util.Arrays;
public class GridBagLayout implements LayoutManager2,
java.io.Serializable {
    static final int EMPIRICMULTIPLIER = 2;
    protected static final int MAXGRIDSIZE = 512;
    protected static final int MINSIZE = 1;
    protected static final int PREFERREDSIZE = 2;
    protected Hashtable<Component,GridBagConstraints> comptable;
    protected GridBagConstraints defaultConstraints;
    protected GridBagLayoutInfo layoutInfo;
    public int columnWidths[];
    public int rowHeights[];
    public double columnWeights[];
    public double rowWeights[];
    private Component componentAdjusting;
    public GridBagLayout () {
        comptable = new Hashtable<Component,GridBagConstraints>();
        defaultConstraints = new GridBagConstraints();
    }
    public void setConstraints(Component comp, GridBagConstraints constraints) {
        comptable.put(comp, (GridBagConstraints)constraints.clone());
    }
    public GridBagConstraints getConstraints(Component comp) {
        GridBagConstraints constraints = comptable.get(comp);
        if (constraints == null) {
            setConstraints(comp, defaultConstraints);
            constraints = comptable.get(comp);
        }
        return (GridBagConstraints)constraints.clone();
    }
    protected GridBagConstraints lookupConstraints(Component comp) {
        GridBagConstraints constraints = comptable.get(comp);
        if (constraints == null) {
            setConstraints(comp, defaultConstraints);
            constraints = comptable.get(comp);
        }
        return constraints;
    }
    private void removeConstraints(Component comp) {
        comptable.remove(comp);
    }
    public Point getLayoutOrigin () {
        Point origin = new Point(0,0);
        if (layoutInfo != null) {
            origin.x = layoutInfo.startx;
            origin.y = layoutInfo.starty;
        }
        return origin;
    }
    public int [][] getLayoutDimensions () {
        if (layoutInfo == null)
            return new int[2][0];
        int dim[][] = new int [2][];
        dim[0] = new int[layoutInfo.width];
        dim[1] = new int[layoutInfo.height];
        System.arraycopy(layoutInfo.minWidth, 0, dim[0], 0, layoutInfo.width);
        System.arraycopy(layoutInfo.minHeight, 0, dim[1], 0, layoutInfo.height);
        return dim;
    }
    public double [][] getLayoutWeights () {
        if (layoutInfo == null)
            return new double[2][0];
        double weights[][] = new double [2][];
        weights[0] = new double[layoutInfo.width];
        weights[1] = new double[layoutInfo.height];
        System.arraycopy(layoutInfo.weightX, 0, weights[0], 0, layoutInfo.width);
        System.arraycopy(layoutInfo.weightY, 0, weights[1], 0, layoutInfo.height);
        return weights;
    }
    public Point location(int x, int y) {
        Point loc = new Point(0,0);
        int i, d;
        if (layoutInfo == null)
            return loc;
        d = layoutInfo.startx;
        if (!rightToLeft) {
            for (i=0; i<layoutInfo.width; i++) {
                d += layoutInfo.minWidth[i];
                if (d > x)
                    break;
            }
        } else {
            for (i=layoutInfo.width-1; i>=0; i--) {
                if (d > x)
                    break;
                d += layoutInfo.minWidth[i];
            }
            i++;
        }
        loc.x = i;
        d = layoutInfo.starty;
        for (i=0; i<layoutInfo.height; i++) {
            d += layoutInfo.minHeight[i];
            if (d > y)
                break;
        }
        loc.y = i;
        return loc;
    }
    public void addLayoutComponent(String name, Component comp) {
    }
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints instanceof GridBagConstraints) {
            setConstraints(comp, (GridBagConstraints)constraints);
        } else if (constraints != null) {
            throw new IllegalArgumentException("cannot add to layout: constraints must be a GridBagConstraint");
        }
    }
    public void removeLayoutComponent(Component comp) {
        removeConstraints(comp);
    }
    public Dimension preferredLayoutSize(Container parent) {
        GridBagLayoutInfo info = getLayoutInfo(parent, PREFERREDSIZE);
        return getMinSize(parent, info);
    }
    public Dimension minimumLayoutSize(Container parent) {
        GridBagLayoutInfo info = getLayoutInfo(parent, MINSIZE);
        return getMinSize(parent, info);
    }
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
    public float getLayoutAlignmentX(Container parent) {
        return 0.5f;
    }
    public float getLayoutAlignmentY(Container parent) {
        return 0.5f;
    }
    public void invalidateLayout(Container target) {
    }
    public void layoutContainer(Container parent) {
        arrangeGrid(parent);
    }
    public String toString() {
        return getClass().getName();
    }
    protected GridBagLayoutInfo getLayoutInfo(Container parent, int sizeflag) {
        return GetLayoutInfo(parent, sizeflag);
    }
    private long[]  preInitMaximumArraySizes(Container parent){
        Component components[] = parent.getComponents();
        Component comp;
        GridBagConstraints constraints;
        int curX, curY;
        int curWidth, curHeight;
        int preMaximumArrayXIndex = 0;
        int preMaximumArrayYIndex = 0;
        long [] returnArray = new long[2];
        for (int compId = 0 ; compId < components.length ; compId++) {
            comp = components[compId];
            if (!comp.isVisible()) {
                continue;
            }
            constraints = lookupConstraints(comp);
            curX = constraints.gridx;
            curY = constraints.gridy;
            curWidth = constraints.gridwidth;
            curHeight = constraints.gridheight;
            // -1==RELATIVE, means that column|row equals to previously added component,
            // since each next Component with gridx|gridy == RELATIVE starts from
            // previous position, so we should start from previous component which
            // already used in maximumArray[X|Y]Index calculation. We could just increase
            // maximum by 1 to handle situation when component with gridx=-1 was added.
            if (curX < 0){
                curX = ++preMaximumArrayYIndex;
            }
            if (curY < 0){
                curY = ++preMaximumArrayXIndex;
            }
            // gridwidth|gridheight may be equal to RELATIVE (-1) or REMAINDER (0)
            // in any case using 1 instead of 0 or -1 should be sufficient to for
            // correct maximumArraySizes calculation
            if (curWidth <= 0){
                curWidth = 1;
            }
            if (curHeight <= 0){
                curHeight = 1;
            }
            preMaximumArrayXIndex = Math.max(curY + curHeight, preMaximumArrayXIndex);
            preMaximumArrayYIndex = Math.max(curX + curWidth, preMaximumArrayYIndex);
        } //for (components) loop
        // Must specify index++ to allocate well-working arrays.
        returnArray[0] = preMaximumArrayXIndex;
        returnArray[1] = preMaximumArrayYIndex;
        return returnArray;
    } //PreInitMaximumSizes
    protected GridBagLayoutInfo GetLayoutInfo(Container parent, int sizeflag) {
        synchronized (parent.getTreeLock()) {
            GridBagLayoutInfo r;
            Component comp;
            GridBagConstraints constraints;
            Dimension d;
            Component components[] = parent.getComponents();
            // Code below will address index curX+curWidth in the case of yMaxArray, weightY
            // ( respectively curY+curHeight for xMaxArray, weightX ) where
            //  curX in 0 to preInitMaximumArraySizes.y
            // Thus, the maximum index that could
            // be calculated in the following code is curX+curX.
            // EmpericMultier equals 2 because of this.
            int layoutWidth, layoutHeight;
            int []xMaxArray;
            int []yMaxArray;
            int compindex, i, k, px, py, pixels_diff, nextSize;
            int curX = 0; // constraints.gridx
            int curY = 0; // constraints.gridy
            int curWidth = 1;  // constraints.gridwidth
            int curHeight = 1;  // constraints.gridheight
            int curRow, curCol;
            double weight_diff, weight;
            int maximumArrayXIndex = 0;
            int maximumArrayYIndex = 0;
            int anchor;
            layoutWidth = layoutHeight = 0;
            curRow = curCol = -1;
            long [] arraySizes = preInitMaximumArraySizes(parent);
            maximumArrayXIndex = (EMPIRICMULTIPLIER * arraySizes[0] > Integer.MAX_VALUE )? Integer.MAX_VALUE : EMPIRICMULTIPLIER*(int)arraySizes[0];
            maximumArrayYIndex = (EMPIRICMULTIPLIER * arraySizes[1] > Integer.MAX_VALUE )? Integer.MAX_VALUE : EMPIRICMULTIPLIER*(int)arraySizes[1];
            if (rowHeights != null){
                maximumArrayXIndex = Math.max(maximumArrayXIndex, rowHeights.length);
            }
            if (columnWidths != null){
                maximumArrayYIndex = Math.max(maximumArrayYIndex, columnWidths.length);
            }
            xMaxArray = new int[maximumArrayXIndex];
            yMaxArray = new int[maximumArrayYIndex];
            boolean hasBaseline = false;
            for (compindex = 0 ; compindex < components.length ; compindex++) {
                comp = components[compindex];
                if (!comp.isVisible())
                    continue;
                constraints = lookupConstraints(comp);
                curX = constraints.gridx;
                curY = constraints.gridy;
                curWidth = constraints.gridwidth;
                if (curWidth <= 0)
                    curWidth = 1;
                curHeight = constraints.gridheight;
                if (curHeight <= 0)
                    curHeight = 1;
                if (curX < 0 && curY < 0) {
                    if (curRow >= 0)
                        curY = curRow;
                    else if (curCol >= 0)
                        curX = curCol;
                    else
                        curY = 0;
                }
                if (curX < 0) {
                    px = 0;
                    for (i = curY; i < (curY + curHeight); i++) {
                        px = Math.max(px, xMaxArray[i]);
                    }
                    curX = px - curX - 1;
                    if(curX < 0)
                        curX = 0;
                }
                else if (curY < 0) {
                    py = 0;
                    for (i = curX; i < (curX + curWidth); i++) {
                        py = Math.max(py, yMaxArray[i]);
                    }
                    curY = py - curY - 1;
                    if(curY < 0)
                        curY = 0;
                }
                px = curX + curWidth;
                if (layoutWidth < px) {
                    layoutWidth = px;
                }
                py = curY + curHeight;
                if (layoutHeight < py) {
                    layoutHeight = py;
                }
                for (i = curX; i < (curX + curWidth); i++) {
                    yMaxArray[i] =py;
                }
                for (i = curY; i < (curY + curHeight); i++) {
                    xMaxArray[i] = px;
                }
                if (sizeflag == PREFERREDSIZE)
                    d = comp.getPreferredSize();
                else
                    d = comp.getMinimumSize();
                constraints.minWidth = d.width;
                constraints.minHeight = d.height;
                if (calculateBaseline(comp, constraints, d)) {
                    hasBaseline = true;
                }
                if (constraints.gridheight == 0 && constraints.gridwidth == 0)
                    curRow = curCol = -1;
                if (constraints.gridheight == 0 && curRow < 0)
                    curCol = curX + curWidth;
                else if (constraints.gridwidth == 0 && curCol < 0)
                    curRow = curY + curHeight;
            } //for (components) loop
            if (columnWidths != null && layoutWidth < columnWidths.length)
                layoutWidth = columnWidths.length;
            if (rowHeights != null && layoutHeight < rowHeights.length)
                layoutHeight = rowHeights.length;
            r = new GridBagLayoutInfo(layoutWidth, layoutHeight);
            curRow = curCol = -1;
            Arrays.fill(xMaxArray, 0);
            Arrays.fill(yMaxArray, 0);
            int[] maxAscent = null;
            int[] maxDescent = null;
            short[] baselineType = null;
            if (hasBaseline) {
                r.maxAscent = maxAscent = new int[layoutHeight];
                r.maxDescent = maxDescent = new int[layoutHeight];
                r.baselineType = baselineType = new short[layoutHeight];
                r.hasBaseline = true;
            }
            for (compindex = 0 ; compindex < components.length ; compindex++) {
                comp = components[compindex];
                if (!comp.isVisible())
                    continue;
                constraints = lookupConstraints(comp);
                curX = constraints.gridx;
                curY = constraints.gridy;
                curWidth = constraints.gridwidth;
                curHeight = constraints.gridheight;
                if (curX < 0 && curY < 0) {
                    if(curRow >= 0)
                        curY = curRow;
                    else if(curCol >= 0)
                        curX = curCol;
                    else
                        curY = 0;
                }
                if (curX < 0) {
                    if (curHeight <= 0) {
                        curHeight += r.height - curY;
                        if (curHeight < 1)
                            curHeight = 1;
                    }
                    px = 0;
                    for (i = curY; i < (curY + curHeight); i++)
                        px = Math.max(px, xMaxArray[i]);
                    curX = px - curX - 1;
                    if(curX < 0)
                        curX = 0;
                }
                else if (curY < 0) {
                    if (curWidth <= 0) {
                        curWidth += r.width - curX;
                        if (curWidth < 1)
                            curWidth = 1;
                    }
                    py = 0;
                    for (i = curX; i < (curX + curWidth); i++){
                        py = Math.max(py, yMaxArray[i]);
                    }
                    curY = py - curY - 1;
                    if(curY < 0)
                        curY = 0;
                }
                if (curWidth <= 0) {
                    curWidth += r.width - curX;
                    if (curWidth < 1)
                        curWidth = 1;
                }
                if (curHeight <= 0) {
                    curHeight += r.height - curY;
                    if (curHeight < 1)
                        curHeight = 1;
                }
                px = curX + curWidth;
                py = curY + curHeight;
                for (i = curX; i < (curX + curWidth); i++) { yMaxArray[i] = py; }
                for (i = curY; i < (curY + curHeight); i++) { xMaxArray[i] = px; }
                if (constraints.gridheight == 0 && constraints.gridwidth == 0)
                    curRow = curCol = -1;
                if (constraints.gridheight == 0 && curRow < 0)
                    curCol = curX + curWidth;
                else if (constraints.gridwidth == 0 && curCol < 0)
                    curRow = curY + curHeight;
                constraints.tempX = curX;
                constraints.tempY = curY;
                constraints.tempWidth = curWidth;
                constraints.tempHeight = curHeight;
                anchor = constraints.anchor;
                if (hasBaseline) {
                    switch(anchor) {
                    case GridBagConstraints.BASELINE:
                    case GridBagConstraints.BASELINE_LEADING:
                    case GridBagConstraints.BASELINE_TRAILING:
                        if (constraints.ascent >= 0) {
                            if (curHeight == 1) {
                                maxAscent[curY] =
                                        Math.max(maxAscent[curY],
                                                 constraints.ascent);
                                maxDescent[curY] =
                                        Math.max(maxDescent[curY],
                                                 constraints.descent);
                            }
                            else {
                                if (constraints.baselineResizeBehavior ==
                                        Component.BaselineResizeBehavior.
                                        CONSTANT_DESCENT) {
                                    maxDescent[curY + curHeight - 1] =
                                        Math.max(maxDescent[curY + curHeight
                                                            - 1],
                                                 constraints.descent);
                                }
                                else {
                                    maxAscent[curY] = Math.max(maxAscent[curY],
                                                           constraints.ascent);
                                }
                            }
                            if (constraints.baselineResizeBehavior ==
                                    Component.BaselineResizeBehavior.CONSTANT_DESCENT) {
                                baselineType[curY + curHeight - 1] |=
                                        (1 << constraints.
                                         baselineResizeBehavior.ordinal());
                            }
                            else {
                                baselineType[curY] |= (1 << constraints.
                                             baselineResizeBehavior.ordinal());
                            }
                        }
                        break;
                    case GridBagConstraints.ABOVE_BASELINE:
                    case GridBagConstraints.ABOVE_BASELINE_LEADING:
                    case GridBagConstraints.ABOVE_BASELINE_TRAILING:
                        // Component positioned above the baseline.
                        // To make the bottom edge of the component aligned
                        // with the baseline the bottom inset is
                        // added to the descent, the rest to the ascent.
                        pixels_diff = constraints.minHeight +
                                constraints.insets.top +
                                constraints.ipady;
                        maxAscent[curY] = Math.max(maxAscent[curY],
                                                   pixels_diff);
                        maxDescent[curY] = Math.max(maxDescent[curY],
                                                    constraints.insets.bottom);
                        break;
                    case GridBagConstraints.BELOW_BASELINE:
                    case GridBagConstraints.BELOW_BASELINE_LEADING:
                    case GridBagConstraints.BELOW_BASELINE_TRAILING:
                        // Component positioned below the baseline.
                        // To make the top edge of the component aligned
                        // with the baseline the top inset is
                        // added to the ascent, the rest to the descent.
                        pixels_diff = constraints.minHeight +
                                constraints.insets.bottom + constraints.ipady;
                        maxDescent[curY] = Math.max(maxDescent[curY],
                                                    pixels_diff);
                        maxAscent[curY] = Math.max(maxAscent[curY],
                                                   constraints.insets.top);
                        break;
                    }
                }
            }
            r.weightX = new double[maximumArrayYIndex];
            r.weightY = new double[maximumArrayXIndex];
            r.minWidth = new int[maximumArrayYIndex];
            r.minHeight = new int[maximumArrayXIndex];
            if (columnWidths != null)
                System.arraycopy(columnWidths, 0, r.minWidth, 0, columnWidths.length);
            if (rowHeights != null)
                System.arraycopy(rowHeights, 0, r.minHeight, 0, rowHeights.length);
            if (columnWeights != null)
                System.arraycopy(columnWeights, 0, r.weightX, 0,  Math.min(r.weightX.length, columnWeights.length));
            if (rowWeights != null)
                System.arraycopy(rowWeights, 0, r.weightY, 0,  Math.min(r.weightY.length, rowWeights.length));
            nextSize = Integer.MAX_VALUE;
            for (i = 1;
                 i != Integer.MAX_VALUE;
                 i = nextSize, nextSize = Integer.MAX_VALUE) {
                for (compindex = 0 ; compindex < components.length ; compindex++) {
                    comp = components[compindex];
                    if (!comp.isVisible())
                        continue;
                    constraints = lookupConstraints(comp);
                    if (constraints.tempWidth == i) {
                        px = constraints.tempX + constraints.tempWidth;
                        weight_diff = constraints.weightx;
                        for (k = constraints.tempX; k < px; k++)
                            weight_diff -= r.weightX[k];
                        if (weight_diff > 0.0) {
                            weight = 0.0;
                            for (k = constraints.tempX; k < px; k++)
                                weight += r.weightX[k];
                            for (k = constraints.tempX; weight > 0.0 && k < px; k++) {
                                double wt = r.weightX[k];
                                double dx = (wt * weight_diff) / weight;
                                r.weightX[k] += dx;
                                weight_diff -= dx;
                                weight -= wt;
                            }
                            r.weightX[px-1] += weight_diff;
                        }
                        pixels_diff =
                            constraints.minWidth + constraints.ipadx +
                            constraints.insets.left + constraints.insets.right;
                        for (k = constraints.tempX; k < px; k++)
                            pixels_diff -= r.minWidth[k];
                        if (pixels_diff > 0) {
                            weight = 0.0;
                            for (k = constraints.tempX; k < px; k++)
                                weight += r.weightX[k];
                            for (k = constraints.tempX; weight > 0.0 && k < px; k++) {
                                double wt = r.weightX[k];
                                int dx = (int)((wt * ((double)pixels_diff)) / weight);
                                r.minWidth[k] += dx;
                                pixels_diff -= dx;
                                weight -= wt;
                            }
                            r.minWidth[px-1] += pixels_diff;
                        }
                    }
                    else if (constraints.tempWidth > i && constraints.tempWidth < nextSize)
                        nextSize = constraints.tempWidth;
                    if (constraints.tempHeight == i) {
                        py = constraints.tempY + constraints.tempHeight;
                        weight_diff = constraints.weighty;
                        for (k = constraints.tempY; k < py; k++)
                            weight_diff -= r.weightY[k];
                        if (weight_diff > 0.0) {
                            weight = 0.0;
                            for (k = constraints.tempY; k < py; k++)
                                weight += r.weightY[k];
                            for (k = constraints.tempY; weight > 0.0 && k < py; k++) {
                                double wt = r.weightY[k];
                                double dy = (wt * weight_diff) / weight;
                                r.weightY[k] += dy;
                                weight_diff -= dy;
                                weight -= wt;
                            }
                            r.weightY[py-1] += weight_diff;
                        }
                        pixels_diff = -1;
                        if (hasBaseline) {
                            switch(constraints.anchor) {
                            case GridBagConstraints.BASELINE:
                            case GridBagConstraints.BASELINE_LEADING:
                            case GridBagConstraints.BASELINE_TRAILING:
                                if (constraints.ascent >= 0) {
                                    if (constraints.tempHeight == 1) {
                                        pixels_diff =
                                            maxAscent[constraints.tempY] +
                                            maxDescent[constraints.tempY];
                                    }
                                    else if (constraints.baselineResizeBehavior !=
                                             Component.BaselineResizeBehavior.
                                             CONSTANT_DESCENT) {
                                        pixels_diff =
                                                maxAscent[constraints.tempY] +
                                                constraints.descent;
                                    }
                                    else {
                                        pixels_diff = constraints.ascent +
                                                maxDescent[constraints.tempY +
                                                   constraints.tempHeight - 1];
                                    }
                                }
                                break;
                            case GridBagConstraints.ABOVE_BASELINE:
                            case GridBagConstraints.ABOVE_BASELINE_LEADING:
                            case GridBagConstraints.ABOVE_BASELINE_TRAILING:
                                pixels_diff = constraints.insets.top +
                                        constraints.minHeight +
                                        constraints.ipady +
                                        maxDescent[constraints.tempY];
                                break;
                            case GridBagConstraints.BELOW_BASELINE:
                            case GridBagConstraints.BELOW_BASELINE_LEADING:
                            case GridBagConstraints.BELOW_BASELINE_TRAILING:
                                pixels_diff = maxAscent[constraints.tempY] +
                                        constraints.minHeight +
                                        constraints.insets.bottom +
                                        constraints.ipady;
                                break;
                            }
                        }
                        if (pixels_diff == -1) {
                            pixels_diff =
                                constraints.minHeight + constraints.ipady +
                                constraints.insets.top +
                                constraints.insets.bottom;
                        }
                        for (k = constraints.tempY; k < py; k++)
                            pixels_diff -= r.minHeight[k];
                        if (pixels_diff > 0) {
                            weight = 0.0;
                            for (k = constraints.tempY; k < py; k++)
                                weight += r.weightY[k];
                            for (k = constraints.tempY; weight > 0.0 && k < py; k++) {
                                double wt = r.weightY[k];
                                int dy = (int)((wt * ((double)pixels_diff)) / weight);
                                r.minHeight[k] += dy;
                                pixels_diff -= dy;
                                weight -= wt;
                            }
                            r.minHeight[py-1] += pixels_diff;
                        }
                    }
                    else if (constraints.tempHeight > i &&
                             constraints.tempHeight < nextSize)
                        nextSize = constraints.tempHeight;
                }
            }
            return r;
        }
    } //getLayoutInfo()
    private boolean calculateBaseline(Component c,
                                      GridBagConstraints constraints,
                                      Dimension size) {
        int anchor = constraints.anchor;
        if (anchor == GridBagConstraints.BASELINE ||
                anchor == GridBagConstraints.BASELINE_LEADING ||
                anchor == GridBagConstraints.BASELINE_TRAILING) {
            // Apply the padding to the component, then ask for the baseline.
            int w = size.width + constraints.ipadx;
            int h = size.height + constraints.ipady;
            constraints.ascent = c.getBaseline(w, h);
            if (constraints.ascent >= 0) {
                // Component has a baseline
                int baseline = constraints.ascent;
                // Adjust the ascent and descent to include the insets.
                constraints.descent = h - constraints.ascent +
                            constraints.insets.bottom;
                constraints.ascent += constraints.insets.top;
                constraints.baselineResizeBehavior =
                        c.getBaselineResizeBehavior();
                constraints.centerPadding = 0;
                if (constraints.baselineResizeBehavior == Component.
                        BaselineResizeBehavior.CENTER_OFFSET) {
                    // Component has a baseline resize behavior of
                    // CENTER_OFFSET, calculate centerPadding and
                    // centerOffset (see the description of
                    // CENTER_OFFSET in the enum for detais on this
                    // algorithm).
                    int nextBaseline = c.getBaseline(w, h + 1);
                    constraints.centerOffset = baseline - h / 2;
                    if (h % 2 == 0) {
                        if (baseline != nextBaseline) {
                            constraints.centerPadding = 1;
                        }
                    }
                    else if (baseline == nextBaseline){
                        constraints.centerOffset--;
                        constraints.centerPadding = 1;
                    }
                }
            }
            return true;
        }
        else {
            constraints.ascent = -1;
            return false;
        }
    }
    protected void adjustForGravity(GridBagConstraints constraints,
                                    Rectangle r) {
        AdjustForGravity(constraints, r);
    }
    protected void AdjustForGravity(GridBagConstraints constraints,
                                    Rectangle r) {
        int diffx, diffy;
        int cellY = r.y;
        int cellHeight = r.height;
        if (!rightToLeft) {
            r.x += constraints.insets.left;
        } else {
            r.x -= r.width - constraints.insets.right;
        }
        r.width -= (constraints.insets.left + constraints.insets.right);
        r.y += constraints.insets.top;
        r.height -= (constraints.insets.top + constraints.insets.bottom);
        diffx = 0;
        if ((constraints.fill != GridBagConstraints.HORIZONTAL &&
             constraints.fill != GridBagConstraints.BOTH)
            && (r.width > (constraints.minWidth + constraints.ipadx))) {
            diffx = r.width - (constraints.minWidth + constraints.ipadx);
            r.width = constraints.minWidth + constraints.ipadx;
        }
        diffy = 0;
        if ((constraints.fill != GridBagConstraints.VERTICAL &&
             constraints.fill != GridBagConstraints.BOTH)
            && (r.height > (constraints.minHeight + constraints.ipady))) {
            diffy = r.height - (constraints.minHeight + constraints.ipady);
            r.height = constraints.minHeight + constraints.ipady;
        }
        switch (constraints.anchor) {
          case GridBagConstraints.BASELINE:
              r.x += diffx/2;
              alignOnBaseline(constraints, r, cellY, cellHeight);
              break;
          case GridBagConstraints.BASELINE_LEADING:
              if (rightToLeft) {
                  r.x += diffx;
              }
              alignOnBaseline(constraints, r, cellY, cellHeight);
              break;
          case GridBagConstraints.BASELINE_TRAILING:
              if (!rightToLeft) {
                  r.x += diffx;
              }
              alignOnBaseline(constraints, r, cellY, cellHeight);
              break;
          case GridBagConstraints.ABOVE_BASELINE:
              r.x += diffx/2;
              alignAboveBaseline(constraints, r, cellY, cellHeight);
              break;
          case GridBagConstraints.ABOVE_BASELINE_LEADING:
              if (rightToLeft) {
                  r.x += diffx;
              }
              alignAboveBaseline(constraints, r, cellY, cellHeight);
              break;
          case GridBagConstraints.ABOVE_BASELINE_TRAILING:
              if (!rightToLeft) {
                  r.x += diffx;
              }
              alignAboveBaseline(constraints, r, cellY, cellHeight);
              break;
          case GridBagConstraints.BELOW_BASELINE:
              r.x += diffx/2;
              alignBelowBaseline(constraints, r, cellY, cellHeight);
              break;
          case GridBagConstraints.BELOW_BASELINE_LEADING:
              if (rightToLeft) {
                  r.x += diffx;
              }
              alignBelowBaseline(constraints, r, cellY, cellHeight);
              break;
          case GridBagConstraints.BELOW_BASELINE_TRAILING:
              if (!rightToLeft) {
                  r.x += diffx;
              }
              alignBelowBaseline(constraints, r, cellY, cellHeight);
              break;
          case GridBagConstraints.CENTER:
              r.x += diffx/2;
              r.y += diffy/2;
              break;
          case GridBagConstraints.PAGE_START:
          case GridBagConstraints.NORTH:
              r.x += diffx/2;
              break;
          case GridBagConstraints.NORTHEAST:
              r.x += diffx;
              break;
          case GridBagConstraints.EAST:
              r.x += diffx;
              r.y += diffy/2;
              break;
          case GridBagConstraints.SOUTHEAST:
              r.x += diffx;
              r.y += diffy;
              break;
          case GridBagConstraints.PAGE_END:
          case GridBagConstraints.SOUTH:
              r.x += diffx/2;
              r.y += diffy;
              break;
          case GridBagConstraints.SOUTHWEST:
              r.y += diffy;
              break;
          case GridBagConstraints.WEST:
              r.y += diffy/2;
              break;
          case GridBagConstraints.NORTHWEST:
              break;
          case GridBagConstraints.LINE_START:
              if (rightToLeft) {
                  r.x += diffx;
              }
              r.y += diffy/2;
              break;
          case GridBagConstraints.LINE_END:
              if (!rightToLeft) {
                  r.x += diffx;
              }
              r.y += diffy/2;
              break;
          case GridBagConstraints.FIRST_LINE_START:
              if (rightToLeft) {
                  r.x += diffx;
              }
              break;
          case GridBagConstraints.FIRST_LINE_END:
              if (!rightToLeft) {
                  r.x += diffx;
              }
              break;
          case GridBagConstraints.LAST_LINE_START:
              if (rightToLeft) {
                  r.x += diffx;
              }
              r.y += diffy;
              break;
          case GridBagConstraints.LAST_LINE_END:
              if (!rightToLeft) {
                  r.x += diffx;
              }
              r.y += diffy;
              break;
          default:
              throw new IllegalArgumentException("illegal anchor value");
        }
    }
    private void alignOnBaseline(GridBagConstraints cons, Rectangle r,
                                 int cellY, int cellHeight) {
        if (cons.ascent >= 0) {
            if (cons.baselineResizeBehavior == Component.
                    BaselineResizeBehavior.CONSTANT_DESCENT) {
                // Anchor to the bottom.
                // Baseline is at (cellY + cellHeight - maxDescent).
                // Bottom of component (maxY) is at baseline + descent
                // of component. We need to subtract the bottom inset here
                // as the descent in the constraints object includes the
                // bottom inset.
                int maxY = cellY + cellHeight -
                      layoutInfo.maxDescent[cons.tempY + cons.tempHeight - 1] +
                      cons.descent - cons.insets.bottom;
                if (!cons.isVerticallyResizable()) {
                    // Component not resizable, calculate y location
                    // from maxY - height.
                    r.y = maxY - cons.minHeight;
                    r.height = cons.minHeight;
                } else {
                    // Component is resizable. As brb is constant descent,
                    // can expand component to fill region above baseline.
                    // Subtract out the top inset so that components insets
                    // are honored.
                    r.height = maxY - cellY - cons.insets.top;
                }
            }
            else {
                // BRB is not constant_descent
                int baseline; // baseline for the row, relative to cellY
                // Component baseline, includes insets.top
                int ascent = cons.ascent;
                if (layoutInfo.hasConstantDescent(cons.tempY)) {
                    // Mixed ascent/descent in same row, calculate position
                    // off maxDescent
                    baseline = cellHeight - layoutInfo.maxDescent[cons.tempY];
                }
                else {
                    // Only ascents/unknown in this row, anchor to top
                    baseline = layoutInfo.maxAscent[cons.tempY];
                }
                if (cons.baselineResizeBehavior == Component.
                        BaselineResizeBehavior.OTHER) {
                    // BRB is other, which means we can only determine
                    // the baseline by asking for it again giving the
                    // size we plan on using for the component.
                    boolean fits = false;
                    ascent = componentAdjusting.getBaseline(r.width, r.height);
                    if (ascent >= 0) {
                        // Component has a baseline, pad with top inset
                        // (this follows from calculateBaseline which
                        // does the same).
                        ascent += cons.insets.top;
                    }
                    if (ascent >= 0 && ascent <= baseline) {
                        // Components baseline fits within rows baseline.
                        // Make sure the descent fits within the space as well.
                        if (baseline + (r.height - ascent - cons.insets.top) <=
                                cellHeight - cons.insets.bottom) {
                            // It fits, we're good.
                            fits = true;
                        }
                        else if (cons.isVerticallyResizable()) {
                            // Doesn't fit, but it's resizable.  Try
                            // again assuming we'll get ascent again.
                            int ascent2 = componentAdjusting.getBaseline(
                                    r.width, cellHeight - cons.insets.bottom -
                                    baseline + ascent);
                            if (ascent2 >= 0) {
                                ascent2 += cons.insets.top;
                            }
                            if (ascent2 >= 0 && ascent2 <= ascent) {
                                // It'll fit
                                r.height = cellHeight - cons.insets.bottom -
                                        baseline + ascent;
                                ascent = ascent2;
                                fits = true;
                            }
                        }
                    }
                    if (!fits) {
                        // Doesn't fit, use min size and original ascent
                        ascent = cons.ascent;
                        r.width = cons.minWidth;
                        r.height = cons.minHeight;
                    }
                }
                // Reset the components y location based on
                // components ascent and baseline for row. Because ascent
                // includes the baseline
                r.y = cellY + baseline - ascent + cons.insets.top;
                if (cons.isVerticallyResizable()) {
                    switch(cons.baselineResizeBehavior) {
                    case CONSTANT_ASCENT:
                        r.height = Math.max(cons.minHeight,cellY + cellHeight -
                                            r.y - cons.insets.bottom);
                        break;
                    case CENTER_OFFSET:
                        {
                            int upper = r.y - cellY - cons.insets.top;
                            int lower = cellY + cellHeight - r.y -
                                cons.minHeight - cons.insets.bottom;
                            int delta = Math.min(upper, lower);
                            delta += delta;
                            if (delta > 0 &&
                                (cons.minHeight + cons.centerPadding +
                                 delta) / 2 + cons.centerOffset != baseline) {
                                // Off by 1
                                delta--;
                            }
                            r.height = cons.minHeight + delta;
                            r.y = cellY + baseline -
                                (r.height + cons.centerPadding) / 2 -
                                cons.centerOffset;
                        }
                        break;
                    case OTHER:
                        // Handled above
                        break;
                    default:
                        break;
                    }
                }
            }
        }
        else {
            centerVertically(cons, r, cellHeight);
        }
    }
    private void alignAboveBaseline(GridBagConstraints cons, Rectangle r,
                                    int cellY, int cellHeight) {
        if (layoutInfo.hasBaseline(cons.tempY)) {
            int maxY; // Baseline for the row
            if (layoutInfo.hasConstantDescent(cons.tempY)) {
                // Prefer descent
                maxY = cellY + cellHeight - layoutInfo.maxDescent[cons.tempY];
            }
            else {
                // Prefer ascent
                maxY = cellY + layoutInfo.maxAscent[cons.tempY];
            }
            if (cons.isVerticallyResizable()) {
                // Component is resizable. Top edge is offset by top
                // inset, bottom edge on baseline.
                r.y = cellY + cons.insets.top;
                r.height = maxY - r.y;
            }
            else {
                // Not resizable.
                r.height = cons.minHeight + cons.ipady;
                r.y = maxY - r.height;
            }
        }
        else {
            centerVertically(cons, r, cellHeight);
        }
    }
    private void alignBelowBaseline(GridBagConstraints cons, Rectangle r,
                                    int cellY, int cellHeight) {
        if (layoutInfo.hasBaseline(cons.tempY)) {
            if (layoutInfo.hasConstantDescent(cons.tempY)) {
                // Prefer descent
                r.y = cellY + cellHeight - layoutInfo.maxDescent[cons.tempY];
            }
            else {
                // Prefer ascent
                r.y = cellY + layoutInfo.maxAscent[cons.tempY];
            }
            if (cons.isVerticallyResizable()) {
                r.height = cellY + cellHeight - r.y - cons.insets.bottom;
            }
        }
        else {
            centerVertically(cons, r, cellHeight);
        }
    }
    private void centerVertically(GridBagConstraints cons, Rectangle r,
                                  int cellHeight) {
        if (!cons.isVerticallyResizable()) {
            r.y += Math.max(0, (cellHeight - cons.insets.top -
                                cons.insets.bottom - cons.minHeight -
                                cons.ipady) / 2);
        }
    }
    protected Dimension getMinSize(Container parent, GridBagLayoutInfo info) {
        return GetMinSize(parent, info);
    }
    protected Dimension GetMinSize(Container parent, GridBagLayoutInfo info) {
        Dimension d = new Dimension();
        int i, t;
        Insets insets = parent.getInsets();
        t = 0;
        for(i = 0; i < info.width; i++)
            t += info.minWidth[i];
        d.width = t + insets.left + insets.right;
        t = 0;
        for(i = 0; i < info.height; i++)
            t += info.minHeight[i];
        d.height = t + insets.top + insets.bottom;
        return d;
    }
    transient boolean rightToLeft = false;
    protected void arrangeGrid(Container parent) {
        ArrangeGrid(parent);
    }
    protected void ArrangeGrid(Container parent) {
        Component comp;
        int compindex;
        GridBagConstraints constraints;
        Insets insets = parent.getInsets();
        Component components[] = parent.getComponents();
        Dimension d;
        Rectangle r = new Rectangle();
        int i, diffw, diffh;
        double weight;
        GridBagLayoutInfo info;
        rightToLeft = !parent.getComponentOrientation().isLeftToRight();
        if (components.length == 0 &&
            (columnWidths == null || columnWidths.length == 0) &&
            (rowHeights == null || rowHeights.length == 0)) {
            return;
        }
        info = getLayoutInfo(parent, PREFERREDSIZE);
        d = getMinSize(parent, info);
        if (parent.width < d.width || parent.height < d.height) {
            info = getLayoutInfo(parent, MINSIZE);
            d = getMinSize(parent, info);
        }
        layoutInfo = info;
        r.width = d.width;
        r.height = d.height;
        diffw = parent.width - r.width;
        if (diffw != 0) {
            weight = 0.0;
            for (i = 0; i < info.width; i++)
                weight += info.weightX[i];
            if (weight > 0.0) {
                for (i = 0; i < info.width; i++) {
                    int dx = (int)(( ((double)diffw) * info.weightX[i]) / weight);
                    info.minWidth[i] += dx;
                    r.width += dx;
                    if (info.minWidth[i] < 0) {
                        r.width -= info.minWidth[i];
                        info.minWidth[i] = 0;
                    }
                }
            }
            diffw = parent.width - r.width;
        }
        else {
            diffw = 0;
        }
        diffh = parent.height - r.height;
        if (diffh != 0) {
            weight = 0.0;
            for (i = 0; i < info.height; i++)
                weight += info.weightY[i];
            if (weight > 0.0) {
                for (i = 0; i < info.height; i++) {
                    int dy = (int)(( ((double)diffh) * info.weightY[i]) / weight);
                    info.minHeight[i] += dy;
                    r.height += dy;
                    if (info.minHeight[i] < 0) {
                        r.height -= info.minHeight[i];
                        info.minHeight[i] = 0;
                    }
                }
            }
            diffh = parent.height - r.height;
        }
        else {
            diffh = 0;
        }
        info.startx = diffw/2 + insets.left;
        info.starty = diffh/2 + insets.top;
        for (compindex = 0 ; compindex < components.length ; compindex++) {
            comp = components[compindex];
            if (!comp.isVisible()){
                continue;
            }
            constraints = lookupConstraints(comp);
            if (!rightToLeft) {
                r.x = info.startx;
                for(i = 0; i < constraints.tempX; i++)
                    r.x += info.minWidth[i];
            } else {
                r.x = parent.width - (diffw/2 + insets.right);
                for(i = 0; i < constraints.tempX; i++)
                    r.x -= info.minWidth[i];
            }
            r.y = info.starty;
            for(i = 0; i < constraints.tempY; i++)
                r.y += info.minHeight[i];
            r.width = 0;
            for(i = constraints.tempX;
                i < (constraints.tempX + constraints.tempWidth);
                i++) {
                r.width += info.minWidth[i];
            }
            r.height = 0;
            for(i = constraints.tempY;
                i < (constraints.tempY + constraints.tempHeight);
                i++) {
                r.height += info.minHeight[i];
            }
            componentAdjusting = comp;
            adjustForGravity(constraints, r);
            if (r.x < 0) {
                r.width += r.x;
                r.x = 0;
            }
            if (r.y < 0) {
                r.height += r.y;
                r.y = 0;
            }
            if ((r.width <= 0) || (r.height <= 0)) {
                comp.setBounds(0, 0, 0, 0);
            }
            else {
                if (comp.x != r.x || comp.y != r.y ||
                    comp.width != r.width || comp.height != r.height) {
                    comp.setBounds(r.x, r.y, r.width, r.height);
                }
            }
        }
    }
    // Added for serial backwards compatibility (4348425)
    static final long serialVersionUID = 8838754796412211005L;
}
