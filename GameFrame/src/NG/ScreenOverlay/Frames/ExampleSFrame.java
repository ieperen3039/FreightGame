package NG.ScreenOverlay.Frames;

import NG.Engine.Game;

import static NG.ScreenOverlay.Frames.SContainer.*;

/**
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public class ExampleSFrame {
    public static SFrame get(Game game) {
        SFrame frame = new SFrame(game, 200, 150);

        SPanel leftPanel = new SPanel(new GridLayoutManager(2, 2));
        leftPanel.setGrowPolicy(true);
        frame.add(leftPanel, LEFT, MIDDLE);

        SPanel rightPanel = new SPanel(new GridLayoutManager(1, 3));
        SPanel rightTop = new SPanel(50, 50);
        SPanel rightBottom = new SPanel(50, 50);
        rightBottom.setGrowPolicy(true);
        rightPanel.add(rightTop, 0, 0);
        rightPanel.add(rightBottom, 0, 2);
        frame.add(rightPanel, RIGHT, MIDDLE);

        frame.setSize(500, 600);

        return frame;
    }
}
