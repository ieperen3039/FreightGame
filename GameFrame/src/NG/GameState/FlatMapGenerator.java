package NG.GameState;

import NG.Core.Game;
import NG.Core.Version;

/**
 * @author Geert van Ieperen created on 28-4-2020.
 */
public class FlatMapGenerator implements MapGeneratorMod {
    private static final int EDGDE_LENGTH = 1;
    private int xSize;
    private int ySize;

    @Override
    public float[][] generateHeightMap() {
        return new float[xSize][ySize];
    }

    @Override
    public int getEdgeLength() {
        return EDGDE_LENGTH;
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
        this.xSize = xSize;
    }

    @Override
    public void setYSize(int ySize) {
        this.ySize = ySize;
    }

    @Override
    public void init(Game game) throws Version.MisMatchException {

    }

    @Override
    public void cleanup() {

    }

    @Override
    public Version getVersionNumber() {
        return new Version(0, 1);
    }
}
