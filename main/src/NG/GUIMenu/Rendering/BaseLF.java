package NG.GUIMenu.Rendering;

import NG.Core.Game;
import NG.Core.Version;
import NG.DataStructures.Generic.Color4f;
import NG.Tools.Logger;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.EnumSet;

import static NG.GUIMenu.Rendering.NGFonts.ORBITRON_MEDIUM;
import static NG.GUIMenu.Rendering.NVGOverlay.Alignment.*;

/**
 * Little more than the absolute basic appearance of a GUI
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public class BaseLF implements SFrameLookAndFeel {
    private static final int INDENT = 3;
    private static final int BUTTON_INDENT = 5;
    private static final int STROKE_WIDTH = 2;

    private static final NGFonts FONT = ORBITRON_MEDIUM;

    private static final Color4f TEXT_COLOR = Color4f.BLACK;
    private static final Color4f PANEL_COLOR = Color4f.WHITE;
    private static final Color4f STROKE_COLOR = Color4f.BLUE;
    private static final Color4f BUTTON_COLOR = Color4f.GREY;
    private static final Color4f INPUT_FIELD_COLOR = Color4f.LIGHT_GREY;
    private Color4f SELECTION_COLOR = Color4f.TRANSPARENT_GREY;

    private NVGOverlay.Painter hud;

    @Override
    public void init(Game game) {
        if (!game.getVersionNumber().isLessThan(2, 0)) {
            Logger.ASSERT.print(this + " is ugly. Please install something better");
        }
    }

    @Override
    public void setPainter(NVGOverlay.Painter painter) {
        this.hud = painter;
        painter.setFillColor(PANEL_COLOR);
        painter.setStroke(STROKE_COLOR, STROKE_WIDTH);
    }

    @Override
    public NVGOverlay.Painter getPainter() {
        return hud;
    }

    @Override
    public int getTextWidth(String text, NGFonts.TextType textType) {
        int textWidth = 0;
        String[] lines = text.split("\n");
        for (String line : lines) {
            int lineWidth = hud.getTextWidth(line, textType.size(), FONT);
            textWidth = Math.max(textWidth, lineWidth);
        }

        return textWidth;
    }

    @Override
    public void draw(UIComponent type, Vector2ic pos, Vector2ic dim) {
        int x = pos.x();
        int y = pos.y();
        int width = dim.x();
        int height = dim.y();
        assert width > 0 && height > 0 : String.format("Non-positive dimensions: height = %d, width = %d", height, width);

        switch (type) {
            case SCROLL_BAR_BACKGROUND:
                break;

            case BUTTON_ACTIVE:
            case BUTTON_INACTIVE:
            case SCROLL_BAR_DRAG_ELEMENT:
                drawRoundedRectangle(x, y, width, height, BUTTON_COLOR);
                break;

            case BUTTON_HOVERED:
                drawRoundedRectangle(x, y, width, height, BUTTON_COLOR.intensify(0.1f));
                break;

            case BUTTON_PRESSED:
                drawRoundedRectangle(x, y, width, height, BUTTON_COLOR.darken(0.5f));
                break;

            case INPUT_FIELD:
                drawRoundedRectangle(x, y, width, height, INPUT_FIELD_COLOR);
                break;

            case SELECTION:
                hud.setStroke(STROKE_COLOR, 0);
                drawRoundedRectangle(x, y, width, height, SELECTION_COLOR);
                break;

            case DROP_DOWN_HEAD_CLOSED:
            case DROP_DOWN_HEAD_OPEN:
            case DROP_DOWN_OPTION_FIELD:
            case PANEL:
            case FRAME_HEADER:
            default:
                drawRoundedRectangle(x, y, width, height);
        }
    }

    private void drawRoundedRectangle(int x, int y, int width, int height, Color4f color) {
        int xMax2 = x + width;
        int yMax2 = y + height;

        hud.polygon(color, STROKE_COLOR, STROKE_WIDTH,
                new Vector2i(x + BUTTON_INDENT, y),
                new Vector2i(xMax2 - BUTTON_INDENT, y),
                new Vector2i(xMax2, y + BUTTON_INDENT),
                new Vector2i(xMax2, yMax2 - BUTTON_INDENT),
                new Vector2i(xMax2 - BUTTON_INDENT, yMax2),
                new Vector2i(x + BUTTON_INDENT, yMax2),
                new Vector2i(x, yMax2 - BUTTON_INDENT),
                new Vector2i(x, y + BUTTON_INDENT)
        );
    }

    private void drawRoundedRectangle(int x, int y, int width, int height) {
        int xMax = x + width;
        int yMax = y + height;

        hud.polygon(
                new Vector2i(x + INDENT, y),
                new Vector2i(xMax - INDENT, y),
                new Vector2i(xMax, y + INDENT),
                new Vector2i(xMax, yMax - INDENT),
                new Vector2i(xMax - INDENT, yMax),
                new Vector2i(x + INDENT, yMax),
                new Vector2i(x, yMax - INDENT),
                new Vector2i(x, y + INDENT)
        );
    }

    @Override
    public void drawText(
            Vector2ic pos, Vector2ic dim, String text, NGFonts.TextType type, Alignment align
    ) {
        if (text == null || text.isEmpty()) return;

        int x = pos.x();
        int y = pos.y();
        int width = dim.x();
        int height = dim.y();
        Color4f textColor = TEXT_COLOR;
        NGFonts font = FONT;

        switch (type) {
            case TITLE:
            case ACCENT:
                break;
            case RED:
                textColor = new Color4f(0.8f, 0.1f, 0.1f);
                break;
        }

        switch (align) {
            case LEFT_TOP:
                hud.text(x, y, type.size(),
                        font, EnumSet.of(ALIGN_LEFT, ALIGN_TOP), textColor, text, width
                );
                break;
            case CENTER_TOP:
                hud.text(x, y, type.size(),
                        font, EnumSet.of(ALIGN_TOP), textColor, text, width
                );
                break;
            case RIGHT_TOP:
                hud.text(x, y, type.size(),
                        font, EnumSet.of(ALIGN_RIGHT, ALIGN_TOP), textColor, text, width
                );
                break;
            case LEFT_MIDDLE:
                hud.text(x, y + (height / 2), type.size(),
                        font, EnumSet.of(ALIGN_LEFT), textColor, text, width
                );
                break;
            case CENTER_MIDDLE:
                hud.text(x, y + (height / 2), type.size(),
                        font, EnumSet.noneOf(NVGOverlay.Alignment.class), textColor, text, width
                );
                break;
            case RIGHT_MIDDLE:
                hud.text(x, y + (height / 2), type.size(),
                        font, EnumSet.of(ALIGN_RIGHT), textColor, text, width
                );
                break;
        }
    }

    @Override
    public void cleanup() {
        hud = null;
    }

    @Override
    public Version getVersionNumber() {
        return new Version(0, 0);
    }
}
