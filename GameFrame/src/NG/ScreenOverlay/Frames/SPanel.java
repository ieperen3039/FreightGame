package NG.ScreenOverlay.Frames;

import NG.ScreenOverlay.ScreenOverlay;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class SPanel extends SContainer {
    private final int minimumWidth;
    private final int minimumHeight;
    private boolean wantGrow;

    /**
     * creates a panel with the given layout manager
     * @param minimumWidth  minimum width of the panel in pixels
     * @param minimumHeight minimum height of the panel in pixels
     * @param layoutManager a new layout manager for this component
     */
    public SPanel(int minimumWidth, int minimumHeight, SLayoutManager layoutManager) {
        super(layoutManager);
        this.minimumWidth = minimumWidth;
        this.minimumHeight = minimumHeight;
        this.wantGrow = false;
    }

    /**
     * creates a panel that uses a grid layout with the given number of rows and columns
     * @param minimumWidth  minimum width of the panel in pixels
     * @param minimumHeight minimum height of the panel in pixels
     * @param rows          number of grid rows in this panel
     * @param cols          number of columns in this panel
     */
    public SPanel(int minimumWidth, int minimumHeight, int rows, int cols) {
        this(minimumWidth, minimumHeight, new GridLayoutManager(rows, cols));
    }

    /**
     * creates a panel with the given layout manager and minimum size of (0, 0)
     * @param layoutManager a new layout manager for this component
     */
    public SPanel(SLayoutManager layoutManager) {
        this(0, 0, layoutManager);
    }

    /**
     * creates a panel that uses the default layout manager of {@link SContainer#SContainer()} and with minimum size of
     * (0, 0)
     */
    public SPanel() {
        this(0, 0);
    }

    /**
     * creates a panel that uses the default layout manager of {@link SContainer#SContainer()} and given minimum size
     * @param minimumWidth  minimum width of the panel in pixels
     * @param minimumHeight minimum height of the panel in pixels
     */
    public SPanel(int minimumWidth, int minimumHeight) {
        super();
        this.minimumWidth = minimumWidth;
        this.minimumHeight = minimumHeight;
    }

    @Override
    public int minWidth() {
        return Math.max(minimumWidth, super.minWidth());
    }

    @Override
    public int minHeight() {
        return Math.max(minimumHeight, super.minHeight());
    }

    @Override
    public boolean wantGrow() {
        return wantGrow;
    }

    @Override
    public void draw(ScreenOverlay.Painter painter) {
        lookFeel.drawRectangle(position, dimensions);
        drawChildren(painter);
    }

    public void setGrowPolicy(boolean wantGrow) {
        this.wantGrow = wantGrow;
    }
}
