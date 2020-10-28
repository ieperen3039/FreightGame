package NG.GameMap;

import NG.Core.Game;
import NG.Mods.Mod;

import java.util.Collection;

/**
 * @author Geert van Ieperen. Created on 29-9-2018.
 */
public interface MapGeneratorMod extends Mod {

    @Override
    default void init(Game game) {
    }

    /**
     * get a list of properties of this generator which can be changed. Units and range should be given in the name. The
     * properties do NOT include the seed of the generator.
     * @return a list of properties and their current value.
     */
    Collection<Property> getProperties();

    void setProperty(String name, float value);

    /**
     * generate a heightmap which will be used to render the world.
     * @return the new heightmap, of size (xSize x ySize)
     */
    float[][] generateHeightMap();

    /**
     * gives an indication of how far {@link #generateHeightMap()} is progressed.
     * @return a float [0, 1] indicating the generation progress. 0 indicates that it hasn't started, 1 indicates that
     * it is done.
     */
    float heightmapProgress();

    void setXSize(int xSize);

    void setYSize(int ySize);

    float getEdgeLength();

    default void setSize(int x, int y) {
        setXSize(x);
        setYSize(y);
    }

    class Property {
        public final String name;
        public final float minimum;
        public final float maximum;
        public float current;

        public Property(String name, float minimum, float maximum, float initial) {
            this.name = name;
            this.minimum = minimum;
            this.maximum = maximum;
            this.current = initial;
        }
    }
}
