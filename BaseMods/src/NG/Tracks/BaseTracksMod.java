package NG.Tracks;

import NG.Core.Game;
import NG.Core.Version;
import NG.Mods.Mod;
import NG.Rendering.MeshLoading.Mesh;
import org.joml.Vector3fc;

/**
 * Implementation of regular rail
 * @author Geert van Ieperen. Created on 19-9-2018.
 */
public class BaseTracksMod implements Mod {

    @Override
    public void init(Game game) throws Version.MisMatchException {
        game.getVersionNumber().requireAtLeast(0, 0);
        game.objectTypes().addTrackTypes(new DebugTrack());
    }

    @Override
    public void cleanup() {
    }

    @Override
    public String getModName() {
        return "BaseTracks";
    }

    @Override
    public Version getVersionNumber() {
        return new Version(0, 0);
    }


    private static class DebugTrack implements TrackType {

        @Override
        public String toString() {
            return "Debug Tracks";
        }

        @Override
        public Mesh generateCircle(float radius, float angle, float endHeight) {
            return TrackType.clickBoxCircle(radius, angle, endHeight);
        }

        @Override
        public Mesh generateStraight(Vector3fc displacement) {
            return TrackType.clickBoxStraight(displacement);
        }

        @Override
        public float minimumRadius() {
            return 1;
        }
    }
}
