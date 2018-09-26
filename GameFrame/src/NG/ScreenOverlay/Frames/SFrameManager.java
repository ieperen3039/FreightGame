package NG.ScreenOverlay.Frames;

import NG.ActionHandling.GLFWListener;
import NG.ActionHandling.MouseAnyClickListener;
import NG.ActionHandling.MouseMoveListener;
import NG.ActionHandling.MouseReleaseListener;
import NG.Engine.Game;
import NG.Engine.GameAspect;
import NG.ScreenOverlay.Frames.Components.SComponent;
import NG.ScreenOverlay.Frames.Components.SFrame;
import NG.ScreenOverlay.ScreenOverlay;
import NG.Tools.Logger;
import org.joml.Vector2i;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class SFrameManager implements GameAspect, MouseAnyClickListener, MouseReleaseListener, MouseMoveListener {
    private Game game;
    /** the first element in this list has focus */
    private Deque<SFrame> frames;
    private int dragButton = 0;
    private MouseMoveListener dragListener = null;
    private MouseReleaseListener releaseListener = null;

    private SFrameLookAndFeel lookAndFeel;

    public SFrameManager() {
        this.frames = new ArrayDeque<>();
    }

    @Override
    public void init(Game game) {
        this.game = game;
        GLFWListener callbacks = game.callbacks();
        callbacks.onMouseButtonClick(this);
        callbacks.onMouseMove(this);
        callbacks.onMouseRelease(this);
        game.painter().addHudItem(this::draw);
    }

    public void draw(ScreenOverlay.Painter painter) {
        frames.removeIf(SFrame::isDisposed);
        lookAndFeel.setPainter(painter);

        Iterator<SFrame> itr = frames.descendingIterator();
        while (itr.hasNext()) {
            itr.next().draw(lookAndFeel, new Vector2i());
        }
    }

    public void addFrame(SFrame frame) {
        int width = game.window().getWidth();
        int height = game.window().getHeight();
        int xPos = width / 2 - frame.getWidth() / 2;
        int yPos = height / 2 - frame.getHeight() / 2;
        addFrame(frame, xPos, yPos);
    }

    public void addFrame(SFrame frame, int x, int y) {
        boolean success = frames.offerFirst(frame);
        if (!success) {
            Logger.DEBUG.print("Too much subframes opened, removing the last one");
            frames.removeLast().dispose();
            frames.addFirst(frame);
        }

        frame.setManager(this);
        frame.setPosition(x, y);
    }

    public void focus(SFrame frame) {
        frames.remove(frame);
        // even if the frame was not opened, show it
        frame.setMinimized(false);
        frames.addFirst(frame);
    }

    public void setLookAndFeel(SFrameLookAndFeel lookAndFeel) {
        this.lookAndFeel = lookAndFeel;
    }

    public SFrameLookAndFeel getLookAndFeel() {
        return lookAndFeel;
    }

    @Override
    public void cleanup() {
        game.callbacks().removeListener(this);
        frames.forEach(SFrame::dispose);
        frames.clear();
    }

    @Override
    public void onClick(int button, int x, int y) {
        if (dragListener != null) return;

        for (SFrame frame : frames) {
            if (frame.contains(x, y)) {
                focus(frame);
                int xr = x - frame.getX();
                int yr = y - frame.getY();
                SComponent comp = frame.getComponentAt(xr, yr);
                Logger.DEBUG.print("Clicked on " + comp);

                if (comp instanceof MouseAnyClickListener) {
                    MouseAnyClickListener cl = (MouseAnyClickListener) comp;
                    // by def. of mouseAnyClickListener, give screen coordinates
                    cl.onClick(button, x, y);
                }
                dragListener = (comp instanceof MouseMoveListener) ? (MouseMoveListener) comp : null;
                releaseListener = (comp instanceof MouseReleaseListener) ? (MouseReleaseListener) comp : null;
                dragButton = button;

                return; // only for top-most frame
            }
        }
    }

    @Override
    public void onRelease(int button, int x, int y) {
        if (button != dragButton) return;

        dragListener = null;
        if (releaseListener != null) {
            releaseListener.onRelease(button, x, y);
            releaseListener = null;
        }
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta) {
        if (dragListener == null) return;
        dragListener.mouseMoved(xDelta, yDelta);
    }
}
