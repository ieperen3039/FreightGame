package NG.ScreenOverlay.Frames.Components;

import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * The S stands for Sub-
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public abstract class SComponent {
    private boolean isVisible = true;

    protected Vector2i position = new Vector2i();
    protected Vector2i dimensions = new Vector2i();

    /**
     * @return minimum width of this component in pixels. The final width can be assumed to be at least this size unless
     *         the layout manger decides otherwise.
     */
    public abstract int minWidth();

    /**
     * @return minimum height of this component in pixels. The final height can be assumed to be at least this size
     *         unless the layout manger decides otherwise.
     */
    public abstract int minHeight();

    /**
     * @return true if this component should expand horizontally when possible. when false, the components should always be its minimum
     *         width.
     */
    public abstract boolean wantHorizontalGrow();

    /**
     * @return true if this component should expand horizontally when possible. When false, the components should always
     *         be its minimum height.
     */
    public abstract boolean wantVerticalGrow();

    public boolean contains(Vector2i v) {
        return contains(v.x, v.y);
    }

    public boolean contains(int x, int y) {
        int xr = x - position.x;
        if (xr >= 0 && xr <= dimensions.x) {
            int yr = y - position.y;
            return yr >= 0 && yr <= dimensions.y;
        }
        return false;
    }

    public void setPosition(int x, int y) {
        position.set(x, y);
    }

    public void setSize(int width, int height) {
        assert width >= 0 : "Negative width: " + width + " (height = " + height + ")";
        assert height >= 0 : "Negative height: " + height + " (width = " + width + ")";
        dimensions.set(width, height);
    }

    // getters
    public int getX() {
        return position.x;
    }

    public int getY() {
        return position.y;
    }

    public Vector2ic getPosition() {
        return position;
    }

    public int getWidth() {
        return dimensions.x;
    }

    public int getHeight() {
        return dimensions.y;
    }

    /**
     * Draw this component.
     * @param design The element that provides functions for drawing
     */
    public abstract void draw(SFrameLookAndFeel design);

    protected void setVisibleFlag(boolean doVisible) {
        isVisible = doVisible;
    }

    public boolean isVisible() {
        return isVisible;
    }
}
