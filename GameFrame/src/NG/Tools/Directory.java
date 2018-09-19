package NG.Tools;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public enum Directory {
    shaders("res", "shaders"),
    meshes("res", "models", "general"),
    fonts("res", "fonts"),
    mods("jar", "Mods");

    private final Path directory;

    Directory() {
        this.directory = currentDirectory();
    }

    Directory(Path directory) {
        this.directory = directory;
    }

    Directory(String first, String... directory) {
        this.directory = Paths.get(first, directory);
    }

    public File getDirectory() {
        return directory.toFile();
    }

    public File getFile(String... path) {
        return currentDirectory()
                .resolve(getPath(path))
                .toFile();
    }

    public Path getPath(String... path) {
        Path dir = this.directory;
        for (String p : path) {
            dir = dir.resolve(p);
        }
        return dir;
    }

    public File[] getFiles() {
        return getDirectory().listFiles();
    }

    public static Path currentDirectory() {
        return Paths.get("").toAbsolutePath();
    }

    public URL toURL() {
        try {
            return directory.toUri().toURL();
        } catch (MalformedURLException e) {
            Logger.ERROR.print(new IOException("Directory does not exist", e));
            return null;
        }
    }
}
