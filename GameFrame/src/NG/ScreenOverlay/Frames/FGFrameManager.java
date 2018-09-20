package NG.ScreenOverlay.Frames;

import NG.ActionHandling.MouseAnyButtonClickListener;
import NG.Engine.Game;
import NG.Engine.GameAspect;
import NG.ScreenOverlay.ScreenOverlay;
import NG.Tools.Logger;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class FGFrameManager implements Consumer<ScreenOverlay.Painter>, GameAspect, MouseAnyButtonClickListener {
    private final Game game;
    /** the first element in this list has focus */
    Deque<FGSubFrame> showFrames;
    Collection<FGSubFrame> minimizedFrames;

    public FGFrameManager(Game game) {
        this.game = game;
        this.showFrames = new LinkedList<>();
        this.minimizedFrames = new HashSet<>();
    }

    @Override
    public void init(Game game) throws Exception {
        game.callbacks().onMouseButtonClick(this);
    }


    @Override
    public void accept(ScreenOverlay.Painter painter) {
        Iterator<FGSubFrame> itr = showFrames.descendingIterator();

        while (itr.hasNext()) {
            itr.next().draw(painter);
        }
    }

    public void addFrame(FGSubFrame frame) {
        boolean success = showFrames.offerFirst(frame);
        if (!success) {
            Logger.DEBUG.print("Too much subframes opened, minimizing the last one");
            minimizedFrames.add(showFrames.removeLast());
            showFrames.addFirst(frame);
        }
    }

    public void minimize(FGSubFrame frame) {
        showFrames.remove(frame);
        // if not shown, still add to minimized
        minimizedFrames.add(frame);
    }

    public void focus(FGSubFrame frame) {
        boolean wasMinimized = minimizedFrames.remove(frame);
        if (!wasMinimized) {
            showFrames.remove(frame);
        }
        // even if the frame was not opened, show it
        showFrames.addFirst(frame);
    }

    @Override
    public void cleanup() {
        game.callbacks().removeListener(this);
    }

    @Override
    public void onClick(int button, int x, int y) {
        for (FGSubFrame frame : showFrames) {
            if (frame.contains(x, y)) {
                frame.onClick(button, x - frame.getX(), y - frame.getY());
                return; // only for top-most frame
            }
        }
    }
}
