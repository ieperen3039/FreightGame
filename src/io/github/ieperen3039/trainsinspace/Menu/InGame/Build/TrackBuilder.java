package io.github.ieperen3039.trainsinspace.Menu.InGame.Build;

import io.github.ieperen3039.ngn.DataStructures.Generic.Color4f;
import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTarget;
import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTool;
import io.github.ieperen3039.ngn.Rendering.MatrixStack.SGL;
import io.github.ieperen3039.ngn.Rendering.Shaders.ClickShader;
import io.github.ieperen3039.ngn.Tools.Logger;
import io.github.ieperen3039.ngn.Tools.TickTime;
import io.github.ieperen3039.ngn.UserInterface.Components.SFrame;
import io.github.ieperen3039.ngn.UserInterface.Components.SToggleButton;
import io.github.ieperen3039.trainsinspace.Core.Coloring;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Entities.GameEntity;
import io.github.ieperen3039.trainsinspace.InputHandling.InputMapping;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.MouseTargetEntity;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.MouseTargetMap;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.MouseToolVisuals;
import io.github.ieperen3039.trainsinspace.Network.NetworkNode;
import io.github.ieperen3039.trainsinspace.Network.RailNode;
import io.github.ieperen3039.trainsinspace.Settings.Settings;
import io.github.ieperen3039.trainsinspace.Tracks.*;
import org.joml.Intersectionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.primitives.Rayf;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Geert van Ieperen created on 16-12-2018.
 */
public class TrackBuilder implements MouseTool {
    private static final float ENDNODE_SELECTION_MARGIN = 0.5f;
    private final Game game;
    protected final Runnable deactivation;
    private final TrackType type;
    private RailNode firstNode;
    private Vector3fc firstPosition;

    private final TrackTypeGhost ghostType;
    private AtomicReference<List<TrackPiece>> ghostTracks = new AtomicReference<>(Collections.emptyList());
    // TODO UI option to change signal distance
    private float signalDistance = 10f;
    private Coloring.Marking mark = new Coloring.Marking();
    private Vector3f cursorPosition;
    private float cursorBaseHeight;
    private SFrame selector;

    /**
     * this mousetool lets the player place a track by clicking on the map
     *
     * @param game   the game instance
     * @param type   the type of track to place
     * @param source button that activated this
     */
    public TrackBuilder(Game game, TrackType type, SToggleButton source) {
        this.game = game;
        this.deactivation = () -> source.setActive(false);
        this.type = type;
        this.ghostType = new TrackTypeGhost(type);
    }

