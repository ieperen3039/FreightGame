package NG.Rendering.Textures;

import NG.AssetHandling.Asset;
import NG.AssetHandling.Resource;
import NG.Tools.Directory;
import NG.Tools.Logger;
import NG.Tools.Toolbox;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author Geert van Ieperen created on 1-2-2019.
 */
public interface Texture {
    /**
     * activate this texture to be applied on the next model
     * @param sampler the texture slot to bind to
     */
    void bind(int sampler);

    /** destroy the resources claimed by the texture */
    void cleanup();

    int getWidth();

    int getHeight();

    int getID();

    default void dump(String fileName) {
        Logger.DEBUG.print("Dumping texture " + fileName);
        int id = getID();
        glBindTexture(GL_TEXTURE_2D, id);
        ByteBuffer buffer = ByteBuffer.allocateDirect(getWidth() * getHeight() * 4);
        glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        Toolbox.checkGLError("texture write");
        Toolbox.writePNG(Directory.screenshots, fileName, buffer, 4, getWidth(), getHeight());
        glBindTexture(GL_TEXTURE_2D, 0);

        Toolbox.checkGLError("texture dump");
    }

    static Asset<Texture> createResource(String... path) {
        return Resource.get(p -> new FileTexture(p.asStream()), new Resource.Path(path));
    }
}
