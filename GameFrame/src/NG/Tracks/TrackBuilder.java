package NG.Tracks;

import NG.Core.Game;
import NG.DataStructures.Generic.Pair;
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
                Logger.DEBUG.print("Clicked on position " + Vectors.toString(liftedPosition));

                if (firstNode != null) {
                    Logger.DEBUG.print("Placing track from " + Vectors.toString(firstNode.getPosition()) +
                            " to " + Vectors.toString(liftedPosition));

                    List<TrackPiece> tracks = RailTools.createNew(game, firstNode, liftedPosition, signalDistance);
                    TrackPiece lastTrack = processTracksReturnLast(game, tracks, signalDistance);
                    firstNode = lastTrack.getEndNode();

                } else if (firstPosition != null) {

                    List<TrackPiece> tracks = RailTools.createNew(game, type, firstPosition, liftedPosition, signalDistance);
                    TrackPiece lastTrack = processTracksReturnLast(game, tracks, signalDistance);
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
                    List<TrackPiece> tracks = RailTools.createNew(game, ghostNode, liftedPosition, signalDistance);
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

                    } else {
                        Pair<List<TrackPiece>, List<TrackPiece>> connection =
                                RailTools.createConnection(game, firstNode, targetNode, signalDistance);
                        processTracksReturnLast(game, connection.left, signalDistance);
                        processTracksReturnLast(game, connection.right, signalDistance);

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
                        RailNode ghostNodeFirst = new RailNode(firstNode, ghostType);

                        RailNode ghostNodeTarget = getIfExisting(game, trackPiece, fraction);

                        if (ghostNodeTarget == null) {
                            Vector3f dir = trackPiece.getDirectionFromFraction(fraction);
                            ghostNodeTarget = new RailNode(closestPoint, ghostType, dir);

                        } else {
                            // make it a ghost type
                            ghostNodeTarget = new RailNode(ghostNodeTarget, ghostType);
                        }

                        Pair<List<TrackPiece>, List<TrackPiece>> connection =
                                RailTools.createConnection(game, ghostNodeFirst, ghostNodeTarget, signalDistance);

                        ghostTracks.addAll(connection.left);
                        ghostTracks.addAll(connection.right);
                    }
                }
            default:
        }
    }

    /**
     * adds connections between all track nodes, and adds all tracks to the game state. Adds a signal to all nodes
     * strictly between the tracks, and returns the last node.
     */
    private static TrackPiece processTracksReturnLast(Game game, List<TrackPiece> tracks, float signalDistance) {
        int lastIndex = tracks.size() - 1;

        TrackPiece firstTrack = tracks.get(0);
        if (firstTrack.getLength() >= signalDistance + 1 / 128f) {
            firstTrack.getStartNode().addSignal(game);
        }

        for (int i = 0; i < lastIndex; i++) {
            TrackPiece track = tracks.get(i);
            NetworkNode.addConnection(track);
            game.state().addEntity(track);
            track.getEndNode().addSignal(game);
        }

        TrackPiece lastTrack = tracks.get(lastIndex);
        NetworkNode.addConnection(lastTrack);
        game.state().addEntity(lastTrack);
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