    @Override
    public HandleResult onPress(int button, MouseTarget target, int xSc, int ySc) {
        if (button == InputMapping.DeactivateMouseTool.getButton()) {
            return HandleResult.DEACTIVATE;
        }

        if (button != InputMapping.UseMouseTool.getButton()) return HandleResult.CONTINUE;

        mark.invalidate();
        swapGhostTracks(Collections.emptyList());

        if (target instanceof MouseTargetMap) {
            if (firstNode != null) {
                List<TrackPiece> tracks = RailTools.createNew(game, firstNode, cursorPosition, signalDistance);
                if (isValidTracks(tracks)) {
                    assert !tracks.isEmpty();
                    TrackPiece lastTrack = processTracksReturnLast(game, tracks);
                    assert lastTrack != null;
                    firstNode = lastTrack.getEndNode();
                }

            } else if (firstPosition != null) {
                List<TrackPiece> tracks = RailTools.createNew(
                        game, type, firstPosition, cursorPosition, signalDistance);
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

            return HandleResult.CAPTURE;
        } else if (target instanceof MouseTargetEntity entity) {
            Logger.DEBUG.print("Clicked on entity " + entity.target());
            if (entity.target() instanceof TrackPiece trackPiece) {
                assert trackPiece.isValid() : trackPiece;
                boolean doContinueBuilding = true;

                Rayf rayf = game.camera().unproject(xSc, ySc, game.getViewPort());
                Vector3f origin = new Vector3f(rayf.oX, rayf.oY, rayf.oZ);
                Vector3f direction = new Vector3f(rayf.dX, rayf.dY, rayf.dZ);

                float fraction = getFraction(trackPiece, origin, direction);
                RailNode targetNode = getIfParallel(origin, direction, trackPiece, fraction, game);

                if (targetNode == null) {
                    doContinueBuilding = false;
                    targetNode = getIfExisting(game, trackPiece, fraction);
                }

                if (targetNode == null) {
                    double gameTime = game.timer().getTime();
                    targetNode = RailTools.createSplit(game, trackPiece, fraction, gameTime);

                }

                if (firstNode == null) {
                    if (firstPosition != null) {
                        List<TrackPiece> tracks = RailTools.createNew(
                                game, targetNode, firstPosition, signalDistance);

                        if (isValidTracks(tracks)) {
                            processTracksReturnLast(game, tracks);
                            // we created this track from node to position - the inverse of what the player did
                            firstNode = tracks.get(0).getStartNode();
                            firstPosition = null;
                        }

                    } else {
                        firstNode = targetNode;
                    }

                } else if (firstNode == targetNode) {
                    return HandleResult.CAPTURE;

                } else {
                    List<TrackPiece> connection =
                            RailTools.createConnection(game, firstNode, targetNode, signalDistance);

                    if (isValidTracks(connection)) {
                        processTracksReturnLast(game, connection);
                    }

                    firstNode = doContinueBuilding ? targetNode : null;
                }
            }

            return HandleResult.CAPTURE;
        }

        return HandleResult.CONTINUE;
    }

    @Override
    public HandleResult onHover(MouseTarget target, int xDelta, int yDelta, float xSc, float ySc) {
        Settings settings = game.settings();

        if (target instanceof MouseTargetMap map) {
            if (game.keyControl().isShiftPressed()) {
                Rayf rayf = game.camera().unproject((int) xSc, (int) ySc, game.getViewPort());

                // find the elevation of the new point
                Vector3f endPoint = new Vector3f(rayf.dX, rayf.dY, rayf.dZ)
                        .mul(settings.Z_FAR - settings.Z_NEAR)
                        .add(rayf.oX, rayf.oY, rayf.oZ);
                Vector3f result = new Vector3f();

                Intersectionf.findClosestPointsLineSegments(
                        cursorPosition.x, cursorPosition.y, cursorBaseHeight,
                        cursorPosition.x, cursorPosition.y, cursorBaseHeight + 100,
                        rayf.oX, rayf.oY, rayf.oZ,
                        endPoint.x, endPoint.y, endPoint.z,
                        result, new Vector3f()
                );

//            Logger.WARN.print(cursorPosition, result);
                cursorPosition = result;

            } else {
                cursorPosition = new Vector3f(map.position()).add(0, 0, settings.TRACK_HEIGHT_ABOVE_GROUND);
                cursorBaseHeight = cursorPosition.z;
            }

            // remake ghost tracks
            mark.invalidate();

            if (firstNode != null) {
                RailNode ghostNode = new RailNode(firstNode, ghostType);
                List<TrackPiece> tracks = RailTools.createNew(game, ghostNode, cursorPosition, Float.POSITIVE_INFINITY);
                checkCollisions(tracks);
                swapGhostTracks(tracks);

            } else if (firstPosition != null) {
                Vector3f toNode = new Vector3f(cursorPosition).sub(firstPosition);
                RailNode ghostNode = new RailNode(game, firstPosition, ghostType, toNode, null);
                StraightTrack track = new StraightTrack(game, ghostType, ghostNode, cursorPosition, true);
                List<TrackPiece> tracks = Collections.singletonList(track);

                checkCollisions(tracks);
                swapGhostTracks(tracks);
            }
            return HandleResult.STOP;

        } else if (target instanceof MouseTargetEntity entity) {
            if (entity.target() instanceof TrackPiece trackPiece) {
                Rayf rayf = game.camera().unproject((int) xSc, (int) ySc, game.getViewPort());
                Vector3f origin = new Vector3f(rayf.oX, rayf.oY, rayf.oZ);
                Vector3f direction = new Vector3f(rayf.dX, rayf.dY, rayf.dZ);
                float fraction = getFraction(trackPiece, origin, direction);

                mark.invalidate();

                if (firstNode == null && firstPosition == null) {
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
                        if (ghostNodeTarget == firstNode) return HandleResult.STOP;
                    }

                    if (ghostNodeTarget == null) {
                        Vector3f pos = trackPiece.getPositionFromFraction(fraction);
                        Vector3f dir = trackPiece.getDirectionFromFraction(fraction);
                        ghostNodeTarget = new RailNode(game, pos, ghostType, dir);

                    } else {
                        // make it a ghost type
                        ghostNodeTarget = new RailNode(ghostNodeTarget, ghostType);
                    }

                    List<TrackPiece> tracks;
                    if (firstPosition != null) {
                        tracks = RailTools.createNew(game, ghostNodeTarget, firstPosition, Float.POSITIVE_INFINITY);

                    } else {
                        RailNode ghostNodeFirst = new RailNode(firstNode, ghostType);
                        tracks = RailTools.createConnection(game, ghostNodeFirst, ghostNodeTarget, Float.POSITIVE_INFINITY);
                    }

                    checkCollisions(tracks);
                    swapGhostTracks(tracks);
                }
            }
            return HandleResult.STOP;
        }

        return HandleResult.CONTINUE;
    }

    private boolean isValidTracks(List<TrackPiece> tracks) {
        if (tracks.isEmpty()) return false;

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
        mark.invalidate();
        mark = new Coloring.Marking(Color4f.RED, Coloring.Priority.MAXIMUM);
        boolean hasCollisions = false;

        for (TrackPiece track : tracks) {
            Collection<GameEntity> collisions = game.state().getCollisions(track);
            collisions.removeIf(other -> other instanceof TrackPiece && isAdjacent(track, (TrackPiece) other));

            if (!collisions.isEmpty()) {
                track.setMarking(mark);
                hasCollisions = true;

                for (GameEntity other : collisions) {
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

    public void swapGhostTracks(List<TrackPiece> newGhostTracks) {
        double gameTime = game.timer().getTime();
        for (TrackPiece ghostTrack : ghostTracks.get()) {
            ghostTrack.despawn(gameTime);
        }
        ghostTracks.set(newGhostTracks);
    }

    @Override
    public void onActivate() {
        selector = new SFrame("Track builder");
        game.state().addEntity(new MouseToolVisuals(selector) {
            @Override
            public void draw(SGL gl, TickTime renderTime) {
                if (gl.getShader() instanceof ClickShader) return;

                for (TrackPiece ghostTrack : ghostTracks.get()) {
                    ghostTrack.draw(gl, renderTime);
                }
            }
        });
    }

    @Override
    public void onDeactivate() {
        swapGhostTracks(Collections.emptyList());
        deactivation.run();
        mark.invalidate();
        selector.dispose();
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

        if (xyDistance < game.settings().CLICK_BOX_WIDTH / 4) return null;

        Vector3f vecOut = new Vector3f(rayPoint).sub(trackPoint).normalize(game.settings().CLICK_BOX_WIDTH / 2);
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
