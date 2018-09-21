package NG.ScreenOverlay.Frames;

import NG.ScreenOverlay.ScreenOverlay;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public abstract class SContainer extends SComponent {
    private final SLayoutManager layout;

    /** when using the default constructor, you can use these values to denote the positions */
    public static final int TOP = 0;
    public static final int LEFT = 0;
    public static final int MIDDLE = 1;
    public static final int BOTTOM = 2;
    public static final int RIGHT = 2;

    /**
     * a container that uses the given manager for its layout
     * @param layout
     */
    public SContainer(SLayoutManager layout) {
        this.layout = layout;
    }

    /**
     * constructor for a container that uses a 3*3 grid layout
     */
    public SContainer() {
        layout = new GridLayoutManager(3, 3);
    }

    /**
     * @param comp the component to be added
     * @param x    the x grid position
     * @param y    the y grip position
     */
    public void add(SComponent comp, int x, int y) {
        comp.setLookAndFeel(lookFeel);
        layout.add(comp, x, y);
    }

    protected Iterable<SComponent> children() {
        return layout.getComponents();
    }

    public void removeComponent(SComponent comp) {
        layout.remove(comp);
    }

    protected void drawChildren(ScreenOverlay.Painter p) {
        for (SComponent component : children()) {
            component.draw(p);
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
    public void setSize(int width, int height) {
        super.setSize(width, height);
        layout.invalidate();
    }
}
