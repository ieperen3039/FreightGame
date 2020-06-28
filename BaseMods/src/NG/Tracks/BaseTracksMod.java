package NG.Tracks;

import NG.Core.Game;
import NG.Core.Version;
import NG.DataStructures.Generic.Color4f;
import NG.Entities.Entity;
import NG.Mods.Mod;
import NG.Rendering.Material;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shapes.CustomShape;
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
        game.objectTypes().trackTypes.add(new DebugTrack());
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
        public static final float WIDTH = 0.3f;
        public static final float HEIGHT = 0.1f;
        public static final int RESOLUTION = 5;

        @Override
        public String toString() {
            return "Debug Tracks";
        }

        @Override
        public Mesh generateCircle(float radius, float angle, float endHeight) {
            float hDelta = endHeight / (angle * radius);
            float length = Math.abs(radius * angle);

            CustomShape frame = TrackType.generateFunctional(
                    t -> new Vector3f(radius * Math.cos(angle * t), radius * Math.sin(angle * t), endHeight * t - HEIGHT / 2),
                    t -> new Vector3f(-Math.sin(angle * t), Math.cos(angle * t), hDelta).normalize(),
                    WIDTH, HEIGHT, (int) (length * RESOLUTION)
            );

            return frame.toFlatMesh();
        }

        @Override
        public Mesh generateStraight(Vector3fc displacement) {
            float length = displacement.length();

            CustomShape frame = TrackType.generateFunctional(
                    t -> new Vector3f(displacement).mul(t).sub(0, 0, HEIGHT / 2),
                    t -> new Vector3f(displacement).div(length),
                    WIDTH, HEIGHT, (int) (length * RESOLUTION)
            );

            return frame.toFlatMesh();
        }

        @Override
        public void setMaterial(
                MaterialShader shader, TrackPiece track, Entity.Marking marking
        ) {
            Color4f color;
            if (marking.isValid()) {
                color = marking.color;

            } else if (track.isOccupied()) {
                color = Color4f.GREY;

            } else {
                color = Color4f.WHITE;
            }

            shader.setMaterial(Material.ROUGH, color);
        }

        @Override
        public float getMaximumSpeed() {
            return 10;
        }
    }
}
