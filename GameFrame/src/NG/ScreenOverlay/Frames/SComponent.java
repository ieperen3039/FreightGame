package NG.ScreenOverlay.Frames;

import NG.ScreenOverlay.ScreenOverlay;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * The S stands for Sub-
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public abstract class SComponent {
    protected SFrameLookAndFeel lookFeel;

    protected Vector2i position;
    protected Vector2i dimensions;

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
     * @return true if this component should expand when possible. When false, the components will always be its minimum
     *         size.
     */
    public abstract boolean wantGrow();

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

    // setters
    public void setLookAndFeel(SFrameLookAndFeel lfSet) {
        this.lookFeel = lfSet;
    }

    public void setPosition(int x, int y) {
        position.set(x, y);
    }

    public void setSize(int width, int height) {
        dimensions.set(width, height);
    }

    public final void setPosition(Vector2ic newPosition) {
        setPosition(newPosition.x(), newPosition.y());
    }

    public final void setSize(Vector2ic newDimensions) {
        setSize(newDimensions.x(), newDimensions.y());
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
     * @param painter The hud on which to drawObjects this element.
     */
    public abstract void draw(ScreenOverlay.Painter painter);
}
