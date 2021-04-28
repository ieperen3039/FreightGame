package NG.Tracks;

import NG.Core.Game;
import NG.Core.Version;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Valuta;
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
        return "Base Tracks";
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
        public Mesh generateSupport(float height) {
            height -= HEIGHT;
            CustomShape frame = new CustomShape();
            float baseHSize = 0.2f;
            float pillarHSize = 0.1f;
            float floor = 0;
            float bottomSupport = Math.min(0.5f, height);

            Vector3f a = new Vector3f(baseHSize, baseHSize, -1);
            Vector3f b = new Vector3f(baseHSize, -baseHSize, -1);
            Vector3f c = new Vector3f(-baseHSize, -baseHSize, -1);
            Vector3f d = new Vector3f(-baseHSize, baseHSize, -1);

            Vector3f a2 = new Vector3f();
            Vector3f b2 = new Vector3f();
            Vector3f c2 = new Vector3f();
            Vector3f d2 = new Vector3f();

            // underground base
            a.z = floor;
            b.z = floor;
            c.z = floor;
            d.z = floor;
            addPillarLayer(frame, a, b, c, d, a2, b2, c2, d2);

            // base support
            a.set(pillarHSize, pillarHSize, bottomSupport);
            b.set(pillarHSize, -pillarHSize, bottomSupport);
            c.set(-pillarHSize, -pillarHSize, bottomSupport);
            d.set(-pillarHSize, pillarHSize, bottomSupport);
            addPillarLayer(frame, a, b, c, d, a2, b2, c2, d2);

            // pillar
            if (bottomSupport < height) {
                a.z = height;
                b.z = height;
                c.z = height;
                d.z = height;
                addPillarLayer(frame, a, b, c, d, a2, b2, c2, d2);
            }

            // close top
            frame.addQuad(a, b, c, d);
            return frame.toFlatMesh();
        }

        private void addPillarLayer(
                CustomShape frame, Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector3f a2, Vector3f b2,
                Vector3f c2, Vector3f d2
        ) {
            frame.addQuad(a, b, b2, a2);
            frame.addQuad(b, c, c2, b2);
            frame.addQuad(c, d, d2, c2);
            frame.addQuad(d, a, a2, d2);
            a2.set(a);
            b2.set(b);
            c2.set(c);
            d2.set(d);
        }

        @Override
        public void setMaterial(
                MaterialShader shader, TrackElement track, Color4f color
        ) {
            shader.setMaterial(Material.ROUGH, color);
        }

        @Override
        public float getMaximumSpeed() {
            return 10;
        }

        @Override
        public Valuta getCostPerMeter() {
            return Valuta.ofUnitValue(1);
        }

        @Override
        public float getMaxSupportLength() {
            return 2.0f;
        }
    }

}
