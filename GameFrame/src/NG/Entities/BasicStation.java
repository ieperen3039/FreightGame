package NG.Entities;

import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.GUIMenu.Components.*;
import NG.InputHandling.MouseTools.SurfaceBuildTool;
import NG.Network.NetworkNode;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Rendering.Shapes.GenericShapes;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import NG.Tracks.TrackPiece;
import NG.Tracks.TrackType;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

import static NG.Tools.Vectors.cos;
import static NG.Tools.Vectors.sin;

/**
 * A basic implementation of a station. There is likely no need for another station
 * @author Geert van Ieperen created on 27-1-2019.
 */
public class BasicStation extends Station {

    private static final float PLATFORM_SIZE = 2;
    private static final float WAGON_LENGTH = 3;
    private int numberOfPlatforms;
    private int platformCapacity;
    private float realLength;
    private float realWidth;

    private NetworkNode[] forwardConnections;
    private NetworkNode[] backwardConnections;
    private TrackType type;

    public BasicStation(Game game, int nrOfPlatforms, int length, TrackType type) {
        super(game, new Vector3f());
        assert nrOfPlatforms > 0 : "created station with " + nrOfPlatforms + " platforms";
        this.type = type;

        setSize(nrOfPlatforms, length);
    }

    public void setSize(int nrOfPlatforms, int length) {
        if (isFixed) {
            Logger.ERROR.print("Tried changing size of a fixed station");
            return;
        }

        this.numberOfPlatforms = nrOfPlatforms;
        this.platformCapacity = length;
        this.realLength = length * WAGON_LENGTH;
        this.realWidth = nrOfPlatforms * PLATFORM_SIZE;
    }

    @Override
    public void update() {

    }

    @Override
    public void fixPosition() {
        assert numberOfPlatforms > 0 : "created station with " + numberOfPlatforms + " platforms";
        super.fixPosition();

        Vector3fc forward = new Vector3f(cos(orientation), sin(orientation), 0).normalize(realLength / 2f);

        forwardConnections = new NetworkNode[numberOfPlatforms];
        backwardConnections = new NetworkNode[numberOfPlatforms];

        if (numberOfPlatforms > 1) {
            Vector3fc toRight = new Vector3f(-sin(orientation), cos(orientation), 0).normalize(realWidth / 2f);
            Vector3fc rightSkip = new Vector3f(toRight).normalize(PLATFORM_SIZE);

            Vector3f frontPos = new Vector3f(getPosition()).sub(toRight).add(rightSkip.x() / 2, rightSkip.y() / 2, 0);
            Vector3f backPos = new Vector3f(frontPos).sub(forward);
            frontPos.add(forward);

            for (int i = 0; i < numberOfPlatforms; i++) {
                TrackPiece trackConnection = NetworkNode.createNewTrack(game, type, frontPos, backPos);

                forwardConnections[i] = trackConnection.getStartNode();
                backwardConnections[i] = trackConnection.getEndNode();

                frontPos.add(rightSkip);
                backPos.add(rightSkip);
            }

        } else { // simplified version of above
            Vector3f frontPos = new Vector3f(getPosition()).add(forward);
            Vector3f backPos = new Vector3f(getPosition()).sub(forward);

            TrackPiece trackConnection = NetworkNode.createNewTrack(game, type, frontPos, backPos);

            forwardConnections[0] = trackConnection.getStartNode();
            backwardConnections[0] = trackConnection.getEndNode();
        }
    }

    @Override
    public void draw(SGL gl) {
        gl.pushMatrix();
        {
            gl.translate(getPosition());
            gl.rotate(Vectors.Z, orientation);
            gl.scale(realLength / 2, realWidth / 2, 0.1f);

            ShaderProgram shader = gl.getShader();
            if (shader instanceof MaterialShader) {
                MaterialShader matShader = (MaterialShader) shader;
                matShader.setMaterial(Material.ROUGH, isFixed ? Color4f.GREEN : Color4f.WHITE);
            }

            gl.render(GenericShapes.CUBE, this);
        }
        gl.popMatrix();
    }

    @Override
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }

    public static class Builder extends SurfaceBuildTool {
        private static final String[] values = new String[12];

        static {
            for (int i = 0; i < values.length; i++) {
                values[i] = String.valueOf(i);
            }
        }

        // TODO remember previous setting
        private BasicStation station;
        private Vector2i relative = new Vector2i();
        private boolean isPositioned = false;
        private SizeSelector selector;

        public Builder(Game game, SToggleButton source, TrackType trackType) {
            super(game, () -> source.setActive(false));
            this.station = new BasicStation(game, 1, 3, trackType);

            selector = new SizeSelector();
            game.gui().addFrame(selector);
        }

        @Override
        public void apply(Entity entity, int xSc, int ySc) {
            // do nothing
        }

        @Override
        public void dispose() {
            super.dispose();
            selector.dispose();
        }

        public void apply(Vector3fc position, int xSc, int ySc) {
            station.setPosition(position);
            game.state().addEntity(station);

            relative = new Vector2i();
            isPositioned = true;
        }

        @Override
        public void mouseMoved(int xDelta, int yDelta, float xPos, float yPos) {
            super.mouseMoved(xDelta, yDelta, xPos, yPos);
            if (!isPositioned) return;

            relative.add(xDelta, yDelta);

            Vector2f direction = new Vector2f(relative).normalize();
            station.setOrientation(-Vectors.arcTan(direction));
        }

        @Override
        public void onRelease(int button, int xSc, int ySc) {
            super.onRelease(button, xSc, ySc);
            if (!isPositioned) return;
            station.fixPosition();
            dispose();
        }

        private class SizeSelector extends SFrame {
            SizeSelector() {
                super("Station Size");
                SPanel panel = new SPanel(2, 2);

                SDropDown capacityChooser = new SDropDown(game.gui(), station.platformCapacity, values);
                SDropDown widthChooser = new SDropDown(game.gui(), station.numberOfPlatforms, values);

                Consumer<Integer> changeListener = (i) -> station.setSize(capacityChooser.getSelectedIndex(), widthChooser
                        .getSelectedIndex());

                capacityChooser.addStateChangeListener(changeListener);
                widthChooser.addStateChangeListener(changeListener);

                panel.add(new STextArea("Wagons per platform", capacityChooser.minHeight()), new Vector2i(0, 0));
                panel.add(capacityChooser, new Vector2i(1, 0));
                panel.add(new STextArea("Number of platforms", widthChooser.minHeight()), new Vector2i(0, 1));
                panel.add(widthChooser, new Vector2i(1, 1));

                setMainPanel(panel);
                pack();
            }
        }
    }
}
