package NG.ScreenOverlay.Frames.Components;

import NG.ScreenOverlay.Frames.LayoutManagers.GridLayoutManager;
import NG.ScreenOverlay.Frames.LayoutManagers.SLayoutManager;
import NG.ScreenOverlay.Frames.SFrameLookAndFeel;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public abstract class SContainer extends SComponent {
    private final SLayoutManager layout;

    /** when using the default constructor, you can use these values to denote the positions */
    public static final int TOP = 1;
    public static final int LEFT = 1;
    public static final int MIDDLE = 2;
    public static final int BOTTOM = 3;
    public static final int RIGHT = 3;

    protected boolean wantHzGrow;
    protected boolean wantVtGrow;

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
        if (isVisible()) layout.invalidateProperties();
    }

    /**
     * @return the class of properties accepted by this container's {@link #add(SComponent, Object)} method
     */
    public Class<?> getLayoutPropertyClass() {
        return layout.getPropertyClass();
    }

    public void setLookAndFeel(SFrameLookAndFeel lfSet) {
        for (SComponent comp : children()) {

        }

    }

    protected Iterable<SComponent> children() {
        return layout.getComponents();
    }

    public void removeComponent(SComponent comp) {
        layout.remove(comp);
    }

    protected void drawChildren(SFrameLookAndFeel lookFeel) {
        for (SComponent component : children()) {
            if (component.isVisible()) component.draw(lookFeel);
        }
    }

    @Override
    public int minWidth() {
        return layout.getMinimumWidth();
    }

    @Override
    public int minHeight() {
        return layout.getMinimumHeight();
    }

    @Override
    public void setVisibleFlag(boolean doVisible) {
        super.setVisibleFlag(doVisible);
        if (doVisible) layout.invalidateProperties();
    }

    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
        invalidateDimensions();
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        invalidateDimensions();
    }

    protected void invalidateDimensions() {
        layout.setDimensions(this.position, this.dimensions);
        if (isVisible()) layout.placeComponents();
    }

    protected void invalidateLayout() {
        layout.setDimensions(this.position, this.dimensions);
        layout.invalidateProperties();
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
