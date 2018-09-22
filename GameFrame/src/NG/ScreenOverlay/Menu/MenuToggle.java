package NG.ScreenOverlay.Menu;

import NG.ScreenOverlay.ScreenOverlay;

import java.util.function.Consumer;

import static NG.ScreenOverlay.Menu.MenuStyleSettings.TEXT_COLOR;
import static NG.ScreenOverlay.Menu.MenuStyleSettings.TEXT_SIZE_LARGE;
import static NG.ScreenOverlay.NGFonts.ORBITRON_MEDIUM;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;

/**
 * @author Jorren
 */
public class MenuToggle extends MenuClickable {
    private String text;
    private boolean value;
    private String[] names;
    private Consumer<Boolean> handler;

    public MenuToggle(String text, int width, int height, String[] names, Consumer<Boolean> handler) {
        super(width, height);
        this.text = text;
        this.names = names;
        this.value = true;
        this.handler = handler;
    }

    public MenuToggle(String text, Consumer<Boolean> handler) {
        this(text, MenuStyleSettings.BUTTON_WIDTH, MenuStyleSettings.BUTTON_HEIGHT, new String[]{"Enabled", "Disabled"}, handler);
    }

    public MenuToggle(String text, String[] names, Consumer<Boolean> handler) {
        this(text, MenuStyleSettings.BUTTON_WIDTH, MenuStyleSettings.BUTTON_HEIGHT, names, handler);
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public void draw(ScreenOverlay.Painter hud) {
        hud.roundedRectangle(x, y, width, height, MenuStyleSettings.INDENT);
        hud.text(x + width / 2, y + TEXT_SIZE_LARGE + 10,
                TEXT_SIZE_LARGE, ORBITRON_MEDIUM, NVG_ALIGN_CENTER,
                TEXT_COLOR, String.format("%1$s: %2$s", text, names[value ? 0 : 1]));
    }

    @Override
    public void onClick() {
        value = !value;
        handler.accept(value);
    }
}
