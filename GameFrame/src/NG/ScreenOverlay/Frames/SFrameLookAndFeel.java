package NG.ScreenOverlay.Frames;

import NG.Mods.Mod;
import NG.ScreenOverlay.NGFonts;
import NG.ScreenOverlay.ScreenOverlay;
import org.joml.Vector2ic;

import java.io.IOException;
import java.nio.file.Path;

/**
 * a stateless mapping from abstract descriptions to drawings in NanoVG
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public interface SFrameLookAndFeel extends Mod {

    /**
     * sets the LF to draw with the specified painter
     * @param painter a new, fresh Painter instance
     */
    void setPainter(ScreenOverlay.Painter painter);

    /**
     * @param pos upper left position of the rectangle
     * @param dim dimension of the rectangle
     */
    void drawRectangle(Vector2ic pos, Vector2ic dim);

    /**
     * draws a button with the given text on it. All the text must fit within the button, but no restrictions are given
     * to the size of the text
     * @param pos   upper left position of the button
     * @param dim   dimension of the button
     * @param text  the text displayed on the button
     * @param state true if the button is activated, ether pressed or toggled on
     */
    void drawButton(Vector2ic pos, Vector2ic dim, String text, boolean state);

    void drawIconButton(Vector2ic pos, Vector2ic dim, Path icon, boolean state) throws IOException;

    /**
     * the position and dimension are hard bounds, the size can be adapted
     * @param pos  upper left position of the area where text may occur
     * @param dim  dimensions of the mentioned area
     * @param text the displayed text
     * @param size the preferred font size of the text
     */
    void drawText(Vector2ic pos, Vector2ic dim, String text, NGFonts.TextType size);

    /**
     * draw a marking to indicate that e.g. a textfield is selected.
     * @param pos upper left position of the selection
     * @param dim dimensions of the selection
     */
    void drawSelection(Vector2ic pos, Vector2ic dim);

}
