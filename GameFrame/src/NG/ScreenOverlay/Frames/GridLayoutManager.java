package NG.ScreenOverlay.Frames;

import NG.Tools.Logger;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public class GridLayoutManager implements SLayoutManager {
    private final SComponent[][] grid;
    private int changeChecker = 0;
    private final int xElts;
    private final int yElts;

    /** maximum of the minimum, used as minimum */
    private int[] minRowHeight;
    private int[] minColWidth;
    private int nOfRowGrows = 0;
    private int nOfColGrows = 0;
    private boolean[] rowWantGrow;
    private boolean[] colWantGrow;

    private Vector2i position = new Vector2i();
    private Vector2i dimensions = new Vector2i();

    public GridLayoutManager(int xElts, int yElts) {
        this.grid = new SComponent[xElts][yElts];
        this.xElts = xElts;
        this.yElts = yElts;
        minColWidth = new int[xElts];
        minRowHeight = new int[yElts];
        colWantGrow = new boolean[xElts];
        rowWantGrow = new boolean[yElts];
    }

    /**
     * adds a component to the grid.
     * @param comp the component to be added
     * @param x    the x grid position
     * @param y    the y grip position
     * @throws IndexOutOfBoundsException if the given x and y fall outside the grid
     */
    @Override
    public void add(SComponent comp, int x, int y) throws IndexOutOfBoundsException {
        grid[x][y] = comp;
        changeChecker++;
        invalidate();
    }

    @Override
    public void add(SComponent comp, int x, int xMax, int y, int yMax) {
        add(comp, x, y); // not supported (yet)
        Logger.ASSERT.print(this.getClass().getName() + " does not support cross-grid elements");
    }

    @Override
    public void remove(SComponent comp) {
        for (int x = 0; x < xElts; x++) {
            SComponent[] col = grid[x];
            for (int y = 0; y < yElts; y++) {
                if (comp.equals(col[y])) {
                    grid[x][y] = null;
                    changeChecker++;
                    return;
                }
            }
        }
    }

    @Override
    public void invalidate() {
        int startChangeNr = changeChecker;
        nOfRowGrows = 0;
        nOfColGrows = 0;
        rowWantGrow = new boolean[yElts];
        colWantGrow = new boolean[xElts];
        minRowHeight = new int[yElts];
        minColWidth = new int[xElts];


        for (int x = 0; x < xElts; x++) {
            SComponent[] col = grid[x];

            for (int y = 0; y < yElts; y++) {
                SComponent elt = col[y];
                if (elt == null) continue;

                minRowHeight[y] = Math.max(elt.minHeight(), minRowHeight[y]);
                minColWidth[x] = Math.max(elt.minWidth(), minColWidth[x]);

                if (elt.wantGrow()) {
                    if (!rowWantGrow[y]) { // we cant do this stateless
                        rowWantGrow[y] = true;
                        nOfRowGrows++;
                    }
                    if (!colWantGrow[x]) {
                        colWantGrow[x] = true;
                        nOfColGrows++;
                    }
                }
            }
        }

        // place components again in the previous dimensions
        setComponents(position, dimensions);

        // if something changed while restructuring, try again
        if (startChangeNr != changeChecker) invalidate();
    }

    @Override
    public Iterable<SComponent> getComponents() {
        return GridIterator::new;
    }

    @Override
    public int getMinimumWidth() {
        int min = 0;
        for (int w : minColWidth) {
            min += w;
        }
        return min;
    }

    @Override
    public int getMinimumHeight() {
        int min = 0;
        for (int w : minRowHeight) {
            min += w;
        }
        return min;
    }

    @Override
    public void setComponents(Vector2ic position, Vector2ic dimensions) {
        this.position.set(position);
        this.dimensions.set(dimensions);

        int[] xPixels = createLayoutDimension(minColWidth, nOfColGrows, colWantGrow, dimensions.x(), position.x());
        int[] yPixels = createLayoutDimension(minRowHeight, nOfRowGrows, rowWantGrow, dimensions.y(), position.y());

        for (int x = 0; x < xElts; x++) {
            for (int y = 0; y < yElts; y++) {
                SComponent elt = grid[x][y];
                if (elt == null) continue;

                int xPixel = xPixels[x] + position.x();
                elt.setPosition(xPixel, yPixels[y]);
            }
        }
    }

    /**
     * calculates the positions of the elements in one dimension
     * @param minSizes     an array with all minimum sizes
     * @param nOfGrows     the number of trues in the {@code wantGrows} table
     * @param wantGrows    for each position, whether at least one element wants to grow
     * @param size         the size of the area where the elements can be placed in
     * @param displacement the initial position of the area
     * @return a list of pixel positions that places the components according to the layout
     */
    private static int[] createLayoutDimension(int[] minSizes, int nOfGrows, boolean[] wantGrows, int size, int displacement) {
        int nOfElements = minSizes.length;

        int spareSize = size;
        for (int s : minSizes) {
            spareSize -= s;
        }

        int growValue = 0;
        if (nOfGrows > 0 && spareSize > 0) {
            growValue = spareSize / nOfGrows;
        }

        int[] pixels = new int[nOfElements];
        pixels[0] = displacement;
        for (int i = 0; i < nOfElements - 1; i++) {
            int eltWidth = minSizes[i];
            if (wantGrows[i]) eltWidth += growValue;
            pixels[i + 1] = pixels[i] + eltWidth;
        }

        return pixels;
    }

    private class GridIterator implements Iterator<SComponent> {
        int startChangeNr = changeChecker;
        int xCur = -1;
        int yCur = 0;

        GridIterator() {
            progress();
        }

        @Override
        public boolean hasNext() {
            return yCur < grid[0].length;
        }

        @Override
        public SComponent next() {
            if (startChangeNr != changeChecker)
                throw new ConcurrentModificationException("Grid changed while iterating");

            SComponent retVal = grid[xCur][yCur];
            progress();
            return retVal;
        }

        private void progress() {
            do {
                xCur++;
                if (xCur == xElts) {
                    xCur = 0;
                    yCur++;
                }
            } while (hasNext() && grid[xCur][yCur] == null);
        }
    }
}
