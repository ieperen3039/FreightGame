package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.InputHandling.MouseClickListener;
import NG.InputHandling.MouseMoveListener;
import NG.InputHandling.MouseReleaseListener;

/**
 * @author Geert van Ieperen. Created on 25-9-2018.
 */
public class SExtendedTextArea extends STextArea
        implements MouseClickListener, MouseReleaseListener, MouseMoveListener {
    private MouseMoveListener dragListener;
    private MouseClickListener clickListener;
    private MouseReleaseListener releaseListener;

    public SExtendedTextArea(
            String frameTitle, int minHeight, int minWidth, boolean doGrowInWidth, NGFonts.TextType textType,
            SFrameLookAndFeel.Alignment alignment
    ) {
        super(frameTitle, minHeight, minWidth, doGrowInWidth, textType, alignment);
    }

    public SExtendedTextArea(STextArea source) {
        this(source.getText(), source.minWidth(), source.minWidth(), source.wantHorizontalGrow(), source.textType, source.alignment);
    }

    public SExtendedTextArea(String text, int minHeight, boolean doGrowInWidth) {
        super(text, minHeight);
        setGrowthPolicy(doGrowInWidth, false);
    }

    @Override
    public void onClick(int button, int xRel, int yRel) {
        if (clickListener == null) return;
        clickListener.onClick(button, xRel, yRel);
    }

    /**
     * returns the movement of the mouse
     */
    public void mouseMoved(int xDelta, int yDelta, int xPos, int yPos) {
        if (dragListener == null) return;
        dragListener.mouseMoved(xDelta, yDelta, xPos, yPos);
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        if (releaseListener == null) return;
        releaseListener.onRelease(button, xSc, ySc);
    }

    public void setDragListener(MouseMoveListener dragListener) {
        this.dragListener = dragListener;
    }

    public void setClickListener(MouseClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setReleaseListener(MouseReleaseListener releaseListener) {
        this.releaseListener = releaseListener;
    }
}
