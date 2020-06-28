package NG.Network;

import NG.Core.AbstractGameObject;
import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.Pair;
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
import java.util.function.Function;

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
    private static final float TRACK_OCCUPATION_PENALTY = 10f;

    /** the node where this signals is placed on */
    private final RailNode hostNode;
    private final Vector3fc ringMiddle;

    /** whether to allow traffic in the direction of targetNode */
    private boolean inSameDirection;
    /** whether to allow traffic in the opposite direction of targetNode */
    private boolean inOppositeDirection;

    private double despawnTime = Double.POSITIVE_INFINITY;
    private Marking marking = new Marking();

    /**
     * @param game                game instance
     * @param targetNode          node to attach to
     * @param inSameDirection     whether to allow traffic in the direction of targetNode
     * @param inOppositeDirection whether to allow traffic in the opposite direction of targetNode
     */
    public Signal(Game game, RailNode targetNode, boolean inSameDirection, boolean inOppositeDirection) {
        super(game);
        assert inSameDirection || inOppositeDirection : "Signal is a block";
        this.hostNode = targetNode;
        this.inOppositeDirection = inOppositeDirection;
        this.inSameDirection = inSameDirection;
        this.ringMiddle = new Vector3f(targetNode.getPosition()).add(0, 0, INNER_RADIUS - MARGIN);
    }

    @Override
    public void update() {
        // TODO maybe color

        if (hostNode.isUnconnected()) {
            despawn(game.timer().getGameTime());
        }
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

            if (!inSameDirection) {
                gl.translate(0, 0, COLOR_OFFSET);
                MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.RED));
                gl.render(RING_MESH.get(), this);
                gl.translate(0, 0, -COLOR_OFFSET);

            } else if (!inOppositeDirection) {
                gl.translate(0, 0, -COLOR_OFFSET);
                MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.RED));
                gl.render(RING_MESH.get(), this);
                gl.translate(0, 0, COLOR_OFFSET);
            }
        }
        gl.popMatrix();
    }

    private Color4f getColor() {
        if (hostNode.isUnconnected()) {
            return Color4f.CYAN;
        }

        return marking.isValid() ? marking.color : Color4f.WHITE;
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
    public void setMarking(Marking marking) {
        this.marking = marking;
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

    /**
     * calculates the path of the given controller, positioned on hostNode, to the targets as given by the controller.
     * @param targets         the target supplier, which should give a next target given a scheduling depth.
     * @param inSameDirection whether the path should be calculated in the direction of the railnode. false iff {@link
     *                        #getNode()}{@link RailNode#isInDirectionOf(TrackPiece) .isInDirectionOf(previousTrack)}
     * @param targetStart     the initial scheduling depth
     * @return a path from hostNode, via any possible targets, to a signal. Returns null if no such path exists.
     * Guaranteed is {@code path == null || path.getLast().hasSignal()}
     */
    private TrackPath getPath(
            Function<Integer, NetworkPosition> targets, boolean inSameDirection, int targetStart
    ) {
        HashMap<Signal, TrackPath> signals = new HashMap<>();
        HashMap<NetworkNode, TrackPath> nodes = new HashMap<>();

        NetworkNode networkNode = hostNode.getNetworkNode();
        List<NetworkNode.Direction> entries = inSameDirection ? networkNode.getEntriesA() : networkNode.getEntriesB();

        // TODO trivial section shortcut ?
        int depth = targetStart;

        for (NetworkNode.Direction entry : entries) {
            TrackPiece trackPiece = entry.trackPiece;
            collectPaths(trackPiece.getNot(hostNode), trackPiece, signals, nodes, new TrackPath(trackPiece));
        }
        NetworkPosition target = targets.apply(depth++);

        if (signals.isEmpty()) {
            return null;

        } else if (target == null) { // reserve random path
            TrackPath[] paths = signals.values().stream()
                    .filter(path -> !path.isOccupied)
                    .toArray(TrackPath[]::new);

            if (paths.length == 0) return null;
            return paths[Toolbox.random.nextInt(paths.length)];
        }

        // target != null

        TrackPath pathViaNodes = new TrackPath();

        do {
            NetworkNode targetOfBest = null;
            TrackPath bestPath = null;
            float lengthOfBest = Float.POSITIVE_INFINITY;

            // get best direct path to target node
            for (Pair<NetworkNode, Boolean> targetNode : target.getNodes()) {
                TrackPath pathToNode = nodes.get(targetNode.left);

                if (pathToNode != null) {
                    TrackPiece arrivalTrack = pathToNode.path.getLast();
                    if (targetNode.left.isInDirectionOf(arrivalTrack) == targetNode.right) {
                        float adjLength = pathToNode.adjLength();
                        if (adjLength < lengthOfBest) {
                            bestPath = pathToNode;
                            lengthOfBest = adjLength;
                            targetOfBest = targetNode.left;
                        }
                    }
                }
            }

            if (bestPath == null) break; // no direct path exists
//            assert targetOfBest != null;

            assert !bestPath.path.isEmpty() : nodes; // usually caused by (targets.apply(depth) == targets.apply(depth + 1))
            pathViaNodes.append(bestPath);

            TrackPiece last = bestPath.path.getLast();
            RailNode node = last.get(targetOfBest);
            if (node.hasSignal()) return pathViaNodes;

            // recalculate paths as if starting from this node
            signals.clear();
            nodes.clear();
            collectPaths(node, last, signals, nodes, new TrackPath());

            NetworkPosition newTarget = targets.apply(depth++);
            if (newTarget == target) break;
            target = newTarget;
        } while (true);

        TrackPath pathToBest = null;
        float leastDistance = Float.POSITIVE_INFINITY;

        for (Signal other : signals.keySet()) {
            TrackPath pathToSignal = signals.get(other);

            float totalDist = getPathToTargetLength(target, other, pathToSignal);

            if (totalDist < leastDistance) {
                leastDistance = totalDist;
                pathToBest = pathToSignal;
            }
        }

        if (pathToBest == null) return null;
        return pathViaNodes.append(pathToBest);
    }

    private float getPathToTargetLength(
            NetworkPosition target, Signal other, TrackPath pathToSignal
    ) {
        TrackPiece lastTrack = pathToSignal.path.getLast();
        NetworkNode otherNetwork = other.hostNode.getNetworkNode();
        boolean inDirection = !other.hostNode.isInDirectionOf(lastTrack);

        NetworkNode startNode;
        float signalToNetworkLength;

        if (otherNetwork.isNetworkCritical()) {
            startNode = otherNetwork;
            signalToNetworkLength = 0;

        } else {
            List<NetworkNode.Direction> directions = inDirection ? otherNetwork.getEntriesA() : otherNetwork.getEntriesB();
            assert directions.size() == 1; // !networkNode.isNetworkCritical() => !networkNode.isSwitch()

            NetworkNode.Direction direction = directions.get(0);
            startNode = direction.network;
            signalToNetworkLength = direction.distanceToNetworkNode;
            inDirection = direction.networkIsInDirection;
        }

        NetworkPathFinder pathFinder = new NetworkPathFinder(startNode, inDirection, target);
        NetworkPathFinder.Path pathNetworkToTarget = pathFinder.call();

        if (pathNetworkToTarget == null) {
            return Float.POSITIVE_INFINITY; // no path exists
        }

        return pathToSignal.adjLength() + signalToNetworkLength + pathNetworkToTarget.getPathLength();
    }

    /**
     * collects the paths to all signals and nodes in the given direction
     * @param node        the node to analyse
     * @param sourceTrack the track where we are coming from, which is connected to node
     * @param signals     the map [signals -> the shortest path to this signal]
     * @param nodes       the map [nodes -> the shortest path to this node] for all nodes
     * @param pathToNode  path to node
     */
    private void collectPaths(
            RailNode node, TrackPiece sourceTrack, Map<Signal, TrackPath> signals,
            Map<NetworkNode, TrackPath> nodes, TrackPath pathToNode
    ) {
        NetworkNode networkNode = node.getNetworkNode();

        if (nodes != null && networkNode.isNetworkCritical()) {
            TrackPath original = nodes.get(networkNode);
            if (original == null || original.adjLength() > pathToNode.adjLength()) {
                nodes.put(networkNode, new TrackPath(pathToNode));
            }
        }

        if (node.hasSignal()) {
            Signal sig = node.getSignal();
            TrackPath original = signals.get(sig);
            if (original == null || original.adjLength() > pathToNode.adjLength()) {
                signals.put(sig, new TrackPath(pathToNode));
            }

        } else {
            List<NetworkNode.Direction> directions = networkNode.getNext(sourceTrack);
            boolean wasOccupied = pathToNode.isOccupied;

            for (NetworkNode.Direction entry : directions) {
                TrackPiece trackPiece = entry.trackPiece;


                // loop without signals: prevent infinite loops
                if (!pathToNode.path.isEmpty() && pathToNode.path.getFirst() == trackPiece) return;

                float trackPieceLength = trackPiece.getLength();
                pathToNode.path.addLast(trackPiece);
                pathToNode.length += trackPieceLength;
                if (trackPiece.isOccupied()) pathToNode.isOccupied = true;

                collectPaths(trackPiece.getNot(node), trackPiece, signals, nodes, pathToNode);

                pathToNode.path.removeLast();
                pathToNode.length -= trackPieceLength;
                pathToNode.isOccupied = wasOccupied;
            }
        }

    }

    /**
     * computes a path p to another signal such that p.getFirst() is the first track on the path and p.getLast() is the
     * last track on the path. This path is a section of the shortest available path towards target. Each element is
     * reserved, and should be freed whenever it is passed, as {@link TrackPiece#setOccupied(boolean)
     * track.setOccupied(false)}.
     * <p>
     * If the path is not empty, then it starts and ends with a signal, with no signal inbetween.
     * @param trackIsInDirection whether the starting direction is the same as the direction of {@link #getNode()}
     * @param targetFunction
     * @return a path from here to the next signal on the shortest available path toward target.
     */
    public Deque<TrackPiece> reservePath(
            boolean trackIsInDirection, Function<Integer, NetworkPosition> targetFunction
    ) {
        if (trackIsInDirection) {
            if (!inOppositeDirection) {
                return getEmptyPath();
            }

        } else {
            if (!inSameDirection) {
                return getEmptyPath();
            }
        }

        TrackPath path = getPath(targetFunction, trackIsInDirection, 0);
        if (path == null || path.isOccupied) return getEmptyPath();

        return reserve(path);
    }

    private Deque<TrackPiece> reserve(TrackPath pathToBest) {
        for (TrackPiece piece : pathToBest.path) {
            assert !piece.isOccupied();
            piece.setOccupied(true);
        }

        return pathToBest.path;
    }

    private Deque<TrackPiece> reserve(TrackPath... pathsToBest) {
        Deque<TrackPiece> combinedPath = new ArrayDeque<>();

        for (TrackPath trackPath : pathsToBest) {
            combinedPath.addAll(reserve(trackPath));
        }

        return combinedPath;
    }

    private List<TrackPiece> convertPath(TrackPiece previousTrack, NetworkPathFinder.Path pathToBest) {
        List<TrackPiece> pathToSignal = new ArrayList<>();
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
                networkNode = pathToBest.removeFirst();
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
        public boolean isOccupied = false;

        public TrackPath() {
            this.path = new ArrayDeque<>();
            this.length = 0;
        }

        public TrackPath(TrackPath other) {
            this.path = new ArrayDeque<>(other.path);
            this.length = other.length;
            this.isOccupied = other.isOccupied;
        }

        public TrackPath(TrackPiece... initial) {
            this.path = new ArrayDeque<>(initial.length);
            this.length = 0;

            for (TrackPiece trackPiece : initial) {
                path.add(trackPiece);
                length += trackPiece.getLength();
                if (trackPiece.isOccupied()) isOccupied = true;
            }
        }

        public TrackPath append(TrackPath other) {
            path.addAll(other.path);
            length += other.length;
            isOccupied = isOccupied || other.isOccupied;
            return this;
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

        @Override
        public String toString() {
            if (isOccupied) {
                return String.format("Path (%5.01fu in %2d pieces, occupied)", length, path.size());
            } else {

                return String.format("Path (%5.01fu in %2d pieces)", length, path.size());
            }
        }

        public float adjLength() {
            return length + (isOccupied ? TRACK_OCCUPATION_PENALTY : 0);
        }
    }
}
