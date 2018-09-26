package NG.ScreenOverlay.Frames.Components;

import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import NG.ScreenOverlay.NGFonts;
import org.joml.Vector2i;
import org.joml.Vector2ic;

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
    public void draw(SFrameLookAndFeel design, Vector2ic offset) {
        Vector2i scPos = new Vector2i(position).add(offset);
        design.drawText(scPos, dimensions, text, textSize);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        int end = Math.min(text.length(), 30);
        return this.getClass().getSimpleName() + " (" + text.substring(0, end) + ")";
    }
}
