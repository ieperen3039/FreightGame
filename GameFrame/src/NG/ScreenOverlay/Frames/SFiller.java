package NG.ScreenOverlay.Frames;

import NG.ScreenOverlay.ScreenOverlay;

/**
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public class SFiller extends SComponent {
    private final int minWidth;
    private final int minHeight;

    public SFiller(int minWidth, int minHeight) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
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
    public boolean wantGrow() {
        return true;
    }

    @Override
    public void draw(ScreenOverlay.Painter painter) {
    }
}
