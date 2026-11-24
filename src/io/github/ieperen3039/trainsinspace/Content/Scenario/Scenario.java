package io.github.ieperen3039.trainsinspace.Content.Scenario;

import io.github.ieperen3039.ngn.Camera.Camera;
import io.github.ieperen3039.trainsinspace.Camera.TycoonCamera;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Core.ModLoader;
import io.github.ieperen3039.trainsinspace.GameMap.DefaultMapGenerator;
import io.github.ieperen3039.trainsinspace.GameMap.MapGeneratorMod;
import io.github.ieperen3039.trainsinspace.GameState.GameState;
import io.github.ieperen3039.trainsinspace.Menu.InGame.MapGenerationProgress;
import io.github.ieperen3039.trainsinspace.Mods.Mod;
import io.github.ieperen3039.trainsinspace.Network.NetworkNode;
import io.github.ieperen3039.trainsinspace.Network.RailNode;
import io.github.ieperen3039.trainsinspace.Settings.Settings;
import io.github.ieperen3039.trainsinspace.Tracks.RailTools;
import io.github.ieperen3039.trainsinspace.Tracks.TrackPiece;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

/**
 * @author Geert van Ieperen created on 19-6-2020.
 */
public abstract class Scenario {
    public static final int X_SIZE = 100;
    public static final int Y_SIZE = 100;


    public void init() {}

    /**
     * @return a list of all mods used in this scenario
     */
    public abstract List<Mod> getMods(ModLoader modLoader);

    public void setMap(Game game, Settings settings) {
        // random map
        MapGeneratorMod mapGenerator = new DefaultMapGenerator(0);
        MapGenerationProgress progressUi = new MapGenerationProgress(mapGenerator::heightmapProgress);

        game.gui().addFrame(progressUi);
        mapGenerator.setSize(X_SIZE, Y_SIZE);
        mapGenerator.setProperty(DefaultMapGenerator.MAJOR_AMPLITUDE, 10);
        mapGenerator.setProperty(DefaultMapGenerator.MINOR_AMPLITUDE, 0);
        game.map().generateNew(mapGenerator);

        progressUi.dispose();
    }

    /**
     * set all entities in this scenario. The default does nothing
     */
    public void setEntities(Game game, Settings settings) {

    }

    /**
     * set the camera to the initial position. The default centers the camera on the map
     */
    public void setCamera(Game game, Settings settings) {
        // set camera to middle of map
        Camera cam = game.camera();
        Vector2f size = game.map().getSize();
        Vector3f cameraFocus = new Vector3f(size.x / 2, size.y / 2, 0);
        if (cam instanceof TycoonCamera tCam) {
            Vector3f cameraEye = new Vector3f(cameraFocus).add(-20, -20, 20);
            tCam.set(cameraFocus, cameraEye);
            tCam.setMap(game.map());
        } else {
            cam.set(cameraFocus);
        }
    }

    /**
     * set the lights on this map. The default installs one sunlight element
     */
    public void setLight(Game game, Settings settings) {
        // add light
        game.lights().addDirectionalLight(
                settings.SUNLIGHT_POSITION, settings.SUNLIGHT_COLOR, settings.SUNLIGHT_INTENSITY
        );
    }

    protected static Vector3f getGroundPos(Game game, Vector2fc origin, Vector3fc offset) {
        Vector3f pos = new Vector3f(origin, 0).add(offset);
        pos.z = game.map().getHeightAt(pos.x, pos.y) + game.settings().TRACK_HEIGHT_ABOVE_GROUND;
        return pos;
    }

    protected static Vector3f getGroundPos(Game game, Vector3fc origin, Vector3fc offset) {
        Vector3f pos = new Vector3f(origin).add(offset);
        pos.z = game.map().getHeightAt(pos.x, pos.y) + game.settings().TRACK_HEIGHT_ABOVE_GROUND;
        return pos;
    }

    protected static void addConnections(Game game, RailNode targetNode, List<RailNode> nodes) {
        for (RailNode node : nodes) {
            addConnections(game, node, targetNode, Float.POSITIVE_INFINITY);
        }
    }

    protected static void addConnections(Game game, RailNode aNode, RailNode bNode, float signalSpacing) {
        GameState gameState = game.state();
        List<TrackPiece> connection = RailTools.createConnection(game, aNode, bNode, signalSpacing);

        for (TrackPiece trackPiece : connection) {
            NetworkNode.addConnection(trackPiece);
            gameState.addEntity(trackPiece);
        }
    }

    public static class Empty extends Scenario {
        @Override
        public List<Mod> getMods(ModLoader modLoader) {
            return modLoader.allMods();
        }
    }
}
