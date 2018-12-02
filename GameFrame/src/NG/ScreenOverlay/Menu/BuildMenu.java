package NG.ScreenOverlay.Menu;

import NG.ActionHandling.MouseTools.MouseTool;
import NG.Engine.Game;
import NG.Entities.Entity;
import NG.Entities.Tracks.CircleTrack;
import NG.Entities.Tracks.StraightTrack;
import NG.Entities.Tracks.TrackMod;
import NG.Entities.Tracks.TrackPiece;
import NG.ScreenOverlay.Frames.Components.*;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector2i;

import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * @author Geert van Ieperen. Created on 5-11-2018.
 */
public class BuildMenu extends SFrame {
    private final Game game;

    public BuildMenu(Game game) {
        super("Build Menu");
        this.game = game;
        List<TrackMod.TrackType> tracks = game.objectTypes().getTrackTypes();

        SPanel mainPanel = new SPanel(1, tracks.size() + 2);

        for (int i = 0; i < tracks.size(); i++) {
            TrackMod.TrackType trackType = tracks.get(i);
            SToggleButton buildTrack = new SToggleButton(trackType.getTypeName(), 250, 50);
            mainPanel.add(buildTrack, new Vector2i(0, i));

            buildTrack.addStateChangeListener(
                    (active) -> game.inputHandling()
                            .setMouseTool(active ? new TrackBuilder(trackType, buildTrack) : null)
            );
        }

        SButton demolishTracks = new SButton("Remove", 250, 50);
        mainPanel.add(demolishTracks, new Vector2i(0, tracks.size()));

        setMainPanel(mainPanel);
        setSize(200, 0);
    }

    private class TrackBuilder extends MouseTool {
        private final TrackMod.TrackType type;
        private final SToggleButton sourceButton;
        private Vector2fc firstPosition;
        private Vector2fc direction;

        TrackBuilder(TrackMod.TrackType type, SToggleButton source) {
            sourceButton = source;
            this.type = type;
        }

        @Override
        public void apply(Vector2fc position) {
            Logger.DEBUG.print("Clicked on position " + Vectors.toString(position));

            if (getButton() == GLFW_MOUSE_BUTTON_RIGHT) {
                close();
                return;
            }

            if (firstPosition == null) {
                firstPosition = new Vector2f(position);

            } else {
                Logger.DEBUG.print("Placing track from " + Vectors.toString(firstPosition) + " to " + Vectors.toString(position));
                TrackPiece newTrack;
                if (direction == null) {
                    newTrack = new StraightTrack(game, type, firstPosition, position);
                } else {
                    newTrack = new CircleTrack(game, firstPosition, direction, position, type);
                }
                game.state().addEntity(newTrack);
            }
        }

        @Override
        public void apply(Entity entity) {
            Logger.DEBUG.print("Clicked on entity " + entity);
            if (getButton() == GLFW_MOUSE_BUTTON_RIGHT) {
                close();
                return;
            }

            if (entity instanceof TrackPiece) {
                TrackPiece targetTrack = (TrackPiece) entity;
                if (firstPosition == null) {
                    firstPosition = targetTrack.getEndPosition();
                    direction = targetTrack.getEndDirection();
                } else {
                    Vector2fc position = targetTrack.getEndPosition();
                    if (direction == null) direction = new Vector2f();
                    TrackPiece newTrack = new CircleTrack(game, firstPosition, direction, position, type);
                    game.state().addEntity(newTrack);
                }
            }
        }

        @Override
        public void apply(SComponent component, int xSc, int ySc) {
            Logger.DEBUG.print("Clicked on component " + component);
            close();
        }

        private void close() {
            Logger.DEBUG.print("Reset tool to default");
            Thread.dumpStack();
//            game.inputHandling().setMouseTool(null); // is done implicitly
            sourceButton.setState(false);
        }

        @Override
        public void mouseMoved(int xDelta, int yDelta) {
            // TODO implement multiple build-possibilities
        }

        @Override
        public void onRelease(int button, int xSc, int ySc) {

        }
    }
}
