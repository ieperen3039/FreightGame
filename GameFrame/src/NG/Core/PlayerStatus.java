package NG.Core;

import NG.DataStructures.Valuta;
import NG.Entities.Station;
import NG.Entities.Train;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geert van Ieperen created on 30-8-2020.
 */
public class PlayerStatus implements GameAspect {
    public final Valuta money = Valuta.ofUnitValue(1000);
    public final List<Train> trains = new ArrayList<>();
    public final List<Station> stations = new ArrayList<>();
    private Game game;

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
