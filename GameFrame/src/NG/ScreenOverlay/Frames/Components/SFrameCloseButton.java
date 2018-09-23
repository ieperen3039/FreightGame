package NG.ScreenOverlay.Frames.Components;

import NG.ScreenOverlay.Frames.SFrameLookAndFeel;

/**
 * @author Geert van Ieperen. Created on 22-9-2018.
 */
public class SFrameCloseButton extends SClickable {
    private final Runnable action;
    private boolean state = false;

    public SFrameCloseButton(SFrame frame) {
        this.action = frame::dispose;
    }

    @Override
    public int minWidth() {
        return SFrame.FRAME_TITLE_BAR_SIZE;
    }

    @Override
    public int minHeight() {
        return SFrame.FRAME_TITLE_BAR_SIZE;
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
        design.drawButton(position, dimensions, "X", state);

//        try {
//            design.drawIconButton(position, dimensions, null, state);
//        } catch (IOException e) {
//            Logger.WARN.print(e);
//        }
    }

    @Override
    public void onLeftClick() {
        state = true;
        action.run();
    }

    @Override
    public void onRightClick() {

    }
}
