package NG.Entities;

import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.GUIMenu.Components.SFrame;
import NG.GameState.Storage;
import NG.Network.NetworkNode;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Rendering.Shapes.GenericShapes;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import NG.Tracks.TrackPiece;
import NG.Tracks.TrackType;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

import static NG.Tools.Vectors.cos;
import static NG.Tools.Vectors.sin;

/**
 * A basic implementation of a station. There is likely no need for another station
 * @author Geert van Ieperen created on 27-1-2019.
 */
public class StationImpl extends Storage implements Station {
    private static final float PLATFORM_SIZE = 2;
    private static final float WAGON_LENGTH = 3;
    public static final float HEIGHT = 0.1f;
    private final String className = this.getClass().getSimpleName() + " " + (nr++);
    private static int nr = 1;
    protected String stationName = "X";
    /** the position and orientation of the station */
    protected float orientation = 0;
    /** whether this station has been placed down. */
    protected boolean isFixed = false;

    private int numberOfPlatforms;
    private int platformCapacity;
    private float realLength;
    private float realWidth;

    private NetworkNode[] forwardConnections;
    private NetworkNode[] backwardConnections;
    private TrackType type;

    public StationImpl(Game game, int nrOfPlatforms, int length, TrackType type) {
        super(new Vector3f(), game);
        this.game = game;
        assert nrOfPlatforms > 0 : "created station with " + nrOfPlatforms + " platforms";
        this.type = type;

        setSize(nrOfPlatforms, length);
    }

    public void setSize(int nrOfPlatforms, int length) {
        if (isFixed) {
            Logger.ERROR.print("Tried changing size of a fixed station");
            return;
        }

        this.numberOfPlatforms = nrOfPlatforms;
        this.platformCapacity = length;
        this.realLength = length * WAGON_LENGTH;
        this.realWidth = nrOfPlatforms * PLATFORM_SIZE;
    }

    @Override
    public void update() {

    }

    public void fixPosition() {
        assert numberOfPlatforms > 0 : "created station with " + numberOfPlatforms + " platforms";
        isFixed = true;

        Vector3fc forward = new Vector3f(cos(orientation), sin(orientation), 0).normalize(realLength / 2f);

        forwardConnections = new NetworkNode[numberOfPlatforms];
        backwardConnections = new NetworkNode[numberOfPlatforms];

        if (numberOfPlatforms > 1) {
            Vector3fc toRight = new Vector3f(sin(orientation), -cos(orientation), 0).normalize(realWidth / 2f);
            Vector3fc rightSkip = new Vector3f(toRight).normalize(PLATFORM_SIZE);

            Vector3f rightMiddle = new Vector3f(getPosition())
                    .sub(toRight)
                    .add(rightSkip.x() / 2, rightSkip.y() / 2, getElevation());

            Vector3f backPos = new Vector3f(rightMiddle).sub(forward);
            Vector3f frontPos = rightMiddle.add(forward);

            for (int i = 0; i < numberOfPlatforms; i++) {
                TrackPiece trackConnection = NetworkNode.createNewTrack(game, type, frontPos, backPos);

                forwardConnections[i] = trackConnection.getStartNode();
                backwardConnections[i] = trackConnection.getEndNode();

                frontPos.add(rightSkip);
                backPos.add(rightSkip);
            }

        } else { // simplified version of above
            Vector3f frontPos = new Vector3f(getPosition()).add(forward).add(0, 0, getElevation());
            Vector3f backPos = new Vector3f(getPosition()).sub(forward).add(0, 0, getElevation());

            TrackPiece trackConnection = NetworkNode.createNewTrack(game, type, frontPos, backPos);

            forwardConnections[0] = trackConnection.getStartNode();
            backwardConnections[0] = trackConnection.getEndNode();
        }
    }

    @Override
    public float getElevation() {
        return HEIGHT;
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
                matShader.setMaterial(Material.ROUGH, isFixed ? Color4f.GREY : Color4f.WHITE);
            }

            { // draw cube
                gl.scale(realLength / 2, realWidth / 2, HEIGHT); // half below ground
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
        if (isFixed) {
            Logger.ERROR.print("Tried changing state of a fixed station");
            return;
        }

        this.orientation = orientation;
    }

    @Override
    public void setPosition(Vector3fc position) {
        if (isFixed) {
            Logger.ERROR.print("Tried changing state of a fixed station");
            return;
        }

        super.setPosition(position);
    }

    @Override
    public String toString() {
        return className + " : " + stationName;
    }

    @Override
    public void onClick(int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            game.gui().addFrame(new StationUI());
        }
    }

    public int getPlatformCapacity() {
        return platformCapacity;
    }

    public int getNumberOfPlatforms() {
        return numberOfPlatforms;
    }

    protected class StationUI extends SFrame {
        StationUI() {
            super(StationImpl.this.toString(), 500, 300);

            // add buttons etc.
        }
    }

}
