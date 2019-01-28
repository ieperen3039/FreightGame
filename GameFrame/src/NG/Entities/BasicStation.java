package NG.Entities;

import NG.ActionHandling.MouseTools.EntityBuildTool;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.Pair;
import NG.DataStructures.Material;
import NG.Engine.Game;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shapes.ShapesGeneric;
import NG.ScreenOverlay.Frames.Components.*;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import NG.Tracks.NetworkNode;
import NG.Tracks.TrackMod;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector2i;
import org.joml.Vector3f;

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
    private TrackMod.TrackType type;

    public BasicStation(Game game, int nrOfPlatforms, int length, TrackMod.TrackType type) {
        super(game, new Vector2f());
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

        Vector2fc forward = new Vector2f(cos(orientation), sin(orientation)).normalize(realLength / 2f);

        if (numberOfPlatforms > 1) {
            Vector2fc sideward = new Vector2f(-sin(orientation), cos(orientation)).normalize(realWidth / 2f);
            Vector2fc sideSkip = new Vector2f(sideward).normalize(PLATFORM_SIZE);

            Vector2f frontPos = new Vector2f(getPosition()).sub(sideward).add(sideSkip.x() / 2, sideSkip.y() / 2);
            Vector2f backPos = new Vector2f(frontPos).sub(forward);
            frontPos.add(forward);

            forwardConnections = new NetworkNode[numberOfPlatforms];
            backwardConnections = new NetworkNode[numberOfPlatforms];

            for (int i = 0; i < numberOfPlatforms; i++) {
                Pair<NetworkNode, NetworkNode> nodePair = NetworkNode.getNodePair(game, type, frontPos, backPos);

                forwardConnections[i] = nodePair.left;
                backwardConnections[i] = nodePair.right;

                frontPos.add(sideSkip);
                backPos.add(sideSkip);
            }

        } else { // simplified version of above
            Vector2f frontPos = new Vector2f(getPosition()).add(forward);
            Vector2f backPos = new Vector2f(getPosition()).sub(forward);

            Pair<NetworkNode, NetworkNode> nodePair = NetworkNode.getNodePair(game, type, frontPos, backPos);

            forwardConnections[0] = nodePair.left;
            backwardConnections[0] = nodePair.right;
        }
    }

    @Override
    public void draw(SGL gl) {
        gl.pushMatrix();
        {
            Vector3f position = game.map().getPosition(getPosition());
            gl.translate(position);
            gl.rotate(Vectors.zVector(), orientation);
            gl.scale(realLength / 2, realWidth / 2, 5);
            gl.getShader().setMaterial(Material.ROUGH, isFixed ? Color4f.GREEN : Color4f.WHITE);
            gl.render(ShapesGeneric.CUBE);
        }
        gl.popMatrix();
    }

    @Override
    public UpdateFrequency getUpdateFrequency() {
        return UpdateFrequency.NEVER;
    }

    public static class Builder extends EntityBuildTool {
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

        public Builder(Game game, SToggleButton source, TrackMod.TrackType trackType) {
            super(game, source);
            this.station = new BasicStation(game, 1, 3, trackType);

            selector = new SizeSelector();
            game.gui().addFrame(selector);
        }

        @Override
        protected void close() {
            super.close();
            selector.dispose();
        }

        @Override
        public void apply(Entity entity, int xSc, int ySc) {
            // do nothing
        }

        @Override
        public void apply(Vector2fc position) {
            station.setPosition(position);
            game.state().addEntity(station);

            relative = new Vector2i();
            isPositioned = true;
        }

        @Override
        public void mouseMoved(int xDelta, int yDelta) {
            super.mouseMoved(xDelta, yDelta);
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
            close();
        }

        private class SizeSelector extends SFrame {
            SizeSelector() {
                super("Station Size");
                SPanel panel = new SPanel(2, 2);

                SDropDown capacityChooser = new SDropDown(game, station.platformCapacity, values);
                SDropDown widthChooser = new SDropDown(game, station.numberOfPlatforms, values);

                Runnable changeListener = () -> station.setSize(capacityChooser.getSelectedIndex(), widthChooser.getSelectedIndex());

                capacityChooser.addStateChangeListener(changeListener);
                widthChooser.addStateChangeListener(changeListener);

                panel.add(new STextArea("Wagons per platform", capacityChooser.minHeight(), true), new Vector2i(0, 0));
                panel.add(capacityChooser, new Vector2i(1, 0));
                panel.add(new STextArea("Number of platforms", widthChooser.minHeight(), true), new Vector2i(0, 1));
                panel.add(widthChooser, new Vector2i(1, 1));

                setMainPanel(panel);
                pack();
            }
        }
    }
}
