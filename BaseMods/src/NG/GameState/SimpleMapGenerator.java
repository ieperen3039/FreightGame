package NG.GameState;

import NG.Core.Game;
import NG.Core.Version;
import NG.Tools.Toolbox;
import org.joml.SimplexNoise;

/**
 * @author Geert van Ieperen. Created on 27-9-2018.
 */
public class SimpleMapGenerator implements MapGeneratorMod {
    public static final int EDGDE_LENGTH = 1;
    private static final float PRIMARY_DENSITY = 50f;
    private static final float SECONDARY_DENSITY = 20f;
    private float progress = 0;
    private int seed;
    private int width;
    private int height;

    @Override
    public void init(Game game) throws Version.MisMatchException {
        seed = Math.abs(Toolbox.random.nextInt());
    }

    @Override
    public float[][] generateHeightMap() {
        float[][] map = new float[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map[x][y] = SimplexNoise.noise(x / PRIMARY_DENSITY, y / PRIMARY_DENSITY);
                map[x][y] += 0.1f * SimplexNoise.noise(x / SECONDARY_DENSITY, y / SECONDARY_DENSITY);
            }
            progress = (float) width / x;
        }
        progress = 1;
        return map;
    }

    @Override
    public float heightmapProgress() {
        return progress;
    }

    @Override
    public void cleanup() {

    }

    @Override
    public int getEdgeLength() {
        return EDGDE_LENGTH;
    }

    @Override
    public int getMapSeed() {
        return seed;
    }

    @Override
    public Version getVersionNumber() {
        return new Version(0, 1);
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
