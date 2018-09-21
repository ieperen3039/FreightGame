package NG.Tracks;

import NG.Engine.Game;
import NG.Engine.Version;
import NG.Mods.TrackMod;
import NG.Settings.Settings;
import NG.Tools.Directory;
import org.joml.Vector2f;
import org.joml.Vector2fc;

import java.util.Collections;
import java.util.List;

/**
 * @author Geert van Ieperen. Created on 19-9-2018.
 */
public class BaseTracksMod implements TrackMod {
    private List<TrackType> types;
    private Game game;

    @Override
    public void init(Game game) throws Version.MisMatchException {
        game.getVersionNumber().requireAtLeast(0, 0);
        types = Collections.singletonList(new RegularTrack());
        this.game = game;
    }

    @Override
    public void cleanup() {
        types = null;
    }

    @Override
    public String getModName() {
        return "BaseTracks";
    }

    @Override
    public List<TrackType> getTypes() {
        return types;
    }

    private class RegularTrack implements TrackType {
        @Override
        public String getTypeName() {
            return "Regular Tracks";
        }

        @Override
        public TrackPiece createNew(Vector2fc startCoord, Vector2fc startDirection, Vector2fc endCoord) {
            if (endCoord.sub(startCoord, new Vector2f()).angle(startDirection) < 0.01f) {
                return new StraightTrack(game, startCoord, endCoord);
            } else {
                return new CircleTrack(game, startCoord, startDirection, endCoord);
            }

        }

        @Override
        public TrackPiece concept(Vector2fc startPosition, Vector2fc startDirection, Vector2fc endPoint) {
            return createNew(startPosition, startDirection, endPoint);
        }
    }

    /** Default main method for Mods. */
    public static void main(String[] args) {
        System.out.println("This is a mod for the game " + Settings.GAME_NAME);
        System.out.println("To use this mod, place this JAR file in folder " + Directory.mods.getFile());
    }
}
