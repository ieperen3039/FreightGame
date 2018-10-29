package NG.ScreenOverlay.Frames.Components;

import NG.ActionHandling.MouseAnyClickListener;
import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import org.joml.Vector2ic;

/**
 * @author Geert van Ieperen. Created on 22-9-2018.
 */
public class SToggleButton extends SComponent implements MouseAnyClickListener {
    private final int minHeight;
    private final int minWidth;
    private boolean vtGrow = false;
    private boolean hzGrow = false;
    private String text;

    private boolean state;

    public SToggleButton(String text, int minWidth, int minHeight, boolean initial) {
        this.minHeight = minHeight;
        this.minWidth = minWidth;
        this.text = text;
        this.state = initial;
    }

    public SToggleButton(String name, int minWidth, int minHeight) {
        this(name, minWidth, minHeight, false);
    }

    public void setGrowthPolicy(boolean horizontal, boolean vertical) {
        hzGrow = horizontal;
        vtGrow = vertical;
    }

    @Override
    public int minWidth() {
        return minWidth;
    }

    @Override
    public int minHeight() {
        return minHeight;
    }

    @Override
    public boolean wantHorizontalGrow() {
        return hzGrow;
    }

    @Override
    public boolean wantVerticalGrow() {
        return vtGrow;
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        if (dimensions.x == 0 || dimensions.y == 0) return;
        design.drawButton(screenPosition, dimensions, text, state);
    }

    @Override
    public void onClick(int button, int xSc, int ySc) {
        // setState(!getState());
        state = !state;
    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (" + getText() + ")";
    }
}
