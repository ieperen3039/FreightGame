package NG.Menu.InGame.Build;

import NG.Core.Game;
import NG.DataStructures.Valuta;
import NG.Entities.*;
import NG.GUIMenu.Components.SActiveTextArea;
import NG.GUIMenu.Components.SButton;
import NG.GUIMenu.Components.SContainer;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.LayoutManagers.GridLayoutManager;
import NG.Menu.Main.MainMenu;
import org.joml.Vector2i;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Geert van Ieperen created on 21-5-2020.
 */
public class TrainConstructionMenu extends SFrame {
    private static int id = 1;

    private final Game game;
    private final Station targetPlace;
    private final Train construction;
    private final Valuta costs = Valuta.ofUnitValue(0);

    public TrainConstructionMenu(Game game, Station place) {
        super("New Train");
        this.game = game;
        targetPlace = place;
        setSize(500, 0);

        double gameTime = game.timer().getGameTime();
        construction = new Train(game, id++, gameTime, place);

        List<Locomotive.Properties> locomotiveTypes = game.objectTypes().locomotiveTypes;
        List<Wagon.Properties> wagonTypes = game.objectTypes().wagonTypes;

        SContainer.GhostContainer elementsPanel = new SContainer.GhostContainer(
                new GridLayoutManager(1, locomotiveTypes.size() + wagonTypes.size())
        );
        int yIndex = 0;

        for (Locomotive.Properties loco : locomotiveTypes) {
            elementsPanel.add(new SButton(
                            "Add " + loco.toString(),
                            () -> add(new Locomotive(loco)),
                            MainMenu.BUTTON_PROPERTIES_STATIC
                    ), new Vector2i(0, yIndex++)
            );
        }

        for (Wagon.Properties wagon : wagonTypes) {
            elementsPanel.add(new SButton(
                            "Add " + wagon.toString(),
                            () -> add(new Wagon(wagon)),
                            MainMenu.BUTTON_PROPERTIES_STATIC
                    ), new Vector2i(0, yIndex++)
            );
        }

        setMainPanel(SContainer.column(
                new SActiveTextArea(costs::toString, MainMenu.TEXT_PROPERTIES),
                elementsPanel,
                new SButton("Remove Last", this::removeLast, MainMenu.BUTTON_PROPERTIES_STATIC),
                new SButton("Build this", this::confirmAndClose, MainMenu.BUTTON_PROPERTIES_STATIC),
                new SActiveTextArea(() -> construction.getElements()
                        .stream()
                        .map(e -> e.getClass().getSimpleName())
                        .collect(Collectors.joining("|")), MainMenu.TEXT_PROPERTIES)
        ));
    }

    private void confirmAndClose() {
        game.getProgress().money.subtract(costs);
        game.state().addEntity(construction);
        targetPlace.addTrain(construction);
        construction.openUI();

        dispose(); // sepukku
    }

    private void removeLast() {
        TrainElement elt = construction.removeLastElement();
        costs.removeUnits(elt.getProperties().buildCost);
    }

    private void add(TrainElement elt) {
        costs.addUnits(elt.getProperties().buildCost);
        construction.addElement(elt);
    }
}
