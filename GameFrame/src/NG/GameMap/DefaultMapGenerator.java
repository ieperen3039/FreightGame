package NG.GameMap;

import NG.Core.Version;
import NG.Tools.OpenSimplexNoise;

import java.util.Collection;
import java.util.Map;

/**
 * @author Geert van Ieperen. Created on 27-9-2018.
 */
public class DefaultMapGenerator implements MapGeneratorMod {
    public static final Version VERSION = new Version(0, 1);

    private static final double AMPLITUDE_SCALE_FACTOR = 0.1;
    private static final double MAJOR_DENSITY = 0.02;
    private static final double MINOR_DENSITY = 0.2;

    public static final String MAJOR_AMPLITUDE = "Major amplitude";
    public static final String MINOR_AMPLITUDE = "Minor amplitude";

    private float progress = 0;
    private int seed;
    private int width;
    private int height;
    private final OpenSimplexNoise majorGenerator;
    private final OpenSimplexNoise minorGenerator;

    private Map<String, Property> properties;

    public DefaultMapGenerator(int seed) {
        this.seed = seed;
        this.majorGenerator = new OpenSimplexNoise(seed);
        this.minorGenerator = new OpenSimplexNoise(seed + 1);
        this.properties = Map.of(
                MAJOR_AMPLITUDE, new Property(MAJOR_AMPLITUDE, 0, 100, 20),
                MINOR_AMPLITUDE, new Property(MINOR_AMPLITUDE, 0, 100, 1)
        );
    }

    @Override
    public Collection<Property> getProperties() {
        return properties.values();
    }

    @Override
    public void setProperty(String name, float value) {
        Property property = properties.get(name);
        if (property == null) throw new IllegalArgumentException("Invalid property " + name);
        if (value > property.maximum || value < property.minimum) {
            throw new IllegalArgumentException("Value out of range: " + value);
        }

        property.current = value;
    }

    @Override
    public float[][] generateHeightMap() {
        float[][] map = new float[width][height];

        double majorAmplitude = properties.get(MAJOR_AMPLITUDE).current * AMPLITUDE_SCALE_FACTOR;
        addNoiseLayer(map, majorGenerator, MAJOR_DENSITY, majorAmplitude);
        progress = 0.5f;

        double minorAmplitude = properties.get(MINOR_AMPLITUDE).current * AMPLITUDE_SCALE_FACTOR;
        addNoiseLayer(map, minorGenerator, MINOR_DENSITY, minorAmplitude);
        progress = 1;

        return map;
    }

    private void addNoiseLayer(
            float[][] map, OpenSimplexNoise noise, double density, double amplitude
    ) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map[x][y] += (float) (amplitude * noise.eval(x * density, y * density));
            }
        }
    }

    @Override
    public float heightmapProgress() {
        return progress;
    }

    @Override
    public void cleanup() {
    }

    @Override
    public Version getVersionNumber() {
        return VERSION;
    }

    @Override
    public void setXSize(int xSize) {
        this.width = xSize;
    }

    @Override
    public void setYSize(int ySize) {
        this.height = ySize;
    }

    @Override
    public String getModName() {
        return "Default Map Generator";
    }

    @Override
    public float getEdgeLength() {
        return 0.5f;
    }
}
