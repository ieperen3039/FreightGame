package NG.Core;

import NG.Mods.Mod;

import java.io.File;
import java.util.List;

/**
 * @author Geert van Ieperen. Created on 5-10-2018.
 */
public interface ModLoader {
    /**
     * starts a game with the loaded mods
     */
    void startGame();

    /**
     * stop a game, unload all mods
     */
    void stopGame();

    /**
     * save the current state of the game to a file
     * @param file
     */
    void saveGame(File file);

    /**
     * loads a game state off a file and calls {@link #startGame()}
     * @param file
     */
    void loadGame(File file);

    /**
     * Starts the given mods. The mods must be unloaded later by {@link #cleanMods()}
     * @param mods a list of mods to be loaded, which have not been started before.
     */
    void initMods(List<Mod> mods);

    /**
     * calls the {@link Mod#cleanup()} method of the mods loaded with {@link #initMods(List)} as to unload these mods.
     */
    void cleanMods();

    /**
     * @return a list of all mods, both loaded and not loaded
     */
    List<Mod> allMods();

    /**
     * searches a mod with a name close to the given name
     * @param name a name of a mod
     * @return the mod with a name exactly equal to the given name
     */
    Mod getModByName(String name);

    class IllegalNumberOfModulesException extends Exception {
        public IllegalNumberOfModulesException(String message) {
            super(message);
        }
    }
}
