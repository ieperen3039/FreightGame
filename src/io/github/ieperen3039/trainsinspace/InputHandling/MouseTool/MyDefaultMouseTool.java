package io.github.ieperen3039.trainsinspace.InputHandling.MouseTool;

import io.github.ieperen3039.ngn.DataStructures.Generic.Color4f;
import io.github.ieperen3039.ngn.InputHandling.KeyControl;
import io.github.ieperen3039.ngn.InputHandling.MouseTools.DefaultMouseTool;
import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTarget;
import io.github.ieperen3039.trainsinspace.Core.Coloring;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Entities.GameEntity;
import io.github.ieperen3039.trainsinspace.Entities.Train;
import io.github.ieperen3039.trainsinspace.InputHandling.InputMapping;
import io.github.ieperen3039.trainsinspace.Menu.EntityScreens.TrainScreen;

public class MyDefaultMouseTool extends DefaultMouseTool {
    private final Game game;
    private Coloring.Marking hoverMark = new Coloring.Marking();

    public MyDefaultMouseTool(Game game) {
        super(game);
        this.game = game;
    }

    @Override
    public HandleResult onHover(MouseTarget target, int xDelta, int yDelta, float xSc, float ySc) {
        if (target instanceof MouseTargetEntity entity) {
            hoverMark.invalidate();
            hoverMark = new Coloring.Marking(Color4f.GREY, Coloring.Priority.MOUSE_HOVER);
            entity.target().setMarking(hoverMark);
            return HandleResult.STOP;
        }

        return super.onHover(target, xDelta, yDelta, xSc, ySc);
    }

    @Override
    public HandleResult onPress(int button, MouseTarget target, int xSc, int ySc) {
        if (target instanceof MouseTargetEntity entity) {
            onEntityClick(entity.target(), button);
        }
        return super.onPress(button, target, xSc, ySc);
    }

    private void onEntityClick(GameEntity entity, int button) {
        KeyControl keys = game.keyControl();

        if (entity instanceof Train train) {
            if (button == InputMapping.UseMouseTool.getButton()) {
                if (keys.isControlPressed()) {
                    train.start();
                    if (train.isStopping()) {
                        train.start();
                    } else {
                        train.stop();
                    }
                } else {
                    game.gui().addFrame(new TrainScreen(game, train));
                }
            }
        }
    }
}
