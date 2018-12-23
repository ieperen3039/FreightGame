package NG.GameState;

import NG.ActionHandling.MouseTools.MouseTool;
import NG.Camera.Camera;
import NG.DataStructures.Color4f;
import NG.DataStructures.Material;
import NG.DataStructures.MatrixStack.Mesh;
import NG.DataStructures.MatrixStack.SGL;
import NG.Engine.Game;
import NG.Rendering.GLFWWindow;
import NG.Rendering.Shapes.FlatMesh;
import NG.ScreenOverlay.Frames.Components.SFiller;
import NG.ScreenOverlay.Frames.Components.SFrame;
import NG.ScreenOverlay.Frames.Components.SPanel;
import NG.ScreenOverlay.Frames.Components.SProgressBar;
import NG.Settings.Settings;
import NG.Tools.Toolbox;
import NG.Tools.Vectors;
import org.joml.*;

import java.lang.Math;
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
    public void draw(SGL gl) {
        if (hasNewWorld) {
            meshOfTheWorld.clear();
            Collection<Mesh> meshes = generateMeshes();
            meshOfTheWorld.addAll(meshes);
            hasNewWorld = false;
        }

        gl.setMaterial(Material.ROUGH, new Color4f(0, 0.5f, 0));
        meshOfTheWorld.forEach(gl::render);
    }

    @Override
    public float getHeightAt(float x, float y) {
        // ASSUMPTION: map (0, 0) is on heightmap [0, 0]
        float xFloat = x / edgeLength;
        float yFloat = y / edgeLength;
        int xMin = (int) xFloat;
        int yMin = (int) yFloat;

        int xSize = heightmap.length;
        int ySize = heightmap[0].length;
        if (xMin < 0 || yMin < 0 || xMin > xSize - 2 || yMin > ySize - 2) {
//            Logger.ASSERT.printf(
//                    "Coord out of bounds: (%1.3f, %1.3f) -> [%d, %d] (size is %d by %d)",
//                    x, y, xMin + 1, yMin + 1, xSize, ySize
//            );
            return 0;
        }

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
    public void cleanup() {
        heightmap = null;
    }

    @Override
    public boolean checkMouseClick(MouseTool tool, int xSc, int ySc) {
        GLFWWindow window = game.window();
        Camera camera = game.camera();

        int width = window.getWidth();
        int height = window.getHeight();
        Matrix4f projection = SGL.getViewProjection(width, height, camera, Settings.ISOMETRIC_VIEW);

        Vector3f origin = new Vector3f();
        Vector3f direction = new Vector3f();
        int[] viewport = {0, 0, width, height};
        projection.unprojectRay(new Vector2f(xSc, ySc), viewport, origin, direction);

        Vector3f pos = intersectWithRay(origin, direction);

        tool.apply(new Vector2f(pos.x, pos.y));

        return true;
    }

    /**
     * returns a vector on the map that results from raytracing the given ray.
     * @param origin    the origin of the ray
     * @param direction the (un-normalized) direction of the ray
     * @return a vector p such that {@code p = origin + t * direction} for minimal t such that p lies on the map.
     */
    private Vector3f intersectWithRay(Vector3fc origin, Vector3fc direction) {
        Vector3f temp = new Vector3f();
        float t = Intersectionf.intersectRayPlane(origin, direction, Vectors.zeroVector(), Vectors.zVector(), 1E-6f);
        return origin.add(direction.mul(t, temp), temp);
    }
}
