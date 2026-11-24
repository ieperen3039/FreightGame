package io.github.ieperen3039.trainsinspace.InputHandling.MouseTool;

import io.github.ieperen3039.ngn.UserInterface.Components.SFrame;
import io.github.ieperen3039.trainsinspace.Core.Coloring;
import io.github.ieperen3039.trainsinspace.Entities.GameEntity;

public abstract class MouseToolVisuals extends GameEntity {
    private SFrame uiElement;

    public MouseToolVisuals(SFrame uiElement) {
        assert uiElement != null;
        this.uiElement = uiElement;
    }

    @Override
    public void setMarking(Coloring.Marking mark) {
    }

    @Override
    public void despawn(double gameTime) {
        uiElement.dispose();
    }

    @Override
    public void update() {
    }

    @Override
    public boolean isDespawnedAt(double gameTime) {
        return uiElement.isDisposed();
    }
}
