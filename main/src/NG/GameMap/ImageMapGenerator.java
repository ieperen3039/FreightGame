package NG.GameMap;

import NG.Core.Version;
import NG.AssetHandling.Asset;
import NG.AssetHandling.ExternalAsset;
import NG.AssetHandling.Resource;
import de.matthiasmann.twl.utils.PNGDecoder;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Geert van Ieperen created on 20-9-2020.
 */
public class ImageMapGenerator implements MapGeneratorMod {
    private final Asset<PNGDecoder> mapResource;
    private final float edgeLength;
    private float progress = 0;
    private float unitHeight = 0.05f;

    public ImageMapGenerator(Path mapFile, float edgeLength) {
        mapResource = ExternalAsset.get(
                path -> new PNGDecoder(new FileInputStream(path.toFile())),
                mapFile
        );
        this.edgeLength = edgeLength;
    }

    public ImageMapGenerator(Resource.Path mapFile, float edgeLength) {
        mapResource = Resource.get(
                path -> new PNGDecoder(path.asStream()),
                mapFile
        );
        this.edgeLength = edgeLength;
    }

    @Override
    public Collection<Property> getProperties() {
        return Collections.emptySet();
    }

    @Override
    public void setProperty(String name, float value) {
        throw new IllegalArgumentException("This generator has no properties");
    }

    @Override
    public float[][] generateHeightMap() {
        PNGDecoder image = mapResource.get();

        int xSize = image.getWidth();
        int ySize = image.getHeight();

        // Load texture contents into a byte buffer
        PNGDecoder.Format format = image.decideTextureFormat(PNGDecoder.Format.RGB);
        int byteSize = format.getNumComponents();

        assert byteSize == 3;
        ByteBuffer buffer = ByteBuffer.allocate(byteSize * xSize * ySize);
        try {
            image.decode(buffer, xSize * byteSize, format);

        } catch (IOException e) {
            throw new Asset.AssetException(e, "failure when decoding image");
        }

        progress = 0.5f;
        buffer.flip();

        // read buffer
        float[][] heightmap = new float[xSize][ySize];
        float progressStep = 0.5f / (xSize + 1);

        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                int r = buffer.get() & 0xff;
//                int g = buffer.get() & 0xff;
//                int b = buffer.get() & 0xff;
                buffer.get();
                buffer.get();

                heightmap[x][y] = r * unitHeight;
            }
            progress += progressStep;
        }

        progress = 1;
        return heightmap;
    }

    @Override
    public float heightmapProgress() {
        return progress;
    }

    @Override
    public void setXSize(int xSize) {
        throw new UnsupportedOperationException("map is fixed size");
    }

    @Override
    public void setYSize(int ySize) {
        throw new UnsupportedOperationException("map is fixed size");
    }

    @Override
    public Version getVersionNumber() {
        return new Version(0, 0);
    }

    public void setUnitHeight(float unitHeight) {
        this.unitHeight = unitHeight;
    }

    @Override
    public void cleanup() {

    }

    @Override
    public float getEdgeLength() {
        return edgeLength;
    }
}
