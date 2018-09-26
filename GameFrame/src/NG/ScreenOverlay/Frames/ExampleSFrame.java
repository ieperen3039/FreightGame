package NG.ScreenOverlay.Frames;

import NG.ScreenOverlay.Frames.Components.SFrame;
import NG.ScreenOverlay.Frames.Components.SPanel;
import NG.ScreenOverlay.Frames.LayoutManagers.GridLayoutManager;

import static NG.ScreenOverlay.Frames.Components.SContainer.MIDDLE;

/**
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public class ExampleSFrame {
    public static SFrame get() {
        SFrame frame = new SFrame("Title", 500, 600);

        SPanel middlePanel = new SPanel(new GridLayoutManager(1, 1), true);
        frame.add(middlePanel, MIDDLE);

        return frame;
    }
}
