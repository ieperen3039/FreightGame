package NG.Tracks;

import NG.ScreenOverlay.Frames.Components.SButton;
import NG.ScreenOverlay.Frames.Components.SFrame;
import NG.ScreenOverlay.Frames.Components.SPanel;
import org.joml.Vector2i;

/**
 * @author Geert van Ieperen. Created on 5-11-2018.
 */
public class BuildMenu extends SFrame {
    public BuildMenu() {
        super("Build Menu");

        SPanel mainPanel = new SPanel(4, 1);

        SButton buildTrack = new SButton("Tracks", 50, 50);
        mainPanel.add(buildTrack, new Vector2i(0, 0));

        SButton demolishTracks = new SButton("Remove", 50, 50);
        mainPanel.add(demolishTracks, new Vector2i(1, 0));

        setMainPanel(mainPanel);
        pack();
    }
}
