package NG.ScreenOverlay.Frames;

import NG.Engine.Game;
import NG.ScreenOverlay.Frames.Components.SFrame;
import NG.ScreenOverlay.Frames.Components.SPanel;
import NG.ScreenOverlay.Frames.LayoutManagers.GridLayoutManager;
import org.joml.Vector2i;

import static NG.ScreenOverlay.Frames.Components.SContainer.MIDDLE;

/**
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public class ExampleSFrame {
    public static SFrame get(Game game) {
        SFrame frame = new SFrame(game, 500, 600);

        SPanel middlePanel = new SPanel(new GridLayoutManager(1, 1), true);
        frame.add(middlePanel, new Vector2i(MIDDLE, MIDDLE));

        return frame;
    }
}
