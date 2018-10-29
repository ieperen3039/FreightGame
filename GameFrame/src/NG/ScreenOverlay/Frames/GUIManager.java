package NG.ScreenOverlay.Frames;

import NG.ActionHandling.MouseAnyClickListener;
import NG.ActionHandling.MouseMoveListener;
import NG.ActionHandling.MouseReleaseListener;
import NG.Engine.GameAspect;
import NG.ScreenOverlay.Frames.Components.SComponent;
import NG.ScreenOverlay.Frames.Components.SFrame;
import NG.ScreenOverlay.ScreenOverlay;

/**
 * @author Geert van Ieperen. Created on 29-9-2018.
 */
public interface GUIManager extends GameAspect, MouseAnyClickListener, MouseReleaseListener, MouseMoveListener {
    /** draws every frome, starting from the last to most previously focussed. */
    void draw(ScreenOverlay.Painter painter);

    /**
     * adds the given frame at a position that the frame manager assumes to be optimal
     * @see #addFrame(SFrame, int, int)
     */
    void addFrame(SFrame frame);

    /**
     * adds a fame on the given position, and focusses it.
     * @param frame the frame to be added.
     * @param x     screen x coordinate in pixels from left
     * @param y     screen y coordinate in pixels from top
     */
    void addFrame(SFrame frame, int x, int y);

    /**
     * brings the given from to the front-most position
     * @param frame a frame that has been added to this manager
     * @throws java.util.NoSuchElementException if the given frame has not been added or has been disposed.
     */
    void focus(SFrame frame);

    void setLookAndFeel(SFrameLookAndFeel lookAndFeel);

    boolean hasLookAndFeel();

    /**
     * the next click action is instead redirected to the given listener.
     * @param listener a listener that receives the button and screen positions of the next click exactly once.
     */
    void setModalListener(SComponent listener);
}
