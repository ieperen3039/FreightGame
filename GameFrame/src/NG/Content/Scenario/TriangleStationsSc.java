package NG.Content.Scenario;

import NG.Core.Game;
import NG.Core.ModLoader;
import NG.Entities.*;
import NG.GameState.GameState;
import NG.Mods.Mod;
import NG.Network.RailNode;
import NG.Settings.Settings;
import NG.Tracks.TrackType;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

/**
 * @author Geert van Ieperen created on 19-6-2020.
 */
public class TriangleStationsSc extends Scenario {
    private static final float TAU = (float) Math.PI * 2;

    public TriangleStationsSc(ModLoader modLoader) {
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

        addConnections(game, bypass1, node12, Float.POSITIVE_INFINITY);
        addConnections(game, bypass1, node31, Float.POSITIVE_INFINITY);
        addConnections(game, bypass2, node12, Float.POSITIVE_INFINITY);
        addConnections(game, bypass2, node23, Float.POSITIVE_INFINITY);
        addConnections(game, bypass3, node31, Float.POSITIVE_INFINITY);
        addConnections(game, bypass3, node23, Float.POSITIVE_INFINITY);

        node12.addSignal(game, true);
        node23.addSignal(game, true);
        node31.addSignal(game, true);
        bypass1.addSignal(game, true);
        bypass2.addSignal(game, true);
        bypass3.addSignal(game, true);

        Train initialTrain1 = new Train(game, 0, now, station1);
        initialTrain1.addElement(new Locomotive(game.objectTypes().locomotiveTypes.get(0)));
        initialTrain1.addElement(new Wagon(game.objectTypes().wagonTypes.get(0)));
        initialTrain1.addElement(new Wagon(game.objectTypes().wagonTypes.get(0)));
        addTrain(game, station1, initialTrain1);

        Train initialTrain2 = new Train(game, 1, now, station2);
        initialTrain2.addElement(new Locomotive(game.objectTypes().locomotiveTypes.get(0)));
        initialTrain2.addElement(new Wagon(game.objectTypes().wagonTypes.get(0)));
        initialTrain2.addElement(new Wagon(game.objectTypes().wagonTypes.get(0)));
        initialTrain2.addElement(new Wagon(game.objectTypes().wagonTypes.get(0)));
        addTrain(game, station2, initialTrain2);

        gameState.addEntity(station1);
        gameState.addEntity(station2);
        gameState.addEntity(station3);
    }

    private static void addTrain(Game game, Station station1, Train train) {
        game.state().addEntity(train);
        station1.addTrain(train);
        game.playerStatus().trains.add(train);
    }

    private RailNode getBypass(Game game, TrackType type, Vector2f center, Vector3f offset, float orientation) {
        Vector3f position = new Vector3f(offset).mul(0.75f);
        Vector3f direction = new Vector3f(org.joml.Math.cos(orientation), org.joml.Math.sin(orientation), 0);

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
            stationNode.addSignal(game, true);
        }

        return node;
    }

}
