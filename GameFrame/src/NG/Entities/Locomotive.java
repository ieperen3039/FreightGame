package NG.Entities;

import NG.Rendering.MeshLoading.Mesh;
import NG.Resources.Resource;

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
        public Properties(String name, float length, float mass, Resource<Mesh> mesh) {
            super(name, length, mass, mesh);
        }
    }
}
