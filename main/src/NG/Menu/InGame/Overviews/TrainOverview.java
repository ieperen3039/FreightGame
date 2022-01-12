package NG.Menu.InGame.Overviews;

import NG.Core.Game;
import NG.Core.PlayerStatus;
import NG.Entities.Train;
import NG.Entities.TrainElement;
import NG.GUIMenu.Components.*;
import NG.Menu.Main.MainMenu;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Geert van Ieperen created on 11-10-2020.
 */
public class TrainOverview extends SFrame {
    public TrainOverview(Game game) {
        super("Trains");

        PlayerStatus playerStatus = game.playerStatus();
        List<Train> trains = playerStatus.trains;

        setMainPanel(SContainer.row(
                new SComponentArea(200, 500),
                new SScrollableList(10, trains.stream()
                        .map(TrainView::new)
                        .collect(Collectors.toList())
                )
        ));
    }

    private static class TrainView extends SPanel {
        public TrainView(Train train) {
            super(SContainer.column(
                    new SExtendedTextArea(train.toString(), MainMenu.TEXT_PROPERTIES)
                            .setClickListener((button, xRel, yRel) -> {
                                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                                    train.openUI();
                                }
                            }),
                    new STextArea(
                            train.getElements().stream()
                                    .map(TrainElement::getProperties)
                                    .map(TrainElement.Properties::toString)
                                    .collect(Collectors.joining(" | ")),
                            MainMenu.TEXT_PROPERTIES
                    ),
                    new SActiveTextArea(() -> train.getContents().toString(), MainMenu.TEXT_PROPERTIES)
            ));
        }
    }
}
