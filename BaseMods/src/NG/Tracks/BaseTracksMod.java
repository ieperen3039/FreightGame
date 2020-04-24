package NG.Tracks;

import NG.Core.Game;
import NG.Core.Version;
import NG.GameState.GameMap;
import NG.Mods.Mod;
import NG.Rendering.MatrixStack.SGL;
import NG.Tools.Toolbox;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;

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
        public static final float TRACK_SPACING = 1;

        @Override
        public String name() {
            return "Debug Tracks";
        }

        @Override
        public void drawCircle(SGL gl, Vector2fc center, float radius, float lowerTheta, float angle, GameMap map) {
            Vector2f coord = new Vector2f();
            float pieceRad = (TRACK_SPACING / radius);

            float upperTheta = lowerTheta + angle;
            for (float p = lowerTheta; p < upperTheta; p += pieceRad) {
                gl.pushMatrix();
                {
                    coord.set((float) Math.cos(p), (float) Math.sin(p));
                    coord.mul(radius);
                    coord.add(center);

                    Vector3f position = map.getPosition(coord);
                    gl.translate(position);
                    Toolbox.drawAxisFrame(gl);
                }
                gl.popMatrix();
            }
        }

        @Override
        public void drawStraight(SGL gl, Vector2fc startCoord, Vector2fc direction, float length, GameMap map) {
            Vector2f coord = new Vector2f();

            for (int l = 0; l < length; l += TRACK_SPACING) {
                gl.pushMatrix();
                {
                    coord.set(direction).mul(l);
                    coord.add(startCoord);

                    Vector3f position = map.getPosition(coord);
                    gl.translate(position);
                    Toolbox.drawAxisFrame(gl);
                }
                gl.popMatrix();
            }
        }
    }
}
