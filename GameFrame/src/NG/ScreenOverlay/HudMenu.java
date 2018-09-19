package NG.ScreenOverlay;

import NG.Engine.Game;
import NG.ScreenOverlay.Userinterface.MenuClickable;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * @author Jorren Hendriks
 * @author Geert van Ieperen
 */

public abstract class HudMenu implements Consumer<ScreenOverlay.Painter> {
    private UIElement[] activeElements;
    private final Game game;

    public HudMenu(Game game) {
        this.game = game;
    }

    @Override
    public void accept(ScreenOverlay.Painter hud) {
        for (UIElement element : activeElements) {
            element.draw(hud);
        }
    }

    /**
     * set the active elements to the defined elements
     * @param newElements new elements of the menu
     */
    public void switchContentTo(UIElement[] newElements) {
        activeElements = newElements.clone();
        // TODO  place menu elements
    }

    public void clickEvent(int x, int y) {
        if (!game.menuMode()) return;

        Arrays.stream(activeElements)
                // take all clickable elements
                .filter(element -> element instanceof MenuClickable)
                // identify
                .map(element -> (MenuClickable) element)
                // take the button that is clicked
                .filter(button -> button.contains(x, y))
                .findAny()
                // execute buttonpress
                .ifPresent(button -> button.onClick(x - button.x, y - button.y));
    }
}
