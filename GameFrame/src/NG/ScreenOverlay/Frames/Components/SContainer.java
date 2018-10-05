package NG.ScreenOverlay.Frames.Components;

import NG.ScreenOverlay.Frames.LayoutManagers.GridLayoutManager;
import NG.ScreenOverlay.Frames.LayoutManagers.SLayoutManager;
import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import org.joml.Vector2i;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public abstract class SContainer extends SComponent {
    public static final int INNER_BORDER = 4;
    public static final int OUTER_BORDER = 4;
    private final SLayoutManager layout;

    /** when using the default constructor, you can use these values to denote the positions */
    public static final Object NORTH = new Vector2i(1, 0);
    public static final Object EAST = new Vector2i(2, 1);
    public static final Object SOUTH = new Vector2i(1, 2);
    public static final Object WEST = new Vector2i(0, 1);
    public static final Object NORTHEAST = new Vector2i(2, 0);
    public static final Object SOUTHEAST = new Vector2i(2, 2);
    public static final Object MORTHWEST = new Vector2i(0, 0);
    public static final Object SOUTHWEST = new Vector2i(0, 2);
    public static final Object MIDDLE = new Vector2i(1, 1);

    protected boolean wantHzGrow;
    protected boolean wantVtGrow;

    public SContainer() {
        this(new GridLayoutManager(3, 3), false, false);
    }

    /**
     * a container that uses the given manager for its layout
     */
    public SContainer(SLayoutManager layout, boolean growHorizontal, boolean growVertical) {
        this.layout = layout;
        this.wantHzGrow = growHorizontal;
        this.wantVtGrow = growVertical;
    }

    /**
     * constructor for a container that uses a grid layout of the given size and a growth policy of true
     * @param xElts nr of elements in width
     * @param yElts nr of elements in height
     * @param growPolicy
     */
    public SContainer(int xElts, int yElts, boolean growPolicy) {
        this(new GridLayoutManager(xElts, yElts), growPolicy, growPolicy);
    }

    /**
     * @param comp the component to be added
     * @param prop the property instance accepted by the current layout manager
     * @see #getLayoutPropertyClass()
     */
    public void add(SComponent comp, Object prop) {
        layout.add(comp, prop);
        if (isVisible()) invalidateLayout();
    }

    /**
     * @return the class of properties accepted by this container's {@link #add(SComponent, Object)} method
     */
    public Class<?> getLayoutPropertyClass() {
        return layout.getPropertyClass();
    }

    protected Iterable<SComponent> children() {
        return layout.getComponents();
    }

    public void removeComponent(SComponent comp) {
        layout.remove(comp);
    }

    protected void drawChildren(SFrameLookAndFeel lookFeel, Vector2i offset) {
        for (SComponent component : children()) {
            if (component.isVisible()) {
                component.draw(lookFeel, offset);
            }
        }
    }

    @Override
    SComponent locate(int x, int y) {
        for (SComponent component : children()) {
            if (component.isVisible() && component.contains(x, y)) {
                int xr = x - component.getX();
                int yr = y - component.getY();
                return component.locate(xr, yr);
            }
        }
        return this;
    }

    @Override
    public int minWidth() {
        return layout.getMinimumWidth() + (INNER_BORDER * 2);
    }

    @Override
    public int minHeight() {
        return layout.getMinimumHeight() + (INNER_BORDER * 2);
    }

    @Override
    protected void setVisibleFlag(boolean doVisible) {
        super.setVisibleFlag(doVisible);
        if (doVisible) {
            layout.invalidateProperties();
            layout.placeComponents();
        }
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        invalidateDimensions();
    }

    @Override
    public void addToSize(int xDelta, int yDelta) {
        super.addToSize(xDelta, yDelta);
        invalidateDimensions();
    }

    protected void invalidateDimensions() {
        setLayoutDimensions();
        if (isVisible()) layout.placeComponents();
    }

    protected void invalidateLayout() {
        layout.invalidateProperties();
        setLayoutDimensions();
        layout.placeComponents();
    }

    private void setLayoutDimensions() {
        Vector2i displacement = new Vector2i(INNER_BORDER, INNER_BORDER);
        Vector2i newDim = new Vector2i(dimensions).sub(2 * INNER_BORDER, 2 * INNER_BORDER);
        layout.setDimensions(displacement, newDim);
    }

    /**
     * sets the want-grow policies.
     * @param horizontal if true, the next invocation of {@link #wantHorizontalGrow()} will return true. Otherwise, it
     *                   will return true iff any of its child components returns true on that method.
     * @param vertical   if true, the next invocation of {@link #wantVerticalGrow()} will return true. Otherwise, it
     *                   will return true iff any of its child components returns true on that method.
     */
    public void setGrowthPolicy(boolean horizontal, boolean vertical) {
        wantHzGrow = horizontal;
        wantVtGrow = vertical;
    }

    @Override
    public boolean wantHorizontalGrow() {
        if (wantHzGrow) return true;
        for (SComponent c : children()) {
            if (c.wantHorizontalGrow()) return true;
        }
        return false;
    }

    @Override
    public boolean wantVerticalGrow() {
        if (wantVtGrow) return true;
        for (SComponent c : children()) {
            if (c.wantVerticalGrow()) return true;
        }
        return false;
    }
}
