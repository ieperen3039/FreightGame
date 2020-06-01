package NG.Network;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.Entities.Entity;
import NG.InputHandling.MouseTools.AbstractMouseTool;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shapes.GenericShapes;
import NG.Resources.GeneratorResource;
import NG.Resources.Resource;
import NG.Tools.NetworkPathFinder;
import NG.Tools.Toolbox;
import NG.Tools.Vectors;
import NG.Tracks.TrackPiece;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;

import static NG.InputHandling.MouseTools.AbstractMouseTool.MouseAction.PRESS_ACTIVATE;

/**
 * @author Geert van Ieperen created on 26-5-2020.
 */
public class Signal extends AbstractGameObject implements Entity {
    /** number of vertices along the circle of the ring */
    private static final int RING_RESOLUTION = 128;
    /** height of middle above the floor of the track */
    private static final float INNER_RADIUS = 0.6f;
    /** size increase of the inner radius in each direction to avoid collision */
    private static final float MARGIN = 0.1f;
    /** how far each color ring is offset from the middle */
    private static final float COLOR_OFFSET = 0.1f;

    private final static Resource<Mesh> RING_MESH = new GeneratorResource<>(() ->
            GenericShapes.createRing(INNER_RADIUS + MARGIN, RING_RESOLUTION, COLOR_OFFSET / 2f), Mesh::dispose
    );

    private final RailNode hostNode;
    private final Vector3fc ringMiddle;

    /** whether to allow traffic in the direction of targetNode */
    private boolean inSameDirection;
    /** whether to allow traffic in the opposite direction of targetNode */
    private boolean inOppositeDirection;

    private double despawnTime = Double.POSITIVE_INFINITY;

    private Map<Signal, TrackPath> aSignals = new HashMap<>();
    private Map<Signal, TrackPath> bSignals = new HashMap<>();
    private boolean connectionsAreValid = false;

    /**
     * @param game                game instance
     * @param targetNode          node to attach to
     * @param inSameDirection     whether to allow traffic in the direction of targetNode
     * @param inOppositeDirection whether to allow traffic in the opposite direction of targetNode
     */
    public Signal(Game game, RailNode targetNode, boolean inSameDirection, boolean inOppositeDirection) {
        super(game);
        this.inOppositeDirection = inOppositeDirection;
        assert inSameDirection || inOppositeDirection : "Signal is a block";
        this.hostNode = targetNode;
        this.inSameDirection = inSameDirection;
        this.ringMiddle = new Vector3f(targetNode.getPosition()).add(0, 0, INNER_RADIUS - MARGIN);
    }

    @Override
    public void update() {
        // TODO color
    }

    @Override
    public void draw(SGL gl) {
        gl.pushMatrix();
        {
            gl.translate(ringMiddle);

            Vector3fc targetDirection = hostNode.getDirection();
            Vector3f cross = Vectors.newZVector().cross(targetDirection);
            gl.rotate(cross, Vectors.Z.angle(targetDirection));

            MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, getColor()));
            gl.render(RING_MESH.get(), this);

