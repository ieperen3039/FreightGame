package NG.Entities;

import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.Pair;
import NG.Freight.Cargo;
import NG.Mods.CargoType;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Resources.Resource;
import NG.Tracks.TrackType;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Geert van Ieperen created on 19-5-2020.
 */
public interface TrainElement {
    default void draw(
            SGL gl, Vector3fc position, Quaternionfc rotation, Entity sourceEntity, Color4f color
    ) {
        MaterialShader.ifPresent(gl, mat -> {
            mat.setMaterial(Material.METAL, color);
        });

        gl.pushMatrix();
        {
            gl.translate(position);
            gl.rotate(rotation);

            gl.render(getProperties().mesh.get(), sourceEntity);
        }
        gl.popMatrix();
    }

    Properties getProperties();

    Map<CargoType, Integer> getCargoTypes();

    CargoType getCurrentCargoType();

    Pair<CargoType, Integer> getContents();

    Collection<Cargo> getContentElements();

    Collection<Cargo> take(int amount);

    Collection<Cargo> takeAll();

    /**
     * adds the given cargo to this train element
     * @param cargo the cargo to load
     * @throws IllegalArgumentException if the cargo cannot be added to this element
     */
    void addContents(Cargo cargo) throws IllegalArgumentException;

    default int getStorableAmount(CargoType type) {
        Pair<CargoType, Integer> contents = getContents();

        if (contents.right == 0) {
            Map<CargoType, Integer> capacity = getCargoTypes();
            //noinspection Java8MapApi
            return capacity.containsKey(type) ? capacity.get(type) : 0;

        } else if (contents.left != type) {
            return 0;

        } else {
            Integer capacity = getCargoTypes().get(type);
            return capacity - contents.right;
        }
    }

    double getLoadTime(Cargo cargo);

    class Properties {
        public final String name;
        public final float length;
        public final float mass;
        public final float linearResistance;
        public final float quadraticResistance;
        public final float maxSpeed;

        public final Resource<Mesh> mesh;
        public int buildCost;
        public float maintenancePerSecond;

        private final List<String> trackTypes;

        public Properties(
                String name, float length, float mass, float linearResistance, float quadraticResistance,
                float maxSpeed, Resource<Mesh> mesh, List<String> trackTypes, int buildCost, float maintenancePerSecond
        ) {
            this.name = name;
            this.length = length;
            this.mass = mass;
            this.linearResistance = linearResistance;
            this.quadraticResistance = quadraticResistance;
            this.maxSpeed = maxSpeed;
            this.mesh = mesh;
            this.trackTypes = trackTypes;
            this.buildCost = buildCost;
            this.maintenancePerSecond = maintenancePerSecond;
        }

        @Override
        public String toString() {
            return name;
        }

        public boolean isCompatibleWith(TrackType type) {
            return trackTypes.contains(type.toString());
        }
    }
}
