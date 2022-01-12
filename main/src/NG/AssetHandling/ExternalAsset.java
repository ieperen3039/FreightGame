package NG.AssetHandling;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ExternalAsset<T> extends Asset<T> {
    private static final Map<Path, ExternalAsset<?>> allExternalAssets = new HashMap<>();

    /** must be relative to file directory, for the sake of serialisation */
    private final Path fileLocation;
    private final FileLoader<T> loader;

    private ExternalAsset(FileLoader<T> loader, Path relativePath) {
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

    public static <T> ExternalAsset<T> get(FileLoader<T> loader, Path path) {
        //noinspection unchecked
        return (ExternalAsset<T>) allExternalAssets.computeIfAbsent(
                path, (p) -> new ExternalAsset<>(loader, p)
        );
    }

    public interface FileLoader<T> extends Serializable {
        T apply(Path path) throws IOException;
    }
}
