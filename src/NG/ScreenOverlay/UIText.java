package NG.ScreenOverlay;


import NG.DataStructures.Color4f;

import java.util.function.Supplier;

import static NG.ScreenOverlay.JFGFonts.ORBITRON_MEDIUM;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_TOP;

public class UIText extends UIElement {
    private Supplier<String> supplier;
    private final int align;
    private Color4f color;

    public UIText(int width, int height, Supplier<String> contentSupplier, Color4f color) {
        this(width, height, NVG_ALIGN_CENTER | NVG_ALIGN_TOP, contentSupplier, color);
    }

    public UIText(int width, int height, int align, Supplier<String> contentSupplier, Color4f color) {
        super(width, height);
        this.supplier = contentSupplier;
        this.align = align;
        this.color = color;
    }

    @Override
    public void draw(ScreenOverlay.Painter hud) {
        hud.text(x, y, 30, ORBITRON_MEDIUM, align, color, supplier.get());
    }
}
