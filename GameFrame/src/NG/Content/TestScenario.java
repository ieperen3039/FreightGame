package NG.Content;

import NG.Core.Game;
import NG.Core.ModLoader;
import NG.Entities.StationImpl;
import NG.GameState.GameState;
import NG.Mods.Mod;
import NG.Network.NetworkNode;
import NG.Network.RailNode;
import NG.Settings.Settings;
import NG.Tracks.RailTools;
import NG.Tracks.TrackPiece;
import NG.Tracks.TrackType;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

import static NG.Tools.Vectors.cos;
import static NG.Tools.Vectors.sin;

/**
 * @author Geert van Ieperen created on 19-6-2020.
 */
public class TestScenario extends Scenario {
    private static final float TAU = (float) Math.PI * 2;

    public TestScenario(ModLoader modLoader) {
        super(modLoader);
    }

    @Override
    protected List<Mod> getMods(ModLoader modLoader) {
        return modLoader.allMods();
    }

    @Override
    protected void setEntities(Game game, Settings settings) {
        double now = game.timer().getGameTime();
        GameState gameState = game.state();

        TrackType type = game.objectTypes().trackTypes.get(0);
        Vector2f center = new Vector2f(game.map().getSize()).mul(0.5f);

        Vector3f offset = new Vector3f(10, 0, 0);
        float orientation = 1 / 4f * TAU;
        Vector3fc stationPos1 = getGroundPos(game, center, offset);
        StationImpl station1 = new StationImpl(game, 2, 6, type, stationPos1, orientation, now);
        RailNode bypass1 = getBypass(game, type, center, offset, orientation);

        offset.rotateZ(1 / 3f * TAU);
        orientation += 1 / 3f * TAU;
        Vector3fc stationPos2 = getGroundPos(game, center, offset);
        StationImpl station2 = new StationImpl(game, 2, 6, type, stationPos2, orientation, now);
        RailNode bypass2 = getBypass(game, type, center, offset, orientation);

        offset.rotateZ(1 / 3f * TAU);
        orientation += 1 / 3f * TAU;
        Vector3fc stationPos3 = getGroundPos(game, center, offset);
        StationImpl station3 = new StationImpl(game, 2, 6, type, stationPos3, orientation, now);
        RailNode bypass3 = getBypass(game, type, center, offset, orientation);

        // add nodes connecting the stations
        offset.rotateZ(0.5f * TAU);
        RailNode node12 = addNodeConnections(game, type, center, offset, station1, station2);
        offset.rotateZ(1 / 3f * TAU);
        RailNode node23 = addNodeConnections(game, type, center, offset, station2, station3);
        offset.rotateZ(1 / 3f * TAU);
        RailNode node31 = addNodeConnections(game, type, center, offset, station3, station1);

        addConnections(game, bypass1, node12);
        addConnections(game, bypass1, node31);
        addConnections(game, bypass2, node12);
        addConnections(game, bypass2, node23);
        addConnections(game, bypass3, node31);
        addConnections(game, bypass3, node23);

        node12.addSignal(game);
        node23.addSignal(game);
        node31.addSignal(game);
        bypass1.addSignal(game);
        bypass2.addSignal(game);
        bypass3.addSignal(game);

        gameState.addEntity(station1);
        gameState.addEntity(station2);
        gameState.addEntity(station3);
    }

    private RailNode getBypass(Game game, TrackType type, Vector2f center, Vector3f offset, float orientation) {
        Vector3f position = new Vector3f(offset).mul(0.7f);
        Vector3f direction = new Vector3f(cos(orientation), sin(orientation), 0);

        return new RailNode(getGroundPos(game, center, position), type, direction);
    }

    private RailNode addNodeConnections(
            Game game, TrackType type, Vector2fc center, Vector3fc offset, StationImpl station1, StationImpl station2
    ) {
        Vector3f direction = new Vector3f(offset).rotateZ(-1 / 4f * TAU);
        RailNode node = new RailNode(getGroundPos(game, center, offset), type, direction);

        List<RailNode> nodesStation12 = station1.getNodesOfDirection(direction);
        addConnections(game, node, nodesStation12);
        List<RailNode> nodesStation21 = station2.getNodesOfDirection(direction.negate());
        addConnections(game, node, nodesStation21);

        for (RailNode stationNode : nodesStation12) {
            stationNode.addSignal(game);
        }

        return node;
    }

    private void addConnections(Game game, RailNode targetNode, List<RailNode> stationNodes) {
        for (RailNode stationNode : stationNodes) {
            addConnections(game, stationNode, targetNode);
        }
    }

    private void addConnections(Game game, RailNode aNode, RailNode bNode) {
        GameState gameState = game.state();
        List<TrackPiece> connection = RailTools.createConnection(game, aNode, bNode, Float.POSITIVE_INFINITY);

        for (TrackPiece trackPiece : connection) {
            NetworkNode.addConnection(trackPiece);
            gameState.addEntity(trackPiece);
        }
    }

    private Vector3f getGroundPos(Game game, Vector2fc origin, Vector3fc offset) {
        Vector3f stationPos = new Vector3f(origin, 0).add(offset);
        stationPos.z = game.map().getHeightAt(stationPos.x, stationPos.y) + Settings.TRACK_HEIGHT_ABOVE_GROUND;
        return stationPos;
    }
}
