package NG.Tracks;

import NG.Core.Game;
import NG.Core.Version;
import NG.DataStructures.Generic.Color4f;
import NG.Mods.Mod;
import NG.Rendering.Material;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Tools.Vectors;
import org.joml.Math;
import org.joml.Vector3f;
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
        public static final float WIDTH = 0.4f;
        public static final float HEIGHT = 0.2f;

        @Override
        public String toString() {
            return "Debug Tracks";
        }

        @Override
        public Mesh generateCircle(float radius, float angle, float endHeight) {
            float hDelta = endHeight / (angle * radius);
            float length = Math.abs(radius * angle);

            return TrackType.generateFunctional(
                    t -> new Vector3f(radius * Math.cos(angle * t), radius * Math.sin(angle * t), endHeight * t),
                    t -> new Vector3f(-Math.sin(angle * t), Math.cos(angle * t), hDelta).normalize(),
                    WIDTH, HEIGHT, (int) (length * 10)
            );
        }

        @Override
        public Mesh generateStraight(Vector3fc displacement) {
            float length = displacement.length();
            return TrackType.generateFunctional(
                    t -> Vectors.newZeroVector().lerp(displacement, t),
                    t -> new Vector3f(displacement).div(length),
                    WIDTH, HEIGHT, (int) (length * 10)
            );
        }

        @Override
        public void setMaterial(MaterialShader shader) {
            shader.setMaterial(Material.ROUGH, Color4f.WHITE);
        }

        @Override
        public float minimumRadius() {
            return 1;
        }
    }
}
