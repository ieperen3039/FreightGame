package NG.Entities;

import NG.Rendering.MeshLoading.Mesh;
import NG.Resources.Resource;

import java.util.List;

/**
 * @author Geert van Ieperen created on 5-5-2020.
 */
public class Locomotive implements TrainElement {
    public final Properties properties;

    public Locomotive(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    public static class Properties extends TrainElement.Properties {
        public final float tractiveEffort;

        public Properties(
                String name, float length, float mass, float linearResistance, float quadraticResistance,
                Resource<Mesh> mesh, List<String> trackTypes, float tractiveEffort
        ) {
            super(name, length, mass, linearResistance, quadraticResistance, 10, mesh, trackTypes);
            this.tractiveEffort = tractiveEffort;
        }
    }
}
