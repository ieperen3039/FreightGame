package NG.Core;

import NG.Mods.Mod;
import NG.Tools.Logger;

import java.io.*;
import java.util.Collection;

/**
 * A file that stores the gaemstate of the given Game
 * @author Geert van Ieperen. Created on 28-9-2018.
 */
public class SaveFile {
    private static final String INITIAL_DATA = "Freight Game SaveFile";

    public static void write(Game game, String name, ModLoader modLoader) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(name);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);

        out.writeUTF(INITIAL_DATA);
        out.writeObject(game.getVersionNumber());

        Collection<Mod> listOfMods = modLoader.allMods();
        out.writeInt(listOfMods.size());

        for (Mod mod : listOfMods) {
            out.writeUTF(mod.getModName());
            out.writeObject(mod.getVersionNumber());
        }

        out.writeObject(game.state());
    }

    public static void read(Game game, String name) throws IOException, ClassNotFoundException {
        FileInputStream fileIn = new FileInputStream(name);
        ObjectInputStream in = new ObjectInputStream(fileIn);

        String initial = in.readUTF();
        if (!initial.equals(INITIAL_DATA)) {
            throw new IOException("File is not a valid savefile for this game");
        }
        Version fileVersion = (Version) in.readObject();
        Logger.INFO.print("Reading SaveFile of version " + fileVersion);

        int nOfMods = in.readInt();
        for (int i = 0; i < nOfMods; i++) {
            String modName = in.readUTF();
            Version version = (Version) in.readObject();
        }

        in.readObject();
    }
}
