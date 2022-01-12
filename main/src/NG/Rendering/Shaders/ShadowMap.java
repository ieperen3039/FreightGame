package NG.Rendering.Shaders;

import NG.Rendering.Textures.Texture;
import NG.Tools.Directory;
import NG.Tools.Logger;
import NG.Tools.Toolbox;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author Dungeons-and-Drawings group
 */
@SuppressWarnings("Duplicates")
public class ShadowMap implements Texture {
    private final int resolution;
    private final int depthMapFBO;
    private final int depthMap;

    public ShadowMap(int resolution) {
        this.resolution = resolution;
        // Allocate Texture and FBO
        depthMapFBO = glGenFramebuffers();
        depthMap = glGenTextures();

        // Create depth map texture
        glBindTexture(GL_TEXTURE_2D, depthMap);
        glTexImage2D(
                GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT,
                resolution, resolution,
                0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null
        );

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // Create FBO
        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthMap, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        // Error Check
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new ShaderException("ShadowMap could not init FrameBuffer");
        }

        // Unbind Depth Map and FBO
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void bind(int sampler) {
        glActiveTexture(sampler);
        glBindTexture(GL_TEXTURE_2D, depthMap);
    }

    @Override
    public void cleanup() {
        glDeleteFramebuffers(depthMapFBO);
        glDeleteTextures(depthMap);
    }

    @Override
    public int getWidth() {
        return resolution;
    }

    @Override
    public int getHeight() {
        return resolution;
    }

    @Override
    public int getID() {
        return depthMap;
    }

    public int getResolution() {
        return resolution;
    }

    public void setToFrameBuffer() {
        glViewport(0, 0, resolution, resolution);
        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    public void dump(String fileName) {
        Logger.DEBUG.print("Dumping texture " + fileName);
        int id = getID();
        glBindTexture(GL_TEXTURE_2D, id);
        ByteBuffer buffer = ByteBuffer.allocateDirect(getWidth() * getHeight());
        glGetTexImage(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_BYTE, buffer);
        Toolbox.checkGLError("texture write");
        boolean finished = false;
        int width = getWidth();
        int height = getHeight();
        String format = "png";
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = (x + (width * y));
                int value = buffer.get(i) & 0xFF;
                image.setRGB(x, height - (y + 1), (0xFF << 24) | (value << 16) | (value << 8) | value);
            }
        }

        try {
            File file = Directory.screenshots.getFile(fileName + "." + format); // The file to save to.
            if (file.exists()) {
                Files.delete(file.toPath());
            } else {
                boolean success = file.mkdirs();
                if (!success) {
                    Logger.ERROR.print("Could not create directories", file);
                    finished = true;
                }
            }
            if (!finished) {
                ImageIO.write(image, format, file);

            }
        } catch (IOException e) {
            Logger.ERROR.print(e);
        }
        glBindTexture(GL_TEXTURE_2D, 0);

        Toolbox.checkGLError("texture dump");
    }
}
