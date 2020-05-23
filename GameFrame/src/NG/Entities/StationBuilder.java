package NG.Entities;

import NG.Core.Game;
import NG.GUIMenu.Components.*;
import NG.InputHandling.MouseTools.ToggleMouseTool;
import NG.Tools.Vectors;
import NG.Tracks.TrackType;
import org.joml.*;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * @author Geert van Ieperen created on 30-4-2020.
 */
public class StationBuilder extends ToggleMouseTool {
    private static final float EPSILON = 1 / 128f;
    private static final String[] numbers = new String[12];

    static {
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = String.valueOf(i);
        }
    }

    private final StationGhost station;
    private final TrackType trackType;
    private final SizeSelector selector;
    private boolean isPositioned = false;

    public StationBuilder(Game game, SToggleButton source, TrackType trackType) {
        super(game, () -> source.setActive(false));
        this.station = new StationGhost(game, 1, 6);
        this.trackType = trackType;

        selector = new SizeSelector();
        game.gui().addFrame(selector);
    }

    @Override
    public void onClick(int button, int x, int y) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            dispose();

        } else {
            super.onClick(button, x, y);
        }
    }

    @Override
    public void apply(Entity entity, Vector3fc origin, Vector3fc direction) {
        // do nothing
    }

    @Override
    public void dispose() {
        selector.dispose();
        super.dispose();
    }

    public void apply(Vector3fc position, Vector3fc origin, Vector3fc direction) {
        switch (getMouseAction()) {
            case PRESS_ACTIVATE:
                station.setPosition(position);
                game.state().addEntity(station);

                isPositioned = true;
                break;

            case DRAG_ACTIVATE:
                if (!isPositioned) return;
                Vector3fc stationPos = station.getPosition();

                // Planef plane = new Planef(station.getPosition(), Vectors.Z);
                // float f = Intersectionf.intersectRayPlane(ray, plane, EPSILON);
                float f = Intersectionf.intersectRayPlane(
                        origin.x(), origin.y(), origin.z(), direction.x(), direction.y(), direction.z(),
                        0, 0, 1, -stationPos.z(), EPSILON
                );
                Vector3f point = new Vector3f(direction).mul(f).add(origin);

                Vector2f direction2D = new Vector2f(point.x - stationPos.x(), point.y - stationPos.y());
                station.setOrientation(Vectors.arcTan(direction2D));
        }
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        super.onRelease(button, xSc, ySc);
        if (!isPositioned) return;
        double gameTime = game.timer().getGameTime();

        Station newStation = station.solidify(game, trackType, gameTime);
        game.state().addEntity(newStation);

        station.despawn(gameTime);
        this.dispose();
    }

    private class SizeSelector extends SFrame {
        // TODO remember previous setting
        SizeSelector() {
            super("Station Size");
            SPanel panel = new SPanel(2, 2);

            SDropDown platformCapacityChooser = new SDropDown(game.gui(), (int) station.getLength(), numbers);
            SDropDown nrOfPlatormChooser = new SDropDown(game.gui(), station.getNumberOfPlatforms(), numbers);

            Consumer<Integer> changeListener = (i) -> station.setSize(
                    nrOfPlatormChooser.getSelectedIndex(), platformCapacityChooser.getSelectedIndex()
            );

            platformCapacityChooser.addStateChangeListener(changeListener);
            nrOfPlatormChooser.addStateChangeListener(changeListener);

            panel.add(new STextArea("Wagons per platform", platformCapacityChooser.minHeight()), new Vector2i(0, 0));
            panel.add(platformCapacityChooser, new Vector2i(1, 0));
            panel.add(new STextArea("Number of platforms", nrOfPlatormChooser.minHeight()), new Vector2i(0, 1));
            panel.add(nrOfPlatormChooser, new Vector2i(1, 1));

            setMainPanel(panel);
            pack();
        }

        @Override
        public void dispose() {
            if (!isDisposed()) {
                super.dispose();
                StationBuilder.this.dispose();
            }
        }
    }
}
