package NG.Entities;

import NG.Rendering.MeshLoading.Mesh;
import NG.Resources.Resource;

import java.util.List;

/**
 * @author Geert van Ieperen created on 20-5-2020.
 */
public class Wagon implements TrainElement {
    public final Properties properties;

    public Wagon(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    public static class Properties extends TrainElement.Properties {
        private final float maxSpeed;

        public Properties(
                String name, float length, float mass, float linearResistance,
                Resource<Mesh> mesh, List<String> trackTypes, float maxSpeed
        ) {
            super(name, length, mass, linearResistance, 0, mesh, trackTypes);
            this.maxSpeed = maxSpeed;
        }
    }
}
