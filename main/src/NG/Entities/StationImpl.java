package NG.Entities;

import NG.Core.Coloring;
import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.Pair;
import NG.DataStructures.Generic.PairList;
import NG.DataStructures.Valuta;
import NG.Freight.Cargo;
import NG.GUIMenu.Components.*;
import NG.InputHandling.ClickShader;
import NG.InputHandling.KeyControl;
import NG.InputHandling.MouseTool.AbstractMouseTool.MouseAction;
import NG.Menu.InGame.Build.TrainConstructionMenu;
import NG.Menu.Main.MainMenu;
import NG.Mods.CargoType;
import NG.Network.NetworkNode;
import NG.Network.RailNode;
import NG.Network.SpecialNetworkNode;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shapes.GenericShapes;
import NG.Rendering.Shapes.Shape;
import NG.Settings.Settings;
import NG.Tracks.StraightTrack;
import NG.Tracks.TrackPiece;
import NG.Tracks.TrackType;
import org.joml.Math;
import org.joml.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * A basic implementation of a station. There is likely no need for another station
 * @author Geert van Ieperen created on 27-1-2019.
 */
public class StationImpl extends Storage implements Station {
    public static final float PLATFORM_SIZE = 1.2f;
    public static final float HEIGHT = 0.1f;
    public static final float HEIGHT_BELOW_STATION = 2f;
    public static final float FLYING_CUBE_SIZE = 0.4f;
    private static int nr = 1;

    protected String stationName = "Station " + (nr++);

    private final float orientation;
    private final int numberOfPlatforms;
    private final float length;
    private final float realWidth;

    private final RailNode[] forwardConnections;
    private final RailNode[] backwardConnections;
    private final TrackPiece[] tracks;
    private final List<Pair<NetworkNode, Boolean>> nodes;
    private final List<Industry> industries = new ArrayList<>();
    // make sure only one of each type is added to this collection
    private final Set<CargoType> industryAcceptedCargo = new HashSet<>();
    private final AABBf hitbox;
    private final Coloring coloring = new Coloring(Color4f.WHITE);
    private final PairList<Shape, Matrix4fc> collisionShape = new PairList<>(1);
    private final List<Train> trains = new ArrayList<>();

    private final List<Runnable> trainArrivalListeners = new ArrayList<>();

