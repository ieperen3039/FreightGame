package NG.GameMap;

import NG.Core.Game;
import NG.DataStructures.Generic.AveragingQueue;
import NG.DataStructures.Generic.Color4f;
import NG.GUIMenu.Components.SFiller;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Components.SPanel;
import NG.GUIMenu.Components.SProgressBar;
import NG.Rendering.IntersectionTester;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.FlatMesh;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.BlinnPhongShader;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.AssetHandling.Asset;
import NG.Tools.Logger;
import NG.Tools.Toolbox;
import org.joml.Math;
import org.joml.*;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * a simple heightmap grid with (0, 0) mapping to (0, 0, map[0][0])
 * @author Geert van Ieperen created on 10-5-2020.
 */
public class HeightMap extends GridMap {
    /// the number of heightmap indices in each dimension that are used to create one chunk
    private static final int INDICES_PER_CHUNK = 100;

    private final List<Asset<Mesh>> chunkMeshes = new CopyOnWriteArrayList<>();
    private float meshProgress;

    private float[][] heightmap;
    private float edgeLength = 0.5f;
    private int xSize;
    private int ySize;

    private AveragingQueue culledChunks = new AveragingQueue(4);

    private final List<ChangeListener> listeners = new ArrayList<>();
    private float maxHeight = Float.POSITIVE_INFINITY;

    @Override
    Float getTileIntersect(Vector3fc origin, Vector3fc direction, int xCoord, int yCoord) {
        if (xCoord < 0 || yCoord < 0 || xCoord >= (xSize - 1) || yCoord >= (ySize - 1)) return null;

        Vector3f a = getPosition(xCoord, yCoord);
        Vector3f b = getPosition(xCoord + 1, yCoord);
        Vector3f c = getPosition(xCoord, yCoord + 1);
        Vector3f d = getPosition(xCoord + 1, yCoord + 1);

        AABBf boundingBox = new AABBf().union(a).union(b).union(c).union(d);
        boolean hits = Intersectionf.intersectRayAab(
                origin.x(), origin.y(), origin.z(),
                direction.x(), direction.y(), direction.z(),
                boundingBox.minX, boundingBox.minY, boundingBox.minZ,
                boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ + EPSILON,
                new Vector2f()
        );

        if (!hits) return Float.POSITIVE_INFINITY;

        float t1 = Intersectionf.intersectRayTriangle(origin, direction, a, b, c, EPSILON);

        if (t1 != -1.0f) {
            return t1;

        } else {
            float t2 = Intersectionf.intersectRayTriangle(origin, direction, b, c, d, EPSILON);

            if (t2 != -1.0f) {
                return t2;
            } else {
                return Float.POSITIVE_INFINITY;
            }
        }
    }

    @Override
    public void generateNew(Game game, MapGeneratorMod mapGenerator) {
        SFrame frame = new SFrame("Generating map...", 500, 200, false);
        SPanel panel = new SPanel();
        panel.add(new SFiller(), SPanel.NORTHEAST);
        panel.add(new SProgressBar(400, 50, () -> (mapGenerator.heightmapProgress() + meshProgress) / 2), SPanel.MIDDLE);
        panel.add(new SFiller(), SPanel.SOUTHWEST);
        frame.setMainPanel(panel);
        frame.setVisible(true);
        game.gui().addFrame(frame);

        generate(mapGenerator);

        frame.dispose();

        Logger.printOnline(() -> "Culled chenks :" + culledChunks.average() + "/" + chunkMeshes.size());
    }

    private void generate(MapGeneratorMod mapGenerator) {
        synchronized (this) {
            meshProgress = 0f;

            // height map generation
            heightmap = mapGenerator.generateHeightMap();

            edgeLength = mapGenerator.getEdgeLength();
            xSize = heightmap.length;
            ySize = heightmap[0].length;

            List<Asset<Mesh>> worldMeshes = generateMeshes(heightmap, xSize, ySize, edgeLength);
            meshProgress = 0.5f;

            chunkMeshes.clear();
            chunkMeshes.addAll(worldMeshes);

            for (int x = 0; x < xSize; x++) {
                for (int y = 0; y < ySize; y++) {
                    float h = heightmap[x][y];
                    if (h > maxHeight) maxHeight = h;
                }
            }

            listeners.forEach(ChangeListener::onMapChange);
            meshProgress = 1f;
        }
    }

