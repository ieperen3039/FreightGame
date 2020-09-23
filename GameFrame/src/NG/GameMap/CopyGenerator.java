package NG.GameMap;

import NG.Core.Version;
import org.joml.Vector2ic;

import java.util.Collection;
import java.util.Collections;

/**
 * Generates the heightmap back from the given map, by querying the heights.
 * @author Geert van Ieperen created on 17-2-2019.
 */
public class CopyGenerator implements MapGeneratorMod {
    private final GridMap target;
    private int xSize = -1;
    private int ySize = -1;
    protected int progress = 0;

    public CopyGenerator(GridMap target) {
        this.target = target;
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
        progress = 0;
        Vector2ic size = target.getCoordinateSize();
        if (xSize == -1) xSize = size.x();
        if (ySize == -1) ySize = size.y();

        float[][] floats = new float[xSize][ySize];

        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                floats[x][y] = target.getHeightAt(x, y);
                progress++;
            }
        }

        return floats;
    }

    @Override
    public float heightmapProgress() {
        return (float) progress / (xSize * ySize); // either progress is 0 or xSize and ySize are positive
    }

    @Override
    public void setXSize(int xSize) {
        this.xSize = xSize;
    }

    @Override
    public void setYSize(int ySize) {
        this.ySize = ySize;
    }

    @Override
    public Version getVersionNumber() {
        return new Version(0, 0);
    }

    @Override
    public void cleanup() {
//        target.cleanup();
    }
}
