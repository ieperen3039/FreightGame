package NG.ScreenOverlay.Menu;

import NG.ActionHandling.MouseAnyButtonClickListener;
import NG.ScreenOverlay.UIElement;
import org.lwjgl.glfw.GLFW;

/**
 * @author Jorren Hendriks.
 */
public abstract class MenuClickable extends UIElement implements MouseAnyButtonClickListener {
    public MenuClickable(int width, int height) {
        super(width, height);
    }

    @Override
    public void onClick(int button, int x, int y) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) onClick();
    }

    public abstract void onClick();
}
