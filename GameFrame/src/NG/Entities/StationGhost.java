package NG.Entities;

import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.GameState.Storage;
import NG.InputHandling.ClickShader;
import NG.InputHandling.MouseTools.AbstractMouseTool.MouseAction;
import NG.Network.RailNode;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Rendering.Shapes.GenericShapes;
import NG.Tools.Vectors;
import NG.Tracks.TrackType;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Collections;
import java.util.Set;

/**
 * A basic implementation of a station. There is likely no need for another station
 * @author Geert van Ieperen created on 27-1-2019.
 */
public class StationGhost extends Storage implements Station {
    private static final float PLATFORM_SIZE = 2;
    public static final float HEIGHT = 0.1f;
    private float orientation = 0;

    private int numberOfPlatforms;
    private float length;
    private float realWidth;

    public StationGhost(Game game, int nrOfPlatforms, int length) {
        super(new Vector3f(), game, game.timer().getGameTime());
        this.game = game;
        assert nrOfPlatforms > 0 : "created station with " + nrOfPlatforms + " platforms";

        setSize(nrOfPlatforms, length);
    }

    public void setSize(int nrOfPlatforms, int length) {
        this.numberOfPlatforms = nrOfPlatforms;
        this.length = length;
        this.realWidth = nrOfPlatforms * PLATFORM_SIZE;
    }

    @Override
    public void update() {
    }

    @Override
    public void draw(SGL gl) {
        gl.pushMatrix();
        {
            gl.translate(getPosition());
            gl.rotate(Vectors.Z, orientation);

            ShaderProgram shader = gl.getShader();
            if (shader instanceof MaterialShader) {
                MaterialShader matShader = (MaterialShader) shader;
                matShader.setMaterial(Material.ROUGH, new Color4f(1.0f, 1.0f, 1.0f, 0.5f));
            }

            if (!(shader instanceof ClickShader)) { // draw cube
                gl.scale(length / 2, realWidth / 2, HEIGHT); // half below ground
                gl.render(GenericShapes.CUBE, this);
            }
        }
        gl.popMatrix();
    }

    @Override
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }

    public void setOrientation(float orientation) {
        this.orientation = orientation;
    }

    public void setPosition(Vector3fc position) {
        // makes it public
        super.setPosition(position);
    }

    @Override
    public void reactMouse(MouseAction action) {
        // handled by StationBuilder
    }

    public float getLength() {
        return length;
    }

    public int getNumberOfPlatforms() {
        return numberOfPlatforms;
    }

    @Override
    public Set<RailNode> getNodes() {
        return Collections.emptySet();
    }

    Station solidify(Game game, TrackType trackType, double gameTime) {
        return new StationImpl(game,
                numberOfPlatforms, (int) length, trackType, getPosition(), orientation, gameTime
        );
    }
}
