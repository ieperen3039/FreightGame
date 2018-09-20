package NG.ScreenOverlay.Frames;

import NG.ActionHandling.MouseButtonClickListener;
import NG.ScreenOverlay.ScreenOverlay;
import NG.ScreenOverlay.UIElement;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class UIClickable extends UIElement implements MouseButtonClickListener {
    public UIClickable(int width, int height) {
        super(width, height);
    }

    @Override
    public void onLeftClick() {

    }

    @Override
    public void onRightClick() {

    }

    @Override
    public void onMiddleButtonClick() {

    }

    @Override
    public void draw(ScreenOverlay.Painter hud) {

    }
}