            if (inSameDirection) {
                gl.translate(0, 0, COLOR_OFFSET);
                MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.GREEN));
                gl.render(RING_MESH.get(), this);
                gl.translate(0, 0, -COLOR_OFFSET);
            }
            if (inOppositeDirection) {
                gl.translate(0, 0, -COLOR_OFFSET);
                MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.GREEN));
                gl.render(RING_MESH.get(), this);
                gl.translate(0, 0, COLOR_OFFSET);
            }
        }
        gl.popMatrix();
    }

    private Color4f getColor() {
        if (connectionsAreValid) {
            return Color4f.WHITE;
        } else {
            return Color4f.BLACK;
        }
    }

    @Override
    public void reactMouse(AbstractMouseTool.MouseAction action) {
        if (action.equals(PRESS_ACTIVATE)) {
            if (inSameDirection && inOppositeDirection) {
                inOppositeDirection = false;

            } else if (inSameDirection) {
                inOppositeDirection = true;
                inSameDirection = false;

            } else if (inOppositeDirection) {
                inSameDirection = true;

            } else {
                assert false : "Impassible signal " + this;
                inSameDirection = true;
            }
        }
    }

    @Override
    public double getDespawnTime() {
        return despawnTime;
    }

    @Override
    public void despawn(double gameTime) {
        despawnTime = gameTime;
    }

    @Override
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }

    public RailNode getNode() {
        return hostNode;
    }

    public void invalidateConnections() {
        connectionsAreValid = false;
    }

    public void validateConnections() {
        if (connectionsAreValid) return;

        NetworkNode networkNode = hostNode.getNetworkNode();
        TrackPath path = new TrackPath();

        for (NetworkNode.Direction entry : networkNode.getEntriesA()) {
            RailNode target = entry.trackPiece.getNot(hostNode);
            registerSignals(target, entry.trackPiece, aSignals, path);
        }

        for (NetworkNode.Direction entry : networkNode.getEntriesB()) {
            RailNode target = entry.trackPiece.getNot(hostNode);
            registerSignals(target, entry.trackPiece, bSignals, path);
        }

        connectionsAreValid = true;
    }

    private void registerSignals(
            RailNode node, TrackPiece sourceTrack, Map<Signal, TrackPath> signals, TrackPath pathToPrevious
    ) {
        pathToPrevious.path.addLast(sourceTrack);
        pathToPrevious.length += sourceTrack.getLength();

        if (node.hasSignal()) {
            signals.put(node.getSignal(), new TrackPath(pathToPrevious));

        } else {
            List<NetworkNode.Direction> directions = node.getNetworkNode().getNext(sourceTrack);
            for (NetworkNode.Direction entry : directions) {
                RailNode target = entry.trackPiece.getNot(node);
                registerSignals(target, entry.trackPiece, signals, pathToPrevious);
            }
        }

        pathToPrevious.path.removeLast();
        pathToPrevious.length -= sourceTrack.getLength();
    }

    private Deque<TrackPiece> reserve(TrackPath pathToBest) {
        for (TrackPiece piece : pathToBest.path) {
            piece.setOccupied(true);
        }

        return pathToBest.path;
    }

    public Deque<TrackPiece> reservePath(NetworkPosition target, TrackPiece previousTrack) {
        validateConnections();

        Map<Signal, TrackPath> signals;

        boolean trackIsInDirection = !hostNode.isInDirectionOf(previousTrack);
        if (trackIsInDirection) {
            if (!inOppositeDirection) {
                return getEmptyPath();
            }
            signals = aSignals;

        } else {
            if (!inSameDirection) {
                return getEmptyPath();
            }
            signals = bSignals;
        }

        if (signals.isEmpty()) return getEmptyPath();

        if (target == null) { // reserve random path
            TrackPath[] paths = signals.values().toArray(new TrackPath[0]);
            TrackPath chosenPath = paths[Toolbox.random.nextInt(paths.length)];
            return reserve(chosenPath);
        }

        TrackPath pathToBest = null;
        float leastDistance = Float.POSITIVE_INFINITY;

        for (Signal other : signals.keySet()) {
            boolean inDirection = other.bSignals.containsKey(this);
            NetworkPathFinder pathFinder = new NetworkPathFinder(other.hostNode, inDirection, target);

            NetworkPathFinder.Path pathSignalToTarget = pathFinder.call();
            TrackPath pathToSignal = signals.get(other);

            if (pathSignalToTarget == null) continue; // no path exists
            float totalDist = pathToSignal.length + pathSignalToTarget.getPathLength();

            if (totalDist < leastDistance) {
                pathToBest = pathToSignal;
            }
        }

        if (pathToBest == null) return getEmptyPath();

        return reserve(pathToBest);
    }

    private List<TrackPiece> convertPath(TrackPiece previousTrack, NetworkPathFinder.Path pathToBest) {
        List<TrackPiece> pathToSignal = new ArrayList<>();
        int nodeIndex = 0;

        RailNode node = hostNode;
        NetworkNode networkNode = hostNode.getNetworkNode();

        do {
            List<NetworkNode.Direction> options = networkNode.getNext(previousTrack);

            NetworkNode.Direction direction;
            if (options.size() == 1) {
                direction = options.get(0);
            } else {
                direction = networkNode.getEntryOfNetwork(networkNode);
            }

            TrackPiece trackPiece = direction.trackPiece;
            pathToSignal.add(trackPiece);

            node = trackPiece.getNot(node);
            if (node.getNetworkNode().equals(networkNode)) {
                networkNode = pathToBest.get(nodeIndex++);
            }
        } while (!node.hasSignal());
        return pathToSignal;
    }

    private static ArrayDeque<TrackPiece> getEmptyPath() {
        return new ArrayDeque<>();
    }

    private static class TrackPath implements java.io.Serializable {
        public final Deque<TrackPiece> path;
        public float length;

        public TrackPath(Deque<TrackPiece> path, float length) {
            this.path = path;
            this.length = length;
        }

        public TrackPath() {
            this.path = new ArrayDeque<>();
            this.length = 0;
        }

        public TrackPath(TrackPath other) {
            this.path = new ArrayDeque<>(other.path);
            this.length = other.length;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if ((o == null) || (getClass() != o.getClass())) return false;

            TrackPath other = (TrackPath) o;
            return Objects.equals(path, other.path) && length == other.length;
        }

        @Override
        public int hashCode() {
            int leftCode = (path != null) ? path.hashCode() : 0;
            int rightCode = Float.floatToIntBits(length);
            return (31 * leftCode) + rightCode;
        }
    }
}
