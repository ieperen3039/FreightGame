package NG.ScreenOverlay.Frames.LayoutManagers;

import NG.ScreenOverlay.Frames.Components.SComponent;
import org.joml.Vector2ic;

/**
 * a layout manager does much the same as an {@link java.awt.LayoutManager}: it lays the components in a layout. Main
 * difference is that adding new items always happens in a grid (like the {@link java.awt.GridLayout} and {@link
 * java.awt.GridBagLayout}).
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public interface SLayoutManager {

    /**
     * adds a component to this layout manager. Every component that has been added must be returned by {@link
     * #getComponents()} (but not necessarily in order). Adding null values is not permitted. This does not invalidate
     * the layout.
     * @param comp the component to be added
     * @param prop the properties that describe position, policies etc.
     * @see #remove(SComponent)
     * @see #invalidateProperties()
     */
    void add(SComponent comp, Object prop);

    /**
     * inverts an action of {@link #add(SComponent, Object)}. After returning, the layout manager has no references to
     * the object. This does not invalidate the layout.
     * @param comp the component to be removed from the layout.
     */
    void remove(SComponent comp);

    /**
     * invalidates the properties of the components, and thus the state of the layout manager. This also invokes {@link
     * #placeComponents()}
     */
    void invalidateProperties();

    /**
     * places the components of the layout at the previously set dimensions. When this method returns, all components
     * will be positioned and sized correctly
     */
    void placeComponents();

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
     * sets the dimensions where the layout is drawn. This does not invalidate the layout.
     * @param position   the position of the top left corner of the
     * @param dimensions the width and height of the are to draw
     */
    void setDimensions(Vector2ic position, Vector2ic dimensions);

    /**
     * @return the properties class expected when adding elements
     * @see #add(SComponent, Object)
     */
    Class<?> getPropertyClass();
}
