package NG.Mods;

import NG.Core.Game;
import NG.Core.GameAspect;
import NG.Core.Version;
import NG.DataStructures.Generic.Pair;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * The default class that every mod of this game should implement. This class must have a no-arg constructor which is
 * called upon loading the mod (advised is to leave out constructors in general)
 * @author Geert van Ieperen. Created on 19-9-2018.
 */
public interface Mod extends GameAspect {
    /**
     * This method is called when the user creates a game with this mod installed. Any big initialisation should be
     * handled here. The overriding class should always have a default constructor, which is called regardless of
     * whether this mod is used.
     * @param game the game in which the mod is used
     * @throws Version.MisMatchException if the version of the game is incompatible with the mod
     */
    void init(Game game) throws Version.MisMatchException, IOException;

    /** @return the name of this mod */
    default String getModName() {
        return getClass().getSimpleName();
    }

    /** The current version of this mod */
    Version getVersionNumber();

    /** @return a list of all modnames and their version required for this mod */
    default List<Pair<String, Version>> getDependencies() {
        return Collections.emptyList();
    }
}
