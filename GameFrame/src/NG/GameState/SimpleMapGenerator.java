package NG.GameState;

import NG.Tools.Toolbox;
import org.joml.SimplexNoise;

/**
 * @author Geert van Ieperen. Created on 27-9-2018.
 */
public class SimpleMapGenerator implements HeightMapGenerator {
    // primary is the most detail
    private static final float PRIMARY_DENSITY = 0.01f;
    private static final float PRIMARY_INTENSITY = 3f;
    private static final float SECONDARY_DENSITY = 0.1f;
    private static final float SECONDARY_INTENSITY = 0.1f;
    private float edgeLength;
    private float progress = 0;
    private int seed;
    private int width;
    private int height;

    public SimpleMapGenerator() {
        this(Math.abs(Toolbox.random.nextInt()));
    }

    public SimpleMapGenerator(int seed) {
        this.seed = seed;
        this.edgeLength = 1;
    }

    @Override
    public float[][] generateHeightMap() {
        float[][] map = new float[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float xReal = x / edgeLength;
                float yReal = y / edgeLength;

                float primary = PRIMARY_INTENSITY * SimplexNoise.noise(xReal * PRIMARY_DENSITY, yReal * PRIMARY_DENSITY);
                float secondary = SECONDARY_INTENSITY * SimplexNoise.noise(xReal * SECONDARY_DENSITY, yReal * SECONDARY_DENSITY);

                map[x][y] = primary + secondary;
            }
            progress = (float) width / x;
        }
        progress = 1;
        return map;
    }

    @Override
    public void setEdgeLength(float edgeLength) {
        this.edgeLength = edgeLength;
    }

    @Override
    public float heightmapProgress() {
        return progress;
    }

    @Override
    public int getMapSeed() {
        return seed;
    }

    @Override
    public void setXSize(int xSize) {
        this.width = xSize;
    }

    @Override
    public void setYSize(int ySize) {
        this.height = ySize;
    }
}
