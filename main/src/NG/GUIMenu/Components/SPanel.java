package NG.GUIMenu.Components;

import NG.GUIMenu.LayoutManagers.GridLayoutManager;
import NG.GUIMenu.LayoutManagers.SLayoutManager;
import NG.GUIMenu.LayoutManagers.SingleElementLayout;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import static NG.GUIMenu.Rendering.SFrameLookAndFeel.UIComponent.PANEL;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class SPanel extends SContainer {

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

    /**
     * creates a panel with the given layout manager
     * @param layoutManager  a new layout manager for this component
     * @param growHorizontal if true, the panel tries to grow in its width
     * @param growVertical   if true, the panel tries to grow in its height
     */
    public SPanel(
            SLayoutManager layoutManager, boolean growHorizontal,
            boolean growVertical
    ) {
        super(layoutManager);
        setGrowthPolicy(growHorizontal, growVertical);
    }

    /**
     * creates a panel that uses a grid layout with the given number of rows and columns
     * @param cols           number of columns in this panel
     * @param rows           number of grid rows in this panel
     * @param growHorizontal if true, the panel tries to grow in its width
     * @param growVertical   if true, the panel tries to grow in its height
     */
    public SPanel(
            int cols, int rows, boolean growHorizontal, boolean growVertical
    ) {
        this(
                (cols * rows > 0 ? new GridLayoutManager(cols, rows) : new SingleElementLayout()),
                growHorizontal, growVertical
        );
    }

    /**
     * the basic panel for holding components. Use {@link Vector2i} for positioning elements. Note that indices are
     * 0-indexed.
     * @param cols number of columns in x direction
     * @param rows number of rows in y direction
     */
    public SPanel(int cols, int rows) {
        super((cols * rows > 0 ? new GridLayoutManager(cols, rows) : new SingleElementLayout()));
    }

    /**
     * creates a panel with the given layout manager, minimum size of (0, 0) and growth policy in both dimensions
     * @param layoutManager a new layout manager for this component
     * @param growPolicy    if true, try to grow
     */
    public SPanel(SLayoutManager layoutManager, boolean growPolicy) {
        this(layoutManager, growPolicy, growPolicy);
    }

    /**
     * creates a panel with the given layout manager, minimum size of (0, 0)
     * @param layoutManager a layout manager for this component
     */
    public SPanel(SLayoutManager layoutManager) {
        super(layoutManager);
    }

    /**
     * creates a panel that uses a default GridLayout of 3x3 and with minimum size of (0, 0) and a no-growth policy.
     * Objects should be located using one of
     * {@link #NORTH},
     * {@link #EAST},
     * {@link #SOUTH},
     * {@link #WEST},
     * {@link #NORTHEAST},
     * {@link #SOUTHEAST},
     * {@link #MORTHWEST},
     * {@link #SOUTHWEST},
     * {@link #MIDDLE}
     */
    public SPanel() {
        super(new GridLayoutManager(3, 3));
    }

    /**
     * creates a panel with a single element. Calling {@link #add(SComponent, Object)} causes this element to be
     * replaced with the new element.
     * @param content the element of this panel.
     */
    public SPanel(SComponent content) {
        super(new SingleElementLayout());
        add(content, null);
    }


    /** creates a new invisible container with the given components on a single column */
    public static SPanel column(SComponent... components) {
        SPanel column = new SPanel(1, components.length, true, true);

        for (int i = 0; i < components.length; i++) {
            column.add(components[i], new Vector2i(0, i));
        }

        return column;
    }

    /** creates a new invisible container with the given components in a single row */
    public static SPanel row(SComponent... components) {
        SPanel row = new SPanel(components.length, 1, true, true);

        for (int i = 0; i < components.length; i++) {
            row.add(components[i], new Vector2i(i, 0));
        }

        return row;
    }

    /** creates a new invisible container with the given components in a grid */
    public static SPanel grid(SComponent[][] components) {
        int xSize = components[0].length;
        int ySize = components.length;
        SPanel grid = new SPanel(xSize, ySize, true, true);

        for (int i = 0; i < xSize; i++) {
            for (int j = 0; j < ySize; j++) {
                grid.add(components[j][i], new Vector2i(i, j));
            }
        }

        return grid;
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        assert getWidth() > 0 && getHeight() > 0 :
                String.format("Non-positive dimensions of %s: width = %d, height = %d", this, getWidth(), getHeight());

        design.draw(PANEL, screenPosition, getSize());
        drawChildren(design, screenPosition);
    }
}
