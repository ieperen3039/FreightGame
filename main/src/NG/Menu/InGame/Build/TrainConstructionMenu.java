package NG.Menu.InGame.Build;

import NG.Core.Game;
import NG.Core.PlayerStatus;
import NG.DataStructures.Valuta;
import NG.Entities.*;
import NG.GUIMenu.Components.*;
import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.Menu.Main.MainMenu;
import NG.Tracks.TrackPiece;
import NG.Tracks.TrackType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Geert van Ieperen created on 21-5-2020.
 */
public class TrainConstructionMenu extends SFrame {
    public static final SComponentProperties PROPERTIES_COMPONENT_PROPERTIES = new SComponentProperties(
            200, 0, false, true, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.LEFT_TOP
    );
    public static final SComponentProperties TRAIN_ELEMENT_BUTTON_PROPS = new SComponentProperties(
            200, 30, false, false, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );

    private static int id = 1;

    private final Game game;
    private final Station targetPlace;
    private final Train construction;
    private final Valuta costs = Valuta.ofUnitValue(0);
    private final float maxSize;
    private final STextArea messageElement;

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

        double gameTime = game.timer().getGameTime();
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
                            new STextArea(
                                    loco + "\n\n" +
                                            "build cost : " + loco.buildCost + "\n" +
                                            "maintenance : " + loco.maintenancePerSecond + "\n" +
                                            "max speed : " + loco.maxSpeed + "\n" +
                                            "tractive effort : " + loco.tractiveEffort + "\n" +
                                            "mass : " + loco.mass + "\n" +
                                            "R1 : " + loco.linearResistance + "\n" +
                                            "R2 : " + loco.quadraticResistance + "\n",
                                    PROPERTIES_COMPONENT_PROPERTIES
                            ),
                            new SButton("Add", () -> add(new Locomotive(loco)), MainMenu.BUTTON_PROPERTIES_STRETCH)
                    );

                    locoButtons.add(new SButton(
                            loco.toString(),
                            () -> trainTabArea.show(trainPanel),
                            TRAIN_ELEMENT_BUTTON_PROPS
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
                            new STextArea(
                                    wagon.name + "\n\n" +
                                            "build cost : " + wagon.buildCost + "\n" +
                                            "maintenance : " + wagon.maintenancePerSecond + "\n" +
                                            "max speed : " + wagon.maxSpeed + "\n" +
                                            "mass : " + wagon.mass + "\n" +
                                            "R1 : " + wagon.linearResistance + "\n",
                                    PROPERTIES_COMPONENT_PROPERTIES
                            ),
                            new SButton("Add", () -> add(new Wagon(wagon)), MainMenu.BUTTON_PROPERTIES_STRETCH)
                    );

                    wagonButtons.add(new SButton(
                            wagon.toString(),
                            () -> wagonTabArea.show(wagonPanel),
                            TRAIN_ELEMENT_BUTTON_PROPS
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
        SComponent trainDisplay = new SActiveTextArea(
                () -> construction.getElements()
                        .stream()
                        .map(e -> e.getClass().getSimpleName())
                        .collect(Collectors.joining("|")),
                MainMenu.BUTTON_PROPERTIES_STRETCH
        );

        messageElement = new STextArea("", MainMenu.TEXT_PROPERTIES);

        setMainPanel(SContainer.column(
                typeSelectionTabs,
                trainDisplay,
                messageElement,
                SContainer.row(
                        new SButton("Build this", this::confirmAndClose, MainMenu.BUTTON_PROPERTIES_STATIC),
                        new SButton("Remove Last", this::removeLast, MainMenu.BUTTON_PROPERTIES_STATIC)
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

        dispose(); // sepukku
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
