package NG.GameState;

import NG.DataStructures.MatrixStack.Mesh;
import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.Game;
import NG.Rendering.Shapes.FlatMesh;
import NG.ScreenOverlay.Frames.Components.SFiller;
import NG.ScreenOverlay.Frames.Components.SFrame;
import NG.ScreenOverlay.Frames.Components.SProgressBar;
import NG.Tools.Toolbox;
import org.joml.Vector2fc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import static NG.ScreenOverlay.Frames.Components.SContainer.*;

/**
 * @author Geert van Ieperen. Created on 27-9-2018.
 */
public class HeightMap implements GameMap {
    private static final int MESH_SIZE_UPPER_BOUND = 100;

    private Game game;
    private float[][] heightmap;

    private Collection<Mesh> meshOfTheWorld = null;
    private int edgeLength;

    private Collection<Supplier<? extends Mesh>> preparedMeshes = new ArrayList<>();
    private float meshProgress;

    @Override
    public void init(Game game) throws Exception {
        this.game = game;
    }

    @Override
    public void generateNew(MapGeneratorMod mapGenerator) {
        SFrame frame = new SFrame("Generating map...", 500, 200);
        frame.add(new SFiller(), NORTHEAST);
        frame.add(new SProgressBar(400, 50, () -> (mapGenerator.heightmapProgress() / 2 + meshProgress)), MIDDLE);
        frame.add(new SFiller(), SOUTHWEST);
        frame.setVisible(true);
        game.gui().addFrame(frame);

        new Thread(() -> {
            generate(mapGenerator);
            frame.dispose();
        }, "Map generator thread").start();
    }

    private void generate(MapGeneratorMod mapGenerator) {
        meshProgress = 0f;

        // height map generation
        heightmap = mapGenerator.generateHeightMap();

        int xSize = heightmap.length;
        int ySize = heightmap[0].length;
        float meshPStep = 1f / (xSize * ySize);

        // make mesh size divisible by both x and y
        int adaptedMeshSize = Toolbox.gcd(xSize, Toolbox.gcd(ySize, MESH_SIZE_UPPER_BOUND));

        for (int xStart = 0; xStart < xSize; xStart += adaptedMeshSize) {
            for (int yStart = 0; yStart < ySize; yStart += adaptedMeshSize) {
                preparedMeshes.add(
                        FlatMesh.meshFromHeightmap(heightmap, xStart, xStart + adaptedMeshSize, yStart, yStart + adaptedMeshSize, edgeLength)
                );

                meshProgress += meshPStep;
            }
        }

        meshProgress = 1f;
    }

    private Collection<Mesh> generateMeshes() {
        Collection<Mesh> worlAsMeshes = new ArrayList<>(preparedMeshes.size());
        for (Supplier<? extends Mesh> preparedMesh : preparedMeshes) {
            Mesh mesh = preparedMesh.get();
            worlAsMeshes.add(mesh);
        }
        preparedMeshes.clear();

        return worlAsMeshes;
    }

    @Override
    public float getHeightAt(Vector2fc position) {
        float xFloat = position.x() * edgeLength;
        float yFloat = position.y() * edgeLength;
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
    public void draw(SGL gl) {
        if (meshOfTheWorld == null) {
            meshOfTheWorld = generateMeshes();
        }

        meshOfTheWorld.forEach(gl::render);
    }

    @Override
    public void cleanup() {

    }
}
