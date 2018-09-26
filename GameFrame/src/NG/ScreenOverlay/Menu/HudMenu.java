package NG.ScreenOverlay.Menu;

import NG.ActionHandling.MouseAnyClickListener;
import NG.Engine.Game;
import NG.Engine.GameAspect;
import NG.ScreenOverlay.ScreenOverlay;
import NG.ScreenOverlay.UIElement;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * @author Jorren Hendriks
 * @author Geert van Ieperen
 */

public abstract class HudMenu implements Consumer<ScreenOverlay.Painter>, GameAspect, MouseAnyClickListener {
    private UIElement[] activeElements;
    private Game game;

    public HudMenu(Game game) {
        this.game = game;
    }

    @Override
    public void init(Game game) {
        game.callbacks().onMouseButtonClick(this);
    }

    @Override
    public void cleanup() {
        game.callbacks().removeListener(this);
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

    @Override
    public void onClick(int button, int x, int y) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Vector2i mouse = game.window().getMousePosition();
            for (UIElement elt : activeElements) {
                if (elt instanceof MenuClickable && elt.contains(mouse)) {
                    MenuClickable accepter = (MenuClickable) elt;
                    accepter.onClick();
                    break;
                }
            }
        }
    }
}
