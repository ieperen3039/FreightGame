package NG.Menu.InGame.Build;

import NG.Core.Coloring;
import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.Entities.Entity;
import NG.GUIMenu.Components.SToggleButton;
import NG.InputHandling.MouseTool.AbstractMouseTool;
import NG.Network.NetworkNode;
import NG.Network.RailNode;
import NG.Rendering.MatrixStack.SGL;
import NG.Settings.Settings;
import NG.Tools.Logger;
import NG.Tracks.*;
import org.joml.Intersectionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Geert van Ieperen created on 16-12-2018.
 */
public class TrackBuilder extends AbstractMouseTool {
    private static final float ENDNODE_SELECTION_MARGIN = 0.5f;
    protected final Runnable deactivation;
    private final TrackType type;
    private RailNode firstNode;
    private Vector3fc firstPosition;

    private final TrackTypeGhost ghostType;
    private List<TrackPiece> ghostTracks = new CopyOnWriteArrayList<>();
    private float signalDistance = 10f;
    private Coloring.Marking mark = new Coloring.Marking();
    private Vector3f cursorPosition;
    private float cursorBaseHaight;

    /**
     * this mousetool lets the player place a track by clicking on the map
     * @param game   the game instance
     * @param type   the type of track to place
     * @param source
     */
    public TrackBuilder(Game game, TrackType type, SToggleButton source) {
        super(game);
        this.deactivation = () -> source.setActive(false);
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
        mark.invalidate();

        if (game.keyControl().isShiftPressed()) {
            // find the elevation of the new point
            Vector3f endPoint = new Vector3f(direction).mul(Settings.Z_FAR - Settings.Z_NEAR).add(origin);
            Vector3f result = new Vector3f();

            Intersectionf.findClosestPointsLineSegments(
                    cursorPosition.x, cursorPosition.y, cursorBaseHaight,
                    cursorPosition.x, cursorPosition.y, cursorBaseHaight + 100,
                    origin.x(), origin.y(), origin.z(),
                    endPoint.x, endPoint.y, endPoint.z,
                    result, new Vector3f()
            );

//            Logger.WARN.print(cursorPosition, result);
            cursorPosition = result;

        } else {
            cursorPosition = new Vector3f(position).add(0, 0, Settings.TRACK_HEIGHT_ABOVE_GROUND);
            cursorBaseHaight = cursorPosition.z;
        }

        switch (getMouseAction()) {
            case PRESS_ACTIVATE -> {
                if (firstNode != null) {
                    List<TrackPiece> tracks = RailTools.createNew(game, firstNode, cursorPosition, signalDistance);
                    if (isValidTracks(tracks)) {
                        assert !tracks.isEmpty();
                        TrackPiece lastTrack = processTracksReturnLast(game, tracks);
                        assert lastTrack != null;
                        firstNode = lastTrack.getEndNode();
                    }

                } else if (firstPosition != null) {
                    List<TrackPiece> tracks = RailTools.createNew(game, type, firstPosition, cursorPosition, signalDistance);
                    if (isValidTracks(tracks)) {
                        assert !tracks.isEmpty();
                        // no validation, as this is a straight track piece
                        TrackPiece lastTrack = processTracksReturnLast(game, tracks);
                        assert lastTrack != null;
                        firstNode = lastTrack.getEndNode();

                        firstPosition = null;
                    }

                } else {
                    firstPosition = new Vector3f(cursorPosition);
                }
            }
            case HOVER -> {
                clearGhostTracks();
                if (firstNode != null) {
                    RailNode ghostNode = new RailNode(firstNode, ghostType);
                    List<TrackPiece> tracks = RailTools.createNew(game, ghostNode, cursorPosition, Float.POSITIVE_INFINITY);
                    checkCollisions(tracks);

                    ghostTracks.addAll(tracks);

                } else if (firstPosition != null) {
                    Vector3f toNode = new Vector3f(cursorPosition).sub(firstPosition);
                    RailNode ghostNode = new RailNode(game, firstPosition, ghostType, toNode, null);
                    StraightTrack track = new StraightTrack(game, ghostType, ghostNode, cursorPosition, true);
                    checkCollisions(Collections.singletonList(track));

                    ghostTracks.add(track);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void apply(Entity entity, Vector3fc origin, Vector3fc direction) {
        mark.invalidate();

        switch (getMouseAction()) {
            case PRESS_ACTIVATE -> {
                Logger.DEBUG.print("Clicked on entity " + entity);
                if (entity instanceof TrackPiece trackPiece) {
                    assert trackPiece.isValid() : trackPiece;
                    boolean doContinueBuilding = true;

                    float fraction = getFraction(trackPiece, origin, direction);
                    RailNode targetNode = getIfParallel(origin, direction, trackPiece, fraction, game);

                    if (targetNode == null) {
                        doContinueBuilding = false;
                        targetNode = getIfExisting(game, trackPiece, fraction);
                    }

                    if (targetNode == null) {
                        double gameTime = game.timer().getGameTime();
                        targetNode = RailTools.createSplit(game, trackPiece, fraction, gameTime);

                    }

                    if (firstNode == null) {
                        firstNode = targetNode;

                    } else if (firstNode == targetNode) {
                        break;

                    } else {
                        List<TrackPiece> connection =
                                RailTools.createConnection(game, firstNode, targetNode, signalDistance);

                        if (isValidTracks(connection)) {
                            processTracksReturnLast(game, connection);
                        }

                        firstNode = doContinueBuilding ? targetNode : null;
                    }
                }
            }
            case HOVER -> {
                clearGhostTracks();
                if (entity instanceof TrackPiece trackPiece) {
                    float fraction = getFraction(trackPiece, origin, direction);

                    if (firstNode == null) {
                        mark = new Coloring.Marking(Color4f.BLUE, Coloring.Priority.MOUSE_HOVER);

                        if (fraction == 0 || fraction == 1) {
                            RailNode node = (fraction == 0) ? trackPiece.getStartNode() : trackPiece.getEndNode();

                            // TODO mark node itself
                            for (NetworkNode.Direction entry : node.getNetworkNode().getAllEntries()) {
                                entry.trackPiece.setMarking(mark);
                            }

                        } else {
                            trackPiece.setMarking(mark);
                        }

                    } else {
                        RailNode ghostNodeTarget = getIfParallel(origin, direction, trackPiece, fraction, game);

                        if (ghostNodeTarget == null) {
                            ghostNodeTarget = getIfExisting(game, trackPiece, fraction);
                            if (ghostNodeTarget == firstNode) return;
                        }

                        if (ghostNodeTarget == null) {
                            Vector3f pos = trackPiece.getPositionFromFraction(fraction);
                            Vector3f dir = trackPiece.getDirectionFromFraction(fraction);
                            ghostNodeTarget = new RailNode(game, pos, ghostType, dir);

                        } else {
                            // make it a ghost type
                            ghostNodeTarget = new RailNode(ghostNodeTarget, ghostType);
                        }

                        RailNode ghostNodeFirst = new RailNode(firstNode, ghostType);
                        List<TrackPiece> connection =
                                RailTools.createConnection(game, ghostNodeFirst, ghostNodeTarget, Float.POSITIVE_INFINITY);

                        checkCollisions(connection);

                        ghostTracks.addAll(connection);
                    }
                }
            }
            default -> {
            }
        }
    }

    private boolean isValidTracks(List<TrackPiece> tracks) {
//        if (checkCollisions(tracks)) return false;

        for (TrackPiece track : tracks) {
            if (track instanceof CircleTrack) {
                float radius = ((CircleTrack) track).getRadius();
                if (type.getMaximumSpeed(radius) == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean checkCollisions(List<TrackPiece> tracks) {
        mark = new Coloring.Marking(Color4f.RED, Coloring.Priority.MAXIMUM);
        boolean hasCollisions = false;

        for (TrackPiece track : tracks) {
            Collection<Entity> collisions = game.state().getCollisions(track);
            collisions.removeIf(other -> other instanceof TrackPiece && isAdjacent(track, (TrackPiece) other));

            if (!collisions.isEmpty()) {
                track.setMarking(mark);
                hasCollisions = true;

                for (Entity other : collisions) {
                    other.setMarking(mark);
                }
            }
        }

        return hasCollisions;
    }

    private static boolean isAdjacent(TrackPiece a, TrackPiece b) {
        Vector3fc aStart = a.getStartNode().getPosition();
        Vector3fc aEnd = a.getEndNode().getPosition();
        Vector3fc bStart = b.getStartNode().getPosition();
        Vector3fc bEnd = b.getEndNode().getPosition();
        return aStart.equals(bStart) || aStart.equals(bEnd) || aEnd.equals(bStart) || aEnd.equals(bEnd);
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

        RailTools.invalidateSignals(tracks.get(0));

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
        deactivation.run();
        mark.invalidate();
    }

    @Override
    public String toString() {
        return "Track Builder (" + type + ")";
    }

    protected static float getFraction(TrackPiece trackPiece, Vector3fc origin, Vector3fc direction) {
        float fraction = trackPiece.getFractionOfClosest(origin, direction);
        if (fraction < 0) {
            fraction = 0;
        } else if (fraction > 1) {
            fraction = 1;
        }
        return fraction;
    }

    protected static RailNode getIfParallel(
            Vector3fc origin, Vector3fc direction, TrackPiece trackPiece, float fraction, Game game
    ) {
        Vector3f trackPoint = trackPiece.getPositionFromFraction(fraction);
        float t = (trackPoint.z - origin.z()) / direction.z();
        Vector3f rayPoint = new Vector3f(direction).mul(t).add(origin);
        float xyDistance = rayPoint.distance(trackPoint);

        if (xyDistance < Settings.CLICK_BOX_WIDTH / 4) return null;

        Vector3f vecOut = new Vector3f(rayPoint).sub(trackPoint).normalize(Settings.CLICK_BOX_WIDTH / 2);
        Vector3f nodePoint = vecOut.add(trackPoint);
        Vector3f trackDirection = trackPiece.getDirectionFromFraction(fraction);

        return new RailNode(game, nodePoint, trackPiece.getType(), trackDirection);
    }

    protected static RailNode getIfExisting(Game game, TrackPiece trackPiece, float fraction) {
        if (game.keyControl().isControlPressed() || trackPiece.isStatic()) {
            if (fraction < 0.5f) {
                return trackPiece.getStartNode();
            } else {
                return trackPiece.getEndNode();
            }
        } else if (fraction * trackPiece.getLength() < ENDNODE_SELECTION_MARGIN) {
            return trackPiece.getStartNode();

        } else if ((1 - fraction) * trackPiece.getLength() < ENDNODE_SELECTION_MARGIN) {
            return trackPiece.getEndNode();

        } else {
            return null;
        }
    }
}
