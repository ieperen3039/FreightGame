package NG.ScreenOverlay.Frames;

import org.joml.Vector2ic;

/**
 * a layout manager does much the same as an {@link java.awt.LayoutManager}: it lays the components in a layout. Main
 * difference is that addition always happens in a grid (like the {@link java.awt.GridLayout} and {@link
 * java.awt.GridBagLayout}).
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public interface SLayoutManager {

    /**
     * adds a component to this layout manager. Every component that has been added must be returned by {@link
     * #getComponents()} (but not necessarily in order)
     * @param comp the component to be added
     * @param x    the x grid position
     * @param y    the y grip position
     * @see #remove(SComponent)
     */
    void add(SComponent comp, int x, int y);

    /**
     * adds a component to this layout manager, spanning multiple grid positions. Every component that has been added
     * must be returned by {@link #getComponents()} (but not necessarily in order)
     * @param comp the component to be added
     * @param x    the x grid start position
     * @param xMax the x grid end position
     * @param y    the y grip start position
     * @param yMax the y grip end position
     * @see #remove(SComponent)
     */
    void add(SComponent comp, int x, int xMax, int y, int yMax);

    /**
     * inverts an action of {@link #add(SComponent, int, int)}. After returning, the layout manager has no references to
     * the object.
     * @param comp the component to be removed from the layout.
     */
    void remove(SComponent comp);

    /**
     * invalidates the properties of the components, and thus the state of the layout manager.
     */
    void invalidate();

    /**
     * @return an iterable view of the loaded components
     */
    Iterable<SComponent> getComponents();

    /**
     * @return the minimum width of the components together in this layout
     */
    int getMinimumWidth();

    /**
     * @return the minimum height of the components together in this layout
     */
    int getMinimumHeight();

    /**
     * places the components of the layout at the given position within the given dimensions. When this method returns,
     * all components will be layed out according to this layout manager.
     * @param position   the position of the top left corner of the
     * @param dimensions the width and height of the are to draw
     */
    void setComponents(Vector2ic position, Vector2ic dimensions);
}
