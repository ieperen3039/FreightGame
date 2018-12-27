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
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * @author Geert van Ieperen created on 16-12-2018.
 */
public class TrackBuilder implements MouseTool {
    private final TrackMod.TrackType type;
    private final SToggleButton sourceButton;
    private Game game;

    private Vector2fc firstPosition; // TODO remove this, only allow building from existing tracks or stations
    private NetworkNode firstNode;
    private int button;

    /**
     * this mousetool lets the player place a track by clicking on the map
     * @param game   the game instance
     * @param type   the type of track to place
     * @param source the button which will be set to untoggled when this mousetool is disposed
     */
    public TrackBuilder(Game game, TrackMod.TrackType type, SToggleButton source) {
        this.game = game;
        sourceButton = source;
        this.type = type;
    }

    @Override
    public void apply(Vector2fc newPosition) {
        if (button == GLFW_MOUSE_BUTTON_RIGHT) {
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
    public void apply(Entity entity, Vector3fc rayCollision) {
        if (button == GLFW_MOUSE_BUTTON_RIGHT) {
            close();
            return;
        }

        Logger.DEBUG.print("Clicked on entity " + entity);
        if (entity instanceof TrackPiece) {
            Vector2f flatRay = new Vector2f(rayCollision.x(), rayCollision.y());
            TrackPiece targetTrack = (TrackPiece) entity;

            if (firstNode == null) {
                NetworkNodePoint aPoint = targetTrack.getStartNodePoint();
                NetworkNodePoint bPoint = targetTrack.getEndNodePoint();
                Vector2f closestPoint = targetTrack.closestPointOf(flatRay);
                NetworkNodePoint newPoint = new NetworkNodePoint(closestPoint);

                firstNode = NetworkNode.split(game, newPoint, aPoint.getNode(), bPoint.getNode());
            } else {
                Logger.ERROR.print("Not implemented yet :(");
            }
        }
    }

    @Override
    public void apply(SComponent component, int xSc, int ySc) {
        Logger.DEBUG.print("Clicked on component " + component);
        close();
        game.inputHandling().getMouseTool().apply(component, xSc, ySc);
    }

    /** disposes this mouse tool, resetting the global tool to default */
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

    /**
     * sets the button field. Should only be called by the input handling
     * @param button a button enum, often {@link GLFW#GLFW_MOUSE_BUTTON_LEFT} or {@link GLFW#GLFW_MOUSE_BUTTON_RIGHT}
     */
    @Override
    public void setButton(int button) {
        this.button = button;
    }
}
