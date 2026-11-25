package io.github.ieperen3039.trainsinspace.Menu.InGame.Build;

import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTarget;
import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTool;
import io.github.ieperen3039.ngn.Rendering.MatrixStack.SGL;
import io.github.ieperen3039.ngn.Tools.TickTime;
import io.github.ieperen3039.ngn.UserInterface.Components.SFrame;
import io.github.ieperen3039.ngn.UserInterface.Components.SToggleButton;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.InputHandling.InputMapping;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.MouseTargetEntity;
import io.github.ieperen3039.trainsinspace.InputHandling.MouseTool.MouseToolVisuals;
import io.github.ieperen3039.trainsinspace.Network.RailNode;
import io.github.ieperen3039.trainsinspace.Network.SignalEntity;
import io.github.ieperen3039.trainsinspace.Tracks.RailTools;
import io.github.ieperen3039.trainsinspace.Tracks.TrackPiece;
import org.joml.Vector3f;
import org.joml.primitives.Rayf;

/**
 * @author Geert van Ieperen created on 24-5-2020.
 */
public class SignalBuilder implements MouseTool {
    private final Game game;
    protected final Runnable deactivation;
    private SignalEntity ghostSignal;
    private SFrame selector;

    public SignalBuilder(Game game, SToggleButton source) {
        this.game = game;
        this.deactivation = () -> source.setActive(false);
    }

    @Override
    public HandleResult onPress(int button, MouseTarget target, int xSc, int ySc) {
        if (button == InputMapping.DeactivateMouseTool.getButton()) {
            return HandleResult.DEACTIVATE;
        }
        if (button == InputMapping.UseMouseTool.getButton()) {
            if (target instanceof MouseTargetEntity entity) {
                if (entity.target() instanceof TrackPiece trackPiece) {
                    buildSignal(trackPiece, xSc, ySc);
                    return HandleResult.CAPTURE;

                } else {
                    ghostSignal = null;
                }
            }
        }

        return HandleResult.CONTINUE;
    }

    private void buildSignal(TrackPiece trackPiece, int xSc, int ySc) {
        assert trackPiece.isValid();

        Rayf rayf = game.camera().unproject(xSc, ySc, game.getViewPort());
        Vector3f origin = new Vector3f(rayf.oX, rayf.oY, rayf.oZ);
        Vector3f direction = new Vector3f(rayf.dX, rayf.dY, rayf.dZ);

        float fraction = TrackBuilder.getFraction(trackPiece, origin, direction);
        RailNode targetNode = TrackBuilder.getIfExisting(game, trackPiece, fraction);

        if (targetNode == null) {
            double gameTime = game.timer().getTime();
            targetNode = RailTools.createSplit(game, trackPiece, fraction, gameTime);
        }

        targetNode.addSignal(game, true);
    }

    @Override
    public HandleResult onHover(MouseTarget target, int xDelta, int yDelta, float xSc, float ySc) {
        if (target instanceof MouseTargetEntity entity) {
            if (entity.target() instanceof TrackPiece trackPiece) {
                Rayf rayf = game.camera().unproject((int) xSc, (int) ySc, game.getViewPort());
                Vector3f origin = new Vector3f(rayf.oX, rayf.oY, rayf.oZ);
                Vector3f direction = new Vector3f(rayf.dX, rayf.dY, rayf.dZ);

                float fraction = TrackBuilder.getFraction(trackPiece, origin, direction);
                Vector3f closestPoint = trackPiece.getPositionFromFraction(fraction);
                RailNode ghostNodeTarget = TrackBuilder.getIfExisting(game, trackPiece, fraction);

                if (ghostNodeTarget == null) {
                    Vector3f dir = trackPiece.getDirectionFromFraction(fraction);
                    ghostNodeTarget = new RailNode(game, closestPoint, trackPiece.getType(), dir);

                } else {
                    // make it a ghost type
                    ghostNodeTarget = new RailNode(ghostNodeTarget, trackPiece.getType());
                }

                if (ghostSignal == null || ghostSignal.getSignal().getNode() != ghostNodeTarget) {
                    ghostSignal = new SignalEntity(game, ghostNodeTarget, true, true);
                }

                return HandleResult.STOP;

            } else {
                ghostSignal = null;
            }
        }

        return HandleResult.CONTINUE;
    }

    @Override
    public void onActivate() {
        selector = new SFrame("Signal builder");
        game.state().addEntity(new MouseToolVisuals(selector) {
            @Override
            public void draw(SGL gl, TickTime renderTime) {
                if (ghostSignal != null) {
                    ghostSignal.draw(gl, renderTime);
                }
            }
        });
    }

    @Override
    public void onDeactivate() {
        deactivation.run();
        selector.dispose();
    }
}
