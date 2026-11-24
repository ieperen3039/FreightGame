package io.github.ieperen3039.trainsinspace.Menu.EntityScreens;


import io.github.ieperen3039.ngn.UserInterface.Components.SActiveTextComponent;
import io.github.ieperen3039.ngn.UserInterface.Components.SButton;
import io.github.ieperen3039.ngn.UserInterface.Components.SContainer;
import io.github.ieperen3039.ngn.UserInterface.Components.SFrame;
import io.github.ieperen3039.ngn.UserInterface.Rendering.NGFont;
import io.github.ieperen3039.ngn.UserInterface.Rendering.SFrameLookAndFeel;
import io.github.ieperen3039.ngn.UserInterface.SComponentProperties;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Entities.Train;
import io.github.ieperen3039.trainsinspace.Network.Schedule;

public class TrainScreen extends SFrame {
    private static final SComponentProperties BUTTON_PROPERTIES = new SComponentProperties(
            300, 50, false, false, NGFont.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );

    public TrainScreen(Game game, Train target) {
        super(target.toString());
        setMainPanel(SContainer.column(
                new SActiveTextComponent(target::getStatusString, 50),
                new SActiveTextComponent(() -> String.format("Speed: %5.01f", target.getSpeed()), 50),
                new SActiveTextComponent(() -> String.format("Cargo: %s", target.getContents()), 50),
                new SButton("Start", BUTTON_PROPERTIES, target::start),
                new SButton("Stop", BUTTON_PROPERTIES, target::stop),
                new SButton("Reverse", BUTTON_PROPERTIES, target::reverse),
                new SButton("Schedule", BUTTON_PROPERTIES, () -> game.gui()
                        .addFrame(new Schedule.ScheduleUI(game, target.getSchedule())))
        ));
        pack();
    }

}
