package NG.ScreenOverlay.Frames.Components;

import NG.ScreenOverlay.Frames.SFrameLookAndFeel;

/**
 * @author Geert van Ieperen. Created on 22-9-2018.
 */
public class SButton extends SClickable {
    private final Runnable leftClickAction;
    private final Runnable rightClickAction;
    private final int minHeight;
    private final int minWidth;

    private String text;
    private boolean isPressed = false;
    private boolean vtGrow = false;
    private boolean hzGrow = false;

    public SButton(String text, Runnable action) {
        this(text, action, 0, 0);
    }

    public SButton(String text, Runnable action, int minHeight, int minWidth) {
        this(text, action, null, minHeight, minWidth);
    }

    public SButton(String text, Runnable onLeftClick, Runnable onRightClick, int minHeight, int minWidth) {
        leftClickAction = onLeftClick;
        rightClickAction = onRightClick;
        this.minHeight = minHeight;
        this.minWidth = minWidth;
        this.text = text;
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
    public void draw(SFrameLookAndFeel design) {
        design.drawButton(position, dimensions, text, isPressed);
        isPressed = false;
    }

    @Override
    public void onLeftClick() {
        leftClickAction.run();
        isPressed = true;
    }

    @Override
    public void onRightClick() {
        rightClickAction.run();
        isPressed = true;
    }
}
