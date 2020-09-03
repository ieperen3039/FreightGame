package NG.Core;

import NG.DataStructures.Valuta;

/**
 * @author Geert van Ieperen created on 30-8-2020.
 */
public class ProgressTracker implements GameAspect {
    public final Valuta money = Valuta.ofUnitValue(1000);

    @Override
    public void init(Game game) throws Exception {

    }

    @Override
    public void cleanup() {

    }
}
