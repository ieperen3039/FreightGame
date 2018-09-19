package NG.ScreenOverlay;

import java.util.function.Consumer;

/**
 * @author Geert van Ieperen. Created on 26-8-2018.
 */
public interface HeadsUpDisplay {
    /**
     * Create something for the hud to be drawn. Package the NanoVG drawObjects commands inside a {@link Consumer <
     * ScreenOverlay.Painter >} which will execute {@link ScreenOverlay.Painter} commands once the Hud is ready to draw
     * @param render The code for drawing inside the hud.
     */
    void addHudItem(Consumer<ScreenOverlay.Painter> render);

    /**
     * Remove an existing drawObjects handler from the Hud.
     * @param render The handler to remove.
     */
    void removeHudItem(Consumer<ScreenOverlay.Painter> render);
}
