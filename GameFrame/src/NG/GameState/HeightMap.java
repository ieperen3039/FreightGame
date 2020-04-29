package NG.GameState;

import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.GUIMenu.Components.SFiller;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Components.SPanel;
import NG.GUIMenu.Components.SProgressBar;
import NG.InputHandling.MouseTools.MouseTool;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.FlatMesh;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Resources.Resource;
import NG.Tools.Toolbox;
import NG.Tools.Vectors;
import org.joml.Intersectionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Geert van Ieperen. Created on 27-9-2018.
 */
public class HeightMap implements GameMap {
    private static final int MESH_SIZE_UPPER_BOUND = 100;
    private final List<ChangeListener> listeners = new ArrayList<>();
    private Game game;

    private float[][] heightmap;
    private final Collection<Resource<Mesh>> meshOfTheWorld = new CopyOnWriteArrayList<>();

    private int edgeLength;
    private float meshProgress;

    @Override
    public void init(Game game) {
        this.game = game;
    }

    @Override
    public void generateNew(MapGeneratorMod mapGenerator) {
        SFrame frame = new SFrame("Generating map...", 500, 200);
        SPanel panel = new SPanel();
        panel.add(new SFiller(), SPanel.NORTHEAST);
        panel.add(new SProgressBar(400, 50, () -> (mapGenerator.heightmapProgress() + meshProgress) / 2), SPanel.MIDDLE);
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

                meshOfTheWorld.add(
                        FlatMesh.meshFromHeightmap(heightmap, xStart, xEnd, yStart, yEnd, edgeLength)
                );

                meshProgress += meshPStep;
            }
        }
        game.lights().addDirectionalLight(new Vector3f(1, 1, 2), Color4f.WHITE, 0.5f);

        listeners.forEach(ChangeListener::onMapChange);
        meshProgress = 1f;
    }

    @Override
    public void draw(SGL gl) {
        ShaderProgram shader = gl.getShader();
        if (shader instanceof MaterialShader) {
            MaterialShader matShader = (MaterialShader) shader;
            matShader.setMaterial(Material.ROUGH, new Color4f(0, 0.5f, 0));
        }

        meshOfTheWorld.forEach(object -> gl.render(object.get(), null));
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
        meshOfTheWorld.clear();
        listeners.clear();
    }

    @Override
    public boolean checkMouse(MouseTool tool, int xSc, int ySc) {
        if (heightmap == null) return false;

        Vector3f origin = new Vector3f();
        Vector3f direction = new Vector3f();

        Vectors.windowCoordToRay(game, xSc, ySc, origin, direction);

        Vector3f pos = intersectWithRay(origin, direction);
        pos.z = getHeightAt(pos.x, pos.y);

        tool.apply(pos, xSc, ySc);

        return true;
    }

    @Override
    public Vector3f intersectWithRay(Vector3fc origin, Vector3fc direction) {
        // TODO more precise calculation
        Vector3f temp = new Vector3f();
        float t = Intersectionf.intersectRayPlane(origin, direction, Vectors.O, Vectors.Z, 1E-6f);
        return origin.add(direction.mul(t, temp), temp);
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }
}
