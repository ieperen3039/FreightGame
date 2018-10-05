package NG.GameState;

import NG.Engine.Game;
import NG.Engine.Version;
import NG.Tools.Toolbox;

/**
 * @author Geert van Ieperen. Created on 27-9-2018.
 */
public class FlatMapGenerator implements MapGeneratorMod {
    public static final int EDGDE_LENGTH = 1;
    private int progress = 0;
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
        return new Version(0, 0);
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
