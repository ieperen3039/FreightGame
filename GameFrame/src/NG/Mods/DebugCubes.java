package NG.Mods;

import NG.DataStructures.Valuta;

/**
 * @author Geert van Ieperen created on 12-6-2020.
 */
public class DebugCubes implements FreightType {
    @Override
    public String name() {
        return "Cubes";
    }

    @Override
    public Valuta valueOverTime(double daysInTransit) {
        return new Valuta(1);
    }
}
