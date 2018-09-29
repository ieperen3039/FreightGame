package NG.ScreenOverlay.Frames;

import NG.ActionHandling.MouseAnyClickListener;
import NG.ActionHandling.MouseMoveListener;
import NG.ActionHandling.MouseReleaseListener;
import NG.Engine.GameAspect;
import NG.ScreenOverlay.Frames.Components.SFrame;
import NG.ScreenOverlay.ScreenOverlay;

/**
 * @author Geert van Ieperen. Created on 29-9-2018.
 */
public interface GUIManager extends GameAspect, MouseAnyClickListener, MouseReleaseListener, MouseMoveListener {
    void draw(ScreenOverlay.Painter painter);

    void addFrame(SFrame frame);

    void addFrame(SFrame frame, int x, int y);

    void focus(SFrame frame);

    void setLookAndFeel(SFrameLookAndFeel lookAndFeel);

    boolean hasLookAndFeel();
}
