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
    hardMods("mods_hard"),
    softMods("mods_soft"),
    screenshots("Screenshots"),
    savedGames("Saved maps");

    private static Path workingDirectory = null;
    private final Path directory; // relative path

    Directory(Path directory) {
        this.directory = directory;
    }

    Directory(String first, String... other) {
        directory = Paths.get(first, other);
        File file = workDirectory().resolve(directory).toFile();
        file.mkdirs();
    }

    public File getDirectory() {
        return getPath().toFile();
    }

    public File getFile(String... path) {
        return getPath(path).toFile();
    }

    public File getFileMakeParents(String... path) {
        File file = getPath(path).toFile();
        file.getParentFile().mkdirs();
        return file;
    }

    /** @return the path local to the work directory */
    public Path getPath(String... path) {
        Path pathBuilder = directory;
        for (String s : path) {
            pathBuilder = pathBuilder.resolve(s);
        }
        return workDirectory().resolve(pathBuilder);
    }

    public Path getPath() {
        return workDirectory().resolve(directory);
    }

    public File[] getFiles() {
        return getDirectory().listFiles();
    }

    public static Path workDirectory() {
        if (workingDirectory == null) {
            workingDirectory = Paths.get("").toAbsolutePath();
        }
        return workingDirectory;
    }

    @Override
    public String toString() {
        return directory.toAbsolutePath().toString();
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
