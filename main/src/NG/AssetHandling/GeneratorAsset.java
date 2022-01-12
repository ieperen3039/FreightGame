package NG.AssetHandling;

import NG.Tools.Toolbox;

/**
 * @author Geert van Ieperen created on 26-2-2020.
 */
public class GeneratorAsset<T> extends Asset<T> {
    private final AssetGenerator<? extends T> generator;
    private final AssetCleaner<T> cleanup;

    /**
     * a resource to use with lambdas.
     * @param generator is called to generate a new element
     * @param cleanup   is called on the element when this is dropped. If no action is required, use null.
     */
    public GeneratorAsset(AssetGenerator<? extends T> generator, AssetCleaner<T> cleanup) {
        this.generator = generator;
        this.cleanup = cleanup;
    }

    public GeneratorAsset(AssetGenerator<? extends T> generator) {
        this.generator = generator;
        this.cleanup = null;
    }

    @Override
    protected T reload() throws AssetException {
        return generator.get();
    }

    @Override
    public void drop() {
        if (cleanup != null && element != null) {
            cleanup.accept(element);
            Toolbox.checkGLError(String.valueOf(element));
        }

        super.drop();
    }

}
