import NG.DataStructures.Color4f;
import NG.Engine.Game;
import NG.ScreenOverlay.Frames.SFrameLookAndFeel;
import NG.ScreenOverlay.Menu.MenuStyleSettings;
import NG.ScreenOverlay.ScreenOverlay;
import NG.Settings.Settings;
import NG.Tools.Directory;
import NG.Tools.Logger;
import org.joml.Vector2ic;

import static NG.ScreenOverlay.JFGFonts.ORBITRON_MEDIUM;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;

/**
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public class SimpleLFMod implements SFrameLookAndFeel {
    private static final int INDENT = 2;
    private static final int STROKE_WIDTH = 2;
    private static final int TEXT_SIZE_LARGE = 12;

    private static final Color4f TEXT_COLOR = Color4f.BLACK;
    private static final Color4f PANEL_COLOR = Color4f.WHITE;
    private static final Color4f STROKE_COLOR = Color4f.BLUE;

    private ScreenOverlay.Painter hud;

    @Override
    public void init(Game game) {
        if (!game.getVersionNumber().isLessThan(1, 0))
            Logger.ASSERT.print("SLF may be outdated.");
    }


    @Override
    public void setPainter(ScreenOverlay.Painter painter) {
        this.hud = painter;
        painter.setFill(PANEL_COLOR);
        painter.setStroke(STROKE_WIDTH, STROKE_COLOR);
    }

    @Override
    public void drawSelection(Vector2ic pos, Vector2ic dim) {

    }

    @Override
    public void drawTextArea(Vector2ic pos, Vector2ic dim, int size) {

    }

    @Override
    public void drawButton(Vector2ic pos, Vector2ic dim, String text, boolean state) {
        int x = pos.x();
        int y = pos.y();
        int width = dim.x();
        int height = dim.y();
        hud.roundedRectangle(x, y, width, height, MenuStyleSettings.INDENT);

        hud.text(x + (width / 2), y + (height / 2) - (TEXT_SIZE_LARGE / 2),
                TEXT_SIZE_LARGE, ORBITRON_MEDIUM, NVG_ALIGN_CENTER, TEXT_COLOR,
                text
        );
    }

    @Override
    public void drawRectangle(Vector2ic pos, Vector2ic dim) {
        hud.roundedRectangle(pos, dim, INDENT);
    }

    @Override
    public void cleanup() {

    }

    /** Default main method for Mods. */
    public static void main(String[] args) {
        System.out.println("This is a mod for the game " + Settings.GAME_NAME);
        System.out.println("To use this mod, place this JAR file in folder " + Directory.mods.getFile());
    }
}
