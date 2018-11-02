package NG.GameState;

import NG.DataStructures.MatrixStack.Mesh;
import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.Game;
import NG.Rendering.Shapes.FlatMesh;
import NG.ScreenOverlay.Frames.Components.SFiller;
import NG.ScreenOverlay.Frames.Components.SFrame;
import NG.ScreenOverlay.Frames.Components.SPanel;
import NG.ScreenOverlay.Frames.Components.SProgressBar;
import NG.Tools.Toolbox;
import org.joml.Vector2fc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * @author Geert van Ieperen. Created on 27-9-2018.
 */
public class HeightMap implements GameMap {
    private static final int MESH_SIZE_UPPER_BOUND = 100;

    private Game game;
    private float[][] heightmap;
    private final Collection<Mesh> meshOfTheWorld = new CopyOnWriteArrayList<>();

    private int edgeLength;

    private Collection<Supplier<? extends Mesh>> preparedMeshes = new ArrayList<>();
    private float meshProgress;
    private boolean hasNewWorld;

    @Override
    public void init(Game game) {
        this.game = game;
    }

    @Override
    public void generateNew(MapGeneratorMod mapGenerator) {
        SFrame frame = new SFrame("Generating map...", 500, 200);
        SPanel panel = new SPanel();
        panel.add(new SFiller(), SPanel.NORTHEAST);
        panel.add(new SProgressBar(400, 50, () -> (mapGenerator.heightmapProgress() / 2 + meshProgress)), SPanel.MIDDLE);
        panel.add(new SFiller(), SPanel.SOUTHWEST);
        frame.setMainPanel(panel);
        frame.setVisible(true);
        game.gui().addFrame(frame);

        generate(mapGenerator);
        frame.dispose();
    }

    private void generate(MapGeneratorMod mapGenerator) {
        meshProgress = 0f;
        edgeLength = mapGenerator.getEdgeLength();

        // height map generation
        heightmap = mapGenerator.generateHeightMap();

        int xSize = heightmap.length;
        int ySize = heightmap[0].length;
        float meshPStep = 1f / (xSize * ySize);

        int adaptedMeshSize = MESH_SIZE_UPPER_BOUND;

        for (int xStart = 0; xStart < xSize; xStart += adaptedMeshSize) {
            for (int yStart = 0; yStart < ySize; yStart += adaptedMeshSize) {
                int xEnd = Math.min(xStart + adaptedMeshSize, xSize - 1);
                int yEnd = Math.min(yStart + adaptedMeshSize, xSize - 1);
                preparedMeshes.add(
                        FlatMesh.meshFromHeightmap(heightmap, xStart, xEnd, yStart, yEnd, edgeLength)
                );

                meshProgress += meshPStep;
            }
        }

        hasNewWorld = true;
        meshProgress = 1f;
    }

    private Collection<Mesh> generateMeshes() {
        assert hasNewWorld;

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
        if (hasNewWorld) {
            meshOfTheWorld.clear();
            Collection<Mesh> meshes = generateMeshes();
            meshOfTheWorld.addAll(meshes);
            hasNewWorld = false;
        }

        meshOfTheWorld.forEach(gl::render);
    }

    @Override
    public void cleanup() {

    }
}
