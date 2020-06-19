package NG.GameMap;

import NG.Core.Version;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Geert van Ieperen created on 6-3-2019.
 */
public class FixedMapGenerator implements MapGeneratorMod {
    private final float[][] map;

    public FixedMapGenerator(float[]... map) {
        this.map = map;
    }

    @Override
    public Collection<Property> getProperties() {
        return Collections.emptySet();
    }

    @Override
    public void setProperty(String name, float value) {
        throw new IllegalArgumentException("This generator has no properties");
    }

    @Override
    public float[][] generateHeightMap() {
        return map;
    }

    @Override
    public int getMapSeed() {
        return 0;
    }

    @Override
    public float heightmapProgress() {
        return 1;
    }

    @Override
    public void setXSize(int xSize) {
        throw new UnsupportedOperationException("map is fixed size");
    }

    @Override
    public void setYSize(int ySize) {
        throw new UnsupportedOperationException("map is fixed size");
    }

    @Override
    public Version getVersionNumber() {
        return new Version(0, 0);
    }

    @Override
    public void cleanup() {

    }
}
