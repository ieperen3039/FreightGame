package io.github.ieperen3039.trainsinspace.Core;

import io.github.ieperen3039.trainsinspace.DataStructures.Valuta;
import io.github.ieperen3039.trainsinspace.Entities.Station;
import io.github.ieperen3039.trainsinspace.Entities.Train;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Geert van Ieperen created on 30-8-2020.
 */
public class PlayerStatus implements GameAspect, Serializable {
    public final Valuta money = Valuta.ofUnitValue(1000);
    public final List<Train> trains = new ArrayList<>();
    public final List<Station> stations = new ArrayList<>();
    private transient Game game;

    @Override
    public void init(Game game) throws Exception {
        this.game = game;
    }

    public void update() {
        double gameTime = game.timer().getGameTime();
        trains.removeIf(t -> t.isDespawnedAt(gameTime));
    }

    @Override
    public void cleanup() {

    }
}
