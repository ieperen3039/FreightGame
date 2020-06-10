package NG.Tracks;

import NG.Core.Game;
import NG.Entities.Entity;
import NG.GUIMenu.Components.SToggleButton;
import NG.InputHandling.MouseTools.ToggleMouseTool;
import NG.Network.NetworkNode;
import NG.Network.RailNode;
import NG.Rendering.MatrixStack.SGL;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Geert van Ieperen created on 16-12-2018.
 */
public class TrackBuilder extends ToggleMouseTool {
    private static final float HEIGHT_ABOVE_GROUND = 0.2f;
    private final TrackType type;
    private RailNode firstNode;
    private Vector3fc firstPosition;

    private final TrackTypeGhost ghostType;
    private List<TrackPiece> ghostTracks = new CopyOnWriteArrayList<>();
    private float signalDistance = 10f;

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
                if (firstNode != null) {
                    Logger.DEBUG.print("Placing track from " + Vectors.toString(firstNode.getPosition()) +
                            " to " + Vectors.toString(liftedPosition));

                    List<TrackPiece> tracks = RailTools.createNew(game, firstNode, liftedPosition, signalDistance);
                    if (isValidTracks(tracks)) {
                        assert !tracks.isEmpty();
                        TrackPiece lastTrack = processTracksReturnLast(game, tracks);
                        assert lastTrack != null;
                        firstNode = lastTrack.getEndNode();
                    }

                } else if (firstPosition != null) {
                    List<TrackPiece> tracks = RailTools.createNew(game, type, firstPosition, liftedPosition, signalDistance);
                    assert !tracks.isEmpty();
                    // no validation, as this is a straight track piece
                    TrackPiece lastTrack = processTracksReturnLast(game, tracks);
                    assert lastTrack != null;
                    firstNode = lastTrack.getEndNode();

                    firstPosition = null;

                } else {
                    firstPosition = new Vector3f(liftedPosition);
                }

                return;
            case HOVER:
                clearGhostTracks();

                if (firstNode != null) {
                    RailNode ghostNode = new RailNode(firstNode, ghostType);
                    List<TrackPiece> tracks = RailTools.createNew(game, ghostNode, liftedPosition, Float.POSITIVE_INFINITY);
                    ghostTracks.addAll(tracks);

                } else if (firstPosition != null) {
                    Vector3f toNode = new Vector3f(liftedPosition).sub(firstPosition);
                    RailNode ghostNode = new RailNode(firstPosition, ghostType, toNode, null);
                    ghostTracks.add(new StraightTrack(game, ghostType, ghostNode, liftedPosition, true));
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
                    RailNode targetNode = getIfExisting(game, trackPiece, fraction);

                    if (targetNode == null) {
                        double gameTime = game.timer().getGameTime();
                        targetNode = RailTools.createSplit(game, trackPiece, fraction, gameTime);
                    }

                    if (firstNode == null) {
                        firstNode = targetNode;

                    } else if (firstNode == targetNode) {
                        return;

                    } else {
                        List<TrackPiece> connection =
                                RailTools.createConnection(game, firstNode, targetNode, signalDistance);

                        if (isValidTracks(connection)) {
                            processTracksReturnLast(game, connection);
                            firstNode = null;
                        }

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
                        RailNode ghostNodeFirst = new RailNode(firstNode, ghostType);
                        RailNode ghostNodeTarget = getIfExisting(game, trackPiece, fraction);

                        if (ghostNodeTarget == firstNode) return;

                        if (ghostNodeTarget == null) {
                            Vector3f dir = trackPiece.getDirectionFromFraction(fraction);
                            ghostNodeTarget = new RailNode(closestPoint, ghostType, dir);

                        } else {
                            // make it a ghost type
                            ghostNodeTarget = new RailNode(ghostNodeTarget, ghostType);
                        }

                        List<TrackPiece> connection =
                                RailTools.createConnection(game, ghostNodeFirst, ghostNodeTarget, Float.POSITIVE_INFINITY);

                        ghostTracks.addAll(connection);
                    }
                }
            default:
        }
    }

    private static boolean isValidTracks(List<TrackPiece> tracks) {
        for (TrackPiece track : tracks) {
            if (track instanceof CircleTrack) {
                float radius = ((CircleTrack) track).getRadius();
                if (radius < TrackType.MINIMUM_RADIUS) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * adds connections between all track nodes, and adds all tracks to the game state. Adds a signal to all nodes
     * strictly between the tracks, and returns the last node.
     */
    private static TrackPiece processTracksReturnLast(Game game, List<TrackPiece> tracks) {
        if (tracks.isEmpty()) return null;

        int lastIndex = tracks.size() - 1;

        for (int i = 0; i < lastIndex; i++) {
            TrackPiece track = tracks.get(i);
            NetworkNode.addConnection(track);
            game.state().addEntity(track);
        }

        TrackPiece lastTrack = tracks.get(lastIndex);
        NetworkNode.addConnection(lastTrack);
        game.state().addEntity(lastTrack);

        RailTools.invalidateSignals(lastTrack);
        return lastTrack;
    }

    public void clearGhostTracks() {
        double gameTime = game.timer().getGameTime();
        for (TrackPiece ghostTrack : ghostTracks) {
            ghostTrack.despawn(gameTime);
        }
        ghostTracks.clear();
    }

    @Override
    public void draw(SGL gl) {
        super.draw(gl);

        for (TrackPiece ghostTrack : ghostTracks) {
            ghostTrack.draw(gl);
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
