package NG.Content.Scenario;

import NG.Core.Game;
import NG.Core.ModLoader;
import NG.Entities.Industry;
import NG.Entities.StationImpl;
import NG.GameMap.DefaultMapGenerator;
import NG.GameMap.GameMap;
import NG.GameMap.MapGeneratorMod;
import NG.Mods.Mod;
import NG.Mods.TypeCollection;
import NG.Network.RailNode;
import NG.Settings.Settings;
import NG.Tracks.TrackType;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

import static org.joml.Math.toRadians;

/**
 * @author Geert van Ieperen created on 1-7-2020.
 */
public class LinearConnectionSc extends Scenario {

    public static final int DISTANCE = 10;
    public static final float SIGNAL_OFFSET = 0.5f;
    public static final int INDUSTRY_DIST = 3;

    public LinearConnectionSc(ModLoader modLoader) {
        super(modLoader);
    }

    @Override
    protected List<Mod> getMods(ModLoader modLoader) {
        return modLoader.allMods();
    }

    @Override
    protected void setMap(Game game, Settings settings) {
        // random map
        MapGeneratorMod mapGenerator = new DefaultMapGenerator(0);
        mapGenerator.setSize(X_SIZE, Y_SIZE);
        mapGenerator.setProperty(DefaultMapGenerator.MAJOR_AMPLITUDE, 2);
        mapGenerator.setProperty(DefaultMapGenerator.MINOR_AMPLITUDE, 0);
        game.map().generateNew(game, mapGenerator);
    }

    @Override
    protected void setEntities(Game game, Settings settings) {
        TypeCollection allTypes = game.objectTypes();
        TrackType type = allTypes.trackTypes.get(0);

        Vector3f aSide = new Vector3f(-DISTANCE, DISTANCE, 0);
        Vector3f bSide = new Vector3f(DISTANCE, -DISTANCE, 0);

        GameMap map = game.map();
        Vector2f center = new Vector2f(map.getSize()).mul(0.5f);

        Industry aIndustry = new Industry(game, getGroundPos(game, center, aSide).add(INDUSTRY_DIST, INDUSTRY_DIST, 0), 0, allTypes
                .getIndustryByName("diamond mine"));
        game.state().addEntity(aIndustry);
        Industry bIndustry = new Industry(game, getGroundPos(game, center, bSide).add(-INDUSTRY_DIST, -INDUSTRY_DIST, 0), 0, allTypes
                .getIndustryByName("diamond ore refinery"));
        game.state().addEntity(bIndustry);

        StationImpl aStation = new StationImpl(game, 2, 6, type, getGroundPos(game, center, aSide), toRadians(-45), 0);
        game.state().addEntity(aStation);
        StationImpl bStation = new StationImpl(game, 2, 6, type, getGroundPos(game, center, bSide), toRadians(-45), 0);
        game.state().addEntity(bStation);

        // i dont know why, but these directions seem right
        List<RailNode> aNodes = aStation.getNodesOfDirection(aSide);
        List<RailNode> bNodes = bStation.getNodesOfDirection(bSide);

        aNodes.forEach(node -> node.addSignal(game, false));
        bNodes.forEach(node -> node.addSignal(game, false));

        RailNode aLeaveNode = new RailNode(game, getGroundPos(game, center, new Vector3f(aSide).mul(0.5f)
                .add(-SIGNAL_OFFSET, -SIGNAL_OFFSET, 0)), type, bSide);
        aLeaveNode.addSignal(game, true);
        RailNode aEnterNode = new RailNode(game, getGroundPos(game, center, new Vector3f(aSide).mul(0.5f)
                .add(SIGNAL_OFFSET, SIGNAL_OFFSET, 0)), type, bSide);
        aEnterNode.addSignal(game, false).allowOppositeTraffic(false);

        RailNode bLeaveNode = new RailNode(game, getGroundPos(game, center, new Vector3f(bSide).mul(0.5f)
                .add(SIGNAL_OFFSET, SIGNAL_OFFSET, 0)), type, aSide);
        bLeaveNode.addSignal(game, true);
        RailNode bEnterNode = new RailNode(game, getGroundPos(game, center, new Vector3f(bSide).mul(0.5f)
                .add(-SIGNAL_OFFSET, -SIGNAL_OFFSET, 0)), type, aSide);
        bEnterNode.addSignal(game, false).allowOppositeTraffic(false);

        addConnections(game, aLeaveNode, aNodes);
        addConnections(game, aEnterNode, aNodes);
        addConnections(game, bLeaveNode, bNodes);
        addConnections(game, bEnterNode, bNodes);
        addConnections(game, aLeaveNode, bEnterNode, 5);
        addConnections(game, bLeaveNode, aEnterNode, 5);
    }
}
