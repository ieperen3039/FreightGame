package NG.Tracks;

import NG.ActionHandling.MouseTools.MouseTool;
import NG.DataStructures.Pair;
import NG.Engine.Game;
import NG.Entities.Entity;
import NG.ScreenOverlay.Frames.Components.SComponent;
import NG.ScreenOverlay.Frames.Components.SToggleButton;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * @author Geert van Ieperen created on 16-12-2018.
 */
public class TrackBuilder extends MouseTool {
    private final TrackMod.TrackType type;
    private final SToggleButton sourceButton;
    private Game game;

    private Vector2fc firstPosition; // TODO remove this, only allow building from existing tracks or stations
    private NetworkNode firstNode;

    public TrackBuilder(Game game, TrackMod.TrackType type, SToggleButton source) {
        this.game = game;
        sourceButton = source;
        this.type = type;
    }

    @Override
    public void apply(Vector2fc newPosition) {
        if (getButton() == GLFW_MOUSE_BUTTON_RIGHT) {
            close();
            return;
        }

        Logger.DEBUG.print("Clicked on position " + Vectors.toString(newPosition));
        if (firstNode != null) {
            Logger.DEBUG.print("Placing track from " + Vectors.toString(firstNode.getPosition()) +
                    " to " + Vectors.toString(newPosition));

            firstNode = NetworkNode.connectToNew(game, firstNode, newPosition);

        } else if (firstPosition != null) {
            Pair<NetworkNode, NetworkNode> pair = NetworkNode.getNodePair(game, type, firstPosition, newPosition);
            // TODO error message: you must connect to another track or a building of yours.
            firstPosition = null;
            firstNode = pair.right;

        } else {
            firstPosition = new Vector2f(newPosition);
        }
    }

    @Override
    public void apply(Entity entity, Vector3f rayCollision) {
        if (getButton() == GLFW_MOUSE_BUTTON_RIGHT) {
            close();
            return;
        }

        Logger.DEBUG.print("Clicked on entity " + entity);
        if (entity instanceof TrackPiece) {
            Vector2f flatRay = new Vector2f(rayCollision.x, rayCollision.y);
            TrackPiece targetTrack = (TrackPiece) entity;

            if (firstPosition == null) {

            }
        }
    }

    @Override
    public void apply(SComponent component, int xSc, int ySc) {
        Logger.DEBUG.print("Clicked on component " + component);
        close();
    }

    private void close() {
        game.inputHandling().setMouseTool(null);
        sourceButton.setState(false);
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta) {
        // TODO implement multiple build-possibilities
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {

    }

    @Override
    public String toString() {
        return "Track Builder (" + type + ")";
    }
}
