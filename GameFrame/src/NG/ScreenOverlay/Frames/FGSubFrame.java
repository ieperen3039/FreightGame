package NG.ScreenOverlay.Frames;

import NG.ActionHandling.MouseButtonClickListener;
import NG.ScreenOverlay.UIElement;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public abstract class FGSubFrame extends UIElement implements MouseButtonClickListener {
    public FGSubFrame(int width, int height) {
        super(width, height);
    }
}
