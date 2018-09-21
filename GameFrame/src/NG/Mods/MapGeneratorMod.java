package NG.Mods;

import NG.Entities.MovingEntity;

/**
 * It is required for this mod to use a new Random instance when generating a map. This for the sake of determinism when
 * recreating a map based on seed.
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public interface MapGeneratorMod extends Mod {

    /**
     * start generating a map. All new entities (of which the world itself is one) should be added to the game using
     * {@link NG.GameState.GameState#addEntity(MovingEntity)}
     * @param seed the seed for generating this map
     */
    void generateNew(int seed);
}
