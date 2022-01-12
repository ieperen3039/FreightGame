package NG.GUIMenu.Rendering;

import NG.AssetHandling.Asset;
import NG.AssetHandling.Resource;
import NG.Tools.Logger;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author Geert van Ieperen. Created on 23-8-2018.
 */
public enum NGFonts {
    ORBITRON_REGULAR("Orbitron", "Orbitron-Regular.ttf"),
    ORBITRON_MEDIUM("Orbitron", "Orbitron-Medium.ttf"),
    ORBITRON_BOLD("Orbitron", "Orbitron-Bold.ttf"),
    ORBITRON_BLACK("Orbitron", "Orbitron-Black.ttf"),
    LUCIDA_CONSOLE("LucidaConsole", "lucon.ttf");

    public static final int SIZE_LARGE = 20;
    public static final int SIZE_REGULAR = 16;
    public static final String FONTS_DIRECTORY = "fonts";

    public final String name;
    public final String source;
    private ByteBuffer byteFormat;
    private Font awtFormat;

    NGFonts(String... relative) {
        this.name = toString().toLowerCase().replace("_", " ");
        Resource.Path path = Resource.Path.get(FONTS_DIRECTORY).resolve(relative);
        this.source = path.toString();

        try (InputStream inputStream = path.asStream()) {
            byte[] bytes = inputStream.readAllBytes();
            byteFormat = BufferUtils.createByteBuffer(bytes.length + 1);
            byteFormat.put(bytes);
            byteFormat.flip();

        } catch (IOException | Asset.AssetException e) {
            Logger.ERROR.print("Error loading font " + path + ": " + e);
        }

        try (InputStream inputStream = path.asStream()) {
            awtFormat = Font.createFont(Font.TRUETYPE_FONT, inputStream);

        } catch (IOException | FontFormatException | Asset.AssetException e) {
            Logger.ERROR.print("Error loading font " + path + ": " + e);
        }

    }

    ByteBuffer asByteBuffer() {
        return byteFormat;
    }

    public Font asAWTFont(float size) {
        return awtFormat.deriveFont(size);
    }

    public enum TextType {
        TITLE(SIZE_LARGE),
        ACCENT(SIZE_LARGE),
        REGULAR(SIZE_REGULAR),
        FANCY(SIZE_REGULAR),
        TOOLTIP(SIZE_REGULAR),
        RED(SIZE_REGULAR),
        FLOATING(SIZE_REGULAR);

        private final int size;

        TextType(int size) {
            this.size = size;
        }

        public int size() {
            return size;
        }
    }
}
