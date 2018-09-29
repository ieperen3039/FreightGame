package NG.GameState;

import NG.DataStructures.MatrixStack.Mesh;
import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.Game;
import NG.Engine.Version;
import NG.Entities.Entity;
import NG.Mods.MapGeneratorMod;
import NG.Rendering.Shapes.FlatMesh;
import NG.Tools.Toolbox;
import org.joml.Vector2fc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Geert van Ieperen. Created on 27-9-2018.
 */
public abstract class HeightMapGeneratorMod implements MapGeneratorMod {
    private static final int MESH_SIZE_UPPER_BOUND = 100;
    private float[][] heightmap;
    private Collection<Mesh> meshOfTheWorld = null;
    private Collection<Supplier<? extends Mesh>> preparedMeshes;
    private float meshProgress = 0;
    private Set<Entity> worldAsEntitySet;

    @Override
    public void init(Game game) {
        worldAsEntitySet = Collections.singleton(new WorldEntity());
        preparedMeshes = new ArrayList<>();
    }

    @Override
    public Iterable<Entity> getWorldEntities() {
        return worldAsEntitySet;
    }

    public void generateMeshes() {
        meshOfTheWorld = new ArrayList<>(preparedMeshes.size());
        for (Supplier<? extends Mesh> preparedMesh : preparedMeshes) {
            Mesh mesh = preparedMesh.get();
            meshOfTheWorld.add(mesh);
        }
        preparedMeshes.clear();
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void generateNew(int seed, int width, int height) {
        meshProgress = 0f;
        int xSize = width * getEdgeLength();
        int ySize = height * getEdgeLength();
        int meshPStep = 1 / (xSize * ySize);

        // height map generation
        heightmap = generateHeightMap(xSize + 1, ySize + 1);

        // make mesh size divisible by both x and y
        int adaptedMeshSize = Toolbox.gcd(xSize, Toolbox.gcd(ySize, MESH_SIZE_UPPER_BOUND));

        for (int xStart = 0; xStart < xSize; xStart += adaptedMeshSize) {
            for (int yStart = 0; yStart < ySize; yStart += adaptedMeshSize) {
                preparedMeshes.add(
                        FlatMesh.meshFromHeightmap(heightmap, xStart, xStart + adaptedMeshSize, yStart, yStart + adaptedMeshSize, getEdgeLength())
                );

                meshProgress += meshPStep;
            }
        }

        meshProgress = 1f;
    }

    @Override
    public float getHeightAt(Vector2fc position) {
        float xFloat = position.x() * getEdgeLength();
        float yFloat = position.y() * getEdgeLength();
        int xMin = (int) xFloat;
        int yMin = (int) yFloat;
        float xFrac = xFloat - xMin;
        float yFrac = yFloat - yMin;

        float a = heightmap[xMin][yMin];
        float b = heightmap[xMin + 1][yMin];
        float c = heightmap[xMin][yMin + 1];
        float d = heightmap[xMin + 1][yMin + 1];

        float smallerXHeight = Toolbox.interpolate(a, b, xFrac);
        float largerXHeight = Toolbox.interpolate(c, d, xFrac);

        return Toolbox.interpolate(smallerXHeight, largerXHeight, yFrac);
    }

    @Override
    public float getProgress() {
        return (heightmapProgress() + meshProgress) / 2;
    }

    /**
     * @return the real distance between two vertices in the height map
     */
    protected abstract int getEdgeLength();

    /**
     * generate a heightmap which will be used to render the world.
     * @param xSize the number of indices that the first argument of the array must have. This has taken the result of
     *              {@link #getEdgeLength()} into account.
     * @param ySize the number of indices that the second argument of the array must have. This has taken the result of
     *              {@link #getEdgeLength()} into account.
     * @return the new heightmap, of size (xSize x ySize)
     */
    protected abstract float[][] generateHeightMap(int xSize, int ySize);

    /**
     * gives an indication of how far {@link #generateHeightMap(int, int)} is progressed.
     * @return a float [0, 1] indicating the generation progress. 0 indicates that it hasn't started, 1 indicates that
     *         it is done.
     */
    protected abstract float heightmapProgress();

    @Override
    public Version getVersionNumber() {
        return new Version(0, 0);
    }

    public class WorldEntity implements Entity {
        @Override
        public void update() {

        }

        @Override
        public void draw(SGL gl) {
            for (Mesh mesh : meshOfTheWorld) {
                gl.render(mesh);
                Toolbox.checkGLError();
            }
        }
    }
}
