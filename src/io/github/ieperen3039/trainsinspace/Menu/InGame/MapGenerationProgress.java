package io.github.ieperen3039.trainsinspace.Menu.InGame;

import io.github.ieperen3039.ngn.UserInterface.Components.SFiller;
import io.github.ieperen3039.ngn.UserInterface.Components.SFrame;
import io.github.ieperen3039.ngn.UserInterface.Components.SNineWayPanel;
import io.github.ieperen3039.ngn.UserInterface.Components.SProgressBar;

import java.util.function.Supplier;

public class MapGenerationProgress extends SFrame {
    private final Supplier<Float> progressSupplier;

    public MapGenerationProgress(Supplier<Float> progressSupplier) {
        super("Generating map...", 500, 200, false);
        this.progressSupplier = progressSupplier;
        SNineWayPanel panel = new SNineWayPanel();
        panel.add(new SFiller(), SNineWayPanel.NORTHEAST);
        panel.add(new SProgressBar(400, 50, progressSupplier), SNineWayPanel.MIDDLE);
        panel.add(new SFiller(), SNineWayPanel.SOUTHWEST);
        setMainPanel(panel);
        setVisible(true);
    }

    @Override
    public boolean isDisposed() {
        return super.isDisposed() || (progressSupplier.get() >= 1.0);
    }
}
