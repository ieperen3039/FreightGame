package NG.ScreenOverlay.Userinterface;

import NG.ScreenOverlay.ScreenOverlay;

import java.util.function.Consumer;

import static NG.ScreenOverlay.JFGFonts.ORBITRON_MEDIUM;
import static NG.ScreenOverlay.MenuStyleSettings.*;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;

/**
 * @author Yoeri Poels
 */
public class MenuToggleMultiple extends MenuClickable {
    private String text;
    private int value;
    private String[] names;
    private Consumer<Integer> handler;

    public MenuToggleMultiple(String text, int width, int height, String[] names, Consumer<Integer> handler, int initial) {
        super(width, height);
        this.text = text;
        this.names = names;
        this.value = initial;
        this.handler = handler;
    }

    public MenuToggleMultiple(String text, String[] names, Consumer<Integer> handler) {
        this(text, BUTTON_WIDTH, BUTTON_HEIGHT, names, handler, 0);
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public void draw(ScreenOverlay.Painter hud) {
        hud.roundedRectangle(x, y, width, height, INDENT);
        hud.text(x + width / 2, y + TEXT_SIZE_LARGE + 10, TEXT_SIZE_LARGE, ORBITRON_MEDIUM, NVG_ALIGN_CENTER,
                TEXT_COLOR, String.format("%1$s: %2$s", text, names[value]));
    }

    @Override
    public void onClick(int x, int y) {
        value = (value + 1) % names.length;
        handler.accept(value);
    }
}
