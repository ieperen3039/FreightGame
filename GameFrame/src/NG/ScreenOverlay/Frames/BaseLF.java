package NG.ScreenOverlay.Frames;

import NG.DataStructures.Color4f;
import NG.Engine.Game;
import NG.Engine.Version;
import NG.ScreenOverlay.NGFonts;
import NG.ScreenOverlay.ScreenOverlay;
import NG.Tools.Logger;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.io.IOException;
import java.nio.file.Path;

import static NG.ScreenOverlay.NGFonts.ORBITRON_MEDIUM;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_MIDDLE;

/**
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public class BaseLF implements SFrameLookAndFeel {
    private static final int INDENT = 5;
    private static final int BUTTON_INDENT = 8;
    private static final int STROKE_WIDTH = 2;
    private static final int TEXT_SIZE_LARGE = 24;
    private static final int BORDER = 3;

    private static final Color4f TEXT_COLOR = Color4f.BLACK;
    private static final Color4f PANEL_COLOR = Color4f.WHITE;
    private static final Color4f STROKE_COLOR = Color4f.BLUE;
    private static final Color4f BUTTON_COLOR = Color4f.GREY;
    private Color4f SELECTION_COLOR = Color4f.TRANSPARENT_GREY;

    private ScreenOverlay.Painter hud;

    @Override
    public void init(Game game) {
        if (!game.getVersionNumber().isLessThan(2, 0))
            Logger.ASSERT.print("SLF is ugly. please install something better");
    }

    @Override
    public void setPainter(ScreenOverlay.Painter painter) {
        this.hud = painter;
        painter.setFillColor(PANEL_COLOR);
        painter.setStroke(STROKE_COLOR, STROKE_WIDTH);
    }

    @Override
    public void drawSelection(Vector2ic pos, Vector2ic dim) {
        hud.rectangle(pos.x(), pos.y(), dim.x(), dim.y(), SELECTION_COLOR, Color4f.INVISIBLE, 1);
    }

    @Override
    public void drawText(Vector2ic pos, Vector2ic dim, String text, NGFonts.TextType size) {
        int x = pos.x();
        int y = pos.y();
        int width = dim.x();
        int height = dim.y();

        hud.text(x + (width / 2), y + (height / 2),
                TEXT_SIZE_LARGE, ORBITRON_MEDIUM, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE, TEXT_COLOR,
                text
        );
    }

    @Override
    public void drawButton(Vector2ic pos, Vector2ic dim, String text, boolean state) {
        Color4f color = BUTTON_COLOR;
        if (state) color = color.darken(0.5f);

        hud.roundedRectangle(pos.x(), pos.y(), dim.x(), dim.y(), BUTTON_INDENT, color, STROKE_COLOR, STROKE_WIDTH);
        drawText(pos, dim, text, NGFonts.TextType.ACCENT);
    }

    @Override
    public void drawIconButton(Vector2ic pos, Vector2ic dim, Path icon, boolean state) throws IOException {
        Color4f buttonColor = Color4f.WHITE;
        if (state) buttonColor = buttonColor.darken(0.5f);
        int iconDisplace = 10;

        hud.roundedRectangle(pos.x(), pos.y(), dim.x(), dim.y(), BUTTON_INDENT, buttonColor, STROKE_COLOR, STROKE_WIDTH);
        Vector2i iconSize = new Vector2i(dim).sub(iconDisplace, iconDisplace);
        Vector2i iconPos = new Vector2i(pos).add(iconDisplace / 2, iconDisplace / 2);
        hud.image(icon, iconPos.x, iconPos.y, iconSize.x, iconSize.y, 1f);
    }

    @Override
    public void drawRectangle(Vector2ic pos, Vector2ic dim) {
        hud.roundedRectangle(pos, dim, INDENT);
    }

    @Override
    public void cleanup() {

    }

    @Override
    public String getModName() {
        return "SLF";
    }

    @Override
    public Version getVersionNumber() {
        return new Version(0, 0);
    }
}