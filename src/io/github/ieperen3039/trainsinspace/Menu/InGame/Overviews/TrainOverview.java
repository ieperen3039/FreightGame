package io.github.ieperen3039.trainsinspace.Menu.InGame.Overviews;

import io.github.ieperen3039.ngn.UserInterface.Components.*;
import io.github.ieperen3039.ngn.UserInterface.LayoutManagers.SingleElementLayout;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Core.PlayerStatus;
import io.github.ieperen3039.trainsinspace.Entities.Train;
import io.github.ieperen3039.trainsinspace.Entities.TrainElement;
import io.github.ieperen3039.trainsinspace.Menu.Main.MainMenu;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Geert van Ieperen created on 11-10-2020.
 */
public class TrainOverview extends SPanel {
    public TrainOverview(Game game) {
        super(new SingleElementLayout());

        PlayerStatus playerStatus = game.playerStatus();
        List<Train> trains = playerStatus.trains;

        add(SPanel.row(
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
                    new SExtendedTextComponent(train.toString(), MainMenu.TEXT_PROPERTIES)
                            .setClickListener((button, xRel, yRel) -> {
                                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                                    train.openUI();
                                }
                            }),
                    new STextComponent(
                            train.getElements().stream()
                                    .map(TrainElement::getProperties)
                                    .map(TrainElement.Properties::toString)
                                    .collect(Collectors.joining(" | ")),
                            MainMenu.TEXT_PROPERTIES
                    ),
                    new SActiveTextComponent(() -> train.getContents().toString(), MainMenu.TEXT_PROPERTIES)
            ));
        }
    }
}
