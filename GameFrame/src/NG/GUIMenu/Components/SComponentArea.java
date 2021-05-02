package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.Tools.Logger;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * an area with fixed minimum size that can show components or be hidden. Components are stretched to fit the designated
 * area. If the minimum size of the component is too large for this area, an assertion is thrown.
 * @author Geert van Ieperen created on 12-7-2019.
 */
public class SComponentArea extends SComponent {
    private static final SFiller FILLER = new SFiller();
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
     * shows the given element in this component. The element is streched to fit this area. if it does not fit, a
     * warning is logged and the element is not added.
     */
    public void show(SComponent element) {
        validateLayout();
        int width = getWidth();
        int height = getHeight();

        if (element.minWidth() <= width && element.minHeight() <= height) {
            element.setSize(width, height);
            this.element = element;
            setVisible(true);

        } else {
            Logger.ASSERT.print("Element too large to show", element, element.getSize(), getSize());
        }
    }

    /**
     * removes the current component, and sets this component's visibility to false
     */
    public void hide() {
        element = FILLER;
        setVisible(false);
    }

    @Override
    public boolean contains(Vector2i v) {
        return isVisible() && element.contains(v);
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
