package NG.Mods;

import NG.Entities.Entity;
import org.joml.Vector2fc;

/**
 * A mod that is capable of generating a map mesh, including everything that can be found on a map.
 * <p>
 * It is required for this mod to use a new Random instance when generating a map. This for the sake of determinism when
 * recreating a map based on seed.
 * @see NG.GameState.HeightMapGeneratorMod
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public interface MapGeneratorMod extends Mod {

    /**
     * start generating a map. This should only change the internal state of this object.
     * @param seed   the seed for generating this map
     * @param width the width of the map, must be divisible by 100
     * @param height the height of the map, must be divisible by 100
     */
    void generateNew(int seed, int width, int height);

    float getHeightAt(Vector2fc position);

    /**
     * gives an indication of how far {@link #generateNew(int, int, int)} is progressed. If this returns 1, {@link
     * #getWorldEntities()} should return the world entities correctly, and this method may not produce any other value
     * than 1 until {@link #generateNew(int, int, int)} is called again..
     * @return a float [0, 1] indicating the generation progress. 0 indicates that it hasn't started, 1 indicates that
     *         it is done.
     */
    float getProgress();

    /**
     * @return the entities generated. The result of this method is undefined when {@link #getProgress()} returns any
     *         value other than 1.
     */
    Iterable<Entity> getWorldEntities();

}
