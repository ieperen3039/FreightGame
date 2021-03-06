package NG.Menu.InGame.Build;

import NG.Core.Game;
import NG.DataStructures.Generic.Color4f;
import NG.Entities.Entity;
import NG.Entities.Station;
import NG.Entities.StationGhost;
import NG.GUIMenu.Components.*;
import NG.InputHandling.MouseTool.AbstractMouseTool;
import NG.Menu.Main.MainMenu;
import NG.Rendering.Material;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.MaterialShader;
import NG.Rendering.Shapes.GenericShapes;
import NG.Resources.GeneratorResource;
import NG.Resources.Resource;
import NG.Tools.Vectors;
import NG.Tracks.TrackType;
import org.joml.Math;
import org.joml.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static NG.Entities.StationImpl.PLATFORM_SIZE;
import static NG.Settings.Settings.STATION_RANGE;

/**
 * @author Geert van Ieperen created on 30-4-2020.
 */
public class StationBuilder extends AbstractMouseTool {
    private static final float EPSILON = 1 / 128f;
    private static final int RING_RADIAL_PARTS = 64;
    private static final float RING_THICKNESS = 0.1f;

    private static final Map<Float, Resource<Mesh>> meshes = new HashMap<>();

    private static final String[] numbers = new String[12];
    protected final Runnable deactivation;

    static {
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = String.valueOf(i);
        }
    }

    private final StationGhost station;
    private final TrackType trackType;
    private final SizeSelector selector;
    private boolean isPositioned = false;
    private final Vector3f cursorPosition = new Vector3f();
    private boolean cursorIsOnMap = false;

    public StationBuilder(Game game, SToggleButton source, TrackType trackType) {
        super(game);
        this.deactivation = () -> source.setActive(false);
        this.station = new StationGhost(game, 1, 6);
        this.trackType = trackType;

        selector = new SizeSelector();
        game.gui().addFrame(selector);
    }

    @Override
    public void apply(Entity entity, Vector3fc origin, Vector3fc direction) {
        cursorIsOnMap = false;
        // do nothing
    }

    @Override
    public void dispose() {
        selector.dispose();
        deactivation.run();
    }

    public void apply(Vector3fc position, Vector3fc origin, Vector3fc direction) {
        switch (getMouseAction()) {
            case PRESS_ACTIVATE:
                station.setPosition(position);
                cursorPosition.set(position);
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
                break;

            case HOVER:
                if (!isPositioned) {
                    cursorPosition.set(position);
                }
                cursorIsOnMap = true;
        }
    }

    @Override
    public void draw(SGL gl) {
        if (isPositioned) {
            station.draw(gl);
        }

        if (cursorIsOnMap) {
            gl.pushMatrix();
            {
                gl.translate(cursorPosition);

                MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.GREY));
                float diameter = pythagoras(station.getLength(), station.getNumberOfPlatforms() * PLATFORM_SIZE);
                gl.render(getRingMesh(diameter / 2), station);

                MaterialShader.ifPresent(gl, m -> m.setMaterial(Material.ROUGH, Color4f.GREEN));
                gl.render(getRingMesh(STATION_RANGE), station);
            }
            gl.popMatrix();
        }
    }

    private float pythagoras(float a, float b) {
        return Math.sqrt(a * a + b * b);
    }

    protected Mesh getRingMesh(float radius) {
        Resource<Mesh> meshResource = meshes.computeIfAbsent(radius, (r) -> new GeneratorResource<>(
                () -> GenericShapes.createRing((r + RING_THICKNESS), RING_RADIAL_PARTS, RING_THICKNESS), Mesh::dispose
        ));
        return meshResource.get();
    }

    @Override
    public void onRelease(int button) {
        super.onRelease(button);
        if (!isPositioned) return;
        double gameTime = game.timer().getGameTime();

        Station newStation = station.solidify(game, trackType, gameTime);
        game.state().addEntity(newStation);
        game.playerStatus().stations.add(station);

        station.despawn(gameTime);
        game.inputHandling().setMouseTool(null);
    }

    private class SizeSelector extends SFrame {
        // TODO remember previous setting
        SizeSelector() {
            super("Station Size");
            SPanel panel = new SPanel(2, 2);

            SDropDown platformCapacityChooser = new SDropDown(game.gui(), (int) station.getLength(), numbers);
            SDropDown nrOfPlatormChooser = new SDropDown(game.gui(), station.getNumberOfPlatforms(), numbers);

            Consumer<Integer> changeListener = (i) -> setSize(platformCapacityChooser, nrOfPlatormChooser);

            platformCapacityChooser.addStateChangeListener(changeListener);
            nrOfPlatormChooser.addStateChangeListener(changeListener);

            panel.add(new STextArea("Wagons per platform", MainMenu.TEXT_PROPERTIES), new Vector2i(0, 0));
            panel.add(platformCapacityChooser, new Vector2i(1, 0));
            panel.add(new STextArea("Number of platforms", MainMenu.TEXT_PROPERTIES), new Vector2i(0, 1));
            panel.add(nrOfPlatormChooser, new Vector2i(1, 1));

            setMainPanel(panel);
            pack();
        }

        private void setSize(SDropDown platformCapacityChooser, SDropDown nrOfPlatormChooser) {
            station.setSize(
                    nrOfPlatormChooser.getSelectedIndex(), platformCapacityChooser.getSelectedIndex()
            );
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
