package NG.ScreenOverlay.Frames;

import NG.ActionHandling.GLFWListener;
import NG.ActionHandling.MouseClickListener;
import NG.ActionHandling.MouseMoveListener;
import NG.ActionHandling.MouseReleaseListener;
import NG.Engine.Game;
import NG.ScreenOverlay.Frames.Components.SComponent;
import NG.ScreenOverlay.Frames.Components.SFrame;
import NG.ScreenOverlay.ScreenOverlay;
import NG.ScreenOverlay.ToolBar;
import NG.Tools.Logger;
import org.joml.Vector2ic;

import java.util.*;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class SFrameManager implements GUIManager {
    private Game game;
    /** the first element in this list has focus */
    private Deque<SFrame> frames;
    private int dragButton = 0;
    private MouseMoveListener dragListener = null;
    private MouseReleaseListener releaseListener = null;

    private SComponent modalSection;

    private SFrameLookAndFeel lookAndFeel;
    private Optional<ToolBar> toolBar = Optional.empty();

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
    }

    @Override
    public void draw(ScreenOverlay.Painter painter) {
        frames.removeIf(SFrame::isDisposed);
        lookAndFeel.setPainter(painter);

        Iterator<SFrame> itr = frames.descendingIterator();
        while (itr.hasNext()) {
            final SFrame f = itr.next();

            if (f.isVisible()) {
                f.validateLayout();
                f.draw(lookAndFeel, f.getPosition());
            }
        }

        if (modalSection != null) {
            modalSection.validateLayout();
            modalSection.draw(lookAndFeel, modalSection.getScreenPosition());
        }

        toolBar.ifPresent(t -> t.draw(lookAndFeel));
    }

    @Override
    public void addFrame(SFrame frame) {
        frame.validateLayout();

        int width = game.window().getWidth();
        int height = game.window().getHeight();
        int xPos = width / 2 - frame.getWidth() / 2;
        int yPos = height / 2 - frame.getHeight() / 2;

        addFrame(frame, xPos, yPos);
    }

    @Override
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

    @Override
    public void focus(SFrame frame) {
        if (frame.isDisposed())
            throw new NoSuchElementException(frame + " is disposed");

        boolean success = frames.remove(frame);
        if (!success)
            throw new NoSuchElementException(frame + " was not part of the window");

        // even if the frame was not opened, show it
        frame.setVisible(true);
        frames.addFirst(frame);
    }

    @Override
    public void setLookAndFeel(SFrameLookAndFeel lookAndFeel) {
        this.lookAndFeel = lookAndFeel;
    }

    @Override
    public boolean hasLookAndFeel() {
        return lookAndFeel != null;
    }

    @Override
    public void setModalListener(SComponent listener) {
        modalSection = listener;
    }

    @Override
    public void setToolBar(ToolBar toolBar) {
        this.toolBar = Optional.ofNullable(toolBar);
    }

    @Override
    public void cleanup() {
        game.callbacks().removeListener(this);
        frames.forEach(SFrame::dispose);
        frames.clear();
    }

    @Override
    public void onClick(int button, int xSc, int ySc) {
        if (dragListener != null) return;
        dragButton = button;

        if (modalSection != null) {
            MouseRelativeClickListener asListener = (MouseRelativeClickListener) modalSection;
            Vector2ic modalPosition = modalSection.getScreenPosition();
            asListener.onClick(button, xSc - modalPosition.x(), ySc - modalPosition.y());
            modalSection = null;
            return;
        }

        if (toolBar.isPresent()) {
            ToolBar bar = toolBar.get();
            if (bar.contains(xSc, ySc)) {
                bar.onClick(button, xSc, ySc);
                releaseListener = bar;
            }
        }

        for (SFrame frame : frames) {
            if (frame.isVisible() && frame.contains(xSc, ySc)) {
                focus(frame);
                int xr = xSc - frame.getX();
                int yr = ySc - frame.getY();
                SComponent component = frame.getComponentAt(xr, yr);
                Logger.DEBUG.print("Clicked on " + component);

                if (component instanceof MouseClickListener) {
                    MouseClickListener cl = (MouseClickListener) component;
                    // by def. of MouseAnyClickListener, give screen coordinates
                    cl.onClick(button, xSc, ySc);

                } else if (component instanceof MouseRelativeClickListener) {
                    MouseRelativeClickListener cl = (MouseRelativeClickListener) component;
                    // by def. of MouseRelativeClickListener, give relative coordinates
                    Vector2ic pos = component.getScreenPosition();
                    cl.onClick(button, xSc - pos.x(), ySc - pos.y());
                }
                dragListener = (component instanceof MouseMoveListener) ? (MouseMoveListener) component : null;
                releaseListener = (component instanceof MouseReleaseListener) ? (MouseReleaseListener) component : null;

                return; // only for top-most frame
            }
        }
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        if (button != dragButton) return;

        dragListener = null;
        if (releaseListener != null) {
            releaseListener.onRelease(button, xSc, ySc);
            releaseListener = null;
        }
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta) {
        if (dragListener == null) return;
        dragListener.mouseMoved(xDelta, yDelta);
    }
}
