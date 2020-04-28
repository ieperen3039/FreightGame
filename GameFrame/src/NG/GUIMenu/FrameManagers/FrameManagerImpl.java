package NG.GUIMenu.FrameManagers;

import NG.Core.Game;
import NG.Core.Version;
import NG.GUIMenu.Components.SComponent;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Components.SToolBar;
import NG.GUIMenu.Rendering.BaseLF;
import NG.GUIMenu.Rendering.NVGOverlay;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.InputHandling.KeyTypeListener;
import NG.InputHandling.MouseClickListener;
import NG.InputHandling.MouseMoveListener;
import NG.InputHandling.MouseReleaseListener;
import NG.Tools.Logger;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.*;

/**
 * Objects of this class can manage an in-game window system that is behaviourally similar to classes in the {@link
 * javax.swing} package. New {@link SFrame} objects can be added using {@link #addFrame(SFrame)}.
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class FrameManagerImpl implements FrameGUIManager {
    private Game game;
    private final Deque<SFrame> frames; // the first element in this list has focus
    private SComponent modalComponent;

    protected int dragButton = 0;
    protected MouseMoveListener dragListener = null;
    protected MouseReleaseListener releaseListener = null;
    protected KeyTypeListener typeListener = null;

    private SFrameLookAndFeel lookAndFeel;
    private SToolBar toolBar = null;

    public FrameManagerImpl() {
        this.frames = new ArrayDeque<>();
        lookAndFeel = new BaseLF();
    }

    @Override
    public void init(Game game) throws Version.MisMatchException {
        if (this.game != null) return;
        this.game = game;
        lookAndFeel.init(game);
    }

    @Override
    public void draw(NVGOverlay.Painter painter) {
        assert hasLookAndFeel();

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

        if (modalComponent != null) {
            modalComponent.validateLayout();
            modalComponent.draw(lookAndFeel, modalComponent.getScreenPosition());
        }

        if (toolBar != null) {
            toolBar.draw(lookAndFeel, null);
        }
    }

    @Override
    public boolean removeElement(SComponent component) {
        if (component instanceof SFrame) {
            ((SFrame) component).dispose();
            return true;
        }

        Optional<SComponent> optParent = component.getParent();
        if (optParent.isPresent()) {
            SComponent parent = optParent.get();
            if (parent instanceof SFrame) {
                ((SFrame) parent).dispose();
                return true;
            }
        }
        return false;
    }

    @Override
    public void addFrame(SFrame frame) {
        frame.validateLayout();

        // TODO remove assumption that toolbar is on top
        int toolbarHeight = toolBar == null ? 0 : toolBar.getHeight();
        int x = 50;
        int y = 50 + toolbarHeight;

        // reposition frame not to overlap other frames (greedy)
        for (Iterator<SFrame> iterator = frames.descendingIterator(); iterator.hasNext(); ) {
            SFrame other = iterator.next();
            if (other.isDisposed() || !other.isVisible()) continue;

            Vector2ic otherPos = other.getScreenPosition();

            if (otherPos.x() == x && otherPos.y() == y) {
                x += 20;
                y += 20; // MS windows-style
            }
        }

        addFrame(frame, x, y);
    }

    @Override
    public void addFrame(SFrame frame, int x, int y) {
        // if the frame was already visible, still add it to make it focused.
        frames.remove(frame);

        boolean success = frames.offerFirst(frame);
        if (!success) {
            Logger.DEBUG.print("Too much subframes opened, removing the last one");
            frames.removeLast().dispose();
            frames.addFirst(frame);
        }

        frame.setPosition(x, y);
    }

    @Override
    public void focus(SFrame frame) {
        if (frame.isDisposed()) {
            throw new NoSuchElementException(frame + " is disposed");
        }

        // even if the frame was not opened, show it
        frame.setVisible(true);

        // no further action when already focused
        if (frame.equals(frames.peekFirst())) return;

        boolean success = frames.remove(frame);
        if (!success) {
            throw new NoSuchElementException(frame + " was not part of the window");
        }

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
        modalComponent = listener;
    }

    @Override
    public void setTextListener(KeyTypeListener listener) {
        typeListener = listener;
    }

    @Override
    public void clear() {
        for (SFrame frame : frames) {
            frame.dispose();
        }
        frames.clear();
    }

    @Override
    public void setToolBar(SToolBar toolBar) {
        this.toolBar = toolBar;
    }

    @Override
    public SToolBar getToolBar() {
        return toolBar;
    }

    @Override
    public void cleanup() {
        frames.forEach(SFrame::dispose);
        frames.clear();
    }

    @Override
    public boolean covers(int xSc, int ySc) {
        if (toolBar != null && toolBar.contains(xSc, ySc)) {
            return true;
        }

        if (modalComponent != null && modalComponent.contains(xSc, ySc)) {
            return true;
        }

        for (SFrame frame : frames) {
            if (frame.isVisible() && frame.contains(xSc, ySc)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean checkMouseClick(int button, final int xSc, final int ySc) {
        // check modal dialogues
        if (modalComponent != null) {
            if (modalComponent.contains(xSc, ySc)) {
                this.apply(button, modalComponent, xSc, ySc);
            }
            modalComponent = null;
            return true;

        } else {
            SComponent component = getComponentAt(xSc, ySc);

            if (component != null) {
                this.apply(button, component, xSc, ySc);
                return true;
            }
        }

        return false;
    }

    private void apply(int button, SComponent component, int xSc, int ySc) {
        if (component instanceof MouseClickListener) {
            MouseClickListener cl = (MouseClickListener) component;
            // by def. of MouseRelativeClickListener, give relative coordinates
            Vector2i pos = component.getScreenPosition();
            cl.onClick(button, xSc - pos.x, ySc - pos.y);
        }

        if (component instanceof MouseMoveListener) {
            dragListener = (MouseMoveListener) component;

        } else {
            dragListener = null;
        }

        if (component instanceof MouseReleaseListener) {
            releaseListener = (MouseReleaseListener) component;

        } else {
            releaseListener = null;
        }
    }

    @Override
    public SComponent getComponentAt(int xSc, int ySc) {
        // check all frames, starting from the front-most frame
        for (SFrame frame : frames) {
            if (frame.isVisible() && frame.contains(xSc, ySc)) {
                focus(frame);
                int xr = xSc - frame.getX();
                int yr = ySc - frame.getY();
                return frame.getComponentAt(xr, yr);
            }
        }

        // check toolbar
        if (toolBar != null) {
            if (toolBar.contains(xSc, ySc)) {
                return toolBar.getComponentAt(xSc, ySc);
            }
        }

        return null;
    }

    @Override
    public void keyTyped(char letter) {
        if (typeListener == null) return;
        typeListener.keyTyped(letter);
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        if (releaseListener == null) return;
        releaseListener.onRelease(button, xSc, ySc);
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta, float xPos, float yPos) {
        if (dragListener == null) return;
        dragListener.mouseMoved(xDelta, yDelta, xPos, yPos);
    }
}
