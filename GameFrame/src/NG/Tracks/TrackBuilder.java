package NG.Tracks;

import NG.Core.Game;
import NG.DataStructures.Generic.Pair;
import NG.Entities.Entity;
import NG.GUIMenu.Components.SToggleButton;
import NG.InputHandling.MouseTools.ToggleMouseTool;
import NG.Network.RailNode;
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
    private static final float HEIGHT_ABOVE_GROUND = 0.2f;
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
    public void apply(Vector3fc position, Vector3fc origin, Vector3fc direction) {
        Vector3f liftedPosition = new Vector3f(position).add(0, 0, HEIGHT_ABOVE_GROUND);
        switch (getMouseAction()) {
            case PRESS_ACTIVATE:
                Logger.DEBUG.print("Clicked on position " + Vectors.toString(liftedPosition));

                if (firstNode != null) {
                    Logger.DEBUG.print("Placing track from " + Vectors.toString(firstNode.getPosition()) +
                            " to " + Vectors.toString(liftedPosition));

                    firstNode = RailTools.createNew(game, firstNode, liftedPosition);

                } else if (firstPosition != null) {
                    TrackPiece trackConnection = RailTools.createNew(game, type, firstPosition, liftedPosition);
                    firstPosition = null;
                    firstNode = trackConnection.getEndNode();

                } else {
                    firstPosition = new Vector3f(liftedPosition);
                }

                return;
            case HOVER:
                clearGhostTracks();

                if (firstNode != null) {
                    Vector3fc dir = firstNode.getDirectionTo(liftedPosition);
                    RailNode ghostNode = new RailNode(firstNode.getPosition(), ghostType, dir, null);
                    ghostTrack1 = RailTools.getTrackPiece(
                            game, ghostType, ghostNode, dir, liftedPosition
                    );

                } else if (firstPosition != null) {
                    Vector3f toNode = new Vector3f(liftedPosition).sub(firstPosition);
                    RailNode ghostNode = new RailNode(firstPosition, ghostType, toNode, null);
                    ghostTrack1 = new StraightTrack(game, ghostType, ghostNode, liftedPosition, true);
                }
            default:
        }
    }

    @Override
    public void apply(Entity entity, Vector3fc origin, Vector3fc direction) {
        switch (getMouseAction()) {
            case PRESS_ACTIVATE:
                Logger.DEBUG.print("Clicked on entity " + entity);
                if (entity instanceof TrackPiece) {
                    TrackPiece trackPiece = (TrackPiece) entity;
                    assert trackPiece.isValid() : trackPiece;

                    float fraction = getFraction(trackPiece, origin, direction);
                    RailNode targetNode = getOrCreateNode(trackPiece, fraction, game);

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
                    float fraction = getFraction(trackPiece, origin, direction);

                    Vector3f closestPoint = trackPiece.getPositionFromFraction(fraction);

                    if (firstNode == null) {
                        // mark

                    } else {
                        Vector3fc fDirection = firstNode.getDirectionTo(closestPoint);
                        RailNode ghostNodeFirst = new RailNode(firstNode.getPosition(), ghostType, fDirection);

                        RailNode ghostNodeTarget;
                        if (game.keyControl().isControlPressed() || trackPiece.isStatic()) {
                            if (fraction < 0.5f) {
                                ghostNodeTarget = trackPiece.getStartNode();
                            } else {
                                ghostNodeTarget = trackPiece.getEndNode();
                            }

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
        double gameTime = game.timer().getGameTime();
        if (ghostTrack1 != null) {
            ghostTrack1.despawn(gameTime);
            ghostTrack1 = null;
        }
        if (ghostTrack2 != null) {
            ghostTrack2.despawn(gameTime);
            ghostTrack2 = null;
        }
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
