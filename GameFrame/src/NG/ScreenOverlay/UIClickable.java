package NG.ScreenOverlay;

import NG.ActionHandling.MouseClickListener;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public abstract class UIClickable extends UIElement implements MouseClickListener {
    public UIClickable(int width, int height) {
        super(width, height);
    }
}
