package NG.AssetHandling;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * contains an object that is not included when serialized. Instead, it is regenerated when necessary, and automatically
 * dropped when not used for some amount of time.
 * @author Geert van Ieperen created on 25-2-2020.
 */
public abstract class Asset<T> implements Serializable {
    private static final List<WeakReference<Asset<?>>> allAssets = new ArrayList<>();
    private static final ReferenceQueue<Asset<?>> assetsToClear = new ReferenceQueue<>();

    /** number of cycles that this asset is kept loaded after being used this cycle {@link #get()}. */
    private static final int HEAT_INCREMENT = 2; // sec * FPS
    /** maximum cycles to keep an asset loaded after its last call to {@link #get()} */
    private static final int MAX_HEAT = 300; // sec * FPS
    /** number of assets that are allowed where cleaning is not required */
    private static final int BASELEVEL_NUM_ASSETS = 100;

    /** the cached element */
    protected transient T element = null;
    /** number of cycles before this may be dropped */
    private transient int heat = 0;
    private transient boolean isUsed = false;

    public Asset() {
        registerAsset();
    }

    private void registerAsset() {
        synchronized (allAssets) {
            allAssets.add(new WeakReference<>(this));
        }
    }

    /**
     * returns the cached element, possibly generating a new element
     * @return the element itself
     * @throws AssetException if the reloading operation fails
     */
    public T get() throws AssetException {
        if (element == null) {
            element = reload();
        }

        isUsed = true;

        return element;
    }

    /**
     * drops the cached element, causing a reload on the next get
     */
    public void drop() {
        element = null;
    }

    /**
     * reloads the resource.
     * @throws AssetException whenever the resource could not be generated
     */
    protected abstract T reload() throws AssetException;

    @Override
    public String toString() {
        if (element == null) {
            return "[empty resource]";
        } else {
            return "[" + element + "]";
        }
    }

    public static int getNrOfActiveResources() {
        synchronized (allAssets) {
            int count = 0;
            for (WeakReference<Asset<?>> assetRef : allAssets) {
                Asset<?> asset = assetRef.get();
                if (asset != null) {
                    if (asset.element != null) {
                        count++;
                    }
                }
            }
            return count;
        }
    }

    public static void cycle() {
        synchronized (allAssets) {
            boolean doDropColdAssets = (allAssets.size() >= BASELEVEL_NUM_ASSETS);

            Iterator<WeakReference<Asset<?>>> iterator = allAssets.iterator();
            while (iterator.hasNext()) {
                Asset<?> asset = iterator.next().get();

                if (asset != null) {
                    asset.updateHeat();

                    if (doDropColdAssets && asset.heat == 0) {
                        asset.drop();
                    }

                } else {
                    // remove this dangling weak reference, the original asset will be cleaned up by dropOrphanedAssets
                    iterator.remove();
                }
            }

            if (doDropColdAssets) {
                allAssets.removeIf(ref -> ref.get() == null);
            }
        }

        dropOrphanedAssets();
    }

    private void updateHeat() {
        if (isUsed) {
            if (heat < MAX_HEAT) {
                heat += HEAT_INCREMENT;
            }
        } else {
            if (heat > 0) {
                heat--;
            }
        }

        isUsed = false;
    }

    /** kill the orphans */
    private static void dropOrphanedAssets() {
        while (true) {
            Reference<? extends Asset<?>> poll = assetsToClear.poll();
            if (poll == null) break;
            Asset<?> asset = poll.get();
            if (asset == null) break;
            asset.drop();
        }
    }

    /** force all assets to be reloaded, including persistent assets. */
    public static void dropAll() {
        synchronized (allAssets) {
            Iterator<WeakReference<Asset<?>>> iterator = allAssets.iterator();
            while (iterator.hasNext()) {
                Asset<?> asset = iterator.next().get();

                if (asset != null) {
                    asset.drop();

                } else {
                    iterator.remove();
                }
            }
        }
    }

    public static void forEach(Consumer<Asset<?>> action) {
        synchronized (allAssets)
        {
            for (WeakReference<Asset<?>> assetRef : allAssets) {
                Asset<?> asset = assetRef.get();
                if (asset != null)
                {
                    action.accept(asset);
                }
            }
        }
    }

    public static void forEachActive(Consumer<Asset<?>> action) {
        forEach(asset -> {
            if (asset.element != null) {
                action.accept(asset);
            }
        });
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        registerAsset();
    }

    /**
     * create a resource that is generated from another resource.
     * @param source    a resource generating an element of type A
     * @param extractor a function that generates the desired element of type B using source
     * @return a resource generating an element of type B
     */
    public static <A, B> Asset<B> derive(Asset<A> source, AssetConverter<A, B> extractor) {
        return new GeneratorAsset<>(() -> extractor.apply(source.get()), null);
    }

    public static <A, B> Asset<B> derive(
            Asset<A> source, AssetConverter<A, B> extractor, AssetCleaner<B> cleanup
    ) {
        return new GeneratorAsset<>(() -> extractor.apply(source.get()), cleanup);
    }

    /** serializable version of {@link Supplier} */
    public interface AssetGenerator<T> extends Supplier<T>, Serializable {
    }

    /** serializable version of {@link Consumer} */
    public interface AssetCleaner<T> extends Consumer<T>, Serializable {
    }

    /** serializable version of {@link Function} */
    public interface AssetConverter<A, B> extends Function<A, B>, Serializable {
    }

    /**
     * error to indicate a failure to fetch an asset.
     */
    public static class AssetException extends RuntimeException {
        public AssetException(String message) {
            super(message);
        }

        public AssetException(Exception cause, String message) {
            super(message, cause);
        }
    }
}
