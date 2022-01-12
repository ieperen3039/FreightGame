package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import org.joml.Vector2ic;

/**
 * A helper class for building specific containers
 * @author Geert van Ieperen created on 21-2-2020.
 */
public abstract class SDecorator extends SComponent {
    private static final SContainer EMPTY = SContainer.singleton(new SFiller());

    private SContainer contents;

    public SDecorator() {
        contents = EMPTY;
    }

    public SDecorator(SComponent... components) {
        this(SContainer.column(components));
    }

    public SDecorator(SContainer contents) {
        setContents(contents);
    }

    protected void setContents(SContainer contents) {
        this.contents = contents;
        contents.setParent(this);
    }

    @Override
    public boolean wantHorizontalGrow() {
        return contents.wantHorizontalGrow();
    }

    @Override
    public boolean wantVerticalGrow() {
        return contents.wantVerticalGrow();
    }

    @Override
    public SDecorator setGrowthPolicy(boolean horizontal, boolean vertical) {
        contents.setGrowthPolicy(horizontal, vertical);
        return this;
    }

    @Override
    public int minWidth() {
        return contents.minWidth();
    }

    @Override
    public int minHeight() {
        return contents.minHeight();
    }

    @Override
    public SComponent getComponentAt(int xRel, int yRel) {
        validateLayout();
        return contents.getComponentAt(xRel, yRel);
    }

    @Override
    public void doValidateLayout() {
        contents.setSize(getWidth(), getHeight());
        contents.validateLayout();
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        validateLayout();
        contents.draw(design, screenPosition);
    }
}
