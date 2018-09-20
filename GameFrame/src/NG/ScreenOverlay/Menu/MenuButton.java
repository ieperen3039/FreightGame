package NG.ScreenOverlay.Menu;

import NG.ScreenOverlay.ScreenOverlay;

import static NG.ScreenOverlay.JFGFonts.ORBITRON_MEDIUM;
import static NG.ScreenOverlay.Menu.MenuStyleSettings.*;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;

/**
 * @author Geert van Ieperen a button with set shape and text
 */
public class MenuButton extends MenuClickable {

    private final String text;
    private final Runnable leftClick;

    public MenuButton(String text, Runnable click) {
        this(text, BUTTON_WIDTH, BUTTON_HEIGHT, click);
    }

    /**
     * create a button that executes a click
     * @param text  the text displayed on the button, will also be used to name in case of error
     * @param leftClick action that is executed when clicking on this button
     */
    public MenuButton(String text, int width, int height, Runnable leftClick) {
        super(width, height);
        this.text = text;
        this.leftClick = leftClick;
    }

    @Override
    public void draw(ScreenOverlay.Painter hud) {
        hud.roundedRectangle(x, y, width, height, MenuStyleSettings.INDENT);

        hud.text(x + (width / 2), y + (height / 2) - (TEXT_SIZE_LARGE / 2),
                TEXT_SIZE_LARGE, ORBITRON_MEDIUM, NVG_ALIGN_CENTER, TEXT_COLOR,
                text
        );
    }

    @Override
    public void onClick() {
        leftClick.run();
    }

}
