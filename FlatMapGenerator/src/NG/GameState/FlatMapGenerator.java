package NG.GameState;

/**
 * @author Geert van Ieperen. Created on 27-9-2018.
 */
public class FlatMapGenerator extends HeightMapGeneratorMod {
    public static final int EDGDE_LENGTH = 1;
    private int progress = 0;

    @Override
    protected float[][] generateHeightMap(int xSize, int ySize) {
        float[][] map = new float[xSize][ySize];
        progress = 1;
        return map;
    }

    @Override
    protected float heightmapProgress() {
        return progress;
    }

    @Override
    protected int getEdgeLength() {
        return EDGDE_LENGTH;
    }
}
