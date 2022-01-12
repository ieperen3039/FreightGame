package NG.AssetHandling;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Geert van Ieperen created on 26-2-2020.
 */
public class Resource<T> extends Asset<T> {
    private static final Map<Path, Resource<?>> allFileResources = new HashMap<>();

    /**
     * must be relative to file directory, for the sake of serialisation
     */
    private final Path fileLocation;
    private final FileLoader<T> loader;

    private Resource(FileLoader<T> loader, Path relativePath) {
        super();
        this.loader = loader;
        this.fileLocation = relativePath;
    }

    @Override
    protected T reload() throws AssetException {
        try {
            return loader.apply(fileLocation);

        } catch (IOException e) {
            throw new AssetException(e, fileLocation + ": " + e.getMessage());
        }
    }

    public static <T> Resource<T> get(FileLoader<T> loader, Path path) {
        //noinspection unchecked
        return (Resource<T>) allFileResources.computeIfAbsent(
                path, (p) -> new Resource<>(loader, p)
        );
    }

    public interface FileLoader<T> extends Serializable {
        T apply(Path path) throws IOException;
    }

    public static class Path {
        String path;

        private Path() {}

        public Path(String... elements) {
            StringBuilder pathBuilder = new StringBuilder(elements[0]);
            for (int i = 1; i < elements.length; i++) {
                pathBuilder.append("/");
                pathBuilder.append(elements[i]);
            }
            this.path = pathBuilder.toString();
        }

        public Path resolve(String... relative) {
            Path newPath = new Path();
            StringBuilder pathBuilder = new StringBuilder(path);
            for (String s : relative) {
                pathBuilder.append("/");
                pathBuilder.append(s);
            }
            newPath.path = pathBuilder.toString();
            return newPath;
        }

        public Path resolve(Path relative) {
            Path newPath = new Path();
            newPath.path = this.path + "/" + relative.path;
            return newPath;
        }

        public static Path get(String... elements) {
            return new Path(elements);
        }

        /**
         * creates a file input stream of the resource indicated by this path.
         * Intended for use as part of a {@link FileLoader}
         *
         * @return this resource as input stream
         */
        public InputStream asStream() throws AssetException {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
            if (stream == null) {
                throw new AssetException("Could not find resource " + path);
            }
            return stream;
        }

        @Override
        public String toString() {
            return path;
        }
    }
}
