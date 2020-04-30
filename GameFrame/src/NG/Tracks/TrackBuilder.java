package NG.Tracks;

import NG.Core.Game;
import NG.DataStructures.Generic.Pair;
import NG.Entities.Entity;
import NG.GUIMenu.Components.SToggleButton;
import NG.InputHandling.MouseTools.ToggleMouseTool;
import NG.Network.NetworkNode;
import NG.Rendering.MatrixStack.SGL;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

/**
 * @author Geert van Ieperen created on 16-12-2018.
 */
public class TrackBuilder extends ToggleMouseTool {
    private final TrackType type;
    private NetworkNode firstNode;
    private Vector3fc firstPosition;

    private final TrackTypeGhost ghostType;
    private TrackPiece ghostTrack1;
    private TrackPiece ghostTrack2;

    /**
     * this mousetool lets the player place a track by clicking on the map
     * @param game   the game instance
     * @param type   the type of track to place
     * @param source
     */
    public TrackBuilder(Game game, TrackType type, SToggleButton source) {
        super(game, () -> source.setActive(false));
        this.type = type;
        this.ghostType = new TrackTypeGhost(type);
    }

    @Override
    public void onClick(int button, int x, int y) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            dispose();

        } else {
            super.onClick(button, x, y);
        }
    }

    @Override
    public void apply(Vector3fc position, int xSc, int ySc) {
        switch (getMouseAction()) {
            case PRESS_ACTIVATE:
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

                return;
            case HOVER:
                clearGhostTracks();

                if (firstNode != null) {
                    Vector3fc direction = firstNode.getDirectionTo(position);
                    NetworkNode ghostNode = new NetworkNode(firstNode.getPosition(), ghostType, direction);
                    ghostTrack1 = TrackPiece.getTrackPiece(
                            game, ghostType, ghostNode, direction, position
                    );

                } else if (firstPosition != null) {
                    Vector3f toNode = new Vector3f(position).sub(firstPosition);
                    NetworkNode ghostNode = new NetworkNode(firstPosition, ghostType, toNode);
                    ghostTrack1 = TrackPiece.getTrackPiece(
                            game, ghostType, ghostNode, toNode, position
                    );
                }
            default:
        }
    }

    @Override
    public void apply(Entity entity, int xSc, int ySc) {
        switch (getMouseAction()) {
            case PRESS_ACTIVATE:
                Logger.DEBUG.print("Clicked on entity " + entity);
                if (entity instanceof TrackPiece) {
                    TrackPiece trackPiece = (TrackPiece) entity;
                    Vector3f origin = new Vector3f();
                    Vector3f direction = new Vector3f();
                    Vectors.windowCoordToRay(game, xSc, ySc, origin, direction);

                    Vector3f closestPoint = trackPiece.closestPointOf(origin, direction);

                    NetworkNode targetNode;
                    if (game.keyControl().isControlPressed()) {
                        targetNode = getClosestNode(trackPiece, closestPoint);
                    } else {
                        targetNode = NetworkNode.createSplit(game, trackPiece, closestPoint);
                    }

                    if (firstNode == null) {
                        firstNode = targetNode;

                    } else {
                        NetworkNode.createConnection(game, firstNode, targetNode);
                        firstNode = null;
                    }
                }
                return;
            case HOVER:
                clearGhostTracks();

                if (entity instanceof TrackPiece) {
                    TrackPiece trackPiece = (TrackPiece) entity;
                    Vector3f origin = new Vector3f();
                    Vector3f direction = new Vector3f();
                    Vectors.windowCoordToRay(game, xSc, ySc, origin, direction);

                    float fraction = trackPiece.getFractionOfClosest(origin, direction);
                    Vector3f closestPoint = trackPiece.getPositionFromFraction(fraction);

                    if (firstNode == null) {
                        // mark

                    } else {
                        Vector3fc fDirection = firstNode.getDirectionTo(closestPoint);
                        NetworkNode ghostNodeFirst = new NetworkNode(firstNode.getPosition(), ghostType, fDirection);

                        NetworkNode ghostNodeTarget;
                        if (game.keyControl().isControlPressed()) {
                            ghostNodeTarget = getClosestNode(trackPiece, closestPoint);

                        } else {
                            Vector3f dir = trackPiece.getDirectionFromFraction(fraction);
                            ghostNodeTarget = new NetworkNode(closestPoint, ghostType, dir);
                        }

                        Vector3fc tDirection = ghostNodeTarget.getDirectionTo(ghostNodeFirst.getPosition());
                        Pair<TrackPiece, TrackPiece> trackPieces = TrackPiece.getTrackPiece(
                                game, ghostType, ghostNodeFirst, fDirection, ghostNodeTarget, tDirection
                        );

                        ghostTrack1 = trackPieces.left;
                        ghostTrack2 = trackPieces.right;
                    }
                }
            default:
        }

    }

    public void clearGhostTracks() {
        if (ghostTrack1 != null) {
            ghostTrack1.dispose();
            ghostTrack1 = null;
        }
        if (ghostTrack2 != null) {
            ghostTrack2.dispose();
            ghostTrack2 = null;
        }
    }

    private static NetworkNode getClosestNode(TrackPiece trackPiece, Vector3f point) {
        NetworkNode startNode = trackPiece.getStartNode();
        NetworkNode endNode = trackPiece.getEndNode();
        float distToStart = point.distanceSquared(startNode.getPosition());
        float distToEnd = point.distanceSquared(endNode.getPosition());

        NetworkNode targetNode;
        if (distToStart < distToEnd) {
            targetNode = startNode;

        } else {
            targetNode = endNode;
        }
        return targetNode;
    }

    @Override
    public void draw(SGL gl) {
        super.draw(gl);

        if (ghostTrack1 != null) {
            ghostTrack1.draw(gl);
        }
        if (ghostTrack2 != null) {
            ghostTrack2.draw(gl);
        }
    }

    @Override
    public void dispose() {
        clearGhostTracks();
        super.dispose();
    }

    @Override
    public String toString() {
        return "Track Builder (" + type + ")";
    }
}
