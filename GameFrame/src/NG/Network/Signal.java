package NG.Network;

import NG.DataStructures.Generic.Pair;
import NG.Tools.NetworkPathFinder;
import NG.Tools.Toolbox;
import NG.Tracks.TrackPiece;

import java.util.*;
import java.util.function.Function;

/**
 * @author Geert van Ieperen created on 1-7-2020.
 */
public class Signal {
    private static final float TRACK_OCCUPATION_PENALTY = 10f;
    /** the node where this signals is placed on */
    protected final RailNode hostNode;
    protected boolean inNodeDirection;
    protected boolean allowOppositeTraffic;

    public enum Direction {
        IN_DIRECTION, AGAINST_DIRECTION, BOTH_DIRECTIONS
    }

    public Signal(RailNode targetNode, boolean inNodeDirection, boolean allowOppositeTraffic) {
        this.hostNode = targetNode;
        this.inNodeDirection = inNodeDirection;
        this.allowOppositeTraffic = allowOppositeTraffic;
    }

    public RailNode getNode() {
        return hostNode;
    }

    /**
     * calculates the path of the given controller, positioned on hostNode, to the targets as given by the controller.
     * @param targets         the target supplier, which should give a next target given a scheduling depth.
     * @param inSameDirection whether the path should be calculated in the direction of the railnode. false iff {@link
     *                        #getNode()}{@link RailNode#isInDirectionOf(TrackPiece) .isInDirectionOf(previousTrack)}
     * @return a path from hostNode, via any possible targets, to a signal. Returns null if no such path exists.
     * Guaranteed is {@code path == null || path.getLast().hasSignal()}
     */
    private Pair<TrackPath, Float> getPath(
            Function<Integer, NetworkPosition> targets, boolean inSameDirection
    ) {
        HashMap<Signal, TrackPath> signals = new HashMap<>();
        HashMap<NetworkNode, TrackPath> nodes = new HashMap<>();

        NetworkNode networkNode = hostNode.getNetworkNode();
        List<NetworkNode.Direction> entries = inSameDirection ? networkNode.getEntriesA() : networkNode.getEntriesB();

        // TODO trivial section shortcut ?
        int depth = 0;

        for (NetworkNode.Direction entry : entries) {
            TrackPiece trackPiece = entry.trackPiece;
            collectPaths(trackPiece.getNot(hostNode), trackPiece, signals, nodes, new TrackPath(trackPiece));
        }
        NetworkPosition target = targets.apply(depth++);

        if (signals.isEmpty()) {
            return null;

        } else if (target == null) {
            // reserve random path
            TrackPath[] paths = signals.values().stream()
                    .filter(path -> !path.isOccupied)
                    .toArray(TrackPath[]::new);

            if (paths.length == 0) return null;
            return new Pair<>(paths[Toolbox.random.nextInt(paths.length)], Float.POSITIVE_INFINITY);
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
            if (node.hasSignal()) return new Pair<>(pathViaNodes, pathViaNodes.adjLength());

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

        for (Signal signal : signals.keySet()) {
            TrackPath pathToSignal = signals.get(signal);
            if (pathToSignal.path.isEmpty()) continue;

            TrackPiece lastTrack = pathToSignal.path.getLast();
            if (signal.allowsPassingFrom(lastTrack)) {
                float totalDist = getPathToTargetLength(target, signal, pathToSignal, false);

                if (totalDist < leastDistance) {
                    leastDistance = totalDist;
                    pathToBest = pathToSignal;
                }
            } else {
                // if there is no place to go, stop at some impassible point
                if (leastDistance == Float.POSITIVE_INFINITY) {
                    pathToBest = pathToSignal;
                }
            }
            // else this is a track-end
        }

        // either we found a path to a node, ending in an EOL
        // or no path exists, and pathViaNodes is empty
        if (pathToBest == null) {
            return new Pair<>(pathViaNodes, Float.POSITIVE_INFINITY);
        }

        return new Pair<>(pathViaNodes.append(pathToBest), pathViaNodes.adjLength() + leastDistance);
    }

    private float getPathToTargetLength(
            NetworkPosition target, Signal other, TrackPath pathToSignal, boolean doRevert
    ) {
        TrackPiece lastTrack = pathToSignal.path.getLast();
        NetworkNode otherNetwork = other.hostNode.getNetworkNode();
        boolean inDirection = other.hostNode.isInDirectionOf(lastTrack) == doRevert;

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
     * @param signals     the map [signals -> the shortest path to this signal], where signal is null for an
     *                    end-of-the-line.
     * @param nodes       the map [nodes -> the shortest path to this node] for all nodes
     * @param pathToNode  path to node
     */
    private void collectPaths(
            RailNode node, TrackPiece sourceTrack, Map<Signal, TrackPath> signals,
            Map<NetworkNode, TrackPath> nodes, TrackPath pathToNode
    ) {
        NetworkNode networkNode = node.getNetworkNode();
        // track all critical nodes
        if (networkNode.isNetworkCritical()) {
            TrackPath original = nodes.get(networkNode);
            if (original == null || original.adjLength() > pathToNode.adjLength()) {
                nodes.put(networkNode, new TrackPath(pathToNode));
            }
        }

        if (node.hasSignal()) {
            Signal sig = node.getSignal();
            // only add it to the planning if we should recognize it
            // if the signal is in opposite direction, we may recognize it as a place to stop
            if (node.isInDirectionOf(sourceTrack) != sig.inNodeDirection || !sig.allowOppositeTraffic) {
                TrackPath original = signals.get(sig);
                if (original == null || original.adjLength() > pathToNode.adjLength()) {
                    signals.put(sig, new TrackPath(pathToNode));
                }

                return;
            }
        }

        assert !networkNode.isEnd() : "End of track has no EOL signal";

        // we continue searching further
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

    /**
     * computes a path p to another signal such that p.getFirst() is the first track on the path and p.getLast() is the
     * last track on the path. This path is a section of the shortest available path towards target. Each element is
     * reserved, and should be freed whenever it is passed, as {@link TrackPiece#setOccupied(boolean)
     * track.setOccupied(false)}.
     * <p>
     * If the path is not empty, then it starts and ends with a signal, with no signal inbetween.
     * @param trackDirection indicates whether the starting direction is the same as the direction of {@link
     *                       #getNode()}
     * @param targetFunction
     * @return a path from here to the next signal on the shortest available path toward target.
     */
    public Deque<TrackPiece> reservePath(
            Direction trackDirection, Function<Integer, NetworkPosition> targetFunction
    ) {
        Pair<TrackPath, Float> path;

        switch (trackDirection) {
            case IN_DIRECTION:
                if (inNodeDirection || allowOppositeTraffic) {
                    path = getPath(targetFunction, true);
                } else {
                    path = null;
                }

                break;

            case AGAINST_DIRECTION:
                if (!inNodeDirection || allowOppositeTraffic) {
                    path = getPath(targetFunction, false);
                } else {
                    path = null;
                }

                break;

            case BOTH_DIRECTIONS:
                Pair<TrackPath, Float> pathInDirection = getPath(targetFunction, true);
                Pair<TrackPath, Float> pathAgainstDirection = allowOppositeTraffic ? getPath(targetFunction, false) : null;

                if (pathInDirection == null) {
                    path = pathAgainstDirection;

                } else if (pathAgainstDirection == null) {
                    path = pathInDirection;

                } else {
                    if (pathInDirection.right < pathAgainstDirection.right) {
                        path = pathInDirection;
                    } else {
                        path = pathAgainstDirection;
                    }
                }
                break;
            default:
                throw new IllegalStateException("unknown enum value " + trackDirection);
        }

        if (path == null || path.left.isOccupied) return Signal.getEmptyPath();
        return reserve(path.left);
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

    protected void revert() {
        inNodeDirection = !inNodeDirection;
    }

    public void allowOppositeTraffic(boolean doAllow) {
        this.allowOppositeTraffic = doAllow;
    }

    private static ArrayDeque<TrackPiece> getEmptyPath() {
        return new ArrayDeque<>();
    }

    private boolean allowsPassingFrom(TrackPiece arrivalTrack) {
        if (allowOppositeTraffic) return true;
        return (inNodeDirection == !hostNode.isInDirectionOf(arrivalTrack));
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
