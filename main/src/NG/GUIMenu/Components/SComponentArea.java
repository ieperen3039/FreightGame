package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * an area with fixed minimum size that can show components or be hidden. Components are stretched to fit the designated
 * area. If the minimum size of the component is too large for this area, an error is thrown.
 * @author Geert van Ieperen created on 12-7-2019.
 */
public class SComponentArea extends SComponent {
    private final SFiller filler = new SFiller();
    private int width;
    private int height;
    private SComponent element;

    public SComponentArea(SComponentProperties props) {
        this.width = props.minWidth;
        this.height = props.minHeight;
        setGrowthPolicy(props.wantHzGrow, props.wantVtGrow);
        hide();
    }

    public SComponentArea(int width, int height) {
        this.width = width;
        this.height = height;
        setGrowthPolicy(false, false);
        hide();
    }

    /**
     * shows the given element in this component. The element is stretched to fit this area. if it does not fit, a
     * warning is logged and the element is not added.
     */
    public void show(SComponent element) {
        validateLayout();
        int width = getWidth();
        int height = getHeight();

        if (element.minWidth() > width || element.minHeight() > height) {
            throw new RuntimeException(String.format(
                    "Element %s too large to show (%s > %s)",
                    element, Vectors.asVectorString(element.minWidth(), element.minHeight()),
                    Vectors.toString(getSize())
            ));
        }

        this.element = element;
        element.setParent(this);
        element.setGrowthPolicy(true, true);
        invalidateLayout();
    }

    /**
     * removes the current component, and sets this component's visibility to false
     */
    public void hide() {
        element = filler;
    }

    @Override
    protected void doValidateLayout() {
        super.doValidateLayout();

        element.setPosition(0, 0);
        element.setSize(getWidth(), getHeight());
        element.validateLayout();
    }

    @Override
    public boolean contains(int x, int y) {
        return element.contains(x, y);
    }

    @Override
    public SComponent getComponentAt(int xRel, int yRel) {
        return element.getComponentAt(xRel, yRel);
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        element.draw(design, screenPosition);
    }

    @Override
    public SComponentArea setGrowthPolicy(boolean horizontal, boolean vertical) {
        super.setGrowthPolicy(horizontal, vertical);
        return this;
    }

    @Override
    public int minWidth() {
        return width;
    }

    @Override
    public int minHeight() {
        return height;
    }
}
