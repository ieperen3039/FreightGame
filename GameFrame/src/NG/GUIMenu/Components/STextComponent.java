package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import org.joml.Vector2ic;

/**
 * @author Geert van Ieperen created on 28-4-2020.
 */
public abstract class STextComponent extends SComponent {
    public static final int TEXT_MIN_X_BORDER = 15;
    protected final int minHeight;
    protected final int minWidth;
    protected final NGFonts.TextType textType;
    protected final SFrameLookAndFeel.Alignment alignment;

    private String text;

    /** minimum border to the left and right of the text */
    protected int minXBorder = TEXT_MIN_X_BORDER;
    protected int textWidth;
    private boolean textWidthIsInvalid = true;

    public STextComponent(
            String text, NGFonts.TextType textType, SFrameLookAndFeel.Alignment alignment, int width, int height
    ) {
        this.minHeight = height;
        this.minWidth = width;
        this.textType = textType;
        setText(text);
        this.alignment = alignment;
    }

    @Override
    public int minWidth() {
        return Math.max(textWidth + 2 * minXBorder, minWidth);
    }

    @Override
    public int minHeight() {
        return minHeight;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        textWidthIsInvalid = true;
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        String text = getText();

        if (textWidthIsInvalid) {
            invalidateLayout();
            textWidth = design.getTextWidth(text, textType);
            validateLayout();
            textWidthIsInvalid = false;
        }

        design.drawText(screenPosition, getSize(), text, textType, alignment);
    }
}
