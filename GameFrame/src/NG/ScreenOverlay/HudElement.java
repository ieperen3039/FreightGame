package NG.ScreenOverlay;

/**
 * Interface for objects that can be drawn on a Hud.
 * @author Jorren
 */
public interface HudElement {

    /**
     * Draw this hud element.
     * @param hud The hud on which to drawObjects this element.
     */
    void draw(ScreenOverlay.Painter hud);
}
