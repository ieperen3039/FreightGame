package NG.Tracks;

import NG.Core.Game;
import NG.DataStructures.Generic.Pair;
import NG.Entities.Entity;
import NG.GUIMenu.Components.SToggleButton;
import NG.InputHandling.MouseTools.ToggleMouseTool;
import NG.Network.RailNode;
import NG.Network.RailTools;
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
    private RailNode firstNode;
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

                    firstNode = RailTools.createNew(game, firstNode, position);

                } else if (firstPosition != null) {
                    TrackPiece trackConnection = RailTools.createNew(game, type, firstPosition, position);
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
                    RailNode ghostNode = new RailNode(firstNode.getPosition(), ghostType, direction);
                    ghostTrack1 = RailTools.getTrackPiece(
                            game, ghostType, ghostNode, direction, position
                    );

                } else if (firstPosition != null) {
                    Vector3f toNode = new Vector3f(position).sub(firstPosition);
                    RailNode ghostNode = new RailNode(firstPosition, ghostType, toNode);
                    ghostTrack1 = RailTools.getTrackPiece(
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
                    assert trackPiece.isValid() : trackPiece;

                    Vector3f origin = new Vector3f();
                    Vector3f direction = new Vector3f();
                    Vectors.windowCoordToRay(game, xSc, ySc, origin, direction);

                    float fraction = trackPiece.getFractionOfClosest(origin, direction);

                    RailNode targetNode;
                    if (game.keyControl().isControlPressed()) {
                        if (fraction < 0.5f) {
                            targetNode = trackPiece.getStartNode();
                        } else {
                            targetNode = trackPiece.getEndNode();
                        }
                    } else {
                        assert fraction >= 0 && fraction <= 1 : fraction;
                        targetNode = RailTools.createSplit(game, trackPiece, fraction);
                    }

                    if (firstNode == null) {
                        firstNode = targetNode;

                    } else {
                        RailTools.createConnection(game, firstNode, targetNode);
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
                    if (fraction < 0 || fraction > 1) return;

                    Vector3f closestPoint = trackPiece.getPositionFromFraction(fraction);

                    if (firstNode == null) {
                        // mark

                    } else {
                        Vector3fc fDirection = firstNode.getDirectionTo(closestPoint);
                        RailNode ghostNodeFirst = new RailNode(firstNode.getPosition(), ghostType, fDirection);

                        RailNode ghostNodeTarget;
                        if (game.keyControl().isControlPressed()) {
                            ghostNodeTarget = getClosestNode(trackPiece, closestPoint);

                        } else {
                            Vector3f dir = trackPiece.getDirectionFromFraction(fraction);
                            ghostNodeTarget = new RailNode(closestPoint, ghostType, dir);
                        }

                        Pair<TrackPiece, TrackPiece> trackPieces = RailTools.getTrackPiece(
                                game, ghostType, ghostNodeFirst, ghostNodeTarget
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

    private static RailNode getClosestNode(TrackPiece trackPiece, Vector3f point) {
        RailNode startNode = trackPiece.getStartNode();
        RailNode endNode = trackPiece.getEndNode();
        float distToStart = point.distanceSquared(startNode.getPosition());
        float distToEnd = point.distanceSquared(endNode.getPosition());

        RailNode targetNode;
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
