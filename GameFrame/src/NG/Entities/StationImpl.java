package NG.Entities;

import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.GUIMenu.Components.SFrame;
import NG.GameState.Storage;
import NG.InputHandling.MouseTools.AbstractMouseTool.MouseAction;
import NG.Network.RailNode;
import NG.Network.SpecialRailNode;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Rendering.Shapes.GenericShapes;
import NG.Tools.Vectors;
import NG.Tracks.StraightTrack;
import NG.Tracks.TrackPiece;
import NG.Tracks.TrackType;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static NG.Tools.Vectors.cos;
import static NG.Tools.Vectors.sin;

/**
 * A basic implementation of a station. There is likely no need for another station
 * @author Geert van Ieperen created on 27-1-2019.
 */
public class StationImpl extends Storage implements Station {
    private static final float PLATFORM_SIZE = 2;
    private static final float HEIGHT = 0.1f;
    private static int nr = 1;

    protected String stationName = "Station " + (nr++);

    private final float orientation;
    private final int numberOfPlatforms;
    private final float length;
    private final float realWidth;

    private final SpecialRailNode[] forwardConnections;
    private final SpecialRailNode[] backwardConnections;
    private final Set<RailNode> nodes;

    public StationImpl(
            Game game, int numberOfPlatforms, int length, TrackType type, Vector3fc position, float orientation,
            double spawnTime
    ) {
        super(position, game, spawnTime);
        assert numberOfPlatforms > 0 : "created station with " + numberOfPlatforms + " platforms";
        this.game = game;
        this.numberOfPlatforms = numberOfPlatforms;
        this.length = length;
        this.realWidth = numberOfPlatforms * PLATFORM_SIZE;
        this.orientation = orientation;

        float trackHeight = HEIGHT + 0.1f;

        Vector3fc forward = new Vector3f(cos(orientation), sin(orientation), 0).normalize(length / 2f);
        Vector3f AToB = new Vector3f(forward).normalize();
        Vector3f BToA = new Vector3f(AToB).negate();

        forwardConnections = new SpecialRailNode[numberOfPlatforms];
        backwardConnections = new SpecialRailNode[numberOfPlatforms];

        // create nodes
        if (numberOfPlatforms > 1) {
            Vector3fc toRight = new Vector3f(sin(orientation), -cos(orientation), 0).normalize(realWidth / 2f);
            Vector3fc rightSkip = new Vector3f(toRight).normalize(PLATFORM_SIZE);

            Vector3f rightMiddle = new Vector3f(getPosition())
                    .sub(toRight)
                    .add(rightSkip.x() / 2, rightSkip.y() / 2, trackHeight);

            Vector3f backPos = new Vector3f(rightMiddle).sub(forward);
            Vector3f frontPos = rightMiddle.add(forward);

            for (int i = 0; i < numberOfPlatforms; i++) {
                forwardConnections[i] = new SpecialRailNode(backPos, type, AToB, this);
                backwardConnections[i] = new SpecialRailNode(frontPos, type, BToA, this);

                frontPos.add(rightSkip);
                backPos.add(rightSkip);
            }

        } else { // simplified version of above
            Vector3f frontPos = new Vector3f(getPosition()).add(forward).add(0, 0, trackHeight);
            Vector3f backPos = new Vector3f(getPosition()).sub(forward).add(0, 0, trackHeight);

            forwardConnections[0] = new SpecialRailNode(backPos, type, AToB, this);
            backwardConnections[0] = new SpecialRailNode(frontPos, type, BToA, this);
        }

        // create tracks
        for (int i = 0; i < numberOfPlatforms; i++) {
            SpecialRailNode A = forwardConnections[i];
            SpecialRailNode B = backwardConnections[i];

            TrackPiece trackConnection = new StraightTrack(game, type, A, B, false);
            RailNode.addConnection(trackConnection, A, B);
            game.state().addEntity(trackConnection);
        }

        nodes = new HashSet<>();
        nodes.addAll(Arrays.asList(forwardConnections));
        nodes.addAll(Arrays.asList(backwardConnections));
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
                matShader.setMaterial(Material.ROUGH, Color4f.GREY);
            }


            float sink = 0.1f; // size below ground
            gl.translate(0, 0, -sink);
            gl.scale(length / 2, realWidth / 2, HEIGHT + sink);

            gl.render(GenericShapes.CUBE, this);
        }
        gl.popMatrix();
    }

    @Override
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }

    @Override
    public String toString() {
        return stationName;
    }

    @Override
    public void reactMouse(MouseAction action) {
        if (action == MouseAction.PRESS_ACTIVATE) {
            game.gui().addFrame(new StationUI());
        }
    }

    public float getLength() {
        return length;
    }

    public int getNumberOfPlatforms() {
        return numberOfPlatforms;
    }

    @Override
    public Set<RailNode> getNodes() {
        return nodes;
    }

    protected class StationUI extends SFrame {
        StationUI() {
            super(StationImpl.this.toString(), 500, 300);

            // add buttons etc.
        }
    }

}
