package NG.Mods;

import NG.Engine.Game;
import NG.Engine.GameAspect;
import NG.Engine.Version;

/**
 * The default class that every mod of this game should implement. This class must have a no-arg constructor which is
 * called upon loading the mod
 * @author Geert van Ieperen. Created on 19-9-2018.
 */
public interface Mod extends GameAspect {
    /**
     * Upon loading this mod, one instance is created and this method is called exactly once. The overriding class
     * should always have a default constructor and use this method for initialisation
     * @param game the game in which the mod is used
     * @throws Version.MisMatchException if the version of the game is incompatible with the mod
     */
    void init(Game game) throws Version.MisMatchException;

    /** @return the name of this mod */
    default String getName() {
        return getClass().getName();
    }
}
