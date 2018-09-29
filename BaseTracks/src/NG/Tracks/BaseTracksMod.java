package NG.Tracks;

import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.Game;
import NG.Engine.Version;
import NG.GameState.GameState;
import NG.Tools.Toolbox;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;

/**
 * @author Geert van Ieperen. Created on 19-9-2018.
 */
public class BaseTracksMod implements TrackMod {
    private List<TrackType> types;
    private float trackSpacing;

    @Override
    public void init(Game game) throws Version.MisMatchException {
        game.getVersionNumber().requireAtLeast(0, 0);
        trackSpacing = game.settings().TRACK_SPACING;
        types = Collections.singletonList(new RegularTrack());
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

    @Override
    public Version getVersionNumber() {
        return new Version(0, 0);
    }

    private class RegularTrack implements TrackType {
        @Override
        public String getTypeName() {
            return "Regular Tracks";
        }

        @Override
        public void drawCircle(SGL gl, Vector2fc center, float radius, float startRadian, float endRadian, GameState gameState) {
            Vector2f coord = new Vector2f();
            float pieceRad = (trackSpacing / radius);

            for (float p = startRadian; p < endRadian; p += pieceRad) {
                gl.pushMatrix();
                {
                    coord.set((float) Math.sin(p), (float) Math.cos(p)).mul(radius);
                    coord.add(center);

                    Vector3f position = gameState.getPosition(coord);
                    gl.translate(position);
                    Toolbox.drawAxisFrame(gl);
                }
                gl.popMatrix();
            }
        }

        @Override
        public void drawStraight(SGL gl, Vector2fc startCoord, float length, Vector2fc direction, GameState gameState) {
            Vector2f coord = new Vector2f();

            for (int l = 0; l < length; l += trackSpacing) {
                gl.pushMatrix();
                {
                    coord.set(direction).mul(l);
                    coord.add(startCoord);

                    Vector3f position = gameState.getPosition(coord);
                    gl.translate(position);
                    Toolbox.drawAxisFrame(gl);
                }
                gl.popMatrix();
            }
        }
    }
}
