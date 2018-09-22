package NG.ScreenOverlay.Frames.Components;

import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import NG.ScreenOverlay.NGFonts;

/**
 * @author Geert van Ieperen. Created on 22-9-2018.
 */
public class STextArea extends SComponent {
    private final boolean doGrowInWidth;
    private final NGFonts.TextType textSize;
    private String text;

    public STextArea(String text, boolean doGrowInWidth) {
        this.text = text;
        this.doGrowInWidth = doGrowInWidth;
        textSize = NGFonts.TextType.REGULAR;
    }

    @Override
    public int minWidth() {
        return 0;
    }

    @Override
    public int minHeight() {
        return 0;
    }

    @Override
    public boolean wantHorizontalGrow() {
        return doGrowInWidth;
    }

    @Override
    public boolean wantVerticalGrow() {
        return false;
    }

    @Override
    public void draw(SFrameLookAndFeel design) {
        design.drawText(position, dimensions, text, textSize);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
