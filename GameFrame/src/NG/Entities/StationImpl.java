package NG.Entities;

import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.Pair;
import NG.Freight.Cargo;
import NG.GUIMenu.Components.SActiveTextArea;
import NG.GUIMenu.Components.SContainer;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Menu.MainMenu;
import NG.GameState.Storage;
import NG.InputHandling.ClickShader;
import NG.InputHandling.MouseTools.AbstractMouseTool.MouseAction;
import NG.Mods.CargoType;
import NG.Network.NetworkNode;
import NG.Network.RailNode;
import NG.Network.SpecialNetworkNode;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shapes.GenericShapes;
import NG.Tools.Vectors;
import NG.Tracks.StraightTrack;
import NG.Tracks.TrackPiece;
import NG.Tracks.TrackType;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

import static NG.Tools.Vectors.cos;
import static NG.Tools.Vectors.sin;

/**
 * A basic implementation of a station. There is likely no need for another station
 * @author Geert van Ieperen created on 27-1-2019.
 */
public class StationImpl extends Storage implements Station {
    public static final CargoType DEBUG_CUBES = new CargoType("Debug Cubes", 100, 1);

    public static final float PLATFORM_SIZE = 1.2f;
    public static final float HEIGHT = 0.1f;
    private static int nr = 1;

    private static final double GOOD_SPAWN_RATE = 0.02; // chunks per second
    private double nextCargoSpawn;

    protected String stationName = "Station " + (nr++);

    private final float orientation;
    private final int numberOfPlatforms;
    private final float length;
    private final float realWidth;

    private final RailNode[] forwardConnections;
    private final RailNode[] backwardConnections;
    private final List<Pair<NetworkNode, Boolean>> nodes;

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
        this.nodes = new ArrayList<>(numberOfPlatforms * 2);
        this.nextCargoSpawn = game.timer().getGameTime();

        float trackHeight = HEIGHT + 0.1f;

        Vector3fc forward = new Vector3f(cos(orientation), sin(orientation), 0).normalize(length / 2f);
        Vector3f AToB = new Vector3f(forward).normalize();
        Vector3f BToA = new Vector3f(AToB).negate();

        forwardConnections = new RailNode[numberOfPlatforms];
        backwardConnections = new RailNode[numberOfPlatforms];

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
                createNodes(type, AToB, BToA, backPos, frontPos, i);

                frontPos.add(rightSkip);
                backPos.add(rightSkip);
            }

        } else { // simplified version of above
            Vector3f frontPos = new Vector3f(getPosition()).add(forward).add(0, 0, trackHeight);
            Vector3f backPos = new Vector3f(getPosition()).sub(forward).add(0, 0, trackHeight);

            createNodes(type, AToB, BToA, frontPos, backPos, 0);
        }

        // create tracks
        for (int i = 0; i < numberOfPlatforms; i++) {
            RailNode A = forwardConnections[i];
            RailNode B = backwardConnections[i];

            TrackPiece trackConnection = new StraightTrack(game, type, A, B, false);
            NetworkNode.addConnection(trackConnection);
            game.state().addEntity(trackConnection);
        }
    }

    private void createNodes(TrackType type, Vector3f AToB, Vector3f BToA, Vector3f aPos, Vector3f bPos, int index) {
        assert AToB.lengthSquared() > 0 : AToB;
        assert BToA.lengthSquared() > 0 : BToA;

        // both nodes have direction to inside the station
        SpecialNetworkNode ANode = new SpecialNetworkNode(this);
        forwardConnections[index] = new RailNode(aPos, type, AToB, ANode);
        nodes.add(new Pair<>(ANode, false));

        SpecialNetworkNode BNode = new SpecialNetworkNode(this);
        backwardConnections[index] = new RailNode(bPos, type, BToA, BNode);
        nodes.add(new Pair<>(BNode, false));
    }

    @Override
    public void update() {
        double now = game.timer().getGameTime();
        if (now > nextCargoSpawn) {
            contents.add(new Cargo(DEBUG_CUBES, 4, nextCargoSpawn, this));
            nextCargoSpawn += 1 / GOOD_SPAWN_RATE;
        }
    }

    @Override
    public void draw(SGL gl) {
        gl.pushMatrix();
        {
            gl.translate(getPosition());
            gl.rotate(Vectors.Z, orientation);

            MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.YELLOW));
            gl.pushMatrix();
            {
                gl.translate(0, 0, 2);
                gl.scale(0.2f, 0.2f, 0.2f);
                gl.render(GenericShapes.CUBE, this);
            }
            gl.popMatrix();

            MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.GREY));
            gl.scale(length / 2, realWidth / 2, HEIGHT / 2);
            gl.render(GenericShapes.CUBE, this);

            boolean isClickShader = gl.getShader() instanceof ClickShader;
            if (!isClickShader) {
                float sink = 2f; // size below ground
                gl.translate(0, 0, -1f);
                gl.scale(1, 1, sink / HEIGHT);
                gl.translate(0, 0, -1f);
                MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.BLACK));
                gl.render(GenericShapes.CUBE, this);
            }
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
    public List<Pair<NetworkNode, Boolean>> getNodes() {
        return nodes;
    }

    public List<RailNode> getNodesOfDirection(Vector3fc direction) {
        Vector3f stationDirection = new Vector3f(cos(orientation), sin(orientation), 0);
        if (stationDirection.dot(direction) > 0) {
            return List.of(forwardConnections);

        } else {
            return List.of(backwardConnections);
        }
    }

    protected class StationUI extends SFrame {
        StationUI() {
            super(stationName, 300, 0);
            setMainPanel(SContainer.column(
                    new SActiveTextArea(() -> String.valueOf(StationImpl.this.contents()), MainMenu.TEXT_PROPERTIES)
            ));
            // add buttons etc.
        }
    }

}
