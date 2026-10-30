package io.github.ieperen3039.trainsinspace.Entities;

import io.github.ieperen3039.trainsinspace.Core.AbstractGameObject;
import io.github.ieperen3039.trainsinspace.Core.Coloring;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.DataStructures.Generic.Color4f;
import io.github.ieperen3039.trainsinspace.DataStructures.Generic.Pair;
import io.github.ieperen3039.trainsinspace.DataStructures.Generic.PairList;
import io.github.ieperen3039.trainsinspace.DataStructures.Valuta;
import io.github.ieperen3039.trainsinspace.Freight.Cargo;
import io.github.ieperen3039.trainsinspace.InputHandling.ClickShader;
import io.github.ieperen3039.trainsinspace.InputHandling.KeyControl;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.AbstractMouseTool.MouseAction;
import io.github.ieperen3039.trainsinspace.Mods.CargoType;
import io.github.ieperen3039.trainsinspace.Network.NetworkNode;
import io.github.ieperen3039.trainsinspace.Rendering.Material;
import io.github.ieperen3039.trainsinspace.Rendering.MatrixStack.SGL;
import io.github.ieperen3039.trainsinspace.Rendering.Shaders.MaterialShader;
import io.github.ieperen3039.trainsinspace.Rendering.Shaders.ShaderProgram;
import io.github.ieperen3039.trainsinspace.Rendering.Shapes.GenericShapes;
import io.github.ieperen3039.trainsinspace.Rendering.Shapes.Shape;
import io.github.ieperen3039.trainsinspace.Tools.Vectors;
import io.github.ieperen3039.trainsinspace.Tracks.TrackPiece;
import io.github.ieperen3039.trainsinspace.Tracks.TrackType;
import org.joml.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static io.github.ieperen3039.trainsinspace.Entities.StationImpl.HEIGHT_BELOW_STATION;
import static io.github.ieperen3039.trainsinspace.Entities.StationImpl.PLATFORM_SIZE;

/**
 * A basic implementation of a station. There is likely no need for another station
 * @author Geert van Ieperen created on 27-1-2019.
 */
public class StationGhost extends AbstractGameObject implements Station {
    public static final float HEIGHT = 0.1f;
    public static final Color4f STATION_COLOR = new Color4f(1.0f, 1.0f, 1.0f, 0.5f);

    private Vector3f position = new Vector3f();
    private float orientation = 0;

    protected double despawnTime = Double.POSITIVE_INFINITY;

    private int numberOfPlatforms;
    private float length;
    private float realWidth;
    private final Coloring coloring = new Coloring(STATION_COLOR);

    public StationGhost(Game game, int nrOfPlatforms, int length) {
        super(game);
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
            gl.translate(position);
            gl.rotate(Vectors.Z, orientation);

            ShaderProgram shader = gl.getShader();
            if (shader instanceof MaterialShader) {
                MaterialShader matShader = (MaterialShader) shader;
                Color4f color = coloring.getColor();
                matShader.setMaterial(Material.ROUGH, color);
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
        this.position.set(position);
    }

    @Override
    public void reactMouse(MouseAction action, KeyControl keys) {
        // handled by StationBuilder
    }

    public void setMarking(Coloring.Marking mark) {
        coloring.addMark(mark);
    }

    @Override
    public void despawn(double gameTime) {
        despawnTime = gameTime;
    }

    public float getLength() {
        return length;
    }

    public int getNumberOfPlatforms() {
        return numberOfPlatforms;
    }

    @Override
    public List<Pair<NetworkNode, Boolean>> getNodes() {
        return Collections.emptyList();
    }

    @Override
    public List<TrackPiece> getTracks() {
        return Collections.emptyList();
    }

    @Override
    public boolean load(Train train, CargoType cargo, int amount, boolean oldFirst) {
        return false;
    }

    @Override
    public Collection<CargoType> getAcceptedCargo() {
        return Collections.emptySet();
    }

    @Override
    public Valuta sell(Cargo cargo) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addTrain(Train train) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<CargoType, Integer> getAvailableCargo() {
        return Collections.emptyMap();
    }

    @Override
    public Vector3fc getPosition() {
        return position;
    }

    public Station solidify(Game game, TrackType trackType, double gameTime) {
        return new StationImpl(game,
                numberOfPlatforms, (int) length, trackType, position, orientation, gameTime
        );
    }

    @Override
    public AABBf getHitbox() {
        AABBf hitbox = new AABBf();
        forEachCorner(hitbox::union);
        hitbox.minZ = position.z() - HEIGHT_BELOW_STATION;
        hitbox.maxZ = position.z() + HEIGHT;
        return hitbox;
    }

    @Override
    public void forEachCorner(Consumer<Vector3fc> action) {
        Station.forEachCorner(position, length, orientation, realWidth, action);
    }

    @Override
    public PairList<Shape, Matrix4fc> getConvexCollisionShapes() {
        Matrix4f transformation = new Matrix4f();
        transformation.translate(position);
        transformation.rotateZ(orientation);

        transformation.scale(length / 2, realWidth / 2, HEIGHT); // half below ground
        PairList<Shape, Matrix4fc> pairs = new PairList<>(1);
        pairs.add(GenericShapes.CUBE, transformation);
        return pairs;
    }

    @Override
    public void restoreFields(Game game) {

    }
}
