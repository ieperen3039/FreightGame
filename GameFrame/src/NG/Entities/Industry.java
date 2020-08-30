package NG.Entities;

import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.Pair;
import NG.Freight.Cargo;
import NG.InputHandling.KeyControl;
import NG.InputHandling.MouseTools.AbstractMouseTool;
import NG.Mods.CargoType;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shapes.GenericShapes;
import org.joml.Vector3fc;

import java.util.*;

/**
 * @author Geert van Ieperen created on 21-6-2020.
 */
public class Industry extends Storage {
    private static final double CARGO_SPAWN_FREQUENCY = 0.2f; // updates per second
    public static final float BASE_CARGO_GENERATION_RATE = 1.0f; // units generated per second
    public static final CargoType DEBUG_CUBES = new CargoType("Debug Cubes", 100, 1);

    private final Properties properties;
    private double nextCargoSpawn;
    private Marking marking = null;

    public Industry(
            Game game, Vector3fc position, double spawnTime, Properties properties
    ) {
        super(game, position, spawnTime);
        this.nextCargoSpawn = game.timer().getGameTime();
        this.properties = properties;

    }

    public static Properties debugPropertiesGive() {
        return new Properties(
                Collections.emptySet(),
                Map.of(DEBUG_CUBES, BASE_CARGO_GENERATION_RATE),
                Collections.emptyMap(),
                Color4f.GREEN
        );
    }

    public static Properties debugPropertiesTake() {
        return new Properties(
                Set.of(DEBUG_CUBES),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Color4f.RED
        );
    }

    @Override
    public void update() {
        double now = game.timer().getGameTime();
        while (now > nextCargoSpawn) {
            Map<CargoType, Float> generatedCargo = properties.generatedCargo;
            for (CargoType type : generatedCargo.keySet()) {

                int chunkSize = (int) (generatedCargo.get(type) / CARGO_SPAWN_FREQUENCY);
                getContents().add(new Cargo(type, chunkSize, nextCargoSpawn, this));
            }

            nextCargoSpawn += 1.0 / CARGO_SPAWN_FREQUENCY;
        }
    }

    @Override
    public void draw(SGL gl) {
        MaterialShader.ifPresent(gl, mat -> mat.setMaterial(Material.ROUGH, properties.color));

        gl.pushMatrix();
        {
            gl.translate(getPosition());
            gl.translate(0, 0, 1);
            gl.render(GenericShapes.CUBE, this);
        }

        gl.popMatrix();
    }

    @Override
    public void reactMouse(AbstractMouseTool.MouseAction action, KeyControl keys) {

    }

    @Override
    public void setMarking(Marking marking) {
        this.marking = marking;
    }

    @Override
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.EVERY_TICK;
    }

    public Collection<CargoType> getAcceptedCargo() {
        return properties.acceptedCargo;
    }

    public static List<Industry> getNearbyIndustries(Game game, Vector3fc position, float range) {
        float rangeSq = range * range;
        List<Industry> nearby = new ArrayList<>();

        for (Entity entity : game.state().entities()) {
            if (entity instanceof Industry) {
                Industry industry = (Industry) entity;
                if (industry.getPosition().distanceSquared(position) < rangeSq) {
                    nearby.add(industry);
                }
            }
        }

        return nearby;
    }

    public static class Properties {
        public final Collection<CargoType> acceptedCargo;
        public final Map<CargoType, Float> generatedCargo;
        public final Map<CargoType, Pair<CargoType, Float>> convertedCargo;
        public final Color4f color; // debug

        public Properties(
                Collection<CargoType> acceptedCargo, Map<CargoType, Float> generatedCargo,
                Map<CargoType, Pair<CargoType, Float>> convertedCargo,
                Color4f color
        ) {
            this.acceptedCargo = acceptedCargo;
            this.generatedCargo = generatedCargo;
            this.convertedCargo = convertedCargo;
            this.color = color;
        }
    }
}
