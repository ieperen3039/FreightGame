package NG.ScreenOverlay;

import NG.Tools.Directory;
import NG.Tools.Logger;
import NG.Tools.Toolbox;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import static NG.Tools.Directory.fonts;

/**
 * @author Geert van Ieperen. Created on 23-8-2018.
 */
public enum JFGFonts {
    ORBITRON_REGULAR(fonts, "Orbitron", "Orbitron-Regular.ttf"),
    ORBITRON_MEDIUM(fonts, "Orbitron", "Orbitron-Medium.ttf"),
    ORBITRON_BOLD(fonts, "Orbitron", "Orbitron-Bold.ttf"),
    ORBITRON_BLACK(fonts, "Orbitron", "Orbitron-Black.ttf"),
    LUCIDA_CONSOLE(fonts, "LucidaConsole", "lucon.ttf");

    public final String name;
    public final String source;
    private ByteBuffer byteFormat;
    private Font awtFormat;

    JFGFonts(Directory dir, String... filepath) {
        this.name = toString().toLowerCase().replace("_", " ");
        this.source = dir.getPath(filepath).toString();
        File file = fonts.getFile(filepath);
        Path path = fonts.getPath(filepath);

        try {
            byteFormat = Toolbox.toByteBuffer(path);
            awtFormat = Font.createFont(Font.TRUETYPE_FONT, file);

        } catch (IOException | FontFormatException e) {
            Logger.ERROR.print("Error loading font " + name + " (" + path + "): " + e.getMessage());
        }
    }

    ByteBuffer asByteBuffer() {
        return byteFormat;
    }

    public java.awt.Font asAWTFont(float size) {
        return awtFormat.deriveFont(size);
    }

}