    private static List<Asset<Mesh>> generateMeshes(float[][] heightmap, int xSize, int ySize, float edgeLength) {
        List<Asset<Mesh>> worldMeshes = new ArrayList<>();

        // note, y in outer loop: creating rows of x
        for (int yStart = 0; yStart < ySize; yStart += INDICES_PER_CHUNK) {
            for (int xStart = 0; xStart < xSize; xStart += INDICES_PER_CHUNK) {
                int xEnd = Math.min(xStart + INDICES_PER_CHUNK, xSize - 1);
                int yEnd = Math.min(yStart + INDICES_PER_CHUNK, xSize - 1);

                worldMeshes.add(
                        FlatMesh.meshFromHeightmap(heightmap, xStart, xEnd, yStart, yEnd, edgeLength)
                );
            }
        }

        return worldMeshes;
    }

    @Override
    public float getHeightAt(float x, float y) {
        // ASSUMPTION: map (0, 0) is on heightmap [0, 0]
        float xFloat = x / edgeLength;
        float yFloat = y / edgeLength;
        int xMin = (int) xFloat;
        int yMin = (int) yFloat;

        if (xMin < 0 || yMin < 0 || xMin > xSize - 2 || yMin > ySize - 2) {
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

        float lerpHeightValue = Toolbox.interpolate(smallerXHeight, largerXHeight, yFrac);
        return lerpHeightValue * edgeLength;
    }

    @Override
    public Vector2i getCoordinate(Vector3fc position) {
        return new Vector2i(
                (int) (position.x() / edgeLength + 0.5f),
                (int) (position.y() / edgeLength + 0.5f)
        );
    }

    @Override
    public Vector3f getPosition(int x, int y) {
        return new Vector3f(
                x * edgeLength,
                y * edgeLength,
                heightmap[x][y] * edgeLength
        );
    }

    @Override
    public Vector2f getCoordPosf(Vector3fc origin) {
        return new Vector2f(origin.x() / edgeLength, origin.y() / edgeLength);
    }

    @Override
    public Vector2f getCoordDirf(Vector3fc direction) {
        return new Vector2f(direction.x() / edgeLength, direction.y() / edgeLength);
    }

    @Override
    public void draw(SGL gl) {
        ShaderProgram shader = gl.getShader();

        if (shader instanceof MaterialShader) {
            MaterialShader mat = (MaterialShader) shader;
            mat.setMaterial(Material.ROUGH, new Color4f(0, 0.8f, 0));
        }
        if (shader instanceof BlinnPhongShader) {
            BlinnPhongShader bps = (BlinnPhongShader) shader;
            bps.setHeightLines(true);
        }

        Matrix4fc viewProjection = gl.getViewProjectionMatrix();
        IntersectionTester viewBoxTester;

        if (viewProjection.isAffine()) {
            viewBoxTester = (minX, minY, minZ, maxX, maxY, maxZ) -> true;

        } else {
            FrustumIntersection fi = new FrustumIntersection(viewProjection, false);
            viewBoxTester = fi::testAab;
        }

        float meshSize = INDICES_PER_CHUNK * edgeLength;
        int numXChunks = (int) Math.ceil((float) xSize / INDICES_PER_CHUNK);
        int numChunksCulled = 0;

        for (int i = 0; i < chunkMeshes.size(); i++) {
            Asset<Mesh> mesh = chunkMeshes.get(i);
            int xInd = i % numXChunks;
            int yInd = i / numXChunks;

            boolean isVisible = viewBoxTester.testAab(
                    xInd * meshSize, yInd * meshSize, 0,
                    (xInd + 1) * meshSize, (yInd + 1) * meshSize, maxHeight
            );

            if (isVisible) {
                gl.render(mesh.get(), null);
            } else {
                numChunksCulled++;
            }
        }

        culledChunks.add(numChunksCulled);

        if (shader instanceof BlinnPhongShader) {
            BlinnPhongShader bps = (BlinnPhongShader) shader;
            bps.setHeightLines(false);
        }
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public Vector2f getSize() {
        return new Vector2f(xSize * edgeLength, ySize * edgeLength);
    }

    @Override
    public Vector2ic getCoordinateSize() {
        // number of voxels is one less than the number of coordinates
        return new Vector2i(xSize - 1, ySize - 1);
    }

    public void cleanup() {
        synchronized (this) {
            heightmap = new float[0][0];
            xSize = 0;
            ySize = 0;

            chunkMeshes.clear();
            listeners.clear();
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        synchronized (this) {
            out.writeInt(xSize);
            out.writeInt(ySize);

            for (int x = 0; x < xSize; x++) {
                float[] slice = heightmap[x];
                for (int y = 0; y < ySize; y++) {
                    out.writeFloat(slice[y]);
                }
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        xSize = in.readInt();
        ySize = in.readInt();
        maxHeight = Float.NEGATIVE_INFINITY;

        heightmap = new float[xSize][ySize];
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                float h = in.readFloat();

                heightmap[x][y] = h;
                if (h > maxHeight) maxHeight = h;
            }
        }

        chunkMeshes.addAll(generateMeshes(heightmap, xSize, ySize, edgeLength));
    }
}
