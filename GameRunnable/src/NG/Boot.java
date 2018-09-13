package NG;

import NG.Engine.FreightGame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Boots the Root
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Boot {
    public static void main(String[] argArray) {
        List<String> args = new ArrayList<>(Arrays.asList(argArray));

        new FreightGame().root();
    }
}
