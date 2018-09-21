package NG.ScreenOverlay.Frames;

import NG.ActionHandling.MouseAnyButtonClickListener;
import NG.Engine.Game;
import NG.Engine.GameAspect;
import NG.ScreenOverlay.ScreenOverlay;
import NG.Tools.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class SFrameManager implements Consumer<ScreenOverlay.Painter>, GameAspect, MouseAnyButtonClickListener {
    private Game game;
    /** the first element in this list has focus */
    private Deque<SFrame> frames;

    public SFrameManager() {
        this.frames = new ArrayDeque<>();
    }

    @Override
    public void init(Game game) throws Exception {
        this.game = game;
        game.callbacks().onMouseButtonClick(this);
    }


    @Override
    public void accept(ScreenOverlay.Painter painter) {
        Iterator<SFrame> itr = frames.descendingIterator();

        while (itr.hasNext()) {
            itr.next().draw(painter);
        }
    }

    public void addFrame(SFrame frame) {
        boolean success = frames.offerFirst(frame);
        if (!success) {
            Logger.DEBUG.print("Too much subframes opened, removing the last one");
            frames.removeLast().dispose();
            frames.addFirst(frame);
        }
    }

    public void focus(SFrame frame) {
        frames.remove(frame);
        // even if the frame was not opened, show it
        frame.setMinimized(false);
        frames.addFirst(frame);
    }

    @Override
    public void cleanup() {
        game.callbacks().removeListener(this);
        frames.forEach(SFrame::dispose);
    }

    @Override
    public void onClick(int button, int x, int y) {
        for (SFrame frame : frames) {
            if (frame.contains(x, y)) {
                focus(frame);
                frame.onClick(button, x - frame.getX(), y - frame.getY());
                return; // only for top-most frame
            }
        }
    }
}
