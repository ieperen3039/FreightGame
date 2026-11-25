package io.github.ieperen3039.trainsinspace.InputHandling.MouseTool;

import io.github.ieperen3039.ngn.UserInterface.Components.SFrame;
import io.github.ieperen3039.trainsinspace.Core.Coloring;
import io.github.ieperen3039.trainsinspace.Entities.GameEntity;

import java.util.function.BooleanSupplier;

public abstract class MouseToolVisuals extends GameEntity {
    private BooleanSupplier disposedLogic;

    public MouseToolVisuals(SFrame uiElement) {
        this(uiElement::isDisposed);
    }

    public MouseToolVisuals(BooleanSupplier disposedLogic) {
        this.disposedLogic = disposedLogic;
    }

    @Override
    public void setMarking(Coloring.Marking mark) {
    }

    @Override
    public void update() {
    }

    @Override
    public boolean isDespawnedAt(double gameTime) {
        return disposedLogic.getAsBoolean();
    }
}
