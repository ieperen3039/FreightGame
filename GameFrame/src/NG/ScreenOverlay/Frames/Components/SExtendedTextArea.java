package NG.ScreenOverlay.Frames.Components;

import NG.ActionHandling.MouseAnyClickListener;
import NG.ActionHandling.MouseMoveListener;
import NG.ActionHandling.MouseReleaseListener;

/**
 * @author Geert van Ieperen. Created on 25-9-2018.
 */
public class SExtendedTextArea extends STextArea implements MouseAnyClickListener, MouseMoveListener, MouseReleaseListener {
    private MouseMoveListener dragListener;
    private MouseAnyClickListener clickListener;
    private MouseReleaseListener releaseListener;

    public SExtendedTextArea(String frameTitle, boolean doGrowInWidth) {
        super(frameTitle, doGrowInWidth);
    }

    public SExtendedTextArea(STextArea source) {
        super(source.getText(), source.wantHorizontalGrow());
    }

    @Override
    public void onClick(int button, int x, int y) {
        if (clickListener == null) return;
        clickListener.onClick(button, x, y);
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta) {
        if (dragListener == null) return;
        dragListener.mouseMoved(xDelta, yDelta);
    }

    @Override
    public void onRelease(int button, int x, int y) {
        if (releaseListener == null) return;
        releaseListener.onRelease(button, x, y);
    }

    public void setDragListener(MouseMoveListener dragListener) {
        this.dragListener = dragListener;
    }

    public void setClickListener(MouseAnyClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setReleaseListener(MouseReleaseListener releaseListener) {
        this.releaseListener = releaseListener;
    }
}
