package io.github.ieperen3039.trainsinspace.Menu.InGame.Build;

import io.github.ieperen3039.ngn.UserInterface.Components.*;
import io.github.ieperen3039.ngn.UserInterface.Rendering.NGFont;
import io.github.ieperen3039.ngn.UserInterface.Rendering.SFrameLookAndFeel;
import io.github.ieperen3039.ngn.UserInterface.SComponentProperties;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Core.PlayerStatus;
import io.github.ieperen3039.trainsinspace.DataStructures.Valuta;
import io.github.ieperen3039.trainsinspace.Entities.*;
import io.github.ieperen3039.trainsinspace.Menu.Main.MainMenu;
import io.github.ieperen3039.trainsinspace.Tracks.TrackPiece;
import io.github.ieperen3039.trainsinspace.Tracks.TrackType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Geert van Ieperen created on 21-5-2020.
 */
public class TrainConstructionMenu extends SFrame {
    public static final SComponentProperties PROPERTIES_COMPONENT_PROPERTIES = new SComponentProperties(
            200, 0, false, true, NGFont.TextType.REGULAR, SFrameLookAndFeel.Alignment.LEFT_TOP
    );
    public static final SComponentProperties TRAIN_ELEMENT_BUTTON_PROPS = new SComponentProperties(
            200, 30, false, false, NGFont.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );

    private static int id = 1;

    private final Game game;
    private final Station targetPlace;
    private final Train construction;
    private final Valuta costs = Valuta.ofUnitValue(0);
    private final float maxSize;
    private final STextComponent messageElement;

    public TrainConstructionMenu(Game game, Station place) {
        super("New Train");
        this.game = game;
        targetPlace = place;

        float tMaxSize = 0;
        for (TrackPiece track : place.getTracks()) {
            float length = track.getLength();
            if (length > tMaxSize) tMaxSize = length;
        }
        maxSize = tMaxSize;

        double gameTime = game.timer().getTime();
        construction = new Train(game, id++, gameTime, place);

        List<Locomotive.Properties> locomotiveTypes = game.objectTypes().locomotiveTypes;
        List<Wagon.Properties> wagonTypes = game.objectTypes().wagonTypes;
        List<TrackType> trackTypes = game.objectTypes().trackTypes;

        /* create tab contents */
        TrackType[] trackTypeArray = trackTypes.toArray(new TrackType[0]);
        SComponent[] typeTabArea = new SComponent[trackTypeArray.length * 2];
        String[] tabLabels = new String[typeTabArea.length];

        for (int i = 0; i < trackTypeArray.length; i++) {
            TrackType trackType = trackTypeArray[i];
            SComponentArea trainTabArea = new SComponentArea(400, 400);

            List<SButton> locoButtons = new ArrayList<>();
            for (Locomotive.Properties loco : locomotiveTypes) {
                if (loco.isCompatibleWith(trackType)) {
                    SContainer trainPanel = SPanel.column(
                            new STextComponent(
                                    loco + "\n\n" +
                                    "build cost : " + loco.buildCost + "\n" +
                                    "maintenance : " + loco.maintenancePerSecond + "\n" +
                                    "max speed : " + loco.maxSpeed + "\n" +
                                    "tractive effort : " + loco.tractiveEffort + "\n" +
                                    "mass : " + loco.mass + "\n" +
                                    "resistance : " + loco.linearResistance + "/" + loco.quadraticResistance + "\n",
                                    PROPERTIES_COMPONENT_PROPERTIES
                            ),
                            new SButton("Add", MainMenu.BUTTON_PROPERTIES_STRETCH, () -> add(new Locomotive(loco)))
                    );

                    locoButtons.add(new SButton(
                            loco.toString(),
                            TRAIN_ELEMENT_BUTTON_PROPS,
                            () -> trainTabArea.show(trainPanel)
                    ));
                }
            }

            tabLabels[i * 2] = trackTypeArray[i].toString() + " Locos";
            typeTabArea[i * 2] = SContainer.row(
                    new SScrollableList(6, locoButtons),
                    trainTabArea
            );

            SComponentArea wagonTabArea = new SComponentArea(400, 400);

            List<SButton> wagonButtons = new ArrayList<>();
            for (Wagon.Properties wagon : wagonTypes) {
                if (wagon.isCompatibleWith(trackType)) {
                    SContainer wagonPanel = SPanel.column(
                            new STextComponent(
                                    wagon.name + "\n\n" +
                                    "build cost : " + wagon.buildCost + "\n" +
                                    "maintenance : " + wagon.maintenancePerSecond + "\n" +
                                    "max speed : " + wagon.maxSpeed + "\n" +
                                    "mass : " + wagon.mass + "\n" +
                                    "resistance : " + wagon.linearResistance + "\n",
                                    PROPERTIES_COMPONENT_PROPERTIES
                            ),
                            new SButton("Add", MainMenu.BUTTON_PROPERTIES_STRETCH, () -> add(new Wagon(wagon)))
                    );

                    wagonButtons.add(new SButton(
                            wagon.toString(),
                            TRAIN_ELEMENT_BUTTON_PROPS,
                            () -> wagonTabArea.show(wagonPanel)
                    ));
                }
            }

            tabLabels[i * 2 + 1] = trackTypeArray[i].toString() + " Wagons";
            typeTabArea[i * 2 + 1] = SContainer.row(
                    new SScrollableList(6, wagonButtons),
                    wagonTabArea
            );
        }

        // tab area
        STabPanel typeSelectionTabs = new STabPanel(tabLabels, typeTabArea, MainMenu.BUTTON_PROPERTIES_STATIC);

        // train preview
        SComponent trainDisplay = new SActiveTextComponent(
                () -> construction.getElements()
                        .stream()
                        .map(e -> e.getClass().getSimpleName())
                        .collect(Collectors.joining("|")),
                MainMenu.BUTTON_PROPERTIES_STRETCH
        );

        messageElement = new STextComponent("", MainMenu.TEXT_PROPERTIES);

        setMainPanel(SContainer.column(
                typeSelectionTabs,
                trainDisplay,
                messageElement,
                SContainer.row(
                        new SButton("Build this", MainMenu.BUTTON_PROPERTIES_STATIC, this::confirmAndClose),
                        new SButton("Remove Last", MainMenu.BUTTON_PROPERTIES_STATIC, this::removeLast)
                )
        ));
    }

    private void confirmAndClose() {
        PlayerStatus player = game.playerStatus();
        player.money.subtract(costs);
        player.trains.add(construction);
        game.state().addEntity(construction);
        targetPlace.addTrain(construction);
        construction.openUI();

        dispose();
    }

    private void removeLast() {
        TrainElement elt = construction.removeLastElement();
        costs.removeUnits(elt.getProperties().buildCost);
        messageElement.setText("Element Removed");
    }

    private void add(TrainElement elt) {
        if (construction.getLength() + elt.getProperties().length > maxSize) {
            messageElement.setText("Train is too long for that element");
            return;

        } else {
            messageElement.setText("Element Added");
        }

        costs.addUnits(elt.getProperties().buildCost);
        construction.addElement(elt);
    }
}
