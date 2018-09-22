package NG.ScreenOverlay.Frames.Components;

import NG.ActionHandling.MouseButtonClickListener;
import NG.ScreenOverlay.Frames.SFrameLookAndFeel;

/**
 * @author Geert van Ieperen. Created on 22-9-2018.
 */
public class SButton extends SComponent implements MouseButtonClickListener {
    private final int minHeight;
    private final int minWidth;
    private final Runnable action;

    private String text;
    private boolean state = false;

    public SButton(String text, Runnable action) {
        this(text, action, 0, 0);
    }

    public SButton(String text, Runnable action, int minHeight, int minWidth) {
        this.minHeight = minHeight;
        this.minWidth = minWidth;
        this.action = action;
        this.text = text;
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
        return false;
    }

    @Override
    public boolean wantVerticalGrow() {
        return false;
    }

    @Override
    public void draw(SFrameLookAndFeel design) {
        design.drawButton(position, dimensions, text, state);
    }

    @Override
    public void onLeftClick() {
        state = true;
        action.run();
    }

    @Override
    public void onRightClick() {

    }

    @Override
    public void onMiddleButtonClick() {

    }
}
