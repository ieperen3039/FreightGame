package NG.ScreenOverlay.Frames;

import NG.ScreenOverlay.ScreenOverlay;
import org.joml.Vector2ic;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public abstract class SContainer extends SComponent {
    private SLayoutManager layout;

    public SContainer(SLayoutManager layout) {
        this();
        this.layout = layout;
    }

    public SContainer() {
    }

    public void setLayoutManager(SLayoutManager layout) {
        this.layout = layout;
    }

    /**
     * @param comp the component to be added
     * @param x    the x grid position
     * @param y    the y grip position
     */
    public void addComponent(int x, int y, SComponent comp) {
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
    public void setDimensions(Vector2ic newDimensions) {
        super.setDimensions(newDimensions);
        layout.invalidate();
    }

    @Override
    public void setDimensions(int width, int height) {
        super.setDimensions(width, height);
        layout.invalidate();
    }
}
