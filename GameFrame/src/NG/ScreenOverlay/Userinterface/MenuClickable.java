package NG.ScreenOverlay.Userinterface;

import NG.ScreenOverlay.UIElement;

/**
 * @author Jorren Hendriks.
 */
public abstract class MenuClickable extends UIElement {

    public MenuClickable(int width, int height) {
        super(width, height);
    }

    /**
     * is called when this element is clicked on. Coordinates are relative
     * @param x relative x coordinate
     * @param y relative y coordinate
     */
    public abstract void onClick(int x, int y);
}
