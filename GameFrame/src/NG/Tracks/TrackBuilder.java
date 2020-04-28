package NG.Tracks;

import NG.Core.Game;
import NG.Entities.Entity;
import NG.GUIMenu.Components.SToggleButton;
import NG.InputHandling.MouseTools.SurfaceBuildTool;
import NG.Network.NetworkNode;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * @author Geert van Ieperen created on 16-12-2018.
 */
public class TrackBuilder extends SurfaceBuildTool {
    private final TrackType type;
    private NetworkNode firstNode;
    private Vector3fc firstPosition;

    /**
     * this mousetool lets the player place a track by clicking on the map
     * @param game   the game instance
     * @param type   the type of track to place
     * @param source
     */
    public TrackBuilder(Game game, TrackType type, SToggleButton source) {
        super(game, () -> source.setActive(false));
        this.type = type;
    }

    @Override
    public void apply(Vector3fc position, int xSc, int ySc) {
        if (getButton() == GLFW_MOUSE_BUTTON_RIGHT) {
            dispose();
            return;
        }

        Logger.DEBUG.print("Clicked on position " + Vectors.toString(position));

        if (firstNode != null) {
            Logger.DEBUG.print("Placing track from " + Vectors.toString(firstNode.getPosition()) +
                    " to " + Vectors.toString(position));

            firstNode = NetworkNode.createNew(game, firstNode, position);

        } else if (firstPosition != null) {
            TrackPiece trackConnection = NetworkNode.createNewTrack(game, type, firstPosition, position);
            firstPosition = null;
            firstNode = trackConnection.getEndNode();

        } else {
            // TODO error message: you must connect to another track or a building of yours.
            firstPosition = new Vector3f(position);
        }
    }

    @Override
    public void apply(Entity entity, int xSc, int ySc) {
        if (getButton() == GLFW_MOUSE_BUTTON_RIGHT) {
            dispose();
            return;
        }

        Logger.DEBUG.print("Clicked on entity " + entity);
        if (entity instanceof TrackPiece) {
            TrackPiece trackPiece = (TrackPiece) entity;
            Vector3f origin = new Vector3f();
            Vector3f direction = new Vector3f();
            Vectors.windowCoordToRay(game, xSc, ySc, origin, direction);

            Vector3f closestPoint = trackPiece.closestPointOf(origin, direction);

            if (game.keyControl().isControlPressed()) {
                NetworkNode startNode = trackPiece.getStartNode();
                NetworkNode endNode = trackPiece.getEndNode();
                float distToStart = closestPoint.distanceSquared(startNode.getPosition());
                float distToEnd = closestPoint.distanceSquared(endNode.getPosition());

                NetworkNode targetNode;
                if (distToStart < distToEnd) {
                    targetNode = startNode;

                } else {
                    targetNode = endNode;
                }

                if (firstNode == null) {
                    firstNode = targetNode;

                } else {
                    NetworkNode.createConnection(game, firstNode, targetNode);
                    firstNode = null;
                }

            } else {
                if (firstNode == null) {
                    firstNode = NetworkNode.createSplit(game, trackPiece, closestPoint);

                } else {
                    NetworkNode targetNode = NetworkNode.createSplit(game, trackPiece, closestPoint);
                    NetworkNode.createConnection(game, firstNode, targetNode);
                    firstNode = null;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Track Builder (" + type + ")";
    }

}
