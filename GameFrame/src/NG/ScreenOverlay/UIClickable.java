package NG.ScreenOverlay;

import NG.ActionHandling.MouseButtonClickListener;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public abstract class UIClickable extends UIElement implements MouseButtonClickListener {
    public UIClickable(int width, int height) {
        super(width, height);
    }
}