    /**
     * create a fixed station
     * @param game              the game instance
     * @param numberOfPlatforms the number of platforms of this station
     * @param length            the length of each platform in meters
     * @param type              the type of track on the station
     * @param position          the position of the middle of the station
     * @param orientation       the horizontal angle of placement in radians from (1, 0, 0) counterclockwise
     * @param spawnTime         time where this entity spawns
     */
    public StationImpl(
            Game game, int numberOfPlatforms, int length, TrackType type, Vector3fc position, float orientation,
            double spawnTime
    ) {
        super(game, position, spawnTime);
        assert numberOfPlatforms > 0 : "created station with " + numberOfPlatforms + " platforms";
        this.game = game;
        this.numberOfPlatforms = numberOfPlatforms;
        this.length = length;
        this.realWidth = numberOfPlatforms * PLATFORM_SIZE;
        this.orientation = orientation;
        this.nodes = new ArrayList<>(numberOfPlatforms * 2);

        float trackHeight = HEIGHT + 0.1f;

        Vector3fc forward = new Vector3f(Math.cos(orientation), Math.sin(orientation), 0).normalize(length / 2f);
        Vector3fc toRight = new Vector3f(Math.sin(orientation), -Math.cos(orientation), 0).normalize(realWidth / 2f);
        Vector3fc AToB = new Vector3f(forward).normalize();
        Vector3fc BToA = new Vector3f(AToB).negate();

        forwardConnections = new RailNode[numberOfPlatforms];
        backwardConnections = new RailNode[numberOfPlatforms];
        tracks = new TrackPiece[numberOfPlatforms];

        // create nodes
        if (numberOfPlatforms > 1) {
            Vector3fc rightSkip = new Vector3f(toRight).normalize(PLATFORM_SIZE);

            Vector3f rightMiddle = new Vector3f(position)
                    .sub(toRight)
                    .add(rightSkip.x() / 2, rightSkip.y() / 2, trackHeight);

            Vector3f aPos = new Vector3f(rightMiddle).sub(forward);
            Vector3f bPos = rightMiddle.add(forward);

            for (int i = 0; i < numberOfPlatforms; i++) {
                createNodes(type, AToB, BToA, aPos, bPos, i);

                bPos.add(rightSkip);
                aPos.add(rightSkip);
            }

        } else { // simplified version of above
            Vector3f bPos = new Vector3f(position).add(forward).add(0, 0, trackHeight);
            Vector3f aPos = new Vector3f(position).sub(forward).add(0, 0, trackHeight);

            createNodes(type, AToB, BToA, aPos, bPos, 0);
        }

        // create tracks
        for (int i = 0; i < numberOfPlatforms; i++) {
            RailNode A = forwardConnections[i];
            RailNode B = backwardConnections[i];

            TrackPiece trackConnection = new StraightTrack(game, type, A, B, false);
            NetworkNode.addConnection(trackConnection);
            game.state().addEntity(trackConnection);
            tracks[i] = trackConnection;
        }

        hitbox = new AABBf();
        Vector3f point = new Vector3f();
        hitbox.union(point.set(position).add(forward).add(toRight));
        hitbox.union(point.set(position).add(forward).sub(toRight));
        hitbox.union(point.set(position).sub(forward).add(toRight));
        hitbox.union(point.set(position).sub(forward).sub(toRight));
        hitbox.minZ = position.z() - HEIGHT_BELOW_STATION;
        hitbox.maxZ = position.z() + HEIGHT;

        Matrix4f transformation = new Matrix4f();
        transformation.translate(getPosition());
        transformation.rotateZ(orientation);

        transformation.scale(length / 2f, realWidth / 2, HEIGHT); // half below ground
        collisionShape.add(GenericShapes.CUBE, transformation);

        recalculateNearbyIndustries();
    }

    private void createNodes(TrackType type, Vector3fc AToB, Vector3fc BToA, Vector3f aPos, Vector3f bPos, int index) {
        assert AToB.lengthSquared() > 0 : AToB;
        assert BToA.lengthSquared() > 0 : BToA;

        // both nodes have direction to inside the station
        SpecialNetworkNode ANode = new SpecialNetworkNode(this);
        forwardConnections[index] = new RailNode(game, aPos, type, AToB, ANode);
        nodes.add(new Pair<>(ANode, false));

        SpecialNetworkNode BNode = new SpecialNetworkNode(this);
        backwardConnections[index] = new RailNode(game, bPos, type, BToA, BNode);
        nodes.add(new Pair<>(BNode, false));
    }

    @Override
    public void update() {
    }

    @Override
    public void draw(SGL gl) {
        gl.pushMatrix();
        {
            gl.translate(getPosition());
            gl.rotateXYZ(0, 0, orientation);

            MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.YELLOW));
            gl.pushMatrix();
            {
                gl.translate(0, 0, 2);
                gl.scale(FLYING_CUBE_SIZE, FLYING_CUBE_SIZE, FLYING_CUBE_SIZE);
                gl.render(GenericShapes.CUBE, this);
            }
            gl.popMatrix();

            MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.GREY));
            gl.scale(length / 2, realWidth / 2, HEIGHT / 2);
            gl.render(GenericShapes.CUBE, this);

            boolean isClickShader = gl.getShader() instanceof ClickShader;
            if (!isClickShader) {
                gl.translate(0, 0, -1f);
                gl.scale(1, 1, HEIGHT_BELOW_STATION / HEIGHT);
                gl.translate(0, 0, -1f);
                Color4f color = coloring.getColor();
                MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, color));
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
    public void reactMouse(MouseAction action, KeyControl keys) {
        if (action == MouseAction.PRESS_ACTIVATE) {
            game.gui().addFrame(new StationUI());
        }
    }

    public void setMarking(Coloring.Marking mark) {
        coloring.addMark(mark);
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

    @Override
    public List<TrackPiece> getTracks() {
        return List.of(tracks);
    }

    public List<RailNode> getNodesOfDirection(Vector3fc direction) {
        Vector3f stationDirection = new Vector3f(Math.cos(orientation), Math.sin(orientation), 0);
        if (stationDirection.dot(direction) > 0) {
            return List.of(forwardConnections);

        } else {
            return List.of(backwardConnections);
        }
    }

    @Override
    public Map<CargoType, Integer> getAvailableCargo() {
        Map<CargoType, Integer> available = getContents().asMap();
        for (Industry industry : industries) {
            industry.getContents().addToMap(available);
        }
        return available;
    }

    @Override
    public boolean load(Train train, CargoType cargoType, int amount, boolean oldFirst) {
        assert amount > 0;
        float remainder = loadThis(train, cargoType, amount);

        if (remainder == 0) return true;

        for (Industry industry : industries) {
            remainder = industry.loadThis(train, cargoType, amount);
            if (remainder == 0) return true;
        }

        return false;
    }

    public void recalculateNearbyIndustries() {
        industries.clear();
        industries.addAll(Industry.getNearbyIndustries(game, getPosition(), Settings.STATION_RANGE));
        industryAcceptedCargo.clear();

        for (Industry industry : industries) {
            Collection<CargoType> acceptedCargo = industry.getAcceptedCargo();
            industryAcceptedCargo.addAll(acceptedCargo);
        }
    }

    @Override
    public Collection<CargoType> getAcceptedCargo() {
        return industryAcceptedCargo;
    }

    @Override
    public Valuta sell(Cargo cargo) {
        double now = game.timer().getGameTime();
        Valuta sellValue = cargo.value(now, this);
        // TODO effect of cargo on industries
        return sellValue;
    }

    @Override
    public void addTrain(Train train) {
        this.trains.add(train);
        trainArrivalListeners.forEach(Runnable::run);
    }

    public void addArrivalListener(Runnable onTrainArrival) {
        trainArrivalListeners.add(onTrainArrival);
    }

    public void removeArrivalListener(Runnable onTrainArrival) {
        trainArrivalListeners.remove(onTrainArrival);
    }

    @Override
    public AABBf getHitbox() {
        return hitbox;
    }

    @Override
    public PairList<Shape, Matrix4fc> getConvexCollisionShapes() {
        return collisionShape;
    }

    @Override
    public void forEachCorner(Consumer<Vector3fc> action) {
        Station.forEachCorner(getPosition(), length, orientation, realWidth, action);
    }

    protected class StationUI extends SFrame {
        private final SScrollableList trainList;
        private final Runnable updateTrainList;

        StationUI() {
            super(stationName, 300, 0);
            setMainPanel(SContainer.column(
                    new SActiveTextArea(() -> "Industries: " + industries, MainMenu.TEXT_PROPERTIES),
                    new SActiveTextArea(this::text, MainMenu.TEXT_PROPERTIES),
                    new SButton("Build Train", this::openTrainBuilder),
                    trainList = new SScrollableList(4)
            ));

            updateTrainList = () -> {
                trainList.clear();

                for (Train train : trains) {
                    SContainer trainComponent = SContainer.row(
                            new SButton(train.toString(), train::openUI, MainMenu.BUTTON_PROPERTIES_STRETCH),
                            new SButton(">", train::start, MainMenu.SQUARE_BUTTON_PROPS)
                    );
                    trainList.add(trainComponent, null);
                }
            };

            addArrivalListener(updateTrainList);
            updateTrainList.run();
        }

        private String text() {
            return "Cargo : " + getAvailableCargo();
        }

        private void openTrainBuilder() {
            Station place = StationImpl.this;
            TrainConstructionMenu frame = new TrainConstructionMenu(game, place);
            game.gui().addFrame(frame);
        }

        @Override
        public void dispose() {
            super.dispose();
            removeArrivalListener(updateTrainList);
        }
    }
}
